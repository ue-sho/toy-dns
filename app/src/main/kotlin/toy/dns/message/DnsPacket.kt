package toy.dns.message

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * DNS message types as defined in RFC 1035
 */
object DnsType {
    const val A = 1        // IPv4 address record
    const val NS = 2       // Name server record
    const val CNAME = 5    // Canonical name record
    const val SOA = 6      // Start of authority record
    const val MX = 15      // Mail exchange record
    const val TXT = 16     // Text record
}

/**
 * DNS classes as defined in RFC 1035
 */
object DnsClass {
    const val IN = 1       // Internet
}

/**
 * Represents a DNS packet with header, questions, answers, authority, and additional sections
 */
class DnsPacket {
    // Header fields
    var id: Short = 0          // 16-bit transaction identifier
    var flags: Short = 0       // Various flags
    var isResponse: Boolean    // Query or response
        get() = (flags.toInt() and 0x8000) != 0
        set(value) {
            flags = if (value) {
                (flags.toInt() or 0x8000).toShort()
            } else {
                (flags.toInt() and 0x7FFF).toShort()
            }
        }

    // Sections
    // https://engineerhead.github.io/dns-server/dns-message-sections.html#dns-message-sections-part-1
	// +---------------------+
    // |        Header       |
    // +---------------------+
    // |       Question      | the question for the name server
    // +---------------------+
    // |        Answer       | RRs answering the question
    // +---------------------+
    // |      Authority      | RRs pointing toward an authority
    // +---------------------+
    // |      Additional     | RRs holding additional information
    // +---------------------+
    val questions = mutableListOf<DnsQuestion>()
    val answers = mutableListOf<DnsResourceRecord>()
    val authority = mutableListOf<DnsResourceRecord>()
    val additional = mutableListOf<DnsResourceRecord>()

    /**
     * Parse a DNS packet from a byte array
     */
    fun parse(data: ByteArray, length: Int): DnsPacket {
        val buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)

        // Parse header
        id = buffer.short
        flags = buffer.short
        val qdCount = buffer.short.toInt()
        val anCount = buffer.short.toInt()
        val nsCount = buffer.short.toInt()
        val arCount = buffer.short.toInt()

        // Parse questions
        for (i in 0 until qdCount) {
            val question = DnsQuestion()
            question.name = parseDomainName(buffer, data)
            question.type = buffer.short
            question.clazz = buffer.short
            questions.add(question)
        }

        // Parse answers
        for (i in 0 until anCount) {
            val record = parseResourceRecord(buffer, data)
            answers.add(record)
        }

        // Parse authority
        for (i in 0 until nsCount) {
            val record = parseResourceRecord(buffer, data)
            authority.add(record)
        }

        // Parse additional
        for (i in 0 until arCount) {
            val record = parseResourceRecord(buffer, data)
            additional.add(record)
        }

        return this
    }

    /**
     * Create a DNS response packet from a request packet
     */
    fun createResponse(): DnsPacket {
        val response = DnsPacket()
        response.id = this.id

        // Set appropriate flags for response
        response.flags = 0x8180.toShort()  // Standard response with recursion available

        // Copy the questions
        response.questions.addAll(this.questions)

        return response
    }

    /**
     * Serialize the DNS packet to a byte array
     */
    fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(512).order(ByteOrder.BIG_ENDIAN)

        // Write header
        buffer.putShort(id)
        buffer.putShort(flags)
        buffer.putShort(questions.size.toShort())
        buffer.putShort(answers.size.toShort())
        buffer.putShort(authority.size.toShort())
        buffer.putShort(additional.size.toShort())

        // Write questions
        for (question in questions) {
            writeDomainName(buffer, question.name)
            buffer.putShort(question.type)
            buffer.putShort(question.clazz)
        }

        // Write answers
        for (answer in answers) {
            writeResourceRecord(buffer, answer)
        }

        // Write authority
        for (authRecord in authority) {
            writeResourceRecord(buffer, authRecord)
        }

        // Write additional
        for (addRecord in additional) {
            writeResourceRecord(buffer, addRecord)
        }

        // Return the filled portion of the buffer
        val result = ByteArray(buffer.position())
        buffer.position(0)
        buffer.get(result)

        return result
    }

    /**
     * Parse a domain name from DNS message format
     */
    private fun parseDomainName(buffer: ByteBuffer, data: ByteArray): String {
        val nameBuilder = StringBuilder()
        var length = buffer.get().toInt() and 0xFF

        while (length > 0) {
            if ((length and 0xC0) == 0xC0) {
                // Pointer to another location in the message
                val pointer = ((length and 0x3F) shl 8) or (buffer.get().toInt() and 0xFF)
                val savedPosition = buffer.position()
                buffer.position(pointer)
                nameBuilder.append(parseDomainName(buffer, data))
                buffer.position(savedPosition)
                return nameBuilder.toString()
            }

            // Regular label
            for (i in 0 until length) {
                nameBuilder.append(buffer.get().toInt().toChar())
            }

            length = buffer.get().toInt() and 0xFF
            if (length > 0) {
                nameBuilder.append('.')
            }
        }

        return nameBuilder.toString()
    }

    /**
     * Parse a resource record from DNS message format
     */
    private fun parseResourceRecord(buffer: ByteBuffer, data: ByteArray): DnsResourceRecord {
        val record = DnsResourceRecord()
        record.name = parseDomainName(buffer, data)
        record.type = buffer.short
        record.clazz = buffer.short
        record.ttl = buffer.int

        val rdLength = buffer.short.toInt()
        val startPos = buffer.position()

        when (record.type.toInt()) {
            DnsType.A -> {
                if (rdLength == 4) {
                    val bytes = ByteArray(4)
                    buffer.get(bytes)
                    record.data = "${bytes[0].toInt() and 0xFF}.${bytes[1].toInt() and 0xFF}.${bytes[2].toInt() and 0xFF}.${bytes[3].toInt() and 0xFF}"
                }
            }
            DnsType.NS, DnsType.CNAME -> {
                record.data = parseDomainName(buffer, data)
            }
            DnsType.MX -> {
                record.preference = buffer.short
                record.data = parseDomainName(buffer, data)
            }
            DnsType.TXT -> {
                val txtLength = buffer.get().toInt() and 0xFF
                val txtBytes = ByteArray(txtLength)
                buffer.get(txtBytes)
                record.data = String(txtBytes)
            }
            DnsType.SOA -> {
                record.mname = parseDomainName(buffer, data)
                record.rname = parseDomainName(buffer, data)
                record.serial = buffer.int
                record.refresh = buffer.int
                record.retry = buffer.int
                record.expire = buffer.int
                record.minimum = buffer.int
            }
            else -> {
                // Skip unknown types
                buffer.position(startPos + rdLength)
            }
        }

        // Ensure we've consumed the entire record data
        if (buffer.position() != startPos + rdLength) {
            buffer.position(startPos + rdLength)
        }

        return record
    }

    /**
     * Write a domain name in DNS message format
     */
    private fun writeDomainName(buffer: ByteBuffer, domainName: String) {
        val labels = domainName.split('.')

        for (label in labels) {
            buffer.put(label.length.toByte())
            label.forEach { buffer.put(it.code.toByte()) }
        }

        buffer.put(0.toByte()) // Root label
    }

    /**
     * Write a resource record in DNS message format
     */
    private fun writeResourceRecord(buffer: ByteBuffer, record: DnsResourceRecord) {
        writeDomainName(buffer, record.name)
        buffer.putShort(record.type)
        buffer.putShort(record.clazz)
        buffer.putInt(record.ttl)

        // Save position for length
        val rdLengthPos = buffer.position()
        buffer.putShort(0) // Placeholder for length

        val dataStartPos = buffer.position()

        when (record.type.toInt()) {
            DnsType.A -> {
                // IPv4 address
                val parts = record.data.split(".")
                if (parts.size == 4) {
                    parts.forEach { buffer.put(it.toInt().toByte()) }
                }
            }
            DnsType.NS, DnsType.CNAME -> {
                // Domain name
                writeDomainName(buffer, record.data)
            }
            DnsType.MX -> {
                // Preference and exchange domain
                buffer.putShort(record.preference)
                writeDomainName(buffer, record.data)
            }
            DnsType.TXT -> {
                // Text record
                buffer.put(record.data.length.toByte())
                record.data.forEach { buffer.put(it.code.toByte()) }
            }
            DnsType.SOA -> {
                // SOA record
                writeDomainName(buffer, record.mname)
                writeDomainName(buffer, record.rname)
                buffer.putInt(record.serial)
                buffer.putInt(record.refresh)
                buffer.putInt(record.retry)
                buffer.putInt(record.expire)
                buffer.putInt(record.minimum)
            }
        }

        // Write the actual length
        val dataLength = buffer.position() - dataStartPos
        val currentPos = buffer.position()
        buffer.position(rdLengthPos)
        buffer.putShort(dataLength.toShort())
        buffer.position(currentPos)
    }
}

/**
 * Represents a DNS question section
 */
data class DnsQuestion(
    var name: String = "",
    var type: Short = 0,
    var clazz: Short = 0
)

/**
 * Represents a DNS resource record
 */
data class DnsResourceRecord(
    var name: String = "",
    var type: Short = 0,
    var clazz: Short = 0,
    var ttl: Int = 0,
    var data: String = "",
    var preference: Short = 0,
    // SOA specific fields
    var mname: String = "",
    var rname: String = "",
    var serial: Int = 0,
    var refresh: Int = 0,
    var retry: Int = 0,
    var expire: Int = 0,
    var minimum: Int = 0
)