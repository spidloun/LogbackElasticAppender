package net.pe3ny.elastic

case class ElasticException(cause: String) extends Exception(s"Elastic exception, cause: ${cause}")
