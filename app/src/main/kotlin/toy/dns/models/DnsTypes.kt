package toy.dns.models

/**
 * Enum representing DNS resource record types
 */
enum class DnsType(val value: Int) {
    A(1),
    NS(2),
    CNAME(5),
    SOA(6),
    PTR(12),
    MX(15),
    TXT(16),
    AAAA(28),
    SRV(33),
    ANY(255);

    companion object {
        fun fromValue(value: Int): DnsType {
            return values().find { it.value == value } ?: throw IllegalArgumentException("Unknown DNS type: $value")
        }
    }
}

/**
 * Enum representing DNS classes
 */
enum class DnsClass(val value: Int) {
    IN(1),  // Internet
    CS(2),  // CSNET
    CH(3),  // CHAOS
    HS(4),  // Hesiod
    ANY(255);

    companion object {
        fun fromValue(value: Int): DnsClass {
            return values().find { it.value == value } ?: throw IllegalArgumentException("Unknown DNS class: $value")
        }
    }
}