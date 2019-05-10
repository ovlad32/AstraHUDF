/*
 * Copyright (C) 2011 The Guava Authors
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

package vendor.google.common.base;

import static vendor.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import vendor.google.common.annotations.GwtCompatible;

/** Implementation of an {@link vendor.google.common.base.Optional} not containing a reference. */
@GwtCompatible
final class Absent<T> extends vendor.google.common.base.Optional<T> {
  static final Absent<Object> INSTANCE = new Absent<>();

  @SuppressWarnings("unchecked") // implementation is "fully variant"
  static <T> vendor.google.common.base.Optional<T> withType() {
    return (vendor.google.common.base.Optional<T>) INSTANCE;
  }

  private Absent() {}

  @Override
  public boolean isPresent() {
    return false;
  }

  @Override
  public T get() {
    throw new IllegalStateException("Optional.get() cannot be called on an absent value");
  }

  @Override
  public T or(T defaultValue) {
    return checkNotNull(defaultValue, "use Optional.orNull() instead of Optional.or(null)");
  }

  @SuppressWarnings("unchecked") // safe covariant cast
  @Override
  public vendor.google.common.base.Optional<T> or(vendor.google.common.base.Optional<? extends T> secondChoice) {
    return (vendor.google.common.base.Optional<T>) checkNotNull(secondChoice);
  }

  @Override
  public T or(Supplier<? extends T> supplier) {
    return checkNotNull(
        supplier.get(), "use Optional.orNull() instead of a Supplier that returns null");
  }

  @Override
  public @Nullable T orNull() {
    return null;
  }

  @Override
  public Set<T> asSet() {
    return Collections.emptySet();
  }

  @Override
  public <V> vendor.google.common.base.Optional<V> transform(Function<? super T, V> function) {
    checkNotNull(function);
    return Optional.absent();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    return object == this;
  }

  @Override
  public int hashCode() {
    return 0x79a31aac;
  }

  @Override
  public String toString() {
    return "Optional.absent()";
  }

  private Object readResolve() {
    return INSTANCE;
  }

  private static final long serialVersionUID = 0;
}