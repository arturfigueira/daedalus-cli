package com.github.daedalus.cli.connection.yaml

import com.google.common.collect.ImmutableList

data class ElasticSearchConfig(
  private val scheme: String,
  val authorization: Authorization,
  private val instances: List<Instance>
) {
  fun getInstances(): List<Instance> = ImmutableList.copyOf(this.instances)
  fun getScheme(): Scheme = Scheme.parse(this.scheme)
}

data class Instance(val host: String, val port: Int)

sealed class Authorization {
  data class BasicAuthorization(val user: String, val password: String) : Authorization()

  data class PemAuthorization(val caCertificate: String) : Authorization()

  data class PkcsAuthorization(
    val trustStore: String,
    val trustStorePassword: String,
    val keyStore: String?,
    val keyStorePassword: String?
  ) : Authorization()
}

enum class Scheme(vararg authSchemes: Class<out Authorization>) {
  HTTP(Authorization.BasicAuthorization::class.java),
  HTTPS(Authorization.PkcsAuthorization::class.java, Authorization.PemAuthorization::class.java);

  private val authClazzs: List<Class<out Authorization>> = authSchemes.asList()

  fun isAllowed(obj: Any?) = authClazzs.stream().anyMatch { it.isInstance(obj) }

  fun asParameter() = this.name.lowercase()

  companion object Object {
    fun parse(name: String) = values().first { dt -> dt.name.equals(name, true) }
  }
}

