package net.pe3ny.elastic

import akka.actor.ActorSystem
import akka.http.scaladsl.ConnectionContext
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorAttributes, OverflowStrategy, Supervision}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import play.api.libs.json.{JsObject, JsValue, Json}
import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.client3.basicRequest
import sttp.model.Uri
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.IterableHasAsScala

class ElasticAppender extends AppenderBase[ILoggingEvent] {
  implicit private val actorSystem: ActorSystem = ActorSystem().classicSystem
  implicit private val executionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(2))
  private val sslEngine = (host: String, port: Int) => {
    val engine = TrustingSSLContext.newInstance.createSSLEngine(host, port)
    engine.setUseClientMode(true)
    engine
  }
  private val httpsContext = ConnectionContext.httpsClient(sslEngine)
  private val httpBackend =
    AkkaHttpBackend.usingActorSystem(actorSystem, customHttpsContext = Some(httpsContext))

  // Properties
  var elkUrl = "NOPE"
  var elkIndex = ""
  var bufferSize = 1000
  var accumulateItems = 5
  var accumulatePeriod = 5
  var basicAuthUser = "USER"
  var basicAuthPass = "PASS"
  var debug = false

  // Setters pro Java-bean-like konfiguraci
  def setElkUrl(url: String): Unit = elkUrl = url
  def setElkIndex(idx: String): Unit = {
    // Nastavi index pomoci setteru a provede substituci data
    elkIndex = idx.replaceAll("%date", LocalDate.now.format(DateTimeFormatter.ISO_LOCAL_DATE))
  }
  def setBufferSize(size: Int): Unit = bufferSize = size
  def setAccumulateItems(count: Int): Unit = accumulateItems = count
  def setAccumulatePeriod(seconds: Int): Unit = accumulatePeriod = seconds
  def setBasicAuthUser(user: String): Unit = basicAuthUser = user
  def setBasicAuthPass(pass: String): Unit = basicAuthPass = pass
  def setDebug(d: Boolean): Unit = debug = d

  private var sendQueue: Option[SourceQueueWithComplete[ILoggingEvent]] = None

  case class ElasticMessage(logger: String, level: String, message: String, jsonPayload: Option[(String, JsValue)] = None) {
    def toJson: JsObject = {
      val basicJson = Json.obj(
        "@timestamp" -> LocalDateTime.now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        "logger" -> logger,
        "level" -> level,
        "message" -> message,
      )
      jsonPayload match {
        case Some(payload) => basicJson.deepMerge(Json.obj(payload._1 -> payload._2))
        case None => basicJson
      }
    }
  }

  override def isStarted: Boolean = super.isStarted && sendQueue.isDefined

  override def start(): Unit = {
    super.start()
    sendQueue = Some(
      Source.queue[ILoggingEvent](bufferSize, OverflowStrategy.backpressure)
        .map[ElasticMessage] { event =>
          ElasticMessage(
            event.getLoggerName,
            event.getLevel.toString,
            event.getMessage,
            Option(event.getMarkerList).flatMap(_.asScala.collectFirst {
              case marker: JsonPayloadMarker => marker
            }.map(mk => (mk.getName, mk.getPayload)))
          )
        }
        .groupedWithin(accumulateItems, accumulatePeriod.seconds)
        .map[String] { msgs =>
          msgs.map {
            case (m: ElasticMessage) =>
              Json.obj("index" -> Json.obj(
                "_index" -> elkIndex
              )).toString() + "\n" + m.toJson.toString()
          }.mkString("\n") + "\n"
        }
        .map[String]{body => if(debug) addInfo(s"REQUEST: ${body}"); body}
        .to(Sink.foreach[String] { body =>
          Uri.parse(elkUrl) match {
            case Left(err) => addError(s"Cannot parse ELK URL from configuration url: ${elkUrl}, error: ${err}")
            case Right(uri) => basicRequest
              .post(uri)
              .header("Content-Type", "application/json")
              .auth.basic(basicAuthUser, basicAuthPass)
              .body(body)
              .send(httpBackend)
              .map { response =>
                response.code match {
                  case x if x.isSuccess => {
                    if (debug) addInfo(s"RESPONSE: ${response.body}")
                    ()
                  }
                  case x => addError(s"An error ocurred while sending message to ELK API code: ${x.code}, body: ${response.body}")
                }
              }
            .recover { ex =>
              addError(s"An exception occurred while sending message to ELK API, ex: ${ex.getMessage}", ex)
            }
          }
        })
        .addAttributes(ActorAttributes.supervisionStrategy({
          ex: Throwable => addError(s"An exception occurred while processing log stream, ex: ${ex.getMessage}", ex); Supervision.Resume
        }))
        .run()
    )
  }

  override def stop(): Unit = {
    super.stop()
    sendQueue = None
  }

  override def append(event: ILoggingEvent): Unit =
    sendQueue
      .map(_ offer event)
}
