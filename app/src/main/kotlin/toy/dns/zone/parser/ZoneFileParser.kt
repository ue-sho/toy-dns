package toy.dns.zone.parser

import java.io.File

data class SOARecord(
    var name: String = "",
    var ttl: Int = 0,
    var mname: String = "",
    var rname: String = "",
    var serial: String = "",
    var refresh: String = "",
    var retry: String = "",
    var expire: String = "",
    var title: String = ""
) {
    override fun toString(): String {
        return "SOARecord(name='$name', ttl=$ttl, mname='$mname', rname='$rname', serial='$serial', refresh='$refresh', retry='$retry', expire='$expire', title='$title')"
    }
}

data class ResourceRecord(
    var name: String = "",
    var ttl: String = "",
    var type: String = "",
    var data: String = "",
    var `class`: String = "",
    var preference: String = ""
) {
    override fun toString(): String {
        return "ResourceRecord(name='$name', ttl='$ttl', type='$type', data='$data', class='$`class`', preference='$preference')"
    }
}

class ZoneFileParser {
    fun processBindFile(filePath: String = "zones/example2.com.zone"): Map<String, MutableList<Any>> {
        println("Starting to process file: $filePath")
        var origin: String? = null
        var ttl: String? = null
        var previousTtl: String? = null
        var previousName: String? = null

        val records = mutableMapOf<String, MutableList<Any>>()
        val recordsType = listOf("SOA", "NS", "A", "CNAME", "MX")
        recordsType.forEach { type ->
            records[type] = mutableListOf()
        }

        var soa = SOARecord()
        var soaLine = 0
        var multiLineSoa = false
        var containsTtl = false
        var type = "SOA"

        val reader = File(filePath).bufferedReader()
        reader.use { bufferedReader ->
            bufferedReader.lineSequence().forEach { line ->
                println("Processing line: $line")
                if (line.length > 0) {
                    var commentedLine = false
                    var l = line.trim()
                        .replace("\t", " ")
                        .replace(Regex("\\s+"), " ")
                    println("Cleaned line: $l")

                    val commentIndex = l.indexOf(';')
                    if (commentIndex != -1) {
                        if (commentIndex != 0) {
                            val m = l.split(';')
                            l = m[0]
                            println("Removed inline comment: $l")
                        } else {
                            commentedLine = true
                            println("Skipping comment line")
                        }
                    }

                    if (!commentedLine) {
                        val splittedLine = l.split(" ")
                        println("Split line: ${splittedLine.joinToString(", ")}")
                        when (splittedLine[0]) {
                            "\$ORIGIN" -> {
                                origin = splittedLine[1]
                                println("Set ORIGIN: $origin")
                            }
                            "\$TTL" -> {
                                ttl = splittedLine[1]
                                println("Set TTL: $ttl")
                            }
                            "\$INCLUDE" -> println("Skipping INCLUDE directive")
                            else -> {
                                if (splittedLine.contains("SOA")) {
                                    println("Processing SOA record")
                                    val currentName = splittedLine[0]
                                    previousName = currentName
                                    soa = SOARecord(
                                        name = currentName,
                                        mname = splittedLine[3],
                                        rname = splittedLine[4]
                                    )
                                    println("Initial SOA record: $soa")

                                    if (splittedLine.contains(")")) {
                                        soa.serial = splittedLine[6]
                                        soa.refresh = splittedLine[7]
                                        soa.retry = splittedLine[8]
                                        soa.expire = splittedLine[9]
                                        soa.title = splittedLine[10]
                                        records["SOA"]?.add(soa)
                                        println("Added single-line SOA record: $soa")
                                    } else {
                                        multiLineSoa = true
                                        soaLine++
                                        println("Started multi-line SOA record, line: $soaLine")
                                    }
                                }

                                if (multiLineSoa) {
                                    println("Processing multi-line SOA, line: $soaLine")
                                    when (soaLine) {
                                        2 -> {
                                            soa.serial = splittedLine[0]
                                            println("Set SOA serial: ${soa.serial}")
                                        }
                                        3 -> {
                                            soa.refresh = splittedLine[0]
                                            println("Set SOA refresh: ${soa.refresh}")
                                        }
                                        4 -> {
                                            soa.retry = splittedLine[0]
                                            println("Set SOA retry: ${soa.retry}")
                                        }
                                        5 -> {
                                            soa.expire = splittedLine[0]
                                            println("Set SOA expire: ${soa.expire}")
                                        }
                                        6 -> {
                                            soa.ttl = splittedLine[0].toInt()
                                            println("Set SOA ttl: ${soa.ttl}")
                                        }
                                    }
                                    if (splittedLine.contains(")")) {
                                        records["SOA"]?.add(soa)
                                        multiLineSoa = false
                                        println("Completed multi-line SOA record: $soa")
                                    }
                                    soaLine++
                                }

                                for (element in recordsType) {
                                    if (splittedLine.contains(element)) {
                                        type = element
                                        if (type != "SOA") {
                                            println("Processing $type record")
                                            val (rr, newPreviousName, newPreviousTtl) = processRr(
                                                splittedLine,
                                                containsTtl,
                                                previousTtl,
                                                previousName,
                                                origin,
                                                ttl
                                            )
                                            previousName = newPreviousName
                                            previousTtl = newPreviousTtl
                                            records[type]?.add(rr)
                                            println("Added $type record: $rr")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        println("Completed processing file. Records: $records")
        return records
    }

    private fun findTtlIndex(splittedLine: List<String>, isMx: Boolean): Int? {
        val index = splittedLine.withIndex()
            .firstOrNull { (_, element) ->
                !element.contains('.') && element.toIntOrNull() != null && !isMx
            }?.index
        println("Found TTL index: $index in line: ${splittedLine.joinToString(", ")}")
        return index
    }

    private fun processRr(
        splittedLine: List<String>,
        containsTtl: Boolean,
        previousTtl: String?,
        previousName: String?,
        origin: String?,
        ttl: String?
    ): Triple<ResourceRecord, String?, String?> {
        println("Processing RR with line: ${splittedLine.joinToString(", ")}")
        println("Initial values - containsTtl: $containsTtl, previousTtl: $previousTtl, previousName: $previousName, origin: $origin, ttl: $ttl")

        var localContainsTtl = containsTtl
        var localPreviousTtl = previousTtl
        var localPreviousName = previousName
        val rr = ResourceRecord()

        val totalLength = splittedLine.size
        val isMx = splittedLine.getOrNull(totalLength - 2)?.toIntOrNull() != null
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
                    rr.`class` = mutableSplittedLine.getOrNull(1) ?: ""
                    rr.type = mutableSplittedLine.getOrNull(2) ?: ""
                    rr.data = mutableSplittedLine.getOrNull(3) ?: ""
                    println("Processed 5-part non-MX record: class=${rr.`class`}, type=${rr.type}, data=${rr.data}")
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
                    rr.`class` = mutableSplittedLine.getOrNull(0) ?: ""
                    rr.type = mutableSplittedLine.getOrNull(1) ?: ""
                    rr.data = mutableSplittedLine.getOrNull(2) ?: ""
                    println("Processed 4-part record with TTL: class=${rr.`class`}, type=${rr.type}, data=${rr.data}")
                } else {
                    if (isMx) {
                        localPreviousName = "@"
                        rr.`class` = mutableSplittedLine.getOrNull(0) ?: ""
                        rr.type = mutableSplittedLine.getOrNull(1) ?: ""
                        rr.preference = mutableSplittedLine.getOrNull(2) ?: ""
                        rr.data = mutableSplittedLine.getOrNull(3) ?: ""
                        println("Processed MX record: class=${rr.`class`}, type=${rr.type}, preference=${rr.preference}, data=${rr.data}")
                    } else {
                        localPreviousName = mutableSplittedLine.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: localPreviousName
                        rr.`class` = mutableSplittedLine.getOrNull(1) ?: ""
                        rr.type = mutableSplittedLine.getOrNull(2) ?: ""
                        rr.data = mutableSplittedLine.getOrNull(3) ?: ""
                        println("Processed 4-part non-MX record: class=${rr.`class`}, type=${rr.type}, data=${rr.data}")
                    }
                }
            }
            3 -> {
                rr.`class` = mutableSplittedLine.getOrNull(0) ?: ""
                rr.type = mutableSplittedLine.getOrNull(1) ?: ""
                rr.data = mutableSplittedLine.getOrNull(2) ?: ""
                println("Processed 3-part record: class=${rr.`class`}, type=${rr.type}, data=${rr.data}")
            }
        }

        rr.name = when {
            !localPreviousName.isNullOrEmpty() -> localPreviousName
            !origin.isNullOrEmpty() -> origin
            else -> ""
        }
        rr.ttl = localPreviousTtl ?: ttl ?: ""
        println("Final record values - name: ${rr.name}, ttl: ${rr.ttl}")

        return Triple(rr, localPreviousName, localPreviousTtl)
    }
}