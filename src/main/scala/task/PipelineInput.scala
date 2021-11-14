package task

import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}
import zio._
import zio.console.putStrLn
import zio.stream.{ZSink, ZStream, ZTransducer}
import java.lang.{Runtime => JavaRuntime}
import JsonCodecs._
import task.MyApp.AppState
import cats.implicits._

object PipelineInput {

  val binaryPath = Task(getClass.getResource("/input.exe").getPath)
    .mapError(BinaryPathError)

  val process: ZManaged[Any, AppError, Process] = ZManaged
    .make {
      for {
        path <- binaryPath
        run <- Task(JavaRuntime.getRuntime).mapError(GetRuntimeError)
        res <- Task(run.exec(path)).mapError(ExecuteBinaryError)
      } yield res
    }(p => UIO(p.destroyForcibly()))

  val ingestPipelineProgram = (state: AppState) => {
    putStrLn("===== Starting to ingest the input =====") *> process.use {
      ps: Process =>
        val chunkIO = ingest(ps)
          .mapM(parseJson)
          .collect { case JsSuccess(value, _) => value }
          .grouped(3)
          .mapM(chunk => updateMap(state, chunk))
          .run(ZSink.drain)

        chunkIO.unit
    }
  }

  def ingest(process: Process) =
    ZStream
      .fromInputStream(process.getInputStream)
      .transduce(ZTransducer.utf8Decode >>> ZTransducer.splitLines)

  private def updateMap(state: AppState, chunk: Chunk[OneEvent]) = {
    val toUpdate =
      chunk.groupBy(_.eventType).map { case (k, v) => (k, v.size) }

    /* Merge two maps */
    state.update(old => old.combine(toUpdate))
  }

  private def parseJson(
      stringToParse: String
  ): ZIO[Any, Nothing, JsResult[OneEvent]] = {
    Task(Json.fromJson[OneEvent](Json.parse(stringToParse))(eventReads))
      .catchAll(throwable => UIO(JsError.apply(throwable.getLocalizedMessage)))
  }

}
