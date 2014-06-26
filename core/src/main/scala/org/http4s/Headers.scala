package org.http4s

import scala.collection.{mutable, immutable}
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.ListBuffer
import org.http4s.HeaderKey.StringKey
import org.http4s.util.CaseInsensitiveString

/** A collection of HTTP Headers */
final class Headers private (headers: List[Header])
  extends immutable.Seq[Header]
  with collection.SeqLike[Header, Headers] {

  override protected def newBuilder: mutable.Builder[Header, Headers] = Headers.newBuilder

  override def tail: Headers = new Headers(headers.tail)

  override def head: Header = headers.head

  override def foreach[B](f: Header => B): Unit = headers.foreach(f)

  def +: (header: Header): Headers = new Headers(header::headers)

  override def drop(n: Int): Headers = new Headers(headers.drop(n))
  
  def length: Int = headers.length

  def apply(idx: Int): Header = headers(idx)

  def iterator: Iterator[Header] = headers.iterator

  /** Attempt to get a [[org.http4s.Header]] of type key.HeaderT from this collection
    *
    * @param key [[HeaderKey.Extractable]] that can identify the required header
    * @return a scala.Option possibly containing the resulting header of type key.HeaderT
  *   @see [[Header]] object and get([[CaseInsensitiveString]])
    */
  def get(key: HeaderKey.Extractable): Option[key.HeaderT] = key.from(this)

  /** Attempt to get a [[org.http4s.Header.Raw]] from this collection of headers
    *
    * @param key name of the header to find
    * @return a scala.Option possibly containing the resulting [[org.http4s.Header.Raw]]
    *
    * @see [[HeaderKey.Default]] in conjunction with get([[HeaderKey]])
    */
  def get(key: CaseInsensitiveString): Option[Header.Raw] = {
    val k = new StringKey { override def name = key }
    get(k).map(_.toRaw)
  }

  /** Make a new collection adding the specified headers, replacing existing headers of the same name
    *
    * @param in multiple [[Header]] to append to the new collection
    * @return a new [[Headers]] containing the sum of the initial and input headers
    */
  def put(in: Header*): Headers = {
    if (in.isEmpty) this
    else {
      val b = new ListBuffer[Header] ++= in
      val n = b.prependToList(headers.filterNot(h => in.exists(_.name == h.name)))
      new Headers(n)
    }
  }
}

object Headers {
  val empty = apply()

  /** Create a new Headers collection from the headers */
  def apply(headers: Header*): Headers = Headers(headers.toList)

  /** Create a new Headers collection from the headers */
  def apply(headers: List[Header]): Headers = new Headers(headers)

  implicit def canBuildFrom: CanBuildFrom[Traversable[Header], Header, Headers] =
    new CanBuildFrom[TraversableOnce[Header], Header, Headers] {
      def apply(from: TraversableOnce[Header]): mutable.Builder[Header, Headers] = newBuilder

      def apply(): mutable.Builder[Header, Headers] = newBuilder
    }

  private def newBuilder: mutable.Builder[Header, Headers] =
    new mutable.ListBuffer[Header] mapResult (b => new Headers(b))
}
