package toy.dns.zone.parser

import java.io.File

/**
 * Parser for DNS zone files in BIND format.
 * Supports parsing SOA, NS, A, CNAME, and MX records.
 */
class ZoneFileParser {
    private var ttl = 0
    private var origin = ""

    private val soaRecordParser = SoaRecordParser()
    private val resourceRecordParsers = listOf(
        NsRecordParser(),
        ARecordParser(),
        CnameRecordParser(),
        MxRecordParser()
    )

    /**
     * Removes comments from a line and cleans it up.
     *
     * @param line The raw line from a zone file
     * @return The cleaned line with comments removed
     */
    private fun trimComment(line: String): String {
        var cleanedLine = line.trim()
            .replace("\t", " ")
            .replace(Regex("\\s+"), " ")
        println("Cleaned line: $cleanedLine")

        val commentIndex = cleanedLine.indexOf(';')
        if (commentIndex == -1) {
            // No comment found
            return cleanedLine
        }

        if (commentIndex != 0) {
            val splittedLineByComment = cleanedLine.split(';')
            println("Removed inline comment: ${splittedLineByComment[0]}")
            return splittedLineByComment[0]
        }

        // If comment line, return empty string
        return ""
    }

    /**
     * Processes a BIND format zone file and extracts DNS records.
     *
     * @param filePath Path to the zone file to process
     * @return A map of record types to lists of parsed records
     */
    fun processBindFile(filePath: String): Map<String, MutableList<Any>> {
        println("Starting to process file: $filePath")
        var previousTtl: String? = null
        var previousName: String? = null

        val records = mutableMapOf<String, MutableList<Any>>()
        val recordsType = listOf("SOA", "NS", "A", "CNAME", "MX")
        recordsType.forEach { type ->
            records[type] = mutableListOf()
        }

        var multiLineSoa = false
        var soaLines = mutableListOf<List<String>>()

        val reader = File(filePath).bufferedReader()
        reader.use { bufferedReader ->
            bufferedReader.lineSequence().forEach { line ->
                println("Processing line: $line")
                if (line.isEmpty()) {
                    return@forEach
                }

                val trimmedCommentLine = trimComment(line)
                if (trimmedCommentLine.isEmpty()) {
                    return@forEach
                }

                val splittedLine = trimmedCommentLine.split(" ")
                println("Split line: ${splittedLine.joinToString(", ")}")

                when (splittedLine[0]) {
                    "\$ORIGIN" -> {
                        origin = splittedLine[1]
                        println("Set ORIGIN: $origin")
                    }
                    "\$TTL" -> {
                        ttl = splittedLine[1].toInt()
                        println("Set TTL: $ttl")
                    }
                    "\$INCLUDE" -> println("Skipping INCLUDE directive")
                    else -> {
                        // Detect SOA record
                        if (soaRecordParser.isMatch(splittedLine)) {
                            soaLines.add(splittedLine)
                            if (soaRecordParser.isLastLine(splittedLine)) {
                                val soa = soaRecordParser.parse(soaLines, previousTtl, previousName, origin, ttl)
                                previousName = soa.name
                                records["SOA"]?.add(soa)
                                soaLines = mutableListOf()
                            }
                            return@forEach
                        }

                        for (parser in resourceRecordParsers) {
                            if (parser.isMatch(splittedLine)) {
                                val recordType = parser.recordType
                                val record = parser.parse(listOf(splittedLine), previousTtl, previousName, origin, ttl)
                                previousName = record.name
                                previousTtl = record.ttl
                                records[recordType]?.add(record)
                                println("Added $recordType record: $record")
                                break
                            }
                        }
                    }
                }
            }
        }
        println("Completed processing file. Records: $records")
        return records
    }
}