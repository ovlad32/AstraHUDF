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

import org.checkerframework.checker.nullness.qual.Nullable;
import vendor.google.common.annotations.GwtIncompatible;

/**
 * A descending wrapper around an {@code ImmutableSortedMultiset}
 *
 * @author Louis Wasserman
 */
@SuppressWarnings("serial") // uses writeReplace, not default serialization
@GwtIncompatible
final class DescendingImmutableSortedMultiset<E> extends vendor.google.common.collect.ImmutableSortedMultiset<E> {
  private final transient vendor.google.common.collect.ImmutableSortedMultiset<E> forward;

  DescendingImmutableSortedMultiset(vendor.google.common.collect.ImmutableSortedMultiset<E> forward) {
    this.forward = forward;
  }

  @Override
  public int count(@Nullable Object element) {
    return forward.count(element);
  }

  @Override
  public Entry<E> firstEntry() {
    return forward.lastEntry();
  }

  @Override
  public Entry<E> lastEntry() {
    return forward.firstEntry();
  }

  @Override
  public int size() {
    return forward.size();
  }

  @Override
  public ImmutableSortedSet<E> elementSet() {
    return forward.elementSet().descendingSet();
  }

  @Override
  Entry<E> getEntry(int index) {
    return forward.entrySet().asList().reverse().get(index);
  }

  @Override
  public vendor.google.common.collect.ImmutableSortedMultiset<E> descendingMultiset() {
    return forward;
  }

  @Override
  public vendor.google.common.collect.ImmutableSortedMultiset<E> headMultiset(E upperBound, vendor.google.common.collect.BoundType boundType) {
    return forward.tailMultiset(upperBound, boundType).descendingMultiset();
  }

  @Override
  public ImmutableSortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
    return forward.headMultiset(lowerBound, boundType).descendingMultiset();
  }

  @Override
  boolean isPartialView() {
    return forward.isPartialView();
  }
}
