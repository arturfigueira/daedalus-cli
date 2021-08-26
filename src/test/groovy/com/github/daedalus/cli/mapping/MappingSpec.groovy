package com.github.daedalus.cli.mapping

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class MappingSpec extends Specification {

    @Shared Mapping mapping

    def setup() {
        mapping = new Mapping()
    }

    @Unroll
    def "Adding a invalid property will throw"(key, datatype) {
        when:
        mapping.addProperty(key, datatype)

        then:
        thrown(MappingException)

        where:
        key  | datatype
        ""   | "boolean"
        "  " | "boolean"
        "id" | ""
        "id" | "   "
        "id" | "ABCDFEG"
    }

    @Unroll
    def "Adding properties where one is invalid will throw"(){
        when:
        mapping.addProperties(properties)

        then:
        thrown(MappingException)

        where:
        properties << [
                ["id": "text", "" : "boolean"],
                ["id": "text", "age" : ""],
                ["id": "text", "age" : "ABCDF"]
        ]
    }
}
