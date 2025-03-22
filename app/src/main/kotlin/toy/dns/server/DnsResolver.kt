package toy.dns.server

import toy.dns.models.*

/**
 * DNS resolver that resolves DNS queries against a zone database
 */
class DnsResolver {
    private val zoneDatabase = mapOf(
        "example.com" to mapOf(
            DnsType.A to listOf(
                DnsResourceRecord(
                    name = "example.com",
                    type = DnsType.A,
                    dnsClass = DnsClass.IN,
                    ttl = 3600,
                    data = "192.168.1.1"
                )
            ),
            DnsType.CNAME to listOf(
                DnsResourceRecord(
                    name = "www.example.com",
                    type = DnsType.CNAME,
                    dnsClass = DnsClass.IN,
                    ttl = 3600,
                    data = "example.com"
                )
            ),
            DnsType.MX to listOf(
                DnsResourceRecord(
                    name = "example.com",
                    type = DnsType.MX,
                    dnsClass = DnsClass.IN,
                    ttl = 3600,
                    data = "mail.example.com"
                )
            ),
            DnsType.TXT to listOf(
                DnsResourceRecord(
                    name = "example.com",
                    type = DnsType.TXT,
                    dnsClass = DnsClass.IN,
                    ttl = 3600,
                    data = "v=spf1 -all"
                )
            )
        ),
        "test.com" to mapOf(
            DnsType.A to listOf(
                DnsResourceRecord(
                    name = "test.com",
                    type = DnsType.A,
                    dnsClass = DnsClass.IN,
                    ttl = 3600,
                    data = "192.168.2.1"
                ),
                DnsResourceRecord(
                    name = "www.test.com",
                    type = DnsType.A,
                    dnsClass = DnsClass.IN,
                    ttl = 3600,
                    data = "192.168.2.2"
                )
            )
        )
    )

    /**
     * Resolve a DNS question to a list of resource records
     */
    fun resolve(question: DnsQuestion): List<DnsResourceRecord> {
        // Split the domain name into parts
        val parts = question.name.split(".")

        // Try to find an exact match first
        var domain = question.name
        var records: List<DnsResourceRecord>? = findRecords(domain, question.type)

        // If no exact match, try parent domains (for wildcard resolution)
        if (records.isNullOrEmpty() && parts.size > 2) {
            for (i in 1 until parts.size - 1) {
                domain = parts.drop(i).joinToString(".")
                records = findRecords(domain, question.type)
                if (!records.isNullOrEmpty()) {
                    break
                }
            }
        }

        // If CNAME lookup and we didn't find a direct A record, try to resolve the CNAME
        if (question.type == DnsType.A && records.isNullOrEmpty()) {
            val cnameRecords = findRecords(question.name, DnsType.CNAME)
            if (!cnameRecords.isNullOrEmpty()) {
                val cname = cnameRecords.first()
                val cnameTarget = cname.data

                // Look up the A record for the CNAME target
                val targetRecords = findRecords(cnameTarget, DnsType.A)
                if (!targetRecords.isNullOrEmpty()) {
                    // Return both the CNAME and the resolved A records
                    return listOf(cname) + targetRecords
                }

                // If we can't resolve the CNAME target, just return the CNAME
                return listOf(cname)
            }
        }

        return records ?: emptyList()
    }

    /**
     * Find resource records for a domain and type
     */
    private fun findRecords(domain: String, type: DnsType): List<DnsResourceRecord>? {
        // Extract the top-level domain
        val domainParts = domain.split(".")
        val tld = if (domainParts.size >= 2) {
            domainParts[domainParts.size - 2] + "." + domainParts[domainParts.size - 1]
        } else {
            return null
        }

        // Check if the TLD exists in our zone database
        val zone = zoneDatabase[tld] ?: return null

        // If ANY type is requested, return all records
        if (type == DnsType.ANY) {
            return zone.values.flatten()
        }

        // Get the records for the requested type
        val records = zone[type] ?: return null

        // Filter for the specific domain name
        return records.filter { it.name.equals(domain, ignoreCase = true) }
    }
}