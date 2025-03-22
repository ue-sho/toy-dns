package toy.dns.encoder

import toy.dns.models.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Encoder for DNS responses
 */
class DnsEncoder {

    /**
     * Encode a DNS message into a byte array
     */
    fun encodeMessage(message: DnsMessage): ByteArray {
        // Calculate buffer size
        var size = 12 // Header is 12 bytes

        // Add size for questions
        for (question in message.questions) {
            size += calculateQuestionSize(question)
        }

        // Add size for answers
        for (answer in message.answers) {
            size += calculateResourceRecordSize(answer)
        }

        val buffer = ByteBuffer.allocate(size)

        // Encode header
        encodeHeader(buffer, message.header)

        // Domain name compression map
        val nameOffsets = mutableMapOf<String, Int>()

        // Encode questions
        for (question in message.questions) {
            encodeQuestion(buffer, question, nameOffsets)
        }

        // Encode answers
        for (answer in message.answers) {
            encodeResourceRecord(buffer, answer, nameOffsets)
        }

        return buffer.array()
    }

    /**
     * Encode a DNS header into the buffer
     */
    private fun encodeHeader(buffer: ByteBuffer, header: DnsHeader) {
        buffer.putShort(header.id.toShort())

        var flags = 0
        if (header.qr) flags = flags or 0x8000
        flags = flags or (header.opcode shl 11)
        if (header.aa) flags = flags or 0x0400
        if (header.tc) flags = flags or 0x0200
        if (header.rd) flags = flags or 0x0100
        if (header.ra) flags = flags or 0x0080
        flags = flags or (header.z shl 4)
        flags = flags or header.rcode

        buffer.putShort(flags.toShort())
        buffer.putShort(header.qdcount.toShort())
        buffer.putShort(header.ancount.toShort())
        buffer.putShort(header.nscount.toShort())
        buffer.putShort(header.arcount.toShort())
    }

    /**
     * Encode a DNS question into the buffer
     */
    private fun encodeQuestion(buffer: ByteBuffer, question: DnsQuestion, nameOffsets: MutableMap<String, Int>) {
        encodeName(buffer, question.name, nameOffsets)
        buffer.putShort(question.type.value.toShort())
        buffer.putShort(question.dnsClass.value.toShort())
    }

    /**
     * Encode a DNS resource record into the buffer
     */
    private fun encodeResourceRecord(buffer: ByteBuffer, record: DnsResourceRecord, nameOffsets: MutableMap<String, Int>) {
        encodeName(buffer, record.name, nameOffsets)
        buffer.putShort(record.type.value.toShort())
        buffer.putShort(record.dnsClass.value.toShort())
        buffer.putInt(record.ttl)

        val dataStart = buffer.position()

        // Reserve 2 bytes for rdlength
        buffer.position(buffer.position() + 2)

        // Encode data based on record type
        when (record.type) {
            DnsType.A -> encodeARecord(buffer, record.data)
            DnsType.CNAME, DnsType.NS, DnsType.PTR -> encodeName(buffer, record.data, nameOffsets)
            DnsType.TXT -> encodeTxtRecord(buffer, record.data)
            else -> throw IllegalArgumentException("Unsupported record type: ${record.type}")
        }

        // Calculate and write rdlength
        val dataLength = buffer.position() - dataStart - 2
        buffer.putShort(dataStart, dataLength.toShort())
    }

    /**
     * Encode a domain name with compression
     */
    private fun encodeName(buffer: ByteBuffer, name: String, offsets: MutableMap<String, Int>) {
        if (name.isEmpty()) {
            buffer.put(0)
            return
        }

        // Check if we can use compression
        if (offsets.containsKey(name)) {
            val offset = offsets[name]!!
            buffer.put((0xC0 or (offset shr 8)).toByte())
            buffer.put((offset and 0xFF).toByte())
            return
        }

        // Record the start position of this name for compression
        offsets[name] = buffer.position()

        // Split the name into labels
        val labels = name.split(".")

        // Write each label
        for (label in labels) {
            if (label.isEmpty()) continue

            // Write the length byte
            buffer.put(label.length.toByte())

            // Write the label bytes
            val labelBytes = label.toByteArray(StandardCharsets.US_ASCII)
            buffer.put(labelBytes)
        }

        // Terminate with a zero byte
        buffer.put(0)
    }

    /**
     * Encode an A record (IPv4 address)
     */
    private fun encodeARecord(buffer: ByteBuffer, data: String) {
        // Parse IPv4 address
        val parts = data.split(".")
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid IPv4 address: $data")
        }

        for (part in parts) {
            buffer.put(part.toInt().toByte())
        }
    }

    /**
     * Encode a TXT record
     */
    private fun encodeTxtRecord(buffer: ByteBuffer, data: String) {
        val bytes = data.toByteArray(StandardCharsets.US_ASCII)
        buffer.put(bytes.size.toByte())
        buffer.put(bytes)
    }

    /**
     * Calculate the size of a question
     */
    private fun calculateQuestionSize(question: DnsQuestion): Int {
        return calculateNameSize(question.name) + 4 // 2 bytes for type + 2 bytes for class
    }

    /**
     * Calculate the size of a resource record
     */
    private fun calculateResourceRecordSize(record: DnsResourceRecord): Int {
        var size = calculateNameSize(record.name) + 10 // 2 bytes for type + 2 bytes for class + 4 bytes for TTL + 2 bytes for rdlength

        // Add size for the data
        size += when (record.type) {
            DnsType.A -> 4 // 4 bytes for IPv4 address
            DnsType.CNAME, DnsType.NS, DnsType.PTR -> calculateNameSize(record.data)
            DnsType.TXT -> record.data.length + 1 // Length byte + data
            else -> throw IllegalArgumentException("Unsupported record type: ${record.type}")
        }

        return size
    }

    /**
     * Calculate the size of a domain name
     */
    private fun calculateNameSize(name: String): Int {
        if (name.isEmpty()) return 1 // Just the terminating zero byte

        val labels = name.split(".")
        var size = 1 // For the terminating zero byte

        for (label in labels) {
            if (label.isEmpty()) continue
            size += 1 + label.length // 1 byte for length + the label itself
        }

        return size
    }
}