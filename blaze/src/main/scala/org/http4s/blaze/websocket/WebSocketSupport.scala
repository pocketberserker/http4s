package org.http4s.blaze
package websocket

import org.http4s._
import http.websocket.{WSFrameAggregator, WebSocketDecoder, ServerHandshaker}
import org.http4s.Header.{`Content-Length`, Connection}
import org.http4s.util.CaseInsensitiveString._

import scalaz.stream.Process
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets._
import org.http4s.Response
import scala.util.{Failure, Success}
import org.http4s.blaze.pipeline.LeafBuilder
import scodec.bits.ByteVector

trait WebSocketSupport extends Http1ServerStage {
  override protected def renderResponse(req: Request, resp: Response): Unit = {
    val ws = resp.attributes.get(org.http4s.websocket.websocketKey)
    logger.debug(s"Websocket key: $ws\nRequest headers: " + req.headers)

    if (ws.isDefined) {
      val hdrs =  req.headers.map(h=>(h.name.toString,h.value))
      if (ServerHandshaker.isWebSocketRequest(hdrs)) {
        ServerHandshaker.handshakeHeaders(hdrs) match {
          case Left((code, msg)) =>
            logger.info(s"Invalid handshake $code, $msg")
            val body = Process.emit(ByteVector(msg.toString.getBytes(req.charset.charset)))
            val headers = Headers(`Content-Length`(msg.length),
                                   Connection("close".ci),
                                   Header.Raw(Header.`Sec-WebSocket-Version`.name, "13"))

            val rsp = Response(status = Status.BadRequest, body = body, headers = headers)
            super.renderResponse(req, rsp)

          case Right(hdrs) =>
            logger.trace("Successful handshake")
            val sb = new StringBuilder
            sb.append("HTTP/1.1 101 Switching Protocols\r\n")
            hdrs.foreach { case (k, v) => sb.append(k).append(": ").append(v).append('\r').append('\n') }
            sb.append('\r').append('\n')

            // write the accept headers and reform the pipeline
            channelWrite(ByteBuffer.wrap(sb.result().getBytes(US_ASCII))).onComplete {
              case Success(_) =>
                logger.trace("Switching pipeline segments.")

                val segment = LeafBuilder(new Http4sWSStage(ws.get))
                              .prepend(new WSFrameAggregator)
                              .prepend(new WebSocketDecoder(false))

                this.replaceInline(segment)

              case Failure(t) => fatalError(t, "Error writing Websocket upgrade response")
            }
        }

      } else super.renderResponse(req, resp)
    } else super.renderResponse(req, resp)
  }
}
