
name := "LogbackElasticAppender"
version := "0.0.1"
organization := "net.pe3ny"

scalaVersion := "2.13.11"

libraryDependencies ++= Seq(
  // JSON lib
  "com.typesafe.play" %% "play-json" % "2.9.4",
  // Akka streaming
  "com.typesafe.akka" %% "akka-stream" % "2.8.3",
  // HTTP/s client
  "com.softwaremill.sttp.client3" %% "core" % "3.0.0-RC7",
  "com.softwaremill.sttp.client3" %% "akka-http-backend" % "3.8.16",
  "ch.qos.logback" % "logback-classic" % "1.3.0"
)
