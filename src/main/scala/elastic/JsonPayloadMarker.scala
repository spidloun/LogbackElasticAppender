package elastic

import org.slf4j.Marker
import play.api.libs.json.JsValue

import java.util

class JsonPayloadMarker(name: String, jsonPayload: JsValue) extends Marker {
  override def getName: String = name
  def getPayload: JsValue = jsonPayload

  /** Not used */
  override def add(reference: Marker): Unit = ()
  override def remove(reference: Marker): Boolean = false
  override def hasChildren: Boolean = hasReferences
  override def hasReferences: Boolean = false
  override def iterator(): util.Iterator[Marker] = new util.ArrayList[Marker]().iterator()
  override def contains(other: Marker): Boolean = false
  override def contains(name: String): Boolean = false
}
