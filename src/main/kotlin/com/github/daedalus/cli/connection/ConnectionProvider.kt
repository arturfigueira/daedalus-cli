package com.github.daedalus.cli.connection

import org.elasticsearch.client.RestHighLevelClient

interface ConnectionProvider {
    fun provide() : RestHighLevelClient;
}