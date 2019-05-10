/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package vendor.google.common.util.concurrent;

import static vendor.google.common.base.Preconditions.checkArgument;
import static vendor.google.common.base.Preconditions.checkNotNull;
import static vendor.google.common.base.Preconditions.checkState;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.google.j2objc.annotations.WeakOuter;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.Nullable;
import vendor.google.common.annotations.Beta;
import vendor.google.common.annotations.GwtIncompatible;

/**
 * Base class for implementing services that can handle {@link #doStart} and {@link #doStop}
 * requests, responding to them with {@link #notifyStarted()} and {@link #notifyStopped()}
 * callbacks. Its subclasses must manage threads manually; consider {@link
 * AbstractExecutionThreadService} if you need only a single execution thread.
 *
 * @author Jesse Wilson
 * @author Luke Sandberg
 * @since 1.0
 */
@Beta
@GwtIncompatible
public abstract class AbstractService implements Service {
  private static final ListenerCallQueue.Event<Listener> STARTING_EVENT =
      new ListenerCallQueue.Event<Listener>() {
        @Override
        public void call(Listener listener) {
          listener.starting();
        }

        @Override
        public String toString() {
          return "starting()";
        }
      };
  private static final ListenerCallQueue.Event<Listener> RUNNING_EVENT =
      new ListenerCallQueue.Event<Listener>() {
        @Override
        public void call(Listener listener) {
          listener.running();
        }

        @Override
        public String toString() {
          return "running()";
        }
      };
  private static final ListenerCallQueue.Event<Listener> STOPPING_FROM_STARTING_EVENT =
      stoppingEvent(State.STARTING);
  private static final ListenerCallQueue.Event<Listener> STOPPING_FROM_RUNNING_EVENT =
      stoppingEvent(State.RUNNING);

  private static final ListenerCallQueue.Event<Listener> TERMINATED_FROM_NEW_EVENT =
      terminatedEvent(State.NEW);
  private static final ListenerCallQueue.Event<Listener> TERMINATED_FROM_STARTING_EVENT =
      terminatedEvent(State.STARTING);
  private static final ListenerCallQueue.Event<Listener> TERMINATED_FROM_RUNNING_EVENT =
      terminatedEvent(State.RUNNING);
  private static final ListenerCallQueue.Event<Listener> TERMINATED_FROM_STOPPING_EVENT =
      terminatedEvent(State.STOPPING);

  private static ListenerCallQueue.Event<Listener> terminatedEvent(final State from) {
    return new ListenerCallQueue.Event<Listener>() {
      @Override
      public void call(Listener listener) {
        listener.terminated(from);
      }

      @Override
      public String toString() {
        return "terminated({from = " + from + "})";
      }
    };
  }

  private static ListenerCallQueue.Event<Listener> stoppingEvent(final State from) {
    return new ListenerCallQueue.Event<Listener>() {
      @Override
      public void call(Listener listener) {
        listener.stopping(from);
      }

      @Override
      public String toString() {
        return "stopping({from = " + from + "})";
      }
    };
  }

  private final Monitor monitor = new Monitor();

  private final Monitor.Guard isStartable = new IsStartableGuard();

  @WeakOuter
  private final class IsStartableGuard extends Monitor.Guard {
    IsStartableGuard() {
      super(AbstractService.this.monitor);
    }

    @Override
    public boolean isSatisfied() {
      return state() == State.NEW;
    }
  }

  private final Monitor.Guard isStoppable = new IsStoppableGuard();

  @WeakOuter
  private final class IsStoppableGuard extends Monitor.Guard {
    IsStoppableGuard() {
      super(AbstractService.this.monitor);
    }

    @Override
    public boolean isSatisfied() {
      return state().compareTo(State.RUNNING) <= 0;
    }
  }

  private final Monitor.Guard hasReachedRunning = new HasReachedRunningGuard();

  @WeakOuter
  private final class HasReachedRunningGuard extends Monitor.Guard {
    HasReachedRunningGuard() {
      super(AbstractService.this.monitor);
    }

    @Override
    public boolean isSatisfied() {
      return state().compareTo(State.RUNNING) >= 0;
    }
  }

  private final Monitor.Guard isStopped = new IsStoppedGuard();

  @WeakOuter
  private final class IsStoppedGuard extends Monitor.Guard {
    IsStoppedGuard() {
      super(AbstractService.this.monitor);
    }

    @Override
    public boolean isSatisfied() {
      return state().isTerminal();
    }
  }

  /** The listeners to notify during a state transition. */
  private final ListenerCallQueue<Listener> listeners = new ListenerCallQueue<>();

  /**
   * The current state of the service. This should be written with the lock held but can be read
   * without it because it is an immutable object in a volatile field. This is desirable so that
   * methods like {@link #state}, {@link #failureCause} and notably {@link #toString} can be run
   * without grabbing the lock.
   *
   * <p>To update this field correctly the lock must be held to guarantee that the state is
   * consistent.
   */
  private volatile StateSnapshot snapshot = new StateSnapshot(State.NEW);

  /** Constructor for use by subclasses. */
  protected AbstractService() {}

  /**
   * This method is called by {@link #startAsync} to initiate service startup. The invocation of
   * this method should cause a call to {@link #notifyStarted()}, either during this method's run,
   * or after it has returned. If startup fails, the invocation should cause a call to {@link
   * #notifyFailed(Throwable)} instead.
   *
   * <p>This method should return promptly; prefer to do work on a different thread where it is
   * convenient. It is invoked exactly once on service startup, even when {@link #startAsync} is
   * called multiple times.
   */
  @ForOverride
  protected abstract void doStart();

  /**
   * This method should be used to initiate service shutdown. The invocation of this method should
   * cause a call to {@link #notifyStopped()}, either during this method's run, or after it has
   * returned. If shutdown fails, the invocation should cause a call to {@link
   * #notifyFailed(Throwable)} instead.
   *
   * <p>This method should return promptly; prefer to do work on a different thread where it is
   * convenient. It is invoked exactly once on service shutdown, even when {@link #stopAsync} is
   * called multiple times.
   *
   * <p>If {@link #stopAsync} is called on a {@link State#STARTING} service, this method is not
   * invoked immediately. Instead, it will be deferred until after the service is {@link
   * State#RUNNING}. Services that need to cancel startup work can override {#link #doCancelStart}.
   */
  @ForOverride
  protected abstract void doStop();

  /**
   * This method is called by {@link #stopAsync} when the service is still starting (i.e. {@link
   * #startAsync} has been called but {@link #notifyStarted} has not). Subclasses can override the
   * method to cancel pending work and then call {@link #notifyStopped} to stop the service.
   *
   * <p>This method should return promptly; prefer to do work on a different thread where it is
   * convenient. It is invoked exactly once on service shutdown, even when {@link #stopAsync} is
   * called multiple times.
   *
   * <p>When this method is called {@link #state()} will return {@link State#STOPPING}, which is the
   * external state observable by the caller of {@link #stopAsync}.
   *
   * @since 27.0
   */
  @ForOverride
  protected void doCancelStart() {}

  @CanIgnoreReturnValue
  @Override
  public final Service startAsync() {
    if (monitor.enterIf(isStartable)) {
      try {
        snapshot = new StateSnapshot(State.STARTING);
        enqueueStartingEvent();
        doStart();
      } catch (Throwable startupFailure) {
        notifyFailed(startupFailure);
      } finally {
        monitor.leave();
        dispatchListenerEvents();
      }
    } else {
      throw new IllegalStateException("Service " + this + " has already been started");
    }
    return this;
  }

  @CanIgnoreReturnValue
  @Override
  public final Service stopAsync() {
    if (monitor.enterIf(isStoppable)) {
      try {
        State previous = state();
        switch (previous) {
          case NEW:
            snapshot = new StateSnapshot(State.TERMINATED);
            enqueueTerminatedEvent(State.NEW);
            break;
          case STARTING:
            snapshot = new StateSnapshot(State.STARTING, true, null);
            enqueueStoppingEvent(State.STARTING);
            doCancelStart();
            break;
          case RUNNING:
            snapshot = new StateSnapshot(State.STOPPING);
            enqueueStoppingEvent(State.RUNNING);
            doStop();
            break;
          case STOPPING:
          case TERMINATED:
          case FAILED:
            // These cases are impossible due to the if statement above.
            throw new AssertionError("isStoppable is incorrectly implemented, saw: " + previous);
        }
      } catch (Throwable shutdownFailure) {
        notifyFailed(shutdownFailure);
      } finally {
        monitor.leave();
        dispatchListenerEvents();
      }
    }
    return this;
  }

  @Override
  public final void awaitRunning() {
    monitor.enterWhenUninterruptibly(hasReachedRunning);
    try {
      checkCurrentState(State.RUNNING);
    } finally {
      monitor.leave();
    }
  }

  @Override
  public final void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
    if (monitor.enterWhenUninterruptibly(hasReachedRunning, timeout, unit)) {
      try {
        checkCurrentState(State.RUNNING);
      } finally {
        monitor.leave();
      }
    } else {
      // It is possible due to races the we are currently in the expected state even though we
      // timed out. e.g. if we weren't event able to grab the lock within the timeout we would never
      // even check the guard. I don't think we care too much about this use case but it could lead
      // to a confusing error message.
      throw new TimeoutException("Timed out waiting for " + this + " to reach the RUNNING state.");
    }
  }

  @Override
  public final void awaitTerminated() {
    monitor.enterWhenUninterruptibly(isStopped);
    try {
      checkCurrentState(State.TERMINATED);
    } finally {
      monitor.leave();
    }
  }

  @Override
  public final void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
    if (monitor.enterWhenUninterruptibly(isStopped, timeout, unit)) {
      try {
        checkCurrentState(State.TERMINATED);
      } finally {
        monitor.leave();
      }
    } else {
      // It is possible due to races the we are currently in the expected state even though we
      // timed out. e.g. if we weren't event able to grab the lock within the timeout we would never
      // even check the guard. I don't think we care too much about this use case but it could lead
      // to a confusing error message.
      throw new TimeoutException(
          "Timed out waiting for "
              + this
              + " to reach a terminal state. "
              + "Current state: "
              + state());
    }
  }

  /** Checks that the current state is equal to the expected state. */
  @GuardedBy("monitor")
  private void checkCurrentState(State expected) {
    State actual = state();
    if (actual != expected) {
      if (actual == State.FAILED) {
        // Handle this specially so that we can include the failureCause, if there is one.
        throw new IllegalStateException(
            "Expected the service " + this + " to be " + expected + ", but the service has FAILED",
            failureCause());
      }
      throw new IllegalStateException(
          "Expected the service " + this + " to be " + expected + ", but was " + actual);
    }
  }

  /**
   * Implementing classes should invoke this method once their service has started. It will cause
   * the service to transition from {@link State#STARTING} to {@link State#RUNNING}.
   *
   * @throws IllegalStateException if the service is not {@link State#STARTING}.
   */
  protected final void notifyStarted() {
    monitor.enter();
    try {
      // We have to examine the internal state of the snapshot here to properly handle the stop
      // while starting case.
      if (snapshot.state != State.STARTING) {
        IllegalStateException failure =
            new IllegalStateException(
                "Cannot notifyStarted() when the service is " + snapshot.state);
        notifyFailed(failure);
        throw failure;
      }

      if (snapshot.shutdownWhenStartupFinishes) {
        snapshot = new StateSnapshot(State.STOPPING);
        // We don't call listeners here because we already did that when we set the
        // shutdownWhenStartupFinishes flag.
        doStop();
      } else {
        snapshot = new StateSnapshot(State.RUNNING);
        enqueueRunningEvent();
      }
    } finally {
      monitor.leave();
      dispatchListenerEvents();
    }
  }

  /**
   * Implementing classes should invoke this method once their service has stopped. It will cause
   * the service to transition from {@link State#STARTING} or {@link State#STOPPING} to {@link
   * State#TERMINATED}.
   *
   * @throws IllegalStateException if the service is not one of {@link State#STOPPING}, {@link
   *     State#STARTING}, or {@link State#RUNNING}.
   */
  protected final void notifyStopped() {
    monitor.enter();
    try {
      State previous = state();
      switch (previous) {
        case NEW:
        case TERMINATED:
        case FAILED:
          throw new IllegalStateException("Cannot notifyStopped() when the service is " + previous);
        case RUNNING:
        case STARTING:
        case STOPPING:
          snapshot = new StateSnapshot(State.TERMINATED);
          enqueueTerminatedEvent(previous);
          break;
      }
    } finally {
      monitor.leave();
      dispatchListenerEvents();
    }
  }

  /**
   * Invoke this method to transition the service to the {@link State#FAILED}. The service will
   * <b>not be stopped</b> if it is running. Invoke this method when a service has failed critically
   * or otherwise cannot be started nor stopped.
   */
  protected final void notifyFailed(Throwable cause) {
    checkNotNull(cause);

    monitor.enter();
    try {
      State previous = state();
      switch (previous) {
        case NEW:
        case TERMINATED:
          throw new IllegalStateException("Failed while in state:" + previous, cause);
        case RUNNING:
        case STARTING:
        case STOPPING:
          snapshot = new StateSnapshot(State.FAILED, false, cause);
          enqueueFailedEvent(previous, cause);
          break;
        case FAILED:
          // Do nothing
          break;
      }
    } finally {
      monitor.leave();
      dispatchListenerEvents();
    }
  }

  @Override
  public final boolean isRunning() {
    return state() == State.RUNNING;
  }

  @Override
  public final State state() {
    return snapshot.externalState();
  }

  /** @since 14.0 */
  @Override
  public final Throwable failureCause() {
    return snapshot.failureCause();
  }

  /** @since 13.0 */
  @Override
  public final void addListener(Listener listener, Executor executor) {
    listeners.addListener(listener, executor);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [" + state() + "]";
  }

  /**
   * Attempts to execute all the listeners in {@link #listeners} while not holding the {@link
   * #monitor}.
   */
  private void dispatchListenerEvents() {
    if (!monitor.isOccupiedByCurrentThread()) {
      listeners.dispatch();
    }
  }

  private void enqueueStartingEvent() {
    listeners.enqueue(STARTING_EVENT);
  }

  private void enqueueRunningEvent() {
    listeners.enqueue(RUNNING_EVENT);
  }

  private void enqueueStoppingEvent(final State from) {
    if (from == State.STARTING) {
      listeners.enqueue(STOPPING_FROM_STARTING_EVENT);
    } else if (from == State.RUNNING) {
      listeners.enqueue(STOPPING_FROM_RUNNING_EVENT);
    } else {
      throw new AssertionError();
    }
  }

  private void enqueueTerminatedEvent(final State from) {
    switch (from) {
      case NEW:
        listeners.enqueue(TERMINATED_FROM_NEW_EVENT);
        break;
      case STARTING:
        listeners.enqueue(TERMINATED_FROM_STARTING_EVENT);
        break;
      case RUNNING:
        listeners.enqueue(TERMINATED_FROM_RUNNING_EVENT);
        break;
      case STOPPING:
        listeners.enqueue(TERMINATED_FROM_STOPPING_EVENT);
        break;
      case TERMINATED:
      case FAILED:
        throw new AssertionError();
    }
  }

  private void enqueueFailedEvent(final State from, final Throwable cause) {
    // can't memoize this one due to the exception
    listeners.enqueue(
        new ListenerCallQueue.Event<Listener>() {
          @Override
          public void call(Listener listener) {
            listener.failed(from, cause);
          }

          @Override
          public String toString() {
            return "failed({from = " + from + ", cause = " + cause + "})";
          }
        });
  }

  /**
   * An immutable snapshot of the current state of the service. This class represents a consistent
   * snapshot of the state and therefore it can be used to answer simple queries without needing to
   * grab a lock.
   */
  // @Immutable except that Throwable is mutable (initCause(), setStackTrace(), mutable subclasses).
  private static final class StateSnapshot {
    /**
     * The internal state, which equals external state unless shutdownWhenStartupFinishes is true.
     */
    final State state;

    /** If true, the user requested a shutdown while the service was still starting up. */
    final boolean shutdownWhenStartupFinishes;

    /**
     * The exception that caused this service to fail. This will be {@code null} unless the service
     * has failed.
     */
    final @Nullable Throwable failure;

    StateSnapshot(State internalState) {
      this(internalState, false, null);
    }

    StateSnapshot(
        State internalState, boolean shutdownWhenStartupFinishes, @Nullable Throwable failure) {
      checkArgument(
          !shutdownWhenStartupFinishes || internalState == State.STARTING,
          "shutdownWhenStartupFinishes can only be set if state is STARTING. Got %s instead.",
          internalState);
      checkArgument(
          !(failure != null ^ internalState == State.FAILED),
          "A failure cause should be set if and only if the state is failed.  Got %s and %s "
              + "instead.",
          internalState,
          failure);
      this.state = internalState;
      this.shutdownWhenStartupFinishes = shutdownWhenStartupFinishes;
      this.failure = failure;
    }

    /** @see Service#state() */
    State externalState() {
      if (shutdownWhenStartupFinishes && state == State.STARTING) {
        return State.STOPPING;
      } else {
        return state;
      }
    }

    /** @see Service#failureCause() */
    Throwable failureCause() {
      checkState(
          state == State.FAILED,
          "failureCause() is only valid if the service has failed, service is %s",
          state);
      return failure;
    }
  }
}