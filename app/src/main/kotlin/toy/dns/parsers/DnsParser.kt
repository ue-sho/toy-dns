package toy.dns.parsers

import toy.dns.models.*
import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * Parser for decoding DNS packets
 */
class DnsParser {

    /**
     * Parse a DNS packet from a byte array
     */
    fun parsePacket(data: ByteArray): DnsMessage {
        val buffer = ByteBuffer.wrap(data)

        // Parse header
        val header = parseHeader(buffer)

        // Parse questions
        val questions = mutableListOf<DnsQuestion>()
        for (i in 0 until header.qdcount) {
            questions.add(parseQuestion(buffer, data))
        }

        return DnsMessage(
            header = header,
            questions = questions
        )
    }

    /**
     * Parse a DNS header from the buffer
     */
    private fun parseHeader(buffer: ByteBuffer): DnsHeader {
        val id = buffer.short.toInt() and 0xFFFF

        val flags = buffer.short.toInt() and 0xFFFF
        val qr = (flags and 0x8000) != 0
        val opcode = (flags and 0x7800) shr 11
        val aa = (flags and 0x0400) != 0
        val tc = (flags and 0x0200) != 0
        val rd = (flags and 0x0100) != 0
        val ra = (flags and 0x0080) != 0
        val z = (flags and 0x0070) shr 4
        val rcode = flags and 0x000F

        val qdcount = buffer.short.toInt() and 0xFFFF
        val ancount = buffer.short.toInt() and 0xFFFF
        val nscount = buffer.short.toInt() and 0xFFFF
        val arcount = buffer.short.toInt() and 0xFFFF

        return DnsHeader(
            id = id,
            qr = qr,
            opcode = opcode,
            aa = aa,
            tc = tc,
            rd = rd,
            ra = ra,
            z = z,
            rcode = rcode,
            qdcount = qdcount,
            ancount = ancount,
            nscount = nscount,
            arcount = arcount
        )
    }

    /**
     * Parse a DNS question from the buffer
     */
    private fun parseQuestion(buffer: ByteBuffer, originalData: ByteArray): DnsQuestion {
        val name = readName(buffer, originalData)
        val typeValue = buffer.short.toInt() and 0xFFFF
        val classValue = buffer.short.toInt() and 0xFFFF

        return DnsQuestion(
            name = name,
            type = DnsType.fromValue(typeValue),
            dnsClass = DnsClass.fromValue(classValue)
        )
    }

    /**
     * Read a domain name from the buffer with support for message compression
     */
    private fun readName(buffer: ByteBuffer, originalData: ByteArray): String {
        val nameParts = mutableListOf<String>()
        var length: Int

        while (true) {
            length = buffer.get().toInt() and 0xFF

            // Check for message compression (two high bits set)
            if ((length and 0xC0) == 0xC0) {
                // Pointer to another location in the message
                val secondByte = buffer.get().toInt() and 0xFF
                val offset = ((length and 0x3F) shl 8) or secondByte

                // Save current position
                val currentPosition = buffer.position()

                // Read name from the offset position
                buffer.position(offset)
                val pointerName = readName(buffer, originalData)

                // Restore position and return the combined name
                buffer.position(currentPosition)

                if (nameParts.isEmpty()) {
                    return pointerName
                }

                return nameParts.joinToString(".") + "." + pointerName
            }

            // Regular name part
            if (length == 0) {
                break
            }

            val bytes = ByteArray(length)
            buffer.get(bytes, 0, length)
            nameParts.add(String(bytes))
        }

        return nameParts.joinToString(".")
    }
}