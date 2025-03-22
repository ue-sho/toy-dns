# DNS Flow and Packet Structure

This document describes the general flow of DNS resolution and the structure of DNS packets.

## DNS Resolution Flow

```mermaid
sequenceDiagram
    participant Client as クライアント
    participant Resolver as ローカルDNSリゾルバ
    participant Root as ルートDNSサーバー
    participant TLD as TLDネームサーバー
    participant Auth as 権威ネームサーバー

    Client->>Resolver: www.example.com の IPアドレスを問い合わせ

    alt キャッシュにある場合
        Resolver-->>Client: キャッシュから IPアドレスを返す
    else キャッシュにない場合
        Resolver->>Root: www.example.com の問い合わせ
        Root-->>Resolver: .com TLDサーバーの情報

        Resolver->>TLD: www.example.com の問い合わせ
        TLD-->>Resolver: example.com の権威サーバー情報

        Resolver->>Auth: www.example.com の問い合わせ
        Auth-->>Resolver: www.example.com の IPアドレス

        Resolver-->>Client: www.example.com の IPアドレスを返す
    end
```

## DNS Recursive Resolution Process

```mermaid
flowchart TD
    A[クライアント] -->|1. www.example.com の問い合わせ| B[ローカルDNSリゾルバ]

    B -->|2. キャッシュ確認| C{キャッシュにある?}
    C -->|はい| D[キャッシュから応答]
    D -->|12. IPアドレス応答| A

    C -->|いいえ| E[ルートDNSサーバーに問い合わせ]
    E -->|3. .com TLDサーバー情報| F[.com TLDサーバーに問い合わせ]
    F -->|4. example.com 権威サーバー情報| G[権威サーバーに問い合わせ]
    G -->|5. www.example.comのIPアドレス| H[結果をキャッシュ]
    H -->|6. IPアドレス応答| A

    subgraph "再帰的解決プロセス"
        E
        F
        G
        H
    end
```

## DNS Packet Structure

```mermaid
classDiagram
    class DNSHeader {
        16ビット: トランザクションID
        16ビット: フラグ
        16ビット: 質問数
        16ビット: 回答RR数
        16ビット: 権威RR数
        16ビット: 追加RR数
    }

    class QuestionSection {
        可変長: クエリ名 (ドメイン名)
        16ビット: クエリタイプ
        16ビット: クエリクラス
    }

    class AnswerSection {
        可変長: ドメイン名
        16ビット: タイプ
        16ビット: クラス
        32ビット: TTL
        16ビット: データ長
        可変長: データ (例: IPアドレス)
    }

    class DNSPacket {
        ヘッダー
        質問セクション
        回答セクション
        権威セクション
        追加情報セクション
    }

    DNSPacket *-- DNSHeader
    DNSPacket *-- QuestionSection
    DNSPacket *-- AnswerSection
```

## DNS Message Format (Binary)

```mermaid
classDiagram
    class Header {
        +ID (16 bits)
        +QR (1 bit): 0=query, 1=response
        +OPCODE (4 bits): 0=standard query
        +AA (1 bit): Authoritative Answer
        +TC (1 bit): Truncation
        +RD (1 bit): Recursion Desired
        +RA (1 bit): Recursion Available
        +Z (3 bits): Reserved
        +RCODE (4 bits): Response code
        +QDCOUNT (16 bits): Question count
        +ANCOUNT (16 bits): Answer count
        +NSCOUNT (16 bits): Authority count
        +ARCOUNT (16 bits): Additional count
    }

    class Question {
        +QNAME: ドメイン名 (可変長)
        +QTYPE (16 bits): リクエストタイプ
        +QCLASS (16 bits): クラス
    }

    class ResourceRecord {
        +NAME: ドメイン名 (可変長)
        +TYPE (16 bits): レコードタイプ
        +CLASS (16 bits): クラス
        +TTL (32 bits): キャッシュ期間
        +RDLENGTH (16 bits): RDATAの長さ
        +RDATA (可変長): リソースデータ
    }

    class DNSMessage {
        +Header
        +Questions[]
        +Answers[]
        +Authority[]
        +Additional[]
    }

    DNSMessage *-- Header
    DNSMessage *-- Question
    DNSMessage *-- ResourceRecord
```

## DNS Record Types

```mermaid
classDiagram
    class RecordTypes {
        A (1): IPv4アドレス
        AAAA (28): IPv6アドレス
        CNAME (5): 正規名
        MX (15): メールサーバー
        NS (2): ネームサーバー
        PTR (12): 逆引き
        SOA (6): 権威の開始
        TXT (16): テキスト
        SRV (33): サービス
    }
```

## DNS Query and Response Processing

```mermaid
flowchart LR
    A[DNSクエリ生成] --> B[UDPパケット作成]
    B --> C[送信]
    C --> D[レスポンス受信]
    D --> E[DNSヘッダー解析]
    E --> F[レスポンスコード確認]

    F -->|成功| G[回答セクション解析]
    F -->|エラー| H[エラー処理]

    G --> I[IPアドレス抽出]
    I --> J[クライアントへ返却]

    subgraph "DNSサーバー内部処理"
        K[クエリ受信] --> L[クエリ解析]
        L --> M{キャッシュにある?}
        M -->|はい| N[キャッシュから応答]
        M -->|いいえ| O[再帰的解決]
        O --> P[応答パケット作成]
        N --> P
        P --> Q[応答送信]
    end
```