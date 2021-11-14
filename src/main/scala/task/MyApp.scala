package task

import zio._
import zio.Console._
import zio.stream._
import ErrorUtils._
import java.lang.{Runtime => JavaRuntime}

object MyApp extends ZIOAppDefault {

  def run = program.catchSome { case err: AppError =>
    printLine(err.getClass) *> printLine(err.msg())
  }

  val binaryPath = Task(getClass.getResource("/input.exe").getPath)
    .mapError(BinaryPathError)

  val program =
    for {
      _ <- printLine("===== Starting to ingest the input =====")
      ps <- process
      _ <- transform(ps).run(ZSink.foreach(in => {
        printLine(in)
      }))
    } yield ()

  def transform(process: Process) =
    ZStream
      .fromInputStream(process.getInputStream)
      .via(ZPipeline.utf8Decode)
      .via(ZPipeline.splitLines)

  val process =
    for {
      runtime <- Task(JavaRuntime.getRuntime).mapError(GetRuntimeError)
      path <- binaryPath
      exec <- Task(runtime.exec(path)).mapError(ExecuteBinaryError)
    } yield exec

}
