package net.pe3ny.elastic

import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy
import org.apache.http.ssl.SSLContexts
import org.elasticsearch.client.RestClientBuilder

import javax.net.ssl.SSLSession

object HTTPClientConfig {
  private val sslContext = SSLContexts
    .custom()
    .loadTrustMaterial(new TrustSelfSignedStrategy())
    .build()

  // A hostname verifier which trusts all hostnames in all ssl sessions.
  private object trustAllHostnameVerifier extends javax.net.ssl.HostnameVerifier {
    def verify(h: String, s: SSLSession) = true
  }

  // We cannot simply use the hostname verifier since the SSL strategy overrides
  // the hostname verifier when HttpAsyncClient is built.
  private val sslSessionStrategy = new SSLIOSessionStrategy(
    sslContext,
    trustAllHostnameVerifier
  )

  /**
   * Basic HTTP authentication provider
   * @param userName HTTP username
   * @param password HTTP password
   * @return
   */
  private def basicAuthProvider(userName: String, password: String) = {
    val provider = new BasicCredentialsProvider
    provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password))
    provider
  }

  def httpTrustingAsyncClientCallback(basicAuthUser: String, basicAuthPass: String): RestClientBuilder.HttpClientConfigCallback = new RestClientBuilder.HttpClientConfigCallback {
    override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
      httpClientBuilder.setSSLStrategy(sslSessionStrategy)
      httpClientBuilder.setDefaultCredentialsProvider(basicAuthProvider(basicAuthUser, basicAuthPass))
    }
  }
}
