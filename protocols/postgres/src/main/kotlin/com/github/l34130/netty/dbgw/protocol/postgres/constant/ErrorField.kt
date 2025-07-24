package com.github.l34130.netty.dbgw.protocol.postgres.constant

// https://www.postgresql.org/docs/current/protocol-error-fields.html
@Suppress("ktlint:standard:enum-entry-name-case", "EnumEntryName")
enum class ErrorField(
    val code: Int,
) {
    S('S'.code),
    V('V'.code),
    C('C'.code),
    M('M'.code),
    D('D'.code),
    H('H'.code),
    P('P'.code),
    p('p'.code),
    q('q'.code),
    W('W'.code),
    s('s'.code),
    t('t'.code),
    c('c'.code),
    d('d'.code),
    n('n'.code),
    F('F'.code),
    L('L'.code),
    R('R'.code),
    ;

    companion object {
        private val map = entries.associateBy { it.code }

        fun from(code: Int): ErrorField? = map[code]

        fun from(byte: Byte): ErrorField? = from(byte.toInt())
    }
}
