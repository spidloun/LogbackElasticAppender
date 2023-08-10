package net.pe3ny.elastic

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorAttributes, OverflowStrategy, Supervision}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, OffsetDateTime, ZoneId}
import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.util.Try

class ElasticAppender extends AppenderBase[ILoggingEvent] {
  private var elasticClient: Option[ElasticClient] = None
  implicit private val actorSystem: ActorSystem = ActorSystem().classicSystem
  implicit private val executionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(2))

  import com.sksamuel.elastic4s.ElasticDsl._

  // Properties
  var url = "NOPE"
  var index = ""
  var bufferSize = 1000
  var accumulateItems = 5
  var accumulatePeriod = 5
  var basicAuthUser = "USER"
  var basicAuthPass = "PASS"
  var debug = false
  var appendDateToIndex = false

  // Setters pro Java-bean-like konfiguraci
  def setUrl(url: String): Unit = this.url = url

  def setIndex(idx: String): Unit = index = idx

  def setBufferSize(size: Int): Unit = bufferSize = size

  def setAccumulateItems(count: Int): Unit = accumulateItems = count

  def setAccumulatePeriod(seconds: Int): Unit = accumulatePeriod = seconds

  def setBasicAuthUser(user: String): Unit = basicAuthUser = user

  def setBasicAuthPass(pass: String): Unit = basicAuthPass = pass

  def setDebug(d: Boolean): Unit = debug = d

  def setAppendDateToIndex(a: Boolean): Unit = appendDateToIndex = a

  private var sendQueue: Option[SourceQueueWithComplete[ILoggingEvent]] = None

  case class ElasticMessagePayload(name: String, items: Map[String, Any])

  case class ElasticMessage(stamp: OffsetDateTime, logger: String, level: String, message: String, payload: Option[ElasticMessagePayload] = None) {
    def toMap: Map[String, Any] = Map[String, Any](
      "@timestamp" -> stamp.toString,
      "logger" -> logger,
      "level" -> level,
      "message" -> message
    ) ++ payload.map { load =>
      Map(load.name -> load.items)
    }.getOrElse(Map.empty)
  }

  override def isStarted: Boolean = super.isStarted && sendQueue.isDefined

  override def start(): Unit = {
    super.start()
    elasticClient = Some(ElasticClient(JavaClient(ElasticProperties(url), httpClientConfigCallback = HTTPClientConfig.httpTrustingAsyncClientCallback(basicAuthUser, basicAuthPass))))
    sendQueue = Some(
      Source.queue[ILoggingEvent](bufferSize, OverflowStrategy.backpressure)
        .map[ElasticMessage] { event =>
          ElasticMessage(
            OffsetDateTime.ofInstant(event.getInstant, ZoneId.systemDefault),
            event.getLoggerName,
            event.getLevel.toString,
            event.getMessage,
            Option(event.getMarkerList).flatMap(_.asScala.collectFirst {
              case marker: PayloadMarker => marker
            }.map(mk => ElasticMessagePayload(mk.getName, mk.getItems)))
          )
        }
        .groupedWithin(accumulateItems, accumulatePeriod.seconds)
        .map { msgs =>
          val client = elasticClient match {
            case Some(client) => client
            case None => throw ElasticException("Appender is not started yet, no Elastic client available")
          }
          val msgRequest =
            bulk(msgs.map(m => indexInto(appendDateToIndex match {
              case true => s"${index}_${LocalDate.now.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
              case false => index
            }).fields(m.toMap)))
          if (debug) {
            client.show(msgRequest)
          }
          client.execute(msgRequest)
        }
        .to(Sink.foreach { req =>
          Try(req.await)
            .map {
              case resp if resp.isSuccess => addInfo(s"Elastic request sent, responseBody: ${resp.body.getOrElse("N/A")}")
              case resp if resp.isError => throw ElasticException(s"Elastic responded with errors: ${resp.error.toString}")
              case _ => addInfo("Elastic request sent with unknown result")
            }
            .recover {
              case ex: Throwable => addError(s"An exception ocurred while sending", ex)
            }
        })
        .addAttributes(ActorAttributes.supervisionStrategy({
          ex: Throwable => addError(s"An exception occurred while processing log stream", ex); Supervision.Resume
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
