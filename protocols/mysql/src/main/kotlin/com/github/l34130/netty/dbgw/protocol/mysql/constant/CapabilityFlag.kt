package com.github.l34130.netty.dbgw.protocol.mysql.constant

import com.github.l34130.netty.dbgw.core.utils.Flag

// https://dev.mysql.com/doc/dev/mysql-server/latest/group__group__cs__capabilities__flags.html
internal enum class CapabilityFlag(
    override val value: ULong,
) : Flag {
    CLIENT_LONG_PASSWORD(1UL shl 0),
    CLIENT_FOUND_ROWS(1UL shl 1),
    CLIENT_LONG_FLAG(1UL shl 2),
    CLIENT_CONNECT_WITH_DB(1UL shl 3),
    CLIENT_NO_SCHEMA(1UL shl 4),
    CLIENT_COMPRESS(1UL shl 5),
    CLIENT_ODBC(1UL shl 6),
    CLIENT_LOCAL_FILES(1UL shl 7),
    CLIENT_IGNORE_SPACE(1UL shl 8),
    CLIENT_PROTOCOL_41(1UL shl 9),
    CLIENT_INTERACTIVE(1UL shl 10),
    CLIENT_SSL(1UL shl 11),
    CLIENT_IGNORE_SIGPIPE(1UL shl 12),
    CLIENT_TRANSACTIONS(1UL shl 13),
    CLIENT_RESERVED(1UL shl 14),

    /**
     * Old flag for CLIENT_SECURE_CONNECTION, deprecated in MySQL 5.6.6.
     */
    CLIENT_RESERVED2(1UL shl 15),
    CLIENT_MULTI_STATEMENTS(1UL shl 16),
    CLIENT_MULTI_RESULTS(1UL shl 17),
    CLIENT_PS_MULTI_RESULTS(1UL shl 18),
    CLIENT_PLUGIN_AUTH(1UL shl 19),
    CLIENT_CONNECT_ATTRS(1UL shl 20),
    CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA(1UL shl 21),
    CLIENT_CAN_HANDLE_EXPIRED_PASSWORDS(1UL shl 22),
    CLIENT_SESSION_TRACK(1UL shl 23),
    CLIENT_DEPRECATE_EOF(1UL shl 24),
    CLIENT_OPTIONAL_RESULTSET_METADATA(1UL shl 25),
    CLIENT_ZSTD_COMPRESSION_ALGORITHM(1UL shl 26),
    CLIENT_QUERY_ATTRIBUTES(1UL shl 27),
    MULTI_FACTOR_AUTHENTICATION(1UL shl 28),
    CLIENT_CAPABILITY_EXTENSION(1UL shl 29),
    CLIENT_SSL_VERIFY_SERVER_CERT(1UL shl 30),
    CLIENT_REMEMBER_OPTIONS(1UL shl 31),
}
