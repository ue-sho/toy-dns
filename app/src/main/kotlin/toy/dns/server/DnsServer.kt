package toy.dns.server

import toy.dns.encoder.DnsEncoder
import toy.dns.models.*
import toy.dns.parsers.DnsParser
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.*

/**
 * DNS server implementation
 */
class DnsServer(
    private val port: Int = 53,
    private val hostname: String = "0.0.0.0"
) {
    private val parser = DnsParser()
    private val encoder = DnsEncoder()
    private val resolver = DnsResolver()
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Start the DNS server
     */
    fun start() {
        println("Starting DNS server on $hostname:$port")

        scope.launch {
            try {
                val socket = DatagramSocket(port, InetAddress.getByName(hostname))
                println("DNS server listening on $hostname:$port")

                while (true) {
                    val buffer = ByteArray(512) // Standard DNS message size
                    val packet = DatagramPacket(buffer, buffer.size)

                    // Wait for a DNS request
                    socket.receive(packet)

                    // Process the request in a coroutine
                    launch {
                        try {
                            handleRequest(socket, packet)
                        } catch (e: Exception) {
                            println("Error handling request: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                println("Server error: ${e.message}")
                e.printStackTrace()
            }
        }

        // Keep the main thread alive
        runBlocking {
            delay(Long.MAX_VALUE)
        }
    }

    /**
     * Handle a DNS request
     */
    private fun handleRequest(socket: DatagramSocket, packet: DatagramPacket) {
        println("Received DNS request from ${packet.address.hostAddress}:${packet.port}")

        try {
            // Parse the DNS request
            val requestData = packet.data.copyOfRange(0, packet.length)
            val request = parser.parsePacket(requestData)

            println("Request: ${request.questions.firstOrNull()?.name ?: "unknown"} " +
                   "(${request.questions.firstOrNull()?.type ?: "unknown"})")

            // Process the request and generate a response
            val response = processRequest(request)

            // Encode the response to a byte array
            val responseData = encoder.encodeMessage(response)

            // Send the response
            val responsePacket = DatagramPacket(
                responseData,
                responseData.size,
                packet.address,
                packet.port
            )
            socket.send(responsePacket)

            println("Sent DNS response to ${packet.address.hostAddress}:${packet.port}")
        } catch (e: Exception) {
            println("Error processing request: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Process a DNS request and create a response
     */
    private fun processRequest(request: DnsMessage): DnsMessage {
        // Create a response header based on the request
        val header = DnsHeader(
            id = request.header.id,
            qr = true, // This is a response
            opcode = request.header.opcode,
            aa = true, // Authoritative answer
            tc = false, // Not truncated
            rd = request.header.rd, // Recursion desired (copy from request)
            ra = false, // Recursion not available
            z = 0,
            rcode = DnsHeader.RCODE_NO_ERROR,
            qdcount = request.questions.size,
            ancount = 0, // Will be set later
            nscount = 0,
            arcount = 0
        )

        // Resolve answers for each question
        val answers = mutableListOf<DnsResourceRecord>()

        for (question in request.questions) {
            val resolved = resolver.resolve(question)
            answers.addAll(resolved)
        }

        // Create the response message
        return DnsMessage(
            header = header.copy(ancount = answers.size),
            questions = request.questions,
            answers = answers
        )
    }
}