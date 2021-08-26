package com.github.daedalus.cli.mapping

/**
 * Represents an index property mapping
 *
 * For more details about an Elastic Search index mapping, refer to the official
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html"> Elastic
 * Search documentation</a>
 */
class Mapping {
    private val properties = HashMap<String, DataType>()

    /**
     * Add a property key with its datatype
     * @throws MappingException if the [dataType] is invalid or [key] is blank
     */
    fun addProperty(key: String, dataType: String) {
        key.isNotBlank() || throw MappingException("Mapping key can not be blank")
        properties[key] = DataType.parse(dataType)
    }

    /**
     * Add a group o properties with its datatype
     * @throws MappingException if the map contains a invalid key or datatype
     */
    fun addProperties(mappings: Map<String, String>) {
        mappings.forEach { (key, value) -> addProperty(key, value) }
    }
}

private enum class DataType {
    Boolean,
    Completion,
    Keyword,
    Date,
    Text,
    Long,
    Integer,
    Short,
    Byte,
    Double,
    Float,
    Ip;

    companion object Object {
        fun parse(typeName: String): DataType {
            try {
                return values().first { dt -> dt.name.equals(typeName, true) }
            } catch (e: NoSuchElementException) {
                throw MappingException("There's no such datatype $typeName")
            }
        }
    }
}