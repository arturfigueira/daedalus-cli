package com.github.daedalus.cli.mapping

import spock.lang.Specification
import spock.lang.Unroll

class DataTypeSpec extends Specification {

    @Unroll
    def "Parse will throw when parse fails to find a datatype for given name"(name) {
        given:
        def obj = new DataType.Object()

        when:
        obj.parse(name)

        then:
        thrown(MappingException)

        where:
        name << ["", "   ", "123-ABC"]
    }

    @Unroll
    def "Parse will return the correct datatype by name"(name, datatype) {
        given:
        def obj = new DataType.Object()

        when:
        def actual = obj.parse(name)

        then:
        actual == datatype

        where:
        name         | datatype
        "boolean"    | DataType.Boolean
        "Boolean"    | DataType.Boolean
        "BOOLEAN"    | DataType.Boolean
        "Ip"         | DataType.Ip
        "Keyword"    | DataType.Keyword
        "completion" | DataType.Completion
        "date"       | DataType.Date
        "text"       | DataType.Text
        "long"       | DataType.Long
        "integer"    | DataType.Integer
        "short"      | DataType.Short
        "byte"       | DataType.Byte
        "double"     | DataType.Double
        "float"      | DataType.Float
    }
}
