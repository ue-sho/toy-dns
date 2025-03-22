package toy.dns.zone.parser

/**
 * Data class representing an SOA (Start of Authority) DNS record.
 *
 * @property name The domain name that this SOA record applies to
 * @property ttl Time to live value in seconds
 * @property mname The primary name server for this zone
 * @property rname The email address of the administrator responsible for this zone
 * @property serial Serial number for this zone file
 * @property refresh Time interval before the zone should be refreshed
 * @property retry Time interval that should elapse before a failed refresh should be retried
 * @property expire Time value that specifies the upper limit on the time interval that can elapse before the zone is no longer authoritative
 * @property title The minimum TTL field that should be exported with any RR from this zone
 */
data class SOARecord(
    val name: String = "",
    val ttl: Int = 0,
    val mname: String = "",
    val rname: String = "",
    val serial: String = "",
    val refresh: String = "",
    val retry: String = "",
    val expire: String = "",
    val title: String = ""
) {
    override fun toString(): String {
        return "SOARecord(name='$name', ttl=$ttl, mname='$mname', rname='$rname', serial='$serial', refresh='$refresh', retry='$retry', expire='$expire', title='$title')"
    }
}

/**
 * Parser for SOA (Start of Authority) records in DNS zone files.
 * Handles both single-line and multi-line SOA record formats.
 */
class SoaRecordParser {
    val recordType = "SOA"
    var isMultiLine = false

    /**
     * Determines if the given line contains an SOA record.
     *
     * @param splittedLine List of strings representing a split line from a zone file
     * @return true if the line contains an SOA record, false otherwise
     */
    fun isMatch(splittedLine: List<String>): Boolean {
        if ("SOA" in splittedLine) {
            isMultiLine = !isLastLine(splittedLine)
            return true
        }
        return isMultiLine
    }

    /**
     * Checks if the given line is the last line of a multi-line SOA record.
     *
     * @param splittedLine List of strings representing a split line from a zone file
     * @return true if the line is the last line of an SOA record, false otherwise
     */
    fun isLastLine(splittedLine: List<String>): Boolean {
       if (splittedLine.contains(")")) {
            isMultiLine = false
            return true
       }
       return false
    }

    /**
     * Parses a complete SOA record from a list of split lines.
     *
     * @param lines List of split lines forming an SOA record
     * @param previousTtl The previously encountered TTL value, if any
     * @param previousName The previously encountered domain name, if any
     * @param origin The origin domain specified in the zone file
     * @param ttl The default TTL value specified in the zone file
     * @return A fully parsed SOARecord
     */
    fun parse(lines: List<List<String>>, previousTtl: String? = null, previousName: String? = null, origin: String? = null, ttl: Int = 0): SOARecord {
        println("Processing SOA record with ${lines.size} lines")
        if (lines.isEmpty()) {
            return SOARecord()
        }

        val firstLine = lines[0]
        val currentName = firstLine[0]

        // Process single-line SOA record
        if (lines.size == 1 && firstLine.contains(")")) {
            return SOARecord(
                name = currentName,
                mname = firstLine[3],
                rname = firstLine[4],
                serial = firstLine[6],
                refresh = firstLine[7],
                retry = firstLine[8],
                expire = firstLine[9],
                title = firstLine[10]
            ).also { println("Added single-line SOA record: $it") }
        }
        // Process multi-line SOA record
        else if (lines.size > 1) {
            var serial = ""
            var refresh = ""
            var retry = ""
            var expire = ""
            var soaTtl = 0

            for (i in 1 until lines.size) {
                val lineNumber = i + 1
                val splittedLine = lines[i]

                when (lineNumber) {
                    2 -> {
                        serial = splittedLine[0]
                        println("Set SOA serial: $serial")
                    }
                    3 -> {
                        refresh = splittedLine[0]
                        println("Set SOA refresh: $refresh")
                    }
                    4 -> {
                        retry = splittedLine[0]
                        println("Set SOA retry: $retry")
                    }
                    5 -> {
                        expire = splittedLine[0]
                        println("Set SOA expire: $expire")
                    }
                    6 -> {
                        soaTtl = splittedLine[0].toInt()
                        println("Set SOA ttl: $soaTtl")
                    }
                }
            }

            return SOARecord(
                name = currentName,
                ttl = soaTtl,
                mname = firstLine[3],
                rname = firstLine[4],
                serial = serial,
                refresh = refresh,
                retry = retry,
                expire = expire
            ).also { println("Processed multi-line SOA record: $it") }
        }

        // Process when only the first line is provided
        return SOARecord(
            name = currentName,
            mname = firstLine[3],
            rname = firstLine[4]
        ).also { println("Initial SOA record: $it") }
    }
}


