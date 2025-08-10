package com.github.l34130.netty.dbgw.core.util

import java.util.EnumSet

interface Flag {
    val value: ULong
}

fun <E> EnumSet<E>.toFlags(): ULong where E : Enum<E>, E : Flag = this.fold(0UL) { acc, flag -> acc or flag.value }

fun <E> ULong.toEnumSet(enumClass: Class<E>): EnumSet<E> where E : Enum<E>, E : Flag {
    val enumConstants = enumClass.enumConstants ?: throw IllegalArgumentException("Enum class $enumClass has no constants")
    return enumConstants
        .filter { (this and it.value) != 0UL }
        .let {
            if (it.isEmpty()) {
                EnumSet.noneOf(enumClass)
            } else {
                EnumSet.copyOf(it)
            }
        }
}

inline fun <reified E> ULong.toEnumSet(): EnumSet<E> where E : Enum<E>, E : Flag = this.toEnumSet(E::class.java)
