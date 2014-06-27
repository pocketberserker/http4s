package org.http4s
package blaze

import org.http4s.blaze.util.ProcessWriter
import scodec.bits.ByteVector
import scala.concurrent.{ExecutionContext, Future}
import scalaz.stream.Process
import scalaz.concurrent.Task
import java.nio.ByteBuffer
import org.http4s.blaze.pipeline.TailStage
import scala.util.{Failure, Success}

/**
 * Created by Bryce Anderson on 6/7/14.
 */

/** Discards the body, killing it so as to clean up resources
  *
  * @param headers ByteBuffer representation of [[Headers]] to send
  * @param pipe the blaze [[TailStage]] which takes ByteBuffers which will send the data downstream
  * @param ec an ExecutionContext which will be used to complete operations
  */
class BodylessWriter(headers: ByteBuffer, pipe: TailStage[ByteBuffer], close: Boolean)
                    (implicit protected val ec: ExecutionContext) extends ProcessWriter {

  private lazy val doneFuture = Future.successful( () )

  override def requireClose(): Boolean = close

  /** Doesn't write the process, just the headers and kills the process, if an error if necessary
    *
    * @param p Process[Task, Chunk] that will be killed
    * @return the Task which when run will send the headers and kill the body process
    */
  override def writeProcess(p: Process[Task, ByteVector]): Task[Unit] = Task.async[Unit] { cb =>
    pipe.channelWrite(headers).onComplete {
      case Success(_) => p.kill.run.runAsync(cb)
      case Failure(t) => p.killBy(t).run.runAsync(cb)
    }
  }

  override protected def writeEnd(chunk: ByteVector): Future[Unit] = doneFuture

  override protected def writeBodyChunk(chunk: ByteVector, flush: Boolean): Future[Unit] = doneFuture
}
