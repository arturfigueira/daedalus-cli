import kotlinx.cli.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {

    val parser = ArgParser("Daedalus CLI")

    val input by parser.option(ArgType.String, shortName = "i", description = "Input Directory")
        .required()

    val mappings by parser.option(
        ArgType.String,
        shortName = "m",
        description = "Mappings File"
    ).required()

    val elastic by parser.option(ArgType.String, shortName = "e", description = "Elastic Config")
        .required()

    parser.parse(args)

    logger.debug { "Input: $input" }
    logger.debug { "Mappings: $mappings" }
    logger.debug { "Elastic: $elastic" }
}

