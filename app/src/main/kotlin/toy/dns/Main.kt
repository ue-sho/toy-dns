package toy.dns

import toy.dns.message.*
import toy.dns.zone.parser.ZoneFileParser
import java.io.File

/**
 * Simple DNS server implementation based on Node.js example
 */
fun main(args: Array<String>) {
    val port = if (args.isNotEmpty() && args[0].toIntOrNull() != null) {
        args[0].toInt()
    } else {
        8053  // Custom DNS port (standard port 53 requires root privileges)
    }

    // Create a new DNS server
    val dnsServer = DnsServer(port)

    // Load zone data
    val defaultZonePath = "src/main/resources/zone.txt"
    val zonePath = args.getOrElse(1) { defaultZonePath }
    println("Loading zone data from $zonePath")

    try {
        val records = dnsServer.loadZoneData(zonePath)
        println("Successfully loaded zone data from $zonePath")
        println("Records loaded: ${records.map { "${it.key}: ${it.value.size}" }}")

        // Start the server
        dnsServer.start()

        // Keep the server running until the process is terminated
        Runtime.getRuntime().addShutdownHook(Thread {
            println("Shutting down DNS server...")
            dnsServer.stop()
        })

        // Keep main thread alive
        Thread.currentThread().join()
    } catch (e: Exception) {
        println("Error loading zone file: ${e.message}")
        println("Current directory: ${File(".").absolutePath}")
        println("Please ensure the zone file exists at the specified path.")
        System.exit(1)
    }
}