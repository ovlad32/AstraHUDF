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

import vendor.google.common.base.Predicate;
import java.util.Map.Entry;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import vendor.google.common.annotations.GwtCompatible;

/**
 * Implementation of {@link Multimaps#filterKeys(vendor.google.common.collect.SetMultimap, Predicate)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class FilteredKeySetMultimap<K, V> extends vendor.google.common.collect.FilteredKeyMultimap<K, V>
    implements FilteredSetMultimap<K, V> {

  FilteredKeySetMultimap(vendor.google.common.collect.SetMultimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
    super(unfiltered, keyPredicate);
  }

  @Override
  public vendor.google.common.collect.SetMultimap<K, V> unfiltered() {
    return (SetMultimap<K, V>) unfiltered;
  }

  @Override
  public Set<V> get(K key) {
    return (Set<V>) super.get(key);
  }

  @Override
  public Set<V> removeAll(Object key) {
    return (Set<V>) super.removeAll(key);
  }

  @Override
  public Set<V> replaceValues(K key, Iterable<? extends V> values) {
    return (Set<V>) super.replaceValues(key, values);
  }

  @Override
  public Set<Entry<K, V>> entries() {
    return (Set<Entry<K, V>>) super.entries();
  }

  @Override
  Set<Entry<K, V>> createEntries() {
    return new EntrySet();
  }

  class EntrySet extends Entries implements Set<Entry<K, V>> {
    @Override
    public int hashCode() {
      return vendor.google.common.collect.Sets.hashCodeImpl(this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return Sets.equalsImpl(this, o);
    }
  }
}