package com.github.daedalus.cli.mapping.json

import com.github.daedalus.cli.mapping.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader

/**
 * Provides mappings from a text file, formatted as a json, which resides at the current filesystem
 * where the application is being executed
 *
 * @property filePath path to the mappings file
 * @constructor creates a new instance of this provider
 */
class JsonMappingsProvider(private val filePath: String) : MappingsProvider {

    private val gson = Gson()
    private val listType = object : TypeToken<ArrayList<JsonMapping?>?>() {}.type

    private data class JsonMapping(val key: String?, val type: String?)

    /**
     * Reads the mappings from the file found at [filePath]
     *
     * @return A [Mapping] instance filled with all mappings found at the file
     * @throws MappingException if the file cant be parsed into mappings
     * @throws MappingsNotFoundException if its is not a file, if it doesn't exists
     */
    override fun read(): Mapping {
        val mappingFile = File(filePath)

        try {
            val lines = BufferedReader(FileReader(mappingFile)).use { reader -> reader.readLines() }
            val jsonMappings: List<JsonMapping> = gson.fromJson(lines.joinToString { "" }, listType)

            val mapping = Mapping()
            jsonMappings.map { jsonMapping ->
                mapping.addProperty(
                    jsonMapping.key!!,
                    jsonMapping.type!!
                )
            }
            return mapping;

        } catch (e: FileNotFoundException) {
            throw MappingsNotFoundException(filePath)
        } catch (e: JsonSyntaxException) {
            throw MappingException("Mappings file could not be parsed into mappings")
        } catch (e: NullPointerException) {
            throw MappingException("Mappings file property list is not well formatted")
        }
    }
}