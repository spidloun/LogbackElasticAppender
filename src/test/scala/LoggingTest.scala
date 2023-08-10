
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger, LoggerContext}
import ch.qos.logback.core.ConsoleAppender
import net.pe3ny.elastic.{ElasticAppender, PayloadMarker}
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.LoggerFactory

class LoggingTest extends AnyFunSuite {
  val logger = LoggerFactory.getLogger("TEST").asInstanceOf[Logger]
  logger.setLevel(Level.INFO)
  logger.info("TEST MESSAGE")
  logger.error(new PayloadMarker("props", Map("one" -> "two")), "PAYLOAD")
}
