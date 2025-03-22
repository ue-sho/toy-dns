package toy.dns.zone.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ZoneFileParserTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var testZoneFile: File
    private lateinit var parser: ZoneFileParser

    @BeforeEach
    fun setup() {
        testZoneFile = File(tempDir, "test.zone")
        parser = ZoneFileParser()
    }

    @Test
    fun `test processBindFile parses SOA record correctly`() {
        testZoneFile.writeText("""
            ${'$'}ORIGIN example.com.
            ${'$'}TTL 3600
            @	IN	SOA	dns1.example.com.	hostmaster.example.com. ( 2001062501 21600 3600 604800 86400 )
        """.trimIndent())

        val records = parser.processBindFile(testZoneFile.absolutePath)

        assertNotNull(records["SOA"])
        assertTrue(records["SOA"]?.isNotEmpty() == true)
        val soa = records["SOA"]?.first() as SOARecord
        assertEquals("@", soa.name)
        assertEquals("dns1.example.com.", soa.mname)
        assertEquals("hostmaster.example.com.", soa.rname)
        assertEquals("2001062501", soa.serial)
        assertEquals("21600", soa.refresh)
        assertEquals("3600", soa.retry)
        assertEquals("604800", soa.expire)
        assertEquals("86400", soa.title)
    }

    @Test
    fun `test processBindFile parses multiple line SOA record correctly`() {
        testZoneFile.writeText("""
            ${'$'}ORIGIN example.com.
            ${'$'}TTL 3600
            @ IN SOA ns1.example.com. admin.example.com. ( ; multiple line
                2023100101 ; serial number
                3600       ; refresh
                1800       ; retry
                604800    ; expire
                86400     ; ttl
            )
        """.trimIndent())

        val records = parser.processBindFile(testZoneFile.absolutePath)

        assertNotNull(records["SOA"])
        assertTrue(records["SOA"]?.isNotEmpty() == true)
        val soa = records["SOA"]?.first() as SOARecord
        assertEquals("@", soa.name)
        assertEquals("ns1.example.com.", soa.mname)
        assertEquals("admin.example.com.", soa.rname)
        assertEquals("2023100101", soa.serial)
        assertEquals("3600", soa.refresh)
        assertEquals("1800", soa.retry)
        assertEquals("604800", soa.expire)
        assertEquals(86400, soa.ttl)
    }

    @Test
    fun `test processBindFile parses NS records correctly`() {
        testZoneFile.writeText("""
            ${'$'}ORIGIN example.com.
            ${'$'}TTL 3600
            @ IN NS ns1.example.com.
            @ IN NS ns2.example.com.
        """.trimIndent())

        val records = parser.processBindFile(testZoneFile.absolutePath)

        assertNotNull(records["NS"])
        assertEquals(2, records["NS"]?.size)
        val ns1 = records["NS"]?.get(0) as ResourceRecord
        assertEquals("@", ns1.name)
        assertEquals("IN", ns1.`class`)
        assertEquals("NS", ns1.type)
        assertEquals("ns1.example.com.", ns1.data)
    }

    @Test
    fun `test processBindFile parses A records correctly`() {
        testZoneFile.writeText("""
            ${'$'}ORIGIN example.com.
            ${'$'}TTL 3600
            www IN A 192.168.1.1
            mail IN A 192.168.1.2
        """.trimIndent())

        val records = parser.processBindFile(testZoneFile.absolutePath)

        assertNotNull(records["A"])
        assertEquals(2, records["A"]?.size)
        val www = records["A"]?.get(0) as ResourceRecord
        assertEquals("www", www.name)
        assertEquals("IN", www.`class`)
        assertEquals("A", www.type)
        assertEquals("192.168.1.1", www.data)
    }

    @Test
    fun `test processBindFile parses MX records correctly`() {
        testZoneFile.writeText("""
            ${'$'}ORIGIN example.com.
            ${'$'}TTL 3600
                IN MX 10 mail.example.com.
                IN MX 20 backup-mail.example.com.
        """.trimIndent())

        val records = parser.processBindFile(testZoneFile.absolutePath)

        assertNotNull(records["MX"])
        assertEquals(2, records["MX"]?.size)
        val mx1 = records["MX"]?.get(0) as ResourceRecord
        assertEquals("@", mx1.name)
        assertEquals("IN", mx1.`class`)
        assertEquals("MX", mx1.type)
        assertEquals("mail.example.com.", mx1.data)
        assertEquals("10", mx1.preference)
    }

    @Test
    fun `test processBindFile parses CNAME records correctly`() {
        testZoneFile.writeText("""
            ${'$'}ORIGIN example.com.
            ${'$'}TTL 3600
            www IN A 192.168.1.1
            ftp IN CNAME www
        """.trimIndent())

        val records = parser.processBindFile(testZoneFile.absolutePath)

        assertNotNull(records["CNAME"])
        assertEquals(1, records["CNAME"]?.size)
        val cname = records["CNAME"]?.get(0) as ResourceRecord
        assertEquals("ftp", cname.name)
        assertEquals("IN", cname.`class`)
        assertEquals("CNAME", cname.type)
        assertEquals("www", cname.data)
    }

    @Test
    fun `test processBindFile handles comments correctly`() {
        testZoneFile.writeText("""
            ; This is a comment
            ${'$'}ORIGIN example.com. ; Origin comment
            ${'$'}TTL 3600
            @ IN SOA ns1.example.com. admin.example.com. ( ; SOA record
                2023100101 ; serial number
                3600       ; refresh
                1800       ; retry
                604800    ; expire
                86400     ; ttl
            )
            ; Another comment
            www IN A 192.168.1.1 ; IP address
        """.trimIndent())

        val records = parser.processBindFile(testZoneFile.absolutePath)

        assertNotNull(records["SOA"])
        assertNotNull(records["A"])
        val a = records["A"]?.get(0) as ResourceRecord
        assertEquals("192.168.1.1", a.data)
    }

    @Test
    fun `test processBindFile handles TTL in records correctly`() {
        testZoneFile.writeText("""
            ${'$'}ORIGIN example.com.
            ${'$'}TTL 3600
            www 7200 IN A 192.168.1.1
            mail IN A 192.168.1.2
        """.trimIndent())

        val records = parser.processBindFile(testZoneFile.absolutePath)

        assertNotNull(records["A"])
        val www = records["A"]?.get(0) as ResourceRecord
        assertEquals("7200", www.ttl)
        val mail = records["A"]?.get(1) as ResourceRecord
        assertEquals("192.168.1.2", mail.data)
    }
}
