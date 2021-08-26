package com.github.daedalus.cli.mapping

/**
 * Represents an index mappings provider.
 *
 * Contain methods to read and parse mappings from an external source
 */
interface MappingsProvider {
    fun read(): Mapping
}