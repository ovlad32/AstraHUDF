/*
 * Copyright (C) 2007 The Guava Authors
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

import static vendor.google.common.base.Preconditions.checkArgument;
import static vendor.google.common.base.Preconditions.checkState;

import vendor.google.common.base.Objects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2objc.annotations.RetainedWith;
import com.google.j2objc.annotations.WeakOuter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import vendor.google.common.annotations.GwtCompatible;
import vendor.google.common.annotations.GwtIncompatible;

/**
 * A general-purpose bimap implementation using any two backing {@code Map} instances.
 *
 * <p>Note that this class contains {@code equals()} calls that keep it from supporting {@code
 * IdentityHashMap} backing maps.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
abstract class AbstractBiMap<K, V> extends ForwardingMap<K, V>
    implements vendor.google.common.collect.BiMap<K, V>, Serializable {

  private transient @MonotonicNonNull Map<K, V> delegate;
  @MonotonicNonNull @RetainedWith transient AbstractBiMap<V, K> inverse;

  /** Package-private constructor for creating a map-backed bimap. */
  AbstractBiMap(Map<K, V> forward, Map<V, K> backward) {
    setDelegates(forward, backward);
  }

  /** Private constructor for inverse bimap. */
  private AbstractBiMap(Map<K, V> backward, AbstractBiMap<V, K> forward) {
    delegate = backward;
    inverse = forward;
  }

  @Override
  protected Map<K, V> delegate() {
    return delegate;
  }

  /** Returns its input, or throws an exception if this is not a valid key. */
  @CanIgnoreReturnValue
  K checkKey(@Nullable K key) {
    return key;
  }

  /** Returns its input, or throws an exception if this is not a valid value. */
  @CanIgnoreReturnValue
  V checkValue(@Nullable V value) {
    return value;
  }

  /**
   * Specifies the delegate maps going in each direction. Called by the constructor and by
   * subclasses during deserialization.
   */
  void setDelegates(Map<K, V> forward, Map<V, K> backward) {
    checkState(delegate == null);
    checkState(inverse == null);
    checkArgument(forward.isEmpty());
    checkArgument(backward.isEmpty());
    checkArgument(forward != backward);
    delegate = forward;
    inverse = makeInverse(backward);
  }

  AbstractBiMap<V, K> makeInverse(Map<V, K> backward) {
    return new Inverse<>(backward, this);
  }

  void setInverse(AbstractBiMap<V, K> inverse) {
    this.inverse = inverse;
  }

  // Query Operations (optimizations)

  @Override
  public boolean containsValue(@Nullable Object value) {
    return inverse.containsKey(value);
  }

  // Modification Operations

  @CanIgnoreReturnValue
  @Override
  public V put(@Nullable K key, @Nullable V value) {
    return putInBothMaps(key, value, false);
  }

  @CanIgnoreReturnValue
  @Override
  public V forcePut(@Nullable K key, @Nullable V value) {
    return putInBothMaps(key, value, true);
  }

  private V putInBothMaps(@Nullable K key, @Nullable V value, boolean force) {
    checkKey(key);
    checkValue(value);
    boolean containedKey = containsKey(key);
    if (containedKey && Objects.equal(value, get(key))) {
      return value;
    }
    if (force) {
      inverse().remove(value);
    } else {
      checkArgument(!containsValue(value), "value already present: %s", value);
    }
    V oldValue = delegate.put(key, value);
    updateInverseMap(key, containedKey, oldValue, value);
    return oldValue;
  }

  private void updateInverseMap(K key, boolean containedKey, V oldValue, V newValue) {
    if (containedKey) {
      removeFromInverseMap(oldValue);
    }
    inverse.delegate.put(newValue, key);
  }

  @CanIgnoreReturnValue
  @Override
  public V remove(@Nullable Object key) {
    return containsKey(key) ? removeFromBothMaps(key) : null;
  }

  @CanIgnoreReturnValue
  private V removeFromBothMaps(Object key) {
    V oldValue = delegate.remove(key);
    removeFromInverseMap(oldValue);
    return oldValue;
  }

  private void removeFromInverseMap(V oldValue) {
    inverse.delegate.remove(oldValue);
  }

  // Bulk Operations

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    this.delegate.replaceAll(function);
    inverse.delegate.clear();
    Entry<K, V> broken = null;
    Iterator<Entry<K, V>> itr = this.delegate.entrySet().iterator();
    while (itr.hasNext()) {
      Entry<K, V> entry = itr.next();
      K k = entry.getKey();
      V v = entry.getValue();
      K conflict = inverse.delegate.putIfAbsent(v, k);
      if (conflict != null) {
        broken = entry;
        // We're definitely going to throw, but we'll try to keep the BiMap in an internally
        // consistent state by removing the bad entry.
        itr.remove();
      }
    }
    if (broken != null) {
      throw new IllegalArgumentException("value already present: " + broken.getValue());
    }
  }

  @Override
  public void clear() {
    delegate.clear();
    inverse.delegate.clear();
  }

  // Views

  @Override
  public BiMap<V, K> inverse() {
    return inverse;
  }

  private transient @MonotonicNonNull Set<K> keySet;

  @Override
  public Set<K> keySet() {
    Set<K> result = keySet;
    return (result == null) ? keySet = new KeySet() : result;
  }

  @WeakOuter
  private class KeySet extends ForwardingSet<K> {
    @Override
    protected Set<K> delegate() {
      return delegate.keySet();
    }

    @Override
    public void clear() {
      AbstractBiMap.this.clear();
    }

    @Override
    public boolean remove(Object key) {
      if (!contains(key)) {
        return false;
      }
      removeFromBothMaps(key);
      return true;
    }

    @Override
    public boolean removeAll(Collection<?> keysToRemove) {
      return standardRemoveAll(keysToRemove);
    }

    @Override
    public boolean retainAll(Collection<?> keysToRetain) {
      return standardRetainAll(keysToRetain);
    }

    @Override
    public Iterator<K> iterator() {
      return Maps.keyIterator(entrySet().iterator());
    }
  }

  private transient @MonotonicNonNull Set<V> valueSet;

  @Override
  public Set<V> values() {
    /*
     * We can almost reuse the inverse's keySet, except we have to fix the
     * iteration order so that it is consistent with the forward map.
     */
    Set<V> result = valueSet;
    return (result == null) ? valueSet = new ValueSet() : result;
  }

  @WeakOuter
  private class ValueSet extends ForwardingSet<V> {
    final Set<V> valuesDelegate = inverse.keySet();

    @Override
    protected Set<V> delegate() {
      return valuesDelegate;
    }

    @Override
    public Iterator<V> iterator() {
      return Maps.valueIterator(entrySet().iterator());
    }

    @Override
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override
    public String toString() {
      return standardToString();
    }
  }

  private transient @MonotonicNonNull Set<Entry<K, V>> entrySet;

  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> result = entrySet;
    return (result == null) ? entrySet = new EntrySet() : result;
  }

  class BiMapEntry extends ForwardingMapEntry<K, V> {
    private final Entry<K, V> delegate;

    BiMapEntry(Entry<K, V> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected Entry<K, V> delegate() {
      return delegate;
    }

    @Override
    public V setValue(V value) {
      checkValue(value);
      // Preconditions keep the map and inverse consistent.
      checkState(entrySet().contains(this), "entry no longer in map");
      // similar to putInBothMaps, but set via entry
      if (Objects.equal(value, getValue())) {
        return value;
      }
      checkArgument(!containsValue(value), "value already present: %s", value);
      V oldValue = delegate.setValue(value);
      checkState(Objects.equal(value, get(getKey())), "entry no longer in map");
      updateInverseMap(getKey(), true, oldValue, value);
      return oldValue;
    }
  }

  Iterator<Entry<K, V>> entrySetIterator() {
    final Iterator<Entry<K, V>> iterator = delegate.entrySet().iterator();
    return new Iterator<Entry<K, V>>() {
      @Nullable Entry<K, V> entry;

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Entry<K, V> next() {
        entry = iterator.next();
        return new BiMapEntry(entry);
      }

      @Override
      public void remove() {
        CollectPreconditions.checkRemove(entry != null);
        V value = entry.getValue();
        iterator.remove();
        removeFromInverseMap(value);
        entry = null;
      }
    };
  }

  @WeakOuter
  private class EntrySet extends ForwardingSet<Entry<K, V>> {
    final Set<Entry<K, V>> esDelegate = delegate.entrySet();

    @Override
    protected Set<Entry<K, V>> delegate() {
      return esDelegate;
    }

    @Override
    public void clear() {
      AbstractBiMap.this.clear();
    }

    @Override
    public boolean remove(Object object) {
      if (!esDelegate.contains(object)) {
        return false;
      }

      // safe because esDelegate.contains(object).
      Entry<?, ?> entry = (Entry<?, ?>) object;
      inverse.delegate.remove(entry.getValue());
      /*
       * Remove the mapping in inverse before removing from esDelegate because
       * if entry is part of esDelegate, entry might be invalidated after the
       * mapping is removed from esDelegate.
       */
      esDelegate.remove(entry);
      return true;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return entrySetIterator();
    }

    // See java.util.Collections.CheckedEntrySet for details on attacks.

    @Override
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override
    public boolean contains(Object o) {
      return Maps.containsEntryImpl(delegate(), o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return standardContainsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return standardRemoveAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return standardRetainAll(c);
    }
  }

  /** The inverse of any other {@code AbstractBiMap} subclass. */
  static class Inverse<K, V> extends AbstractBiMap<K, V> {
    Inverse(Map<K, V> backward, AbstractBiMap<V, K> forward) {
      super(backward, forward);
    }

    /*
     * Serialization stores the forward bimap, the inverse of this inverse.
     * Deserialization calls inverse() on the forward bimap and returns that
     * inverse.
     *
     * If a bimap and its inverse are serialized together, the deserialized
     * instances have inverse() methods that return the other.
     */

    @Override
    K checkKey(K key) {
      return inverse.checkValue(key);
    }

    @Override
    V checkValue(V value) {
      return inverse.checkKey(value);
    }

    /** @serialData the forward bimap */
    @vendor.google.common.annotations.GwtIncompatible // java.io.ObjectOutputStream
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(inverse());
    }

    @vendor.google.common.annotations.GwtIncompatible // java.io.ObjectInputStream
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      setInverse((AbstractBiMap<V, K>) stream.readObject());
    }

    @vendor.google.common.annotations.GwtIncompatible
        // Not needed in the emulated source.
    Object readResolve() {
      return inverse().inverse();
    }

    @vendor.google.common.annotations.GwtIncompatible // Not needed in emulated source.
    private static final long serialVersionUID = 0;
  }

  @GwtIncompatible // Not needed in emulated source.
  private static final long serialVersionUID = 0;
}
