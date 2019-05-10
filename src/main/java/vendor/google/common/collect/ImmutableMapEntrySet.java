/*
 * Copyright (C) 2008 The Guava Authors
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

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import vendor.google.common.annotations.GwtCompatible;
import vendor.google.common.annotations.GwtIncompatible;

/**
 * {@code entrySet()} implementation for {@link vendor.google.common.collect.ImmutableMap}.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
abstract class ImmutableMapEntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
  static final class RegularEntrySet<K, V> extends ImmutableMapEntrySet<K, V> {
    private final transient vendor.google.common.collect.ImmutableMap<K, V> map;
    private final transient vendor.google.common.collect.ImmutableList<Entry<K, V>> entries;

    RegularEntrySet(vendor.google.common.collect.ImmutableMap<K, V> map, Entry<K, V>[] entries) {
      this(map, vendor.google.common.collect.ImmutableList.<Entry<K, V>>asImmutableList(entries));
    }

    RegularEntrySet(vendor.google.common.collect.ImmutableMap<K, V> map, vendor.google.common.collect.ImmutableList<Entry<K, V>> entries) {
      this.map = map;
      this.entries = entries;
    }

    @Override
    vendor.google.common.collect.ImmutableMap<K, V> map() {
      return map;
    }

    @Override
    @vendor.google.common.annotations.GwtIncompatible("not used in GWT")
    int copyIntoArray(Object[] dst, int offset) {
      return entries.copyIntoArray(dst, offset);
    }

    @Override
    public UnmodifiableIterator<Entry<K, V>> iterator() {
      return entries.iterator();
    }

    @Override
    public Spliterator<Entry<K, V>> spliterator() {
      return entries.spliterator();
    }

    @Override
    public void forEach(Consumer<? super Entry<K, V>> action) {
      entries.forEach(action);
    }

    @Override
    ImmutableList<Entry<K, V>> createAsList() {
      return new vendor.google.common.collect.RegularImmutableAsList<>(this, entries);
    }
  }

  ImmutableMapEntrySet() {}

  abstract vendor.google.common.collect.ImmutableMap<K, V> map();

  @Override
  public int size() {
    return map().size();
  }

  @Override
  public boolean contains(@Nullable Object object) {
    if (object instanceof Entry) {
      Entry<?, ?> entry = (Entry<?, ?>) object;
      V value = map().get(entry.getKey());
      return value != null && value.equals(entry.getValue());
    }
    return false;
  }

  @Override
  boolean isPartialView() {
    return map().isPartialView();
  }

  @Override
  @vendor.google.common.annotations.GwtIncompatible
      // not used in GWT
  boolean isHashCodeFast() {
    return map().isHashCodeFast();
  }

  @Override
  public int hashCode() {
    return map().hashCode();
  }

  @vendor.google.common.annotations.GwtIncompatible // serialization
  @Override
  Object writeReplace() {
    return new EntrySetSerializedForm<>(map());
  }

  @GwtIncompatible // serialization
  private static class EntrySetSerializedForm<K, V> implements Serializable {
    final vendor.google.common.collect.ImmutableMap<K, V> map;

    EntrySetSerializedForm(ImmutableMap<K, V> map) {
      this.map = map;
    }

    Object readResolve() {
      return map.entrySet();
    }

    private static final long serialVersionUID = 0;
  }
}
