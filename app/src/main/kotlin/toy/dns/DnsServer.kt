package toy.dns

import toy.dns.message.*
import toy.dns.zone.parser.ZoneFileParser
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * DNS Server implementation based on Node.js example
 */
class DnsServer(private val port: Int = 53) {
    private val socket = DatagramSocket(port)
    private var isRunning = false
    private val zoneData = mutableMapOf<String, MutableList<Any>>()

    /**
     * Load zone data from a BIND format file
     *
     * @param filePath Path to the zone file to process
     * @return A map of record types to lists of parsed records
     */
    fun loadZoneData(filePath: String): Map<String, MutableList<Any>> {
        val parser = ZoneFileParser()
        val records = parser.processBindFile(filePath)

        // Store the records for DNS lookup
        zoneData.putAll(records)

        return records
    }

    /**
     * Start the DNS server and begin listening for DNS queries
     */
    fun start() {
        if (isRunning) {
            println("DNS Server is already running")
            return
        }

        isRunning = true
        println("DNS Server started on port $port")

        // Buffer for incoming packets
        val buffer = ByteArray(512) // Standard DNS message size

        // Start a thread to handle incoming DNS queries
        Thread {
            while (isRunning) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    // Process the DNS query in a separate thread
                    Thread {
                        handleDnsQuery(packet)
                    }.start()
                } catch (e: Exception) {
                    if (isRunning) {
                        println("Error receiving packet: ${e.message}")
                    }
                }
            }
        }.start()
    }

    /**
     * Handle an incoming DNS query packet
     */
    private fun handleDnsQuery(packet: DatagramPacket) {
        val clientAddress = packet.address
        val clientPort = packet.port

        try {
            // Parse the DNS request
            val dnsPacket = DnsPacket().parse(packet.data, packet.length)

            // Create a response
            val response = createDnsResponse(dnsPacket)

            // Convert the response to bytes
            val responseBytes = response.toBytes()

            // Send the response back to the client
            val responsePacket = DatagramPacket(
                responseBytes,
                responseBytes.size,
                clientAddress,
                clientPort
            )
            socket.send(responsePacket)

            // Log the query
            val question = dnsPacket.questions.firstOrNull()
            if (question != null) {
                println("DNS Query from ${clientAddress.hostAddress}:$clientPort for ${question.name}")
            }
        } catch (e: Exception) {
            println("Error handling DNS query from ${clientAddress.hostAddress}:$clientPort: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Create a DNS response for the given query
     */
    private fun createDnsResponse(request: DnsPacket): DnsPacket {
        val response = request.createResponse()

        // For each question, look for matching records
        for (question in request.questions) {
            val domainName = question.name
            val queryType = question.type.toInt()

            when (queryType) {
                DnsType.A -> addARecords(response, domainName)
                DnsType.NS -> addNSRecords(response, domainName)
                DnsType.MX -> addMXRecords(response, domainName)
                DnsType.CNAME -> addCNAMERecords(response, domainName)
                DnsType.SOA -> addSOARecords(response, domainName)
                DnsType.TXT -> addTXTRecords(response, domainName)
                else -> {
                    // Unknown type, try A record as fallback
                    addARecords(response, domainName)
                }
            }
        }

        return response
    }

    /**
     * Add A records to the response for the given domain
     */
    private fun addARecords(response: DnsPacket, domainName: String) {
        val aRecords = zoneData["A"]

        if (aRecords.isNullOrEmpty()) {
            // If no A records, provide a default response for testing
            val record = DnsResourceRecord()
            record.name = domainName
            record.type = DnsType.A.toShort()
            record.clazz = DnsClass.IN.toShort()
            record.ttl = 300
            record.data = "8.8.8.8" // Example IP address
            response.answers.add(record)
            return
        }

        // Find matching A records
        aRecords.forEach { aRecord ->
            if (aRecord is toy.dns.zone.parser.ResourceRecord && (aRecord.name == domainName || aRecord.name == "@")) {
                val record = DnsResourceRecord()
                record.name = domainName
                record.type = DnsType.A.toShort()
                record.clazz = DnsClass.IN.toShort()
                record.ttl = aRecord.ttl.toIntOrNull() ?: 300
                record.data = aRecord.data
                response.answers.add(record)
            }
        }
    }

    /**
     * Add NS records to the response for the given domain
     */
    private fun addNSRecords(response: DnsPacket, domainName: String) {
        val nsRecords = zoneData["NS"]

        if (nsRecords.isNullOrEmpty()) {
            return
        }

        // Find matching NS records
        nsRecords.forEach { nsRecord ->
            if (nsRecord is toy.dns.zone.parser.ResourceRecord && (nsRecord.name == domainName || nsRecord.name == "@")) {
                val record = DnsResourceRecord()
                record.name = domainName
                record.type = DnsType.NS.toShort()
                record.clazz = DnsClass.IN.toShort()
                record.ttl = nsRecord.ttl.toIntOrNull() ?: 300
                record.data = nsRecord.data
                response.answers.add(record)
            }
        }
    }

    /**
     * Add MX records to the response for the given domain
     */
    private fun addMXRecords(response: DnsPacket, domainName: String) {
        val mxRecords = zoneData["MX"]

        if (mxRecords.isNullOrEmpty()) {
            return
        }

        // Find matching MX records
        mxRecords.forEach { mxRecord ->
            if (mxRecord is toy.dns.zone.parser.ResourceRecord && (mxRecord.name == domainName || mxRecord.name == "@")) {
                val record = DnsResourceRecord()
                record.name = domainName
                record.type = DnsType.MX.toShort()
                record.clazz = DnsClass.IN.toShort()
                record.ttl = mxRecord.ttl.toIntOrNull() ?: 300
                record.preference = mxRecord.preference.toShortOrNull() ?: 10
                record.data = mxRecord.data
                response.answers.add(record)
            }
        }
    }

    /**
     * Add CNAME records to the response for the given domain
     */
    private fun addCNAMERecords(response: DnsPacket, domainName: String) {
        val cnameRecords = zoneData["CNAME"]

        if (cnameRecords.isNullOrEmpty()) {
            return
        }

        // Find matching CNAME records
        cnameRecords.forEach { cnameRecord ->
            if (cnameRecord is toy.dns.zone.parser.ResourceRecord && cnameRecord.name == domainName) {
                val record = DnsResourceRecord()
                record.name = domainName
                record.type = DnsType.CNAME.toShort()
                record.clazz = DnsClass.IN.toShort()
                record.ttl = cnameRecord.ttl.toIntOrNull() ?: 300
                record.data = cnameRecord.data
                response.answers.add(record)
            }
        }
    }

    /**
     * Add SOA records to the response for the given domain
     */
    private fun addSOARecords(response: DnsPacket, domainName: String) {
        val soaRecords = zoneData["SOA"]

        if (soaRecords.isNullOrEmpty()) {
            return
        }

        // Find matching SOA records
        soaRecords.forEach { soaRecord ->
            if (soaRecord is toy.dns.zone.parser.SOARecord && (soaRecord.name == domainName || soaRecord.name == "@")) {
                val record = DnsResourceRecord()
                record.name = domainName
                record.type = DnsType.SOA.toShort()
                record.clazz = DnsClass.IN.toShort()
                record.ttl = soaRecord.ttl
                record.mname = soaRecord.mname
                record.rname = soaRecord.rname
                record.serial = soaRecord.serial.toIntOrNull() ?: 1
                record.refresh = soaRecord.refresh.toIntOrNull() ?: 3600
                record.retry = soaRecord.retry.toIntOrNull() ?: 600
                record.expire = soaRecord.expire.toIntOrNull() ?: 86400
                record.minimum = soaRecord.title.toIntOrNull() ?: 600
                response.answers.add(record)
            }
        }
    }

    /**
     * Add TXT records to the response for the given domain
     */
    private fun addTXTRecords(response: DnsPacket, domainName: String) {
        val txtRecords = zoneData["TXT"]

        if (txtRecords.isNullOrEmpty()) {
            return
        }

        // Find matching TXT records
        txtRecords.forEach { txtRecord ->
            if (txtRecord is toy.dns.zone.parser.ResourceRecord && (txtRecord.name == domainName || txtRecord.name == "@")) {
                val record = DnsResourceRecord()
                record.name = domainName
                record.type = DnsType.TXT.toShort()
                record.clazz = DnsClass.IN.toShort()
                record.ttl = txtRecord.ttl.toIntOrNull() ?: 300
                record.data = txtRecord.data
                response.answers.add(record)
            }
        }
    }

    /**
     * Stop the DNS server
     */
    fun stop() {
        if (!isRunning) {
            return
        }

        isRunning = false
        socket.close()
        println("DNS Server stopped")
    }
}