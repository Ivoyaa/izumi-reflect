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

package izumi.reflect

import izumi.reflect.internal.fundamentals.platform.console.TrivialLogger

import scala.annotation.tailrec
import scala.reflect.api.Universe

private[reflect] object ReflectionUtil {

  /** Mini `normalize`. `normalize` is deprecated and we don't want to do scary things such as evaluate type-lambdas anyway.
    * And AFAIK the only case that can make us confuse a type-parameter for a non-parameter is an empty refinement `T {}`.
    * So we just strip it when we get it.
    */
  // ReflectionUtil.norm but with added logging
  @tailrec @inline
  def norm(u: Universe, logger: TrivialLogger)(x: u.Type): u.Type = {
    import u._
    x match {
      case RefinedType(t :: Nil, m) if m.isEmpty =>
        logger.log(s"Stripped empty refinement of type $t. member scope $m")
        norm(u, logger)(t)
      case AnnotatedType(_, t) =>
        norm(u, logger)(t)
      case _ =>
        x
    }
  }

  def allPartsStrong(tpe: Universe#Type): Boolean = {
    val selfStrong = isSelfStrong(tpe)
    def prefixStrong = {
      tpe match {
        case t: Universe#TypeRefApi =>
          allPartsStrong(t.pre.dealias)
        case _ =>
          true
      }
    }
    val dealiased = tpe.dealias
    def typeCtorStrong = {
      val resType = dealiased.finalResultType
      val ctor = resType.typeConstructor
      (resType == dealiased) || (ctor == resType) || (ctor == tpe) ||
      tpe.typeParams.contains(ctor.typeSymbol) || allPartsStrong(ctor)
    }
    def argsStrong = {
      dealiased.finalResultType.typeArgs.forall {
        arg =>
          tpe.typeParams.contains(arg.typeSymbol) ||
          allPartsStrong(arg)
      }
    }
    def intersectionStructStrong = {
      tpe match {
        case t: Universe#RefinedTypeApi =>
          t.parents.forall(allPartsStrong) &&
          t.decls.toSeq.forall((s: Universe#Symbol) => s.isTerm || allPartsStrong(s.asType.typeSignature.dealias))
        case _ =>
          true
      }
    }

    selfStrong && prefixStrong && typeCtorStrong && argsStrong && intersectionStructStrong
  }

  def isSelfStrong(tpe: Universe#Type): Boolean = {
    // FIXME: strengthening check to capture `IntersectionBlockingIO` test case causes StackOverflow during implicit search
//    def intersectionMembersStrong = {
//      tpe match {
//        case t: Universe#RefinedTypeApi =>
//          t.parents.forall(isSelfStrong)
//        case _ => true
//      }
//    }

    !(tpe.typeSymbol.isParameter || (
      // we regard abstract types like T in trait X { type T; Tag[this.T] } - when we are _inside_ the definition template
      // as 'type parameters' too. So that you could define `implicit def tagForT: Tag[this.T]` and the tag would be resolved
      // to this implicit correctly, instead of generating a useless `X::this.type::T` tag.
      tpe.isInstanceOf[Universe#TypeRefApi] &&
      tpe.asInstanceOf[Universe#TypeRefApi].pre.isInstanceOf[Universe#ThisTypeApi] &&
      tpe.typeSymbol.isAbstract && !tpe.typeSymbol.isClass && isNotDealiasedFurther(tpe)
    )) /*&& intersectionMembersStrong*/ ||
    tpe.typeParams.exists {
      t =>
        t == tpe.typeSymbol ||
        t.typeSignature == tpe.typeSymbol.typeSignature ||
        (t.name eq tpe.typeSymbol.name)
    }
  }

  def isNotDealiasedFurther(tpe: Universe#Type): Boolean = {
    val u: Universe = null
    val tpe1: u.Type = tpe.asInstanceOf[u.Type]
    tpe1.dealias =:= tpe1
  }

  def kindOf(tpe: Universe#Type): Kind = {
    Kind(tpe.typeParams.map(t => kindOf(t.typeSignature)))
  }

  final case class Kind(args: List[Kind]) {
    def format(typeName: String) = s"$typeName${if (args.nonEmpty) args.mkString("[", ", ", "]") else ""}"
    override def toString: String = format("_")
  }

}
