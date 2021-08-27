package com.github.daedalus.cli.mapping

class MappingsNotFoundException(filePath: String) :
    MappingException("$filePath could not be found or is not a file") {
}