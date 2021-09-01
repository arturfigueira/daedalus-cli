package com.github.daedalus.cli.connection

import com.google.common.collect.ImmutableList
import com.sksamuel.hoplite.ConfigLoader
import org.elasticsearch.client.RestHighLevelClient

class YamlConnectionProvider(configFilePath: String) : ConnectionProvider {
    private val config: ElasticSearch = ConfigLoader().loadConfigOrThrow(configFilePath)

    override fun provide(): RestHighLevelClient {
        val scheme = config.getScheme()
        scheme.isAllowed(config.authorization)
                || throw ConnectionException("Incompatible Authentication / Scheme ")

        if()
    }

    data class Instance(private val host: String, private val port: Int)

    sealed class Authorization {
        data class BasicAuthorization(val user: String, val password: String)
        data class PkcsAuthorization(val trustStore: String, val keyStore: String?)
        data class PemAuthorization(val caCertificate: String)
    }

    enum class Scheme(vararg authSchemes: Class<*>) {
        HTTP(Authorization.BasicAuthorization::class.java),
        HTTPS(
            Authorization.PkcsAuthorization::class.java,
            Authorization.PemAuthorization::class.java
        );

        private val authClazzs: List<Class<*>> = authSchemes.asList();

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
        fun getInstances(): List<Instance>? = ImmutableList.copyOf(this.instances)
        fun getScheme(): Scheme = Scheme.parse(this.scheme)
    }
}