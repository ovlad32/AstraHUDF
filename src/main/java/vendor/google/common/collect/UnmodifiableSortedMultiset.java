/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vendor.google.common.collect;

import java.util.Comparator;
import java.util.NavigableSet;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import vendor.google.common.annotations.GwtCompatible;

/**
 * Implementation of {@link Multisets#unmodifiableSortedMultiset(vendor.google.common.collect.SortedMultiset)}, split out into
 * its own file so it can be GWT emulated (to deal with the differing elementSet() types in GWT and
 * non-GWT).
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
final class UnmodifiableSortedMultiset<E> extends Multisets.UnmodifiableMultiset<E>
    implements vendor.google.common.collect.SortedMultiset<E> {
  UnmodifiableSortedMultiset(vendor.google.common.collect.SortedMultiset<E> delegate) {
    super(delegate);
  }

  @Override
  protected vendor.google.common.collect.SortedMultiset<E> delegate() {
    return (vendor.google.common.collect.SortedMultiset<E>) super.delegate();
  }

  @Override
  public Comparator<? super E> comparator() {
    return delegate().comparator();
  }

  @Override
  NavigableSet<E> createElementSet() {
    return Sets.unmodifiableNavigableSet(delegate().elementSet());
  }

  @Override
  public NavigableSet<E> elementSet() {
    return (NavigableSet<E>) super.elementSet();
  }

  private transient @MonotonicNonNull UnmodifiableSortedMultiset<E> descendingMultiset;

  @Override
  public vendor.google.common.collect.SortedMultiset<E> descendingMultiset() {
    UnmodifiableSortedMultiset<E> result = descendingMultiset;
    if (result == null) {
      result = new UnmodifiableSortedMultiset<E>(delegate().descendingMultiset());
      result.descendingMultiset = this;
      return descendingMultiset = result;
    }
    return result;
  }

  @Override
  public Multiset.Entry<E> firstEntry() {
    return delegate().firstEntry();
  }

  @Override
  public Multiset.Entry<E> lastEntry() {
    return delegate().lastEntry();
  }

  @Override
  public Multiset.Entry<E> pollFirstEntry() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Multiset.Entry<E> pollLastEntry() {
    throw new UnsupportedOperationException();
  }

  @Override
  public vendor.google.common.collect.SortedMultiset<E> headMultiset(E upperBound, vendor.google.common.collect.BoundType boundType) {
    return Multisets.unmodifiableSortedMultiset(delegate().headMultiset(upperBound, boundType));
  }

  @Override
  public vendor.google.common.collect.SortedMultiset<E> subMultiset(
          E lowerBound, vendor.google.common.collect.BoundType lowerBoundType, E upperBound, vendor.google.common.collect.BoundType upperBoundType) {
    return Multisets.unmodifiableSortedMultiset(
        delegate().subMultiset(lowerBound, lowerBoundType, upperBound, upperBoundType));
  }

  @Override
  public SortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
    return Multisets.unmodifiableSortedMultiset(delegate().tailMultiset(lowerBound, boundType));
  }

  private static final long serialVersionUID = 0;
}
