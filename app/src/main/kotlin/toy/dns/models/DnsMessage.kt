package toy.dns.models

/**
 * Data class representing a DNS message structure
 */
data class DnsMessage(
    val header: DnsHeader,
    val questions: List<DnsQuestion>,
    val answers: List<DnsResourceRecord> = listOf(),
    val authorities: List<DnsResourceRecord> = listOf(),
    val additionals: List<DnsResourceRecord> = listOf()
)