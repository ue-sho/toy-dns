package toy.dns.models

/**
 * Data class representing a DNS header structure
 */
data class DnsHeader(
    val id: Int,
    val qr: Boolean,
    val opcode: Int,
    val aa: Boolean,
    val tc: Boolean,
    val rd: Boolean,
    val ra: Boolean,
    val z: Int,
    val rcode: Int,
    val qdcount: Int,
    val ancount: Int,
    val nscount: Int,
    val arcount: Int
) {
    companion object {
        // Response Codes
        const val RCODE_NO_ERROR = 0
        const val RCODE_FORMAT_ERROR = 1
        const val RCODE_SERVER_FAILURE = 2
        const val RCODE_NAME_ERROR = 3
        const val RCODE_NOT_IMPLEMENTED = 4
        const val RCODE_REFUSED = 5
    }
}