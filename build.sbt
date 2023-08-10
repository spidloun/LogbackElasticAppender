
name := "LogbackElasticAppender"
version := "0.1.0"
organization := "net.pe3ny"

scalaVersion := "2.13.11"

// GitHub packages publishing
githubOwner := "spidloun"
githubRepository := "LogbackElasticAppender"
githubTokenSource := TokenSource.GitConfig("github.token")

libraryDependencies ++= Seq(
  // Logback classic core - must be >= 1.3.0
  "ch.qos.logback" % "logback-classic" % "1.3.0",
  // Akka streaming
  "com.typesafe.akka" %% "akka-stream" % "2.8.3",
  // Elastic4s
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "8.8.1",
  "com.sksamuel.elastic4s" %% "elastic4s-json-play" % "8.8.1",
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "8.8.1",
  // Unit testing
  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5" % "test",
)
