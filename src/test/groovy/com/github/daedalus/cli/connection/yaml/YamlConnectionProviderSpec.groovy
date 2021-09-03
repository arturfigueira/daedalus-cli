package com.github.daedalus.cli.connection.yaml

import com.github.daedalus.cli.connection.ConnectionException
import org.elasticsearch.client.RestClient
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class YamlConnectionProviderSpec extends Specification {

    @Shared
    def resourcePath = "/connection/yaml"

    @Unroll
    def "Wont provide when using incompatible authorization and scheme"() {
        given:
        def provider = new YamlConnectionProvider(configPath)

        when:
        provider.provide()

        then:
        thrown(ConnectionException)

        where:
        configPath << ["$resourcePath/invalid-basic-elastic.yml",
                       "$resourcePath/invalid-pem-elastic.yml",
                       "$resourcePath/invalid-pkcs-elastic.yml"]
    }

    @Unroll
    def "Should provides a #label Rest Client from #configPath"() {
        given:
        def provider = new YamlConnectionProvider(configPath)

        when:
        def actualConnection = provider.provide()

        then:
        actualConnection != null

        where:
        label                    | configPath
        "Basic authentication"   | "$resourcePath/basic-elastic.yml"
        "CA Cert authentication" | "$resourcePath/cacert-elastic.yml"
    }

    def "Should configure multiple hosts"() {
        given:
        def provider = new YamlConnectionProvider("$resourcePath/basic-elastic.yml")

        when:
        def actualConnection = provider.provide()

        then:
        ((RestClient) actualConnection.properties.get("lowLevelClient"))
                .nodes.collect { it.host.toURI() } ==
                ["http://localhost:9200", "http://localhost:9201"]
    }

}
