package net.pe3ny.elastic

import org.slf4j.Marker

import java.util

class PayloadMarker(name: String, items: Map[String, Any]) extends Marker {
  override def getName: String = name
  def getItems: Map[String, Any] = items

  /** Not used */
  override def add(reference: Marker): Unit = ()
  override def remove(reference: Marker): Boolean = false
  override def hasChildren: Boolean = hasReferences
  override def hasReferences: Boolean = false
  override def iterator(): util.Iterator[Marker] = new util.ArrayList[Marker]().iterator()
  override def contains(other: Marker): Boolean = false
  override def contains(name: String): Boolean = false
}
