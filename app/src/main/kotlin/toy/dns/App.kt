package toy.dns

import toy.dns.server.DnsServer

/**
 * Main application entry point
 */
fun main() {
    val server = DnsServer()
    server.start()
}