# LogbackElasticAppender
Logback appender implementation which sends logback events to a _bulk API endpoint of Elastic Search.

Features:
- multiple ELK nodes supported (since version 0.1.0)
- uses Akka streaming & queue to buffer incoming events and send them in accumulated batches
- supports HTTP/HTTPs ELK endpoints
- hostname verification disabled & accepts all certificates - yes, very ugly and insecure, you have been warned
- supports basic HTTP auth
- includes logback`s Marker implementation to include custom JSON in log event in order to get custom fields at ELK stack
- uses elastic4s as backend for contacting ELK (since version 0.1.0)
- ability to format index name - as for now only date appendix is supported

## Appender config example

```
    <appender name="CUSTOM" class="net.pe3ny.elastic.ElasticAppender">
        <url>http://node1,https:node2</url>
        <index>indexName</index>
        <basicAuthUser>logger</basicAuthUser>
        <basicAuthPass>REDACTED</basicAuthPass>
        <accumulateItems>5</accumulateItems>
        <accumulatePeriod>5</accumulatePeriod>
        <debug>false</debug>
        <appendDateToIndex>true</appendDateToIndex>
    </appender>
```

## Usage in code

Usage via Typesafe`s logger. Sending simple log and log with payload.

```
  val logger = LoggerFactory.getLogger("TEST").asInstanceOf[Logger]
  logger.setLevel(Level.INFO)
  logger.info("TEST MESSAGE")
  logger.error(new PayloadMarker("props", Map("one" -> "two")), "PAYLOAD")
```
