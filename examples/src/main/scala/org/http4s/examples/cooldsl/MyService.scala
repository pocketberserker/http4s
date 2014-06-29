package org.http4s.examples.cooldsl

import org.http4s.cooldsl._
import org.http4s.cooldsl.swagger.SwaggerSupport
import org.http4s.cooldsl.Decoder

/**
 * Created by Bryce Anderson on 5/9/14.
 */
class MyService extends CoolService with SwaggerSupport {

  GET / "hello" / parse[String] ^ "Says hello" |>>> { (s: String) => s"Hello world: $s" }

  GET / "needQuery" -? query[Int]("id") |>>> { i: Int => s"Received an int: $i" }

  (POST / "post" decoding(Decoder.strDec)) |>>> { s: String => s"Received a strong: $s" }

//  for {
//    i <- 0 until 2
//    j <- 0 until 2
//    k <- 0 until 2
//    z <- 0 until 2 } {
//    Get / s"route_$i" / s"route_$j" / s"route_$k" / s"route_$z" / parse[String] |>>> { (s: String) =>
//      s"Route ($i, $j, $k, $z), $s"
//    }
//  }

}
