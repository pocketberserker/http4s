package org.http4s.cooldsl

import shapeless.{::, HNil, HList}
import shapeless.ops.hlist.Prepend
import org.http4s.{Response, Method}
import org.http4s.cooldsl.BodyCodec.Decoder
import scalaz.concurrent.Task

import scala.language.existentials

/**
 * Created by Bryce Anderson on 4/28/14.
 *
 * The goal of a PathBuilder is to allow the composition of what is typically on the status line
 * of a HTTP request. That includes the request method, path, and query params.
 */

////////////////// Status line combinators //////////////////////////////////////////
/** PathBuilder that disallows modifications to path but allows further query params mode */
final class FinishedPathBuilder[T <: HList](val m: Method, private[cooldsl] val path: PathRule[T])
                                 extends PathBuilderBase[T] {
  def -?[T1](q: QueryMapper[T1]): FinishedPathBuilder[T1::T] = new FinishedPathBuilder(m, path.and(q))
  def &[T1](q: QueryMapper[T1]): FinishedPathBuilder[T1::T] = -?(q)
}

/** Fully functional path building */
final class PathBuilder[T <: HList](val m: Method, private[cooldsl] val path: PathRule[T]) extends PathBuilderBase[T] {

  def -?[T1](q: QueryMapper[T1]): FinishedPathBuilder[T1::T] = new FinishedPathBuilder(m, path.and(q))

  def /(t: CaptureTail) : FinishedPathBuilder[List[String]::T] = new FinishedPathBuilder(m, path.and(t))

  def /(s: String): PathBuilder[T] = new PathBuilder(m, path.and(PathMatch(s)))

  def /(s: Symbol): PathBuilder[String::T] = new PathBuilder(m, path.and(PathCapture(StringParser.strParser)))

  def /[T2 <: HList](t: CombinablePathRule[T2])(implicit prep: Prepend[T2, T]) : PathBuilder[prep.Out] =
    new PathBuilder(m, path.and(t))

  def /[T2 <: HList](t: PathBuilder[T2])(implicit prep: Prepend[T2, T]) : PathBuilder[prep.Out] =
    new PathBuilder(m, path.and(t.path))

  def /[T2 <: HList](t: FinishedPathBuilder[T2])(implicit prep: Prepend[T2, T]) : FinishedPathBuilder[prep.Out] =
    new FinishedPathBuilder(m, path.and(t.path))
}

////////////////// AST representation of operations supported on the path ///////////////////
sealed trait PathRule[T <: HList] {
  def and[T2 <: HList](p2: PathRule[T2])(implicit prep: Prepend[T2,T]): PathRule[prep.Out] =
    PathAnd(this, p2)

  def &&[T2 <: HList](p2: PathRule[T2])(implicit prep: Prepend[T2,T]): PathRule[prep.Out] = and(p2)

  def or(p2: PathRule[T]): PathRule[T] = PathOr(this, p2)

  def ||(p2: PathRule[T]): PathRule[T] = or(p2)
}

sealed trait CombinablePathRule[T <: HList] extends PathRule[T] {
  /** These methods differ in their return type */
  def and[T2 <: HList](p2: CombinablePathRule[T2])(implicit prep: Prepend[T2,T]): CombinablePathRule[prep.Out] =
    PathAnd(this, p2)

  def &&[T2 <: HList](p2: CombinablePathRule[T2])(implicit prep: Prepend[T2,T]): CombinablePathRule[prep.Out] = and(p2)

  def or(p2: CombinablePathRule[T]): CombinablePathRule[T] = PathOr(this, p2)

  def ||(p2: CombinablePathRule[T]): CombinablePathRule[T] = or(p2)

  def /(s: String): CombinablePathRule[T] = PathAnd(this, PathMatch(s))

  def /(s: Symbol): CombinablePathRule[String::T] = PathAnd(this, PathCapture(StringParser.strParser))

  def /[T2 <: HList](t: CombinablePathRule[T2])(implicit prep: Prepend[T2, T]) : CombinablePathRule[prep.Out] =
    PathAnd(this, t)
}

sealed trait PathBuilderBase[T <: HList] {
  def m: Method
  private[cooldsl] def path: PathRule[T]

  final def toAction: Runnable[T, HNil] = validate(EmptyHeaderRule)

  final def validate[T1 <: HList](h2: HeaderRule[T1])(implicit prep: Prepend[T1,T]): Runnable[prep.Out, T1] =
    Runnable(m, path, h2)

  final def >>>[T1 <: HList](h2: HeaderRule[T1])(implicit prep: Prepend[T1,T]): Runnable[prep.Out, T1] = validate(h2)

  final def decoding[R](dec: Decoder[R]): CodecRunnable[T, HNil, R] = CodecRunnable(toAction, dec)

  final def ==>[F](f: F)(implicit hf: HListToFunc[T,Task[Response],F]): Goal = RouteExecutor.compile(toAction, f, hf)
}

/** Actual elements which build up the AST */

private[cooldsl] case class PathAnd[T <: HList](p1: PathRule[_ <: HList], p2: PathRule[_ <: HList]) extends CombinablePathRule[T]

private[cooldsl] case class PathOr[T <: HList](p1: PathRule[T], p2: PathRule[T]) extends CombinablePathRule[T]

private[cooldsl] case class PathMatch(s: String) extends CombinablePathRule[HNil]

private[cooldsl] case class PathCapture[T](parser: StringParser[T]) extends CombinablePathRule[T::HNil]

// These don't fit the  operations of CombinablePathSyntax because they may
// result in a change of the type of PathBulder
// TODO: can I make this a case object?
case class CaptureTail() extends PathRule[List[String]::HNil]

private[cooldsl] case object PathEmpty extends PathRule[HNil]

private[cooldsl] case class QueryMapper[T](name: String, p: StringParser[T]) extends PathRule[T::HNil]

