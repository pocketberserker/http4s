package org.http4s

import scalaz.concurrent.Task

package object client {

  /** Some syntactic sugar for making requests */
  implicit class ClientSyntax(request: Task[Request]) {
    def exec(implicit client: Client): Task[Response] = client.prepare(request)
    def collect[T](parser: Response => Task[T])(implicit client: Client): Task[T] =
      client.request(request, parser)
  }
}
