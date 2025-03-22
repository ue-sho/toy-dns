package toy.dns.zone.parser

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


class SoaRecordParser {
    fun isMatch(splittedLine: List<String>): Boolean {
        return "SOA" in splittedLine
    }

    fun parse(lines: List<List<String>>, previousTtl: String? = null, previousName: String? = null, origin: String? = null, ttl: Int = 0): SOARecord {
        println("Processing SOA record with ${lines.size} lines")
        if (lines.isEmpty()) {
            return SOARecord()
        }

        val firstLine = lines[0]
        val currentName = firstLine[0]
        val soa = SOARecord(
            name = currentName,
            mname = firstLine[3],
            rname = firstLine[4]
        )
        println("Initial SOA record: $soa")

        // 単一行SOAレコードの処理
        if (lines.size == 1 && firstLine.contains(")")) {
            soa.serial = firstLine[6]
            soa.refresh = firstLine[7]
            soa.retry = firstLine[8]
            soa.expire = firstLine[9]
            soa.title = firstLine[10]
            println("Added single-line SOA record: $soa")
            return soa
        }
        // 複数行SOAレコードの処理
        else if (lines.size > 1) {
            for (i in 1 until lines.size) {
                processMultiLineSoaLine(soa, lines[i], i + 1)
            }
            println("Processed multi-line SOA record: $soa")
            return soa
        }

        // 最初の行だけが渡された場合
        return soa
    }

    private fun processMultiLineSoaLine(soa: SOARecord, splittedLine: List<String>, lineNumber: Int): SOARecord {
        println("Processing multi-line SOA, line: $lineNumber")
        when (lineNumber) {
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
        return soa
    }
}


