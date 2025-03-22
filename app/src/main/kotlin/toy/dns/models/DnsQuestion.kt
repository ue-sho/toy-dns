package toy.dns.models

/**
 * Data class representing a DNS question
 */
data class DnsQuestion(
    val name: String,
    val type: DnsType,
    val dnsClass: DnsClass
)