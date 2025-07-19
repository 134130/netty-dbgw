package com.github.l34130.netty.dbgw.app

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.MySqlGateway
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import java.io.OutputStreamWriter
import kotlin.system.exitProcess

class Args(
    parser: ArgParser,
) {
    val port by parser.storing("--port", help = "Port to listen gateway on") {
        toInt().also {
            require(it in (1..65535)) { "Port must be between 1 and 65535" }
        }
    }

    val upstream by parser.storing("--upstream", help = "Upstream to connect to") {
        val parts = split(":")
        require(parts.size == 2) { "Upstream must be in the format host:port" }
        Pair(parts[0], parts[1].toInt()).also {
            require(it.second in (1..65535)) { "Upstream port must be between 1 and 65535" }
        }
    }
}

fun main(args: Array<String>) {
    mainBody {
        val parser = ArgParser(args)
        val gateway =
            try {
                parser.parseInto(::Args).run {
                    MySqlGateway(
                        port = port,
                        upstream = upstream,
                    )
                }
            } catch (e: SystemExitException) {
                if (e is ShowHelpException) {
                    throw e
                }

                val writer = OutputStreamWriter(if (e.returnCode == 0) System.out else System.err)
                writer.write("Error: ")
                e.printUserMessage(writer, null, 0)
                writer.write("\n")
                writer.flush()

                ArgParser(arrayOf("--help")).parseInto(::Args)
                exitProcess(e.returnCode)
            }

        try {
            gateway.start()
        } finally {
            gateway.shutdown()
        }
    }
}
