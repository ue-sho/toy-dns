package toy.dns.zone.parser

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

interface ResourceRecordParser {
    val recordType: String

    fun isMatch(splittedLine: List<String>): Boolean {
        return recordType in splittedLine
    }

    fun parse(lines: List<List<String>>, previousTtl: String? = null, previousName: String? = null, origin: String? = null, ttl: Int = 0): ResourceRecord
}

class NsRecordParser: ResourceRecordParser {
    override val recordType = "NS"

    override fun parse(lines: List<List<String>>, previousTtl: String?, previousName: String?, origin: String?, ttl: Int): ResourceRecord {
        println("Processing NS record")
        if (lines.isEmpty()) {
            return ResourceRecord(type = recordType)
        }
        val rr = processRr(lines[0], false, previousTtl, previousName, origin, ttl.toString())
        return rr
    }
}

class ARecordParser: ResourceRecordParser {
    override val recordType = "A"

    override fun parse(lines: List<List<String>>, previousTtl: String?, previousName: String?, origin: String?, ttl: Int): ResourceRecord {
        println("Processing A record")
        if (lines.isEmpty()) {
            return ResourceRecord(type = recordType)
        }
        val rr = processRr(lines[0], false, previousTtl, previousName, origin, ttl.toString())
        return rr
    }
}

class CnameRecordParser: ResourceRecordParser {
    override val recordType = "CNAME"

    override fun parse(lines: List<List<String>>, previousTtl: String?, previousName: String?, origin: String?, ttl: Int): ResourceRecord {
        println("Processing CNAME record")
        if (lines.isEmpty()) {
            return ResourceRecord(type = recordType)
        }
        val rr = processRr(lines[0], false, previousTtl, previousName, origin, ttl.toString())
        return rr
    }
}

class MxRecordParser: ResourceRecordParser {
    override val recordType = "MX"

    override fun parse(lines: List<List<String>>, previousTtl: String?, previousName: String?, origin: String?, ttl: Int): ResourceRecord {
        println("Processing MX record")
        if (lines.isEmpty()) {
            return ResourceRecord(type = recordType)
        }
        val rr = processRr(lines[0], false, previousTtl, previousName, origin, ttl.toString(), true)
        return rr
    }
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
    ttl: String?,
    isMxType: Boolean = false
): ResourceRecord {
    println("Processing RR with line: ${splittedLine.joinToString(", ")}")
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

    rr.name = when {
        !localPreviousName.isNullOrEmpty() -> localPreviousName
        !origin.isNullOrEmpty() -> origin
        else -> ""
    }
    rr.ttl = localPreviousTtl ?: ttl ?: ""
    println("Final record values - name: ${rr.name}, ttl: ${rr.ttl}")

    return rr
}
