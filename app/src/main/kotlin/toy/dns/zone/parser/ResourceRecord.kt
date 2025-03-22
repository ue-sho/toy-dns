package toy.dns.zone.parser

/**
 * Data class representing a DNS resource record.
 *
 * @property name The domain name this record applies to
 * @property ttl Time to live value in seconds
 * @property type The DNS record type (e.g. A, NS, MX, CNAME)
 * @property data The record data (varies based on record type)
 * @property recordClass The DNS class (usually IN for Internet)
 * @property preference Priority value for MX records
 */
data class ResourceRecord(
    var name: String = "",
    var ttl: String = "",
    var type: String = "",
    var data: String = "",
    var recordClass: String = "",
    var preference: String = ""
) {
    override fun toString(): String {
        return "ResourceRecord(name='$name', ttl='$ttl', type='$type', data='$data', class='$recordClass', preference='$preference')"
    }
}

/**
 * Interface for DNS resource record parsers.
 * Defines common functionality for all record type parsers.
 */
interface ResourceRecordParser {
    val recordType: String

    /**
     * Determines if a line contains a record of this parser's type.
     *
     * @param splittedLine List of strings representing a split line from a zone file
     * @return true if the line contains this record type, false otherwise
     */
    fun isMatch(splittedLine: List<String>): Boolean {
        return recordType in splittedLine
    }

    /**
     * Parses lines containing a resource record into a ResourceRecord object.
     *
     * @param lines List of split lines forming a resource record
     * @param previousTtl The previously encountered TTL value, if any
     * @param previousName The previously encountered domain name, if any
     * @param origin The origin domain specified in the zone file
     * @param ttl The default TTL value specified in the zone file
     * @return A fully parsed ResourceRecord
     */
    fun parse(lines: List<List<String>>, previousTtl: String? = null, previousName: String? = null, origin: String? = null, ttl: Int = 0): ResourceRecord
}

/**
 * Parser for NS (Name Server) records in DNS zone files.
 */
class NsRecordParser: ResourceRecordParser {
    override val recordType = "NS"

    /**
     * Parses NS records into ResourceRecord objects.
     *
     * @param lines List of split lines forming an NS record
     * @param previousTtl The previously encountered TTL value, if any
     * @param previousName The previously encountered domain name, if any
     * @param origin The origin domain specified in the zone file
     * @param ttl The default TTL value specified in the zone file
     * @return A ResourceRecord representing the NS record
     */
    override fun parse(lines: List<List<String>>, previousTtl: String?, previousName: String?, origin: String?, ttl: Int): ResourceRecord {
        println("Processing NS record")
        if (lines.isEmpty()) {
            return ResourceRecord(type = recordType)
        }
        val rr = processRr(lines[0], false, previousTtl, previousName, origin, ttl.toString())
        return rr
    }
}

/**
 * Parser for A (Address) records in DNS zone files.
 */
class ARecordParser: ResourceRecordParser {
    override val recordType = "A"

    /**
     * Parses A records into ResourceRecord objects.
     *
     * @param lines List of split lines forming an A record
     * @param previousTtl The previously encountered TTL value, if any
     * @param previousName The previously encountered domain name, if any
     * @param origin The origin domain specified in the zone file
     * @param ttl The default TTL value specified in the zone file
     * @return A ResourceRecord representing the A record
     */
    override fun parse(lines: List<List<String>>, previousTtl: String?, previousName: String?, origin: String?, ttl: Int): ResourceRecord {
        println("Processing A record")
        if (lines.isEmpty()) {
            return ResourceRecord(type = recordType)
        }
        val rr = processRr(lines[0], false, previousTtl, previousName, origin, ttl.toString())
        return rr
    }
}

/**
 * Parser for CNAME (Canonical Name) records in DNS zone files.
 */
class CnameRecordParser: ResourceRecordParser {
    override val recordType = "CNAME"

    /**
     * Parses CNAME records into ResourceRecord objects.
     *
     * @param lines List of split lines forming a CNAME record
     * @param previousTtl The previously encountered TTL value, if any
     * @param previousName The previously encountered domain name, if any
     * @param origin The origin domain specified in the zone file
     * @param ttl The default TTL value specified in the zone file
     * @return A ResourceRecord representing the CNAME record
     */
    override fun parse(lines: List<List<String>>, previousTtl: String?, previousName: String?, origin: String?, ttl: Int): ResourceRecord {
        println("Processing CNAME record")
        if (lines.isEmpty()) {
            return ResourceRecord(type = recordType)
        }
        val rr = processRr(lines[0], false, previousTtl, previousName, origin, ttl.toString())
        return rr
    }
}

/**
 * Parser for MX (Mail Exchange) records in DNS zone files.
 */
class MxRecordParser: ResourceRecordParser {
    override val recordType = "MX"

    /**
     * Parses MX records into ResourceRecord objects.
     *
     * @param lines List of split lines forming an MX record
     * @param previousTtl The previously encountered TTL value, if any
     * @param previousName The previously encountered domain name, if any
     * @param origin The origin domain specified in the zone file
     * @param ttl The default TTL value specified in the zone file
     * @return A ResourceRecord representing the MX record
     */
    override fun parse(lines: List<List<String>>, previousTtl: String?, previousName: String?, origin: String?, ttl: Int): ResourceRecord {
        println("Processing MX record")
        if (lines.isEmpty()) {
            return ResourceRecord(type = recordType)
        }
        val rr = processRr(lines[0], false, previousTtl, previousName, origin, ttl.toString(), true)
        return rr
    }
}

/**
 * Finds the index of the TTL value in a split line.
 *
 * @param splittedLine List of strings representing a split line from a zone file
 * @param isMx Whether the line is for an MX record
 * @return The index of the TTL value, or null if not found
 */
private fun findTtlIndex(splittedLine: List<String>, isMx: Boolean): Int? {
    val index = splittedLine.withIndex()
        .firstOrNull { (_, element) ->
            !element.contains('.') && element.toIntOrNull() != null && !isMx
        }?.index
    println("Found TTL index: $index in line: ${splittedLine.joinToString(", ")}")
    return index
}

/**
 * Processes a DNS resource record line into a ResourceRecord object.
 *
 * @param splittedLine List of strings representing a split line from a zone file
 * @param containsTtl Whether the line contains a TTL value
 * @param previousTtl The previously encountered TTL value, if any
 * @param previousName The previously encountered domain name, if any
 * @param origin The origin domain specified in the zone file
 * @param ttl The default TTL value specified in the zone file
 * @param isMxType Whether the record is an MX record
 * @return A processed ResourceRecord
 */
private fun processRr(
    splittedLine: List<String>,
    containsTtl: Boolean,
    previousTtl: String?,
    previousName: String?,
    origin: String?,
    ttl: String?,
    isMxType: Boolean = false
): ResourceRecord {
    println("Processing Resource Record with line: ${splittedLine.joinToString(", ")}")
    println("Initial values - containsTtl: $containsTtl, previousTtl: $previousTtl, previousName: $previousName, origin: $origin, ttl: $ttl")

    var localContainsTtl = containsTtl
    var localPreviousTtl = previousTtl
    var localPreviousName = previousName
    val rr = ResourceRecord()

    val totalLength = splittedLine.size
    val isMx = isMxType || splittedLine.getOrNull(totalLength - 2)?.toIntOrNull() != null
    println("Line length: $totalLength, isMX: $isMx")

    val mutableSplittedLine = splittedLine.toMutableList()

    when (totalLength) {
        5 -> {
            findTtlIndex(splittedLine, isMx)?.let { index ->
                localContainsTtl = true
                localPreviousTtl = splittedLine[index]
                mutableSplittedLine.removeAt(index)
                println("Found TTL in 5-part line: $localPreviousTtl")
            }

            if (!isMx) {
                localPreviousName = mutableSplittedLine.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: localPreviousName
                rr.recordClass = mutableSplittedLine.getOrNull(1) ?: ""
                rr.type = mutableSplittedLine.getOrNull(2) ?: ""
                rr.data = mutableSplittedLine.getOrNull(3) ?: ""
                println("Processed 5-part non-MX record: class=${rr.recordClass}, type=${rr.type}, data=${rr.data}")
            }
        }
        4 -> {
            findTtlIndex(splittedLine, isMx)?.let { index ->
                localContainsTtl = true
                localPreviousTtl = splittedLine[index]
                mutableSplittedLine.removeAt(index)
                println("Found TTL in 4-part line: $localPreviousTtl")
            }

            if (localContainsTtl) {
                rr.recordClass = mutableSplittedLine.getOrNull(0) ?: ""
                rr.type = mutableSplittedLine.getOrNull(1) ?: ""
                rr.data = mutableSplittedLine.getOrNull(2) ?: ""
                println("Processed 4-part record with TTL: class=${rr.recordClass}, type=${rr.type}, data=${rr.data}")
            } else {
                if (isMx) {
                    localPreviousName = "@"
                    rr.recordClass = mutableSplittedLine.getOrNull(0) ?: ""
                    rr.type = mutableSplittedLine.getOrNull(1) ?: ""
                    rr.preference = mutableSplittedLine.getOrNull(2) ?: ""
                    rr.data = mutableSplittedLine.getOrNull(3) ?: ""
                    println("Processed MX record: class=${rr.recordClass}, type=${rr.type}, preference=${rr.preference}, data=${rr.data}")
                } else {
                    localPreviousName = mutableSplittedLine.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: localPreviousName
                    rr.recordClass = mutableSplittedLine.getOrNull(1) ?: ""
                    rr.type = mutableSplittedLine.getOrNull(2) ?: ""
                    rr.data = mutableSplittedLine.getOrNull(3) ?: ""
                    println("Processed 4-part non-MX record: class=${rr.recordClass}, type=${rr.type}, data=${rr.data}")
                }
            }
        }
        3 -> {
            rr.recordClass = mutableSplittedLine.getOrNull(0) ?: ""
            rr.type = mutableSplittedLine.getOrNull(1) ?: ""
            rr.data = mutableSplittedLine.getOrNull(2) ?: ""
            println("Processed 3-part record: class=${rr.recordClass}, type=${rr.type}, data=${rr.data}")
        }
    }

    rr.name = localPreviousName ?: origin ?: ""
    rr.ttl = localPreviousTtl ?: ttl ?: ""
    println("Final record values - name: ${rr.name}, ttl: ${rr.ttl}")

    return rr
}
