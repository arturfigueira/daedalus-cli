package com.github.daedalus.cli.connection.yaml

import org.elasticsearch.client.RestClient
import spock.lang.Specification
import spock.lang.Unroll

class YamlConnectionProviderSpec extends Specification {

    @Unroll
    def "Should provides a #label Rest Client from #configPath"() {
        given:
        def provider = new YamlConnectionProvider(configPath)

        when:
        def actualConnection = provider.provide()

        then:
        actualConnection != null

        where:
        label                  | configPath
        "Basic authentication" | "/connection/yaml/basic-elastic.yml"
    }

    def "Should configure multiple hosts"() {
        given:
        def provider = new YamlConnectionProvider("/connection/yaml/basic-elastic.yml")

        when:
        def actualConnection = provider.provide()

        then:
        ((RestClient) actualConnection.properties.get("lowLevelClient"))
                .nodes.collect { it.host.toURI() } ==
                ["http://localhost:9200", "http://localhost:9201"]
    }

}
