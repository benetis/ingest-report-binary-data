package task

import task.MyApp.WordCount

object Utils {

  def printResponseString(map: Map[EventType, WordCount]): String = {
    map
      .map { case (eventType, count) =>
        s"""$eventType:
         |${wordCountPrint(count).mkString(",")}
         |""".stripMargin
      }
      .mkString("\n\r")
  }

  private def wordCountPrint(wordCount: WordCount): Iterable[String] = {
    wordCount.map { case (str, i) => s"$str -> $i" }
  }

}
