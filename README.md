# Toy DNS Server

A simple DNS server implementation in Kotlin. This project is based on the Node.js implementation [engineerhead/dns-server](https://github.com/engineerhead/dns-server).

## Features

- Parse zone files in BIND format
- Serve A, NS, MX, CNAME, SOA and TXT records
- UDP server implementation
- Thread-based processing of DNS queries

## Building the Project

```bash
./gradlew build
```

## Running the DNS Server

Run using Gradle:

```bash
./gradlew run
```

Or run the JAR directly (requires sudo privileges to bind to port 53):

```bash
sudo java -jar app/build/libs/toy-dns-dist.jar [PORT] [ZONE_FILE]
```

- `PORT`: Optional. The port to listen on (default: 53)
- `ZONE_FILE`: Optional. Path to the zone file (default: app/src/main/resources/zone.txt)

## Testing the DNS Server

You can test the DNS server using tools like `dig`:

```bash
dig @127.0.0.1 -p 53 example.com
```

Or using `nslookup`:

```bash
nslookup example.com 127.0.0.1
```

## Creating a Zone File

The DNS server uses BIND format zone files. A sample zone file is provided in `app/src/main/resources/zone.txt`.

Here's a basic example:

```
$ORIGIN example.com.
$TTL 86400

; SOA Record
@       IN      SOA     ns1.example.com. admin.example.com. (
                        2023010101 ; Serial
                        3600       ; Refresh
                        1800       ; Retry
                        604800     ; Expire
                        86400 )    ; Minimum TTL

; A Records
@       IN      A       192.168.1.1
www     IN      A       192.168.1.2

; MX Records
@       IN      MX      10      mail.example.com.
```

## Project Structure

- `app/src/main/kotlin/toy/dns/`: Main DNS server implementation
  - `Main.kt`: Entry point of the application
  - `DnsServer.kt`: Core DNS server implementation
  - `message/`: DNS packet parsing and serialization
  - `zone/parser/`: Zone file parsing
