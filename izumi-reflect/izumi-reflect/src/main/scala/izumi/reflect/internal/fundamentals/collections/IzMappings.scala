/*
 * Copyright 2019-2020 Septimal Mind Ltd
 * Copyright 2020 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package izumi.reflect.internal.fundamentals.collections

import scala.annotation.nowarn
import scala.collection.mutable

private[reflect] final class IzMappings[A, B](private val list: scala.collection.compat.IterableOnce[(A, B)]) extends AnyVal {
  @nowarn("msg=deprecated")
  def toMutableMultimap: MutableMultiMap[A, B] = {
    list.foldLeft(new mutable.HashMap[A, mutable.Set[B]] with mutable.MultiMap[A, B]) {
      (map, pair) => map.addBinding(pair._1, pair._2)
    }
  }

  @nowarn("msg=Unused import")
  def toMultimap: ImmutableMultiMap[A, B] = {
    import scala.collection.compat._
    toMutableMultimap.view.mapValues(_.toSet).toMap
  }
}
