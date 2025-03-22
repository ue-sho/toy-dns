package toy.dns.models

/**
 * Data class representing a DNS resource record
 */
data class DnsResourceRecord(
    val name: String,
    val type: DnsType,
    val dnsClass: DnsClass,
    val ttl: Int,
    val data: String
)