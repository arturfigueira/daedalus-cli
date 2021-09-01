package com.github.daedalus.cli.connection

import com.google.common.collect.ImmutableList
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
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext


class YamlConnectionProvider(configFilePath: String) : ConnectionProvider {
    private val config: ElasticSearch = ConfigLoader().loadConfigOrThrow(configFilePath)

    override fun provide(): RestHighLevelClient {
        val scheme = config.getScheme()
        scheme.isAllowed(config.authorization)
                || throw ConnectionException("Incompatible Authentication / Scheme ")

        val httpHosts = config.getInstances().map { instance ->
            HttpHost(instance.host, instance.port, scheme.asParameter())
        }

        when (config.authorization) {
            is Authorization.BasicAuthorization -> {
                val credentialsProvider: CredentialsProvider = BasicCredentialsProvider()
                credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    UsernamePasswordCredentials(
                        config.authorization.user,
                        config.authorization.password
                    )
                )

                val setupCallback = { httpAsyncClientBuilder: HttpAsyncClientBuilder ->
                    httpAsyncClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                }
                return RestHighLevelClient(
                    RestClient.builder(*httpHosts.toTypedArray())
                        .setHttpClientConfigCallback(setupCallback)
                )
            }

            is Authorization.PkcsAuthorization -> {
                val trustStorePath = Paths.get(config.authorization.trustStore)
                val truststore = KeyStore.getInstance("pkcs12")

                Files.newInputStream(trustStorePath).use { `is` ->
                    truststore.load(
                        `is`,
                        config.authorization.password.toCharArray()
                    )
                }
                val sslBuilder: SSLContextBuilder = SSLContexts.custom()
                    .loadTrustMaterial(truststore, null)

                val sslContext = sslBuilder.build()
                val setupCallback: (httpClientBuilder: HttpAsyncClientBuilder) -> HttpAsyncClientBuilder =
                    { httpClientBuilder ->
                        httpClientBuilder.setSSLContext(
                            sslContext
                        )
                    }

                return RestHighLevelClient(
                    RestClient.builder(*httpHosts.toTypedArray())
                        .setHttpClientConfigCallback(setupCallback)
                )
            }

            is Authorization.PemAuthorization -> {
                val caCertificatePath = Paths.get(config.authorization.caCertificate)

                val factory = CertificateFactory.getInstance("X.509")

                var trustedCa: Certificate?

                Files.newInputStream(caCertificatePath).use { `is` ->
                    trustedCa = factory.generateCertificate(`is`)
                }

                val trustStore = KeyStore.getInstance("pkcs12")
                trustStore.load(null, null)
                trustStore.setCertificateEntry("ca", trustedCa)

                val sslContextBuilder = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null)
                val sslContext: SSLContext = sslContextBuilder.build()

                val setupCallBack: (httpClientBuilder: HttpAsyncClientBuilder) -> HttpAsyncClientBuilder =
                    { httpClientBuilder ->
                        httpClientBuilder.setSSLContext(
                            sslContext
                        )
                    }

                return RestHighLevelClient(
                    RestClient.builder(*httpHosts.toTypedArray())
                        .setHttpClientConfigCallback(setupCallBack)
                )
            }
        }
    }

    data class Instance(val host: String, val port: Int)

    sealed class Authorization {
        data class BasicAuthorization(val user: String, val password: String) : Authorization()

        data class PkcsAuthorization(
            val trustStore: String,
            val password: String,
            val keyStore: String?
        ) :
            Authorization()

        data class PemAuthorization(val caCertificate: String) : Authorization()
    }

    enum class Scheme(vararg authSchemes: Class<out Authorization>) {
        HTTP(Authorization.BasicAuthorization::class.java),
        HTTPS(
            Authorization.PkcsAuthorization::class.java,
            Authorization.PemAuthorization::class.java
        );

        private val authClazzs: List<Class<out Authorization>> = authSchemes.asList();

        fun isAllowed(obj: Any?) = authClazzs.stream().anyMatch { it.isInstance(obj) }

        fun asParameter() = this.name.lowercase()

        companion object Object {
            fun parse(name: String): Scheme {
                return values().first { dt -> dt.name.equals(name, true) }
            }
        }
    }

    data class ElasticSearch(
        private val scheme: String,
        val authorization: Authorization,
        private val instances: List<Instance>
    ) {
        fun getInstances(): List<Instance> = ImmutableList.copyOf(this.instances)
        fun getScheme(): Scheme = Scheme.parse(this.scheme)
    }
}