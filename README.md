# Toy DNS Server

A simple DNS server implementation in Kotlin.

## Features

- Basic DNS request and response handling
- Support for A, CNAME, MX, and TXT record types
- Simple in-memory zone database for domain resolution
- Asynchronous request handling with coroutines

## Running the server

```bash
./gradlew run
```

Note: Running a DNS server typically requires root privileges as it uses port 53.

## Testing

You can test the DNS server using tools like `dig`:

```bash
dig @127.0.0.1 example.com
dig @127.0.0.1 www.example.com
dig @127.0.0.1 test.com
```

## Implementation Details

The server is implemented with the following components:

- `DnsServer`: Main server that listens for UDP packets and handles requests
- `DnsParser`: Parses DNS request packets into model objects
- `DnsEncoder`: Encodes DNS response models back into binary format
- `DnsResolver`: Resolves domain names from a simple in-memory zone database

## To Do

- Implement more record types (AAAA, SRV, etc.)
- Add zone file loading
- Support for DNS over TCP
- Secondary/slave support
- Cache for frequently requested domains
