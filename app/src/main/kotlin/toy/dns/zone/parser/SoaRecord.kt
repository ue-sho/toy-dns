package toy.dns.zone.parser

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


class SoaRecordParser {
    val recordType = "SOA"
    var isMultiLine = false

    fun isMatch(splittedLine: List<String>): Boolean {
        if ("SOA" in splittedLine) {
            isMultiLine = !isLastLine(splittedLine)
            return true
        }
        return isMultiLine
    }

    fun isLastLine(splittedLine: List<String>): Boolean {
       if (splittedLine.contains(")")) {
            isMultiLine = false
            return true
       }
       return false
    }

    fun parse(lines: List<List<String>>, previousTtl: String? = null, previousName: String? = null, origin: String? = null, ttl: Int = 0): SOARecord {
        println("Processing SOA record with ${lines.size} lines")
        if (lines.isEmpty()) {
            return SOARecord()
        }

        val firstLine = lines[0]
        val currentName = firstLine[0]

        // 単一行SOAレコードの処理
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
        // 複数行SOAレコードの処理
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

        // 最初の行だけが渡された場合
        return SOARecord(
            name = currentName,
            mname = firstLine[3],
            rname = firstLine[4]
        ).also { println("Initial SOA record: $it") }
    }
}


