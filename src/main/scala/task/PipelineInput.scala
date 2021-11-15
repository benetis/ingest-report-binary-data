package task

import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}
import zio._
import zio.console.putStrLn
import zio.stream.{ZSink, ZStream, ZTransducer}

import java.lang.{Runtime => JavaRuntime}
import JsonCodecs._
import task.MyApp.{AppState, WordCount}
import cats.implicits._
import java.io.File

object PipelineInput {

  val binaryPath = Task(new File("src/main/resources/input.x64").getPath)
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
          .tap(e => putStrLn(s"$e"))
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
      chunk.groupBy(_.eventType).map { case (k, v) => (k, chunkToWordCount(v)) }

    /* Merge two maps */
    state.update(old => old.combine(toUpdate))
  }

  private def chunkToWordCount(chunk: Chunk[OneEvent]): WordCount = {
    chunk.map(_.data).groupBy(identity).view.mapValues(_.size).toMap
  }

  private def parseJson(
      stringToParse: String
  ): ZIO[Any, Nothing, JsResult[OneEvent]] = {
    Task(Json.fromJson[OneEvent](Json.parse(stringToParse))(eventReads))
      .catchAll(throwable => UIO(JsError.apply(throwable.getLocalizedMessage)))
  }

}
