package task

import zio._
import zhttp.http._
import zhttp.service.Server
import zio.console._

object MyApp extends zio.App {

  type Count = Int
  type AppState = Ref[Map[EventType, Count]]

  val routes = (state: AppState) =>
    Http.collectM[Request] { case Method.GET -> Root / "count" =>
      state.get
        .map(s => Response.text(s.toString()))
    }

  val httpApp = (state: AppState) =>
    Server.start(8090, routes(state)).mapError(HttpServerError)

  val wholeProgram = for {
    state <- Ref.make(Map.empty[EventType, Count])
    _ <- PipelineInput
      .ingestPipelineProgram(state)
      .catchSome { case err: AppError =>
        putStrLn(err.getClass.getName) *> putStrLn(err.msg())
      }
      .fork
    startHttpApp <- httpApp(state).forkDaemon
    _ <- startHttpApp.join
  } yield ()

  def run(args: List[String]) = wholeProgram.exitCode

}
