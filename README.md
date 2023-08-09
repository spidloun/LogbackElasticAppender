# LogbackElasticAppender
Logback appender implementation which sends logback events to a _bulk API endpoint of Elastic Search.

Features:
- uses Akka streaming & queue to buffer incoming events and send them in accumulated batches
- supports HTTP/HTTPs ELK endpoints
- hostname verification disabled & accepts all certificates - yes, very ugly and insecure, you have been warned
- supports basic HTTP auth
- includes logback`s Marker implementation to include custom JSON in log event in order to get custom fields at ELK stack

## Appender config example

```
    <appender name="CUSTOM" class="net.pe3ny.elastic.ElasticAppender">
        <elkUrl>https://whatever_elk_url:9200/_bulk</elkUrl>
        <elkIndex>brasius-test</elkIndex>
        <basicAuthUser>logger</basicAuthUser>
        <basicAuthPass>REDACTED</basicAuthPass>
        <accumulateItems>5</accumulateItems>
        <accumulatePeriod>5</accumulatePeriod>
        <debug>false</debug>
    </appender>
```

## Usage in code
