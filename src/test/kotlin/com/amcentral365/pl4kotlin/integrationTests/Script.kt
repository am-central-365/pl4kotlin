package com.amcentral365.pl4kotlin.integrationTests

import mu.KotlinLogging
import java.io.File
import java.io.InputStream
import java.sql.SQLException

internal fun runSqlSetup()    = runStatements(composeScriptFileName(SETUP_SUFFIX),    setupScripts)
internal fun runSqlTeardown() = runStatements(composeScriptFileName(TEARDOWN_SUFFIX), teardownScripts)


private val logger = KotlinLogging.logger {}

private const val SETUP_SUFFIX    = "Setup"
private const val TEARDOWN_SUFFIX = "Teardown"

private val setupScripts:    List<ScriptStatement> by lazy { parseStatements(getVendorScriptStream(SETUP_SUFFIX)) }
private val teardownScripts: List<ScriptStatement> by lazy { parseStatements(getVendorScriptStream(TEARDOWN_SUFFIX)) }

private fun composeScriptFileName(suffix: String) = connInfo!!.dbVendor + suffix + ".sql"


private fun getVendorScriptStream(suffix: String): InputStream {
    require( connInfo != null )
    val scriptFileName = composeScriptFileName(suffix)
    logger.debug("looking for script file $scriptFileName")

    val file = File(scriptFileName)
    return if( file.canRead() ) {
        logger.info { "using script file $scriptFileName from the current directory" }
        file.inputStream()
    } else {
        logger.debug { "couldn't locate local file, checking internal resources..." }
        try {
            val inputStream = ClassLoader.getSystemClassLoader().getResource(scriptFileName).openStream()
            logger.info { "using script file $scriptFileName from the jar" }
            inputStream
        } catch (x: Exception) {
            logger.warn { "script file $scriptFileName isn't present in the local directory nor known to the test suite, skipping" }
            "".byteInputStream()
        }
    }
}


private data class ScriptStatement(val lineNo: Int, val text: String)

/**
 * Break file into (potentially multi-line) statements, separated by blank lines, ';', or '/'
 * Limitations:
 *   any blank line is treated as statement terminator. No blank lines inside PL/SQL block.
 */
private fun parseStatements(inputStream: InputStream): List<ScriptStatement> {
    val scriptStatements: MutableList<ScriptStatement> = mutableListOf()
    var lineNo = 0
    var scriptLine = 0
    fun inScript() = scriptLine > 0

    val sb = StringBuilder()
    for(line in inputStream.bufferedReader().lines()) {
        lineNo++

        if( !inScript() && (line.isBlank() || line.startsWith('#')) )  // skip blank and comment lines
            continue

        val trimmed = line.trimEnd()
        if( trimmed == ";" || trimmed == "/" || trimmed.isBlank() ) {
            if( inScript() ) {
                scriptStatements.add(ScriptStatement(scriptLine, sb.toString()))
                sb.setLength(0)
                scriptLine = 0
            } else {
                logger.warn { "line $lineNo: blank statement" }
            }
        } else {
            if( !inScript() )
                scriptLine = lineNo
            sb.append(line).append('\n')
        }
    }

    if( inScript() )
        scriptStatements.add(ScriptStatement(scriptLine, sb.toString()))

    return scriptStatements
}


private fun runStatements(scriptFileName: String, statements: List<ScriptStatement>, ignoreErrors: Boolean=true) {
    require( connInfo != null )
    logger.info { "runStatements($scriptFileName), ignoreErrors: $ignoreErrors" }

    getConnection().use { conn ->
        statements.forEach {
            logger.info { "running statement defined on line ${it.lineNo} of $scriptFileName" }
            logger.debug { it.text }
            try {
                conn.createStatement().execute(it.text)
            } catch(x: SQLException) {
                logger.warn {
                    "Error running statement defined on line ${it.lineNo} of $scriptFileName:\n" +
                    "${x.message}\n" +
                    "Statement:\n${it.text}"
                }
                if( !ignoreErrors )
                    throw x
            }
        }
    }
}
