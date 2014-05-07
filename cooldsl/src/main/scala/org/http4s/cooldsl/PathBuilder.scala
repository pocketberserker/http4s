package org.http4s.cooldsl

import scala.language.existentials

import shapeless.{::, HNil, HList}
import shapeless.ops.hlist.Prepend
import org.http4s.Method
import org.http4s.cooldsl.BodyCodec.Decoder
import org.http4s.cooldsl.bits.{StringParser, HListToFunc}
import scala.reflect.Manifest

/**
 * Created by Bryce Anderson on 4/28/14.
 *
 * The goal of a PathBuilder is to allow the composition of what is typically on the status line
 * of a HTTP request. That includes the request method, path, and query params.
 */

/** Fully functional path building */
final class PathBuilder[T <: HList](val method: Method, private[cooldsl] val path: PathRule[T])
                  extends PathBuilderBase[T] with HeaderAppendable[T] with MetaDataSyntax {

  type Self = PathBuilder[T]

  def -?[T1](q: QueryRule[T1]): Router[T1::T] = new Router(method, path, q)

  def /(t: CaptureTail) : Router[List[String]::T] = new Router(method, PathAnd(path,t), EmptyHeaderRule)

  def /(s: String): PathBuilder[T] = new PathBuilder(method, PathAnd(path,PathMatch(s)))

  def /(s: Symbol): PathBuilder[String::T] = new PathBuilder(method, PathAnd(path,PathCapture(StringParser.strParser)))

  def /[T2 <: HList](t: CombinablePathRule[T2])(implicit prep: Prepend[T2, T]) : PathBuilder[prep.Out] =
    new PathBuilder(method, PathAnd(path,t))

  def /[T2 <: HList](t: PathBuilder[T2])(implicit prep: Prepend[T2, T]) : PathBuilder[prep.Out] =
    new PathBuilder(method, PathAnd(path, t.path))

  override protected def addMetaData(data: MetaData): PathBuilder[T] = new PathBuilder[T](method, PathAnd(path, data))
}

////////////////// AST representation of operations supported on the path ///////////////////

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

sealed trait PathBuilderBase[T <: HList] extends RouteExecutable[T] with HeaderAppendable[T] {
  def method: Method
  private[cooldsl] def path: PathRule[T]

  override private[cooldsl] def validators: HeaderRule[_ <: HList] = EmptyHeaderRule

  final def toAction: Router[T] = validate(EmptyHeaderRule)

  final def validate[T1 <: HList](h2: HeaderRule[T1])(implicit prep: Prepend[T1,T]): Router[prep.Out] =
    Router(method, path, h2)

  override final def >>>[T1 <: HList](h2: HeaderRule[T1])(implicit prep: Prepend[T1,T]): Router[prep.Out] = validate(h2)

  final def decoding[R](dec: Decoder[R]): CodecRouter[T, R] = CodecRouter(toAction, dec)

  final def makeAction[F, O](f: F)(implicit hf: HListToFunc[T,O,F]): CoolAction[T, F, O] =
    new CoolAction(Router(method, path, EmptyHeaderRule), f, hf)
}

/** Actual elements which build up the AST */
/** The root type of the parser AST */
private[cooldsl] sealed trait PathRule[T <: HList] {
  def documentation: Option[String] = None
}

private[cooldsl] case class PathAnd[T <: HList](p1: PathRule[_ <: HList], p2: PathRule[_ <: HList]) extends CombinablePathRule[T]

private[cooldsl] case class PathOr[T <: HList](p1: PathRule[T], p2: PathRule[T]) extends CombinablePathRule[T]

private[cooldsl] case class PathMatch(s: String, override val documentation: Option[String] = None) extends CombinablePathRule[HNil]

private[cooldsl] case class PathCapture[T](parser: StringParser[T],
                                           override val documentation: Option[String] = None)
                                          (implicit val m: Manifest[T]) extends CombinablePathRule[T::HNil]

// These don't fit the  operations of CombinablePathSyntax because they may
// result in a change of the type of PathBulder
// TODO: can I make this a case object?
case class CaptureTail(override val documentation: Option[String] = None) extends PathRule[List[String]::HNil]

private[cooldsl] case object PathEmpty extends PathRule[HNil]

trait MetaData extends PathRule[HNil]

case class PathDescription(desc: String) extends MetaData

