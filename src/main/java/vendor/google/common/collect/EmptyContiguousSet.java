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
package vendor.google.common.collect;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import vendor.google.common.annotations.GwtCompatible;
import vendor.google.common.annotations.GwtIncompatible;

/**
 * An empty contiguous set.
 *
 * @author Gregory Kick
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("rawtypes") // allow ungenerified Comparable types
final class EmptyContiguousSet<C extends Comparable> extends ContiguousSet<C> {
  EmptyContiguousSet(vendor.google.common.collect.DiscreteDomain<C> domain) {
    super(domain);
  }

  @Override
  public C first() {
    throw new NoSuchElementException();
  }

  @Override
  public C last() {
    throw new NoSuchElementException();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public ContiguousSet<C> intersection(ContiguousSet<C> other) {
    return this;
  }

  @Override
  public Range<C> range() {
    throw new NoSuchElementException();
  }

  @Override
  public Range<C> range(vendor.google.common.collect.BoundType lowerBoundType, BoundType upperBoundType) {
    throw new NoSuchElementException();
  }

  @Override
  ContiguousSet<C> headSetImpl(C toElement, boolean inclusive) {
    return this;
  }

  @Override
  ContiguousSet<C> subSetImpl(
      C fromElement, boolean fromInclusive, C toElement, boolean toInclusive) {
    return this;
  }

  @Override
  ContiguousSet<C> tailSetImpl(C fromElement, boolean fromInclusive) {
    return this;
  }

  @Override
  public boolean contains(Object object) {
    return false;
  }

  @vendor.google.common.annotations.GwtIncompatible // not used by GWT emulation
  @Override
  int indexOf(Object target) {
    return -1;
  }

  @Override
  public UnmodifiableIterator<C> iterator() {
    return Iterators.emptyIterator();
  }

  @vendor.google.common.annotations.GwtIncompatible // NavigableSet
  @Override
  public UnmodifiableIterator<C> descendingIterator() {
    return Iterators.emptyIterator();
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public ImmutableList<C> asList() {
    return ImmutableList.of();
  }

  @Override
  public String toString() {
    return "[]";
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof Set) {
      Set<?> that = (Set<?>) object;
      return that.isEmpty();
    }
    return false;
  }

  @vendor.google.common.annotations.GwtIncompatible // not used in GWT
  @Override
  boolean isHashCodeFast() {
    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @vendor.google.common.annotations.GwtIncompatible // serialization
  private static final class SerializedForm<C extends Comparable> implements Serializable {
    private final vendor.google.common.collect.DiscreteDomain<C> domain;

    private SerializedForm(DiscreteDomain<C> domain) {
      this.domain = domain;
    }

    private Object readResolve() {
      return new EmptyContiguousSet<C>(domain);
    }

    private static final long serialVersionUID = 0;
  }

  @vendor.google.common.annotations.GwtIncompatible // serialization
  @Override
  Object writeReplace() {
    return new SerializedForm<C>(domain);
  }

  @GwtIncompatible // NavigableSet
  @Override
  ImmutableSortedSet<C> createDescendingSet() {
    return ImmutableSortedSet.emptySet(Ordering.natural().reverse());
  }
}
