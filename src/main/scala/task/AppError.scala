package task
import ErrorUtils._

trait AppError {
  def msg(): String
}

case class BinaryPathError(throwable: Throwable) extends AppError {
  override def msg(): String =
    throwable.messageAndStacktrace("Check if binary path is correct")
}

case class GetRuntimeError(throwable: Throwable) extends AppError {
  override def msg(): String = throwable.messageAndStacktrace()
}

case class ExecuteBinaryError(throwable: Throwable) extends AppError {
  override def msg(): String = throwable.messageAndStacktrace()
}

object ErrorUtils {
  implicit class ThrowableExt(throwable: Throwable) {

    def messageAndStacktrace(customTitle: String = ""): String =
      s"""------------------------------
      $customTitle
      ------------------------------
      ${throwable.getLocalizedMessage} 
      ${throwable.stackToString()}
      """.split("\n").map(_.stripLeading()).mkString("\n")

    def stackToString(): String = {
      import java.io.PrintWriter
      import java.io.StringWriter
      val sw = new StringWriter
      val pw = new PrintWriter(sw)
      throwable.printStackTrace(pw)
      sw.toString
    }
  }
}
