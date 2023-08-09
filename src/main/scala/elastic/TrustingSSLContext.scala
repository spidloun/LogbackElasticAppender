package elastic

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, SSLEngine, X509TrustManager}

object TrustingSSLContext {
  // Silenost povolujici self-trusted certs pri TLS
  val trustManager = new X509TrustManager {
    override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = ()
    override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = ()
    override def getAcceptedIssuers: Array[X509Certificate] = Array.empty[X509Certificate]
  }

  def newInstance: SSLContext = {
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, Array(trustManager), new SecureRandom())
    sslContext
  }
}
