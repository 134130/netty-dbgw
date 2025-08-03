# netty-dbgw

`netty-dbgw` is a proxy server for the [MySQL protocol](https://dev.mysql.com/doc/dev/mysql-server/latest/PAGE_PROTOCOL.html), 
built using the [Netty](https://netty.io/) framework. It acts as an intermediary between a MySQL client and a MySQL server, 
providing the capability to parse and handle protocol packets at a granular level.

## How to Run

### Prerequisites
- Java 21 or higher
- Run MySQL server with docker container:
  ```shell
  docker run --name mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -d mysql:latest
  ```

### Run

```shell
./gradlew run --args='--port=3307 --upstream=localhost:3306'
```

# or
```shell
./gradlew installDist
./app/build/install/app/bin/app --port=3307 --upstream=localhost:3306
```


## Features & Roadmap

### MySQL Protocol Features

- [x] **Connection Phase**
    - [x] Initial Handshake
        - [x] Plain Handshake
        - [x] SSL Handshake
            - [x] TLS Handshake
            - [ ] mTLS Handshake 
        - [x] Capability Negotiation
    - [ ] Authentication Phase Fast Path
    - [x] Authentication Method Mismatch
    - [ ] COM_CHANGE_USER
    - [x] Authentication Methods
        - [x] Old Password Authentication
        - [x] caching_sha2_password
        - [x] sha256_password
        - [x] Clear text client plugin
        - [ ] Windows Native Authentication
        - [ ] authentication_webauthn
    - [ ] Multi-Factor Authentication (MFA)
- [x] **Command Phase**
    - [x] Text Protocol: `COM_QUERY`
        - [ ] LOCAL INFILE Request
        - [x] Text Resultset Response
    - [ ] Utility Commands
        - [x] COM_QUIT
        - [ ] COM_INIT_DB
        - [ ] COM_FIELD_LIST
        - [ ] COM_STATISTICS
        - [x] COM_DEBUG
        - [x] COM_PING
        - [ ] COM_CHANGE_USER
        - [ ] COM_RESET_CONNECTION
        - [ ] COM_SET_OPTION
    - [x] Prepared Statements 
        - [x] COM_STMT_PREPARE
        - [x] COM_STMT_EXECUTE
        - [x] COM_STMT_CLOSE
        - [ ] COM_STMT_RESET
        - [ ] COM_STMT_SEND_LONG_DATA
    - [ ] Stored Programs
        - [ ] Multi-Resultset
        - [ ] Multi-Statement
- [ ] **Replication Protocol**
    - [ ] Binlog File 
    - [ ] Binlog Network Stream 
    - [ ] Binlog Version
    - [ ] Binlog Event
    - [ ] COM_BINLOG_DUMP

### Security Features

- [ ] Connection Rate Limiting
- [ ] Modifying Server Capabilities
- [ ] Authentication Modification
- [x] Preventing SQL Queries
- [x] Modifying SQL Queries
- [x] Query Logging and Auditing
- [ ] Query Rate Limiting
- [x] Modifying Result Sets
- [x] Row-Level Security

### Another Database Protocols

- [x] PostgreSQL

## Benchmarks

### Handling a 4GB Result Set (1.4k rows / 3MB each)

- Direct MySQL connection: ~37.75 seconds
- With `netty-dbgw` proxy: ~38.61 seconds