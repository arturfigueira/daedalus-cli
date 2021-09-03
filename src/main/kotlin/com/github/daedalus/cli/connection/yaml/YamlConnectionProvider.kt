package com.github.daedalus.cli.connection.yaml

import com.github.daedalus.cli.connection.ConnectionException
import com.github.daedalus.cli.connection.ConnectionProvider
import com.sksamuel.hoplite.ConfigLoader
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.ssl.SSLContextBuilder
import org.apache.http.ssl.SSLContexts
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext


class YamlConnectionProvider(configFilePath: String) : ConnectionProvider {
  private val rootFolder: Path = Path.of(configFilePath).parent
  private val config: ElasticSearchConfig = ConfigLoader().loadConfigOrThrow(configFilePath)

  override fun provide(): RestHighLevelClient {
    val scheme = config.getScheme()

    scheme.isAllowed(config.authorization)
        || throw ConnectionException("Incompatible Authentication / Scheme ")

    val httpHosts = config.getInstances().map { instance ->
      HttpHost(instance.host, instance.port, scheme.asParameter())
    }

    return when (config.authorization) {
      is Authorization.BasicAuthorization -> provideBasicClient(config.authorization, httpHosts)

      is Authorization.PkcsAuthorization -> providePkcsClient(config.authorization, httpHosts)

      is Authorization.PemAuthorization -> providePemClient(config.authorization, httpHosts)
    }
  }

  private fun providePemClient(
    authorization: Authorization.PemAuthorization,
    httpHosts: List<HttpHost>
  ): RestHighLevelClient {
    val caCertificate = resolveResourcePath(authorization.caCertificate)

    val factory = CertificateFactory.getInstance("X.509")
    var trustedCa: Certificate?
    Files.newInputStream(caCertificate).use { `is` ->
      trustedCa = factory.generateCertificate(`is`)
    }

    val trustStore = KeyStore.getInstance("pkcs12")
    trustStore.load(null, null)
    trustStore.setCertificateEntry("ca", trustedCa)

    val sslContextBuilder = SSLContexts.custom().loadTrustMaterial(trustStore, null)
    val sslContext: SSLContext = sslContextBuilder.build()

    val setupCallBack: (httpClientBuilder: HttpAsyncClientBuilder) -> HttpAsyncClientBuilder =
      { httpClientBuilder ->
        httpClientBuilder.setSSLContext(
          sslContext
        )
      }

    return setupClient(httpHosts, setupCallBack)
  }

  private fun providePkcsClient(
    authorization: Authorization.PkcsAuthorization,
    httpHosts: List<HttpHost>
  ): RestHighLevelClient {
    val trustStorePath = resolveResourcePath(authorization.trustStore)

    val trustStore = KeyStore.getInstance("pkcs12")
    Files.newInputStream(trustStorePath).use { `is` ->
      trustStore.load(`is`, authorization.trustStorePassword.toCharArray())
    }

    var sslBuilder: SSLContextBuilder = SSLContexts.custom()
      .loadTrustMaterial(trustStore, null)


    authorization.keyStore?.let {
      val keyStorePath = resolveResourcePath(it)
      val keyStore = KeyStore.getInstance("pkcs12")
      val keyStorePassword = authorization.keyStorePassword?.toCharArray()

      Files.newInputStream(keyStorePath).use { `is` ->
        keyStore.load(`is`, keyStorePassword)
      }

      sslBuilder = SSLContexts.custom()
        .loadTrustMaterial(trustStore, null)
        .loadKeyMaterial(keyStore, keyStorePassword)
    }

    val sslContext = sslBuilder.build()
    val setupCallback: (httpClientBuilder: HttpAsyncClientBuilder) -> HttpAsyncClientBuilder =
      { httpClientBuilder -> httpClientBuilder.setSSLContext(sslContext) }

    return setupClient(httpHosts, setupCallback)
  }

  private fun provideBasicClient(
    authorization: Authorization.BasicAuthorization,
    httpHosts: List<HttpHost>
  ): RestHighLevelClient {
    val credentialsProvider: CredentialsProvider = BasicCredentialsProvider()
    val usernamePasswordCredentials = UsernamePasswordCredentials(
      authorization.user,
      authorization.password
    )

    credentialsProvider.setCredentials(AuthScope.ANY, usernamePasswordCredentials)

    val setupCallback = { httpAsyncClientBuilder: HttpAsyncClientBuilder ->
      httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
    }

    return setupClient(httpHosts, setupCallback)
  }

  private fun resolveResourcePath(resource : String) : Path{
    val resourcePath = Paths.get(resource)
    return if (resourcePath.isAbsolute) resourcePath else resolveFromClassPath(resource)
  }

  private fun resolveFromClassPath(resource: String) =
    Path.of(
      this::class.java.classLoader.getResource(
        rootFolder.resolve(resource).toString()
      )?.toURI()
    )

  private fun setupClient(
    httpHosts: List<HttpHost>,
    setupCallback: (HttpAsyncClientBuilder) -> HttpAsyncClientBuilder
  ) = RestHighLevelClient(
    RestClient.builder(*httpHosts.toTypedArray()).setHttpClientConfigCallback(setupCallback)
  )
}