package task
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class EventType(value: String)
case class OneEvent(eventType: EventType, data: String, timestamp: Long)

object JsonCodecs {

  val eventReads: Reads[OneEvent] =
    ((JsPath \ "event_type").read[String].map(EventType) and
      (JsPath \ "data").read[String] and
      (JsPath \ "timestamp").read[Long]).apply(OneEvent.apply _)

}
