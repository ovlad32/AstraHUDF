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

/**
 * This package contains generic collection interfaces and implementations, and other utilities for
 * working with collections. It is a part of the open-source <a
 * href="http://github.com/google/guava">Guava</a> library.
 *
 * <h2>Collection Types</h2>
 *
 * <dl>
 *   <dt>{@link vendor.google.common.collect.BiMap}
 *   <dd>An extension of {@link java.util.Map} that guarantees the uniqueness of its values as well
 *       as that of its keys. This is sometimes called an "invertible map," since the restriction on
 *       values enables it to support an {@linkplain vendor.google.common.collect.BiMap#inverse inverse
 *       view} -- which is another instance of {@code BiMap}.
 *   <dt>{@link vendor.google.common.collect.Multiset}
 *   <dd>An extension of {@link java.util.Collection} that may contain duplicate values like a
 *       {@link java.util.List}, yet has order-independent equality like a {@link java.util.Set}.
 *       One typical use for a multiset is to represent a histogram.
 *   <dt>{@link vendor.google.common.collect.Multimap}
 *   <dd>A new type, which is similar to {@link java.util.Map}, but may contain multiple entries
 *       with the same key. Some behaviors of {@link vendor.google.common.collect.Multimap} are left
 *       unspecified and are provided only by the subtypes mentioned below.
 *   <dt>{@link vendor.google.common.collect.ListMultimap}
 *   <dd>An extension of {@link vendor.google.common.collect.Multimap} which permits duplicate entries,
 *       supports random access of values for a particular key, and has <i>partially order-dependent
 *       equality</i> as defined by {@link vendor.google.common.collect.ListMultimap#equals(Object)}.
 *       {@code ListMultimap} takes its name from the fact that the {@linkplain
 *       vendor.google.common.collect.ListMultimap#get collection of values} associated with a given
 *       key fulfills the {@link java.util.List} contract.
 *   <dt>{@link vendor.google.common.collect.SetMultimap}
 *   <dd>An extension of {@link vendor.google.common.collect.Multimap} which has order-independent
 *       equality and does not allow duplicate entries; that is, while a key may appear twice in a
 *       {@code SetMultimap}, each must map to a different value. {@code SetMultimap} takes its name
 *       from the fact that the {@linkplain vendor.google.common.collect.SetMultimap#get collection of
 *       values} associated with a given key fulfills the {@link java.util.Set} contract.
 *   <dt>{@link vendor.google.common.collect.SortedSetMultimap}
 *   <dd>An extension of {@link vendor.google.common.collect.SetMultimap} for which the {@linkplain
 *       vendor.google.common.collect.SortedSetMultimap#get collection values} associated with a given
 *       key is a {@link java.util.SortedSet}.
 *   <dt>{@link vendor.google.common.collect.Table}
 *   <dd>A new type, which is similar to {@link java.util.Map}, but which indexes its values by an
 *       ordered pair of keys, a row key and column key.
 *   <dt>{@link vendor.google.common.collect.ClassToInstanceMap}
 *   <dd>An extension of {@link java.util.Map} that associates a raw type with an instance of that
 *       type.
 * </dl>
 *
 * <h2>Collection Implementations</h2>
 *
 * <h3>of {@link java.util.List}</h3>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.ImmutableList}
 * </ul>
 *
 * <h3>of {@link java.util.Set}</h3>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.ImmutableSet}
 *   <li>{@link vendor.google.common.collect.ImmutableSortedSet}
 *   <li>{@link vendor.google.common.collect.ContiguousSet} (see {@code Range})
 * </ul>
 *
 * <h3>of {@link java.util.Map}</h3>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.ImmutableMap}
 *   <li>{@link vendor.google.common.collect.ImmutableSortedMap}
 *   <li>{@link vendor.google.common.collect.MapMaker}
 * </ul>
 *
 * <h3>of {@link vendor.google.common.collect.BiMap}</h3>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.ImmutableBiMap}
 *   <li>{@link vendor.google.common.collect.HashBiMap}
 *   <li>{@link vendor.google.common.collect.EnumBiMap}
 *   <li>{@link vendor.google.common.collect.EnumHashBiMap}
 * </ul>
 *
 * <h3>of {@link vendor.google.common.collect.Multiset}</h3>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.ImmutableMultiset}
 *   <li>{@link vendor.google.common.collect.HashMultiset}
 *   <li>{@link vendor.google.common.collect.LinkedHashMultiset}
 *   <li>{@link vendor.google.common.collect.TreeMultiset}
 *   <li>{@link vendor.google.common.collect.EnumMultiset}
 *   <li>{@link vendor.google.common.collect.ConcurrentHashMultiset}
 * </ul>
 *
 * <h3>of {@link vendor.google.common.collect.Multimap}</h3>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.ImmutableMultimap}
 *   <li>{@link vendor.google.common.collect.ImmutableListMultimap}
 *   <li>{@link vendor.google.common.collect.ImmutableSetMultimap}
 *   <li>{@link vendor.google.common.collect.ArrayListMultimap}
 *   <li>{@link vendor.google.common.collect.HashMultimap}
 *   <li>{@link vendor.google.common.collect.TreeMultimap}
 *   <li>{@link vendor.google.common.collect.LinkedHashMultimap}
 *   <li>{@link vendor.google.common.collect.LinkedListMultimap}
 * </ul>
 *
 * <h3>of {@link vendor.google.common.collect.Table}</h3>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.ImmutableTable}
 *   <li>{@link vendor.google.common.collect.ArrayTable}
 *   <li>{@link vendor.google.common.collect.HashBasedTable}
 *   <li>{@link vendor.google.common.collect.TreeBasedTable}
 * </ul>
 *
 * <h3>of {@link vendor.google.common.collect.ClassToInstanceMap}</h3>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.ImmutableClassToInstanceMap}
 *   <li>{@link vendor.google.common.collect.MutableClassToInstanceMap}
 * </ul>
 *
 * <h2>Classes of static utility methods</h2>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.Collections2}
 *   <li>{@link vendor.google.common.collect.Iterators}
 *   <li>{@link vendor.google.common.collect.Iterables}
 *   <li>{@link vendor.google.common.collect.Lists}
 *   <li>{@link vendor.google.common.collect.Maps}
 *   <li>{@link vendor.google.common.collect.Queues}
 *   <li>{@link vendor.google.common.collect.Sets}
 *   <li>{@link vendor.google.common.collect.Multisets}
 *   <li>{@link vendor.google.common.collect.Multimaps}
 *   <li>{@link vendor.google.common.collect.Tables}
 *   <li>{@link vendor.google.common.collect.ObjectArrays}
 * </ul>
 *
 * <h2>Comparison</h2>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.Ordering}
 *   <li>{@link vendor.google.common.collect.ComparisonChain}
 * </ul>
 *
 * <h2>Abstract implementations</h2>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.AbstractIterator}
 *   <li>{@link vendor.google.common.collect.AbstractSequentialIterator}
 *   <li>{@link vendor.google.common.collect.ImmutableCollection}
 *   <li>{@link vendor.google.common.collect.UnmodifiableIterator}
 *   <li>{@link vendor.google.common.collect.UnmodifiableListIterator}
 * </ul>
 *
 * <h2>Ranges</h2>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.Range}
 *   <li>{@link vendor.google.common.collect.RangeMap}
 *   <li>{@link vendor.google.common.collect.DiscreteDomain}
 *   <li>{@link vendor.google.common.collect.ContiguousSet}
 * </ul>
 *
 * <h2>Other</h2>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.Interner}, {@link vendor.google.common.collect.Interners}
 *   <li>{@link vendor.google.common.collect.MapDifference}, {@link
 *       vendor.google.common.collect.SortedMapDifference}
 *   <li>{@link vendor.google.common.collect.MinMaxPriorityQueue}
 *   <li>{@link vendor.google.common.collect.PeekingIterator}
 * </ul>
 *
 * <h2>Forwarding collections</h2>
 *
 * <ul>
 *   <li>{@link vendor.google.common.collect.ForwardingCollection}
 *   <li>{@link vendor.google.common.collect.ForwardingConcurrentMap}
 *   <li>{@link vendor.google.common.collect.ForwardingIterator}
 *   <li>{@link vendor.google.common.collect.ForwardingList}
 *   <li>{@link vendor.google.common.collect.ForwardingListIterator}
 *   <li>{@link vendor.google.common.collect.ForwardingListMultimap}
 *   <li>{@link vendor.google.common.collect.ForwardingMap}
 *   <li>{@link vendor.google.common.collect.ForwardingMapEntry}
 *   <li>{@link vendor.google.common.collect.ForwardingMultimap}
 *   <li>{@link vendor.google.common.collect.ForwardingMultiset}
 *   <li>{@link vendor.google.common.collect.ForwardingNavigableMap}
 *   <li>{@link vendor.google.common.collect.ForwardingNavigableSet}
 *   <li>{@link vendor.google.common.collect.ForwardingObject}
 *   <li>{@link vendor.google.common.collect.ForwardingQueue}
 *   <li>{@link vendor.google.common.collect.ForwardingSet}
 *   <li>{@link vendor.google.common.collect.ForwardingSetMultimap}
 *   <li>{@link vendor.google.common.collect.ForwardingSortedMap}
 *   <li>{@link vendor.google.common.collect.ForwardingSortedMultiset}
 *   <li>{@link vendor.google.common.collect.ForwardingSortedSet}
 *   <li>{@link vendor.google.common.collect.ForwardingSortedSetMultimap}
 *   <li>{@link vendor.google.common.collect.ForwardingTable}
 * </ul>
 */
@CheckReturnValue
@ParametersAreNonnullByDefault
package vendor.google.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
