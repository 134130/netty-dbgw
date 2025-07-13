package com.github.l34130.netty.dbgw.utils

import java.util.EnumSet

interface Flag {
    val value: Int
}

fun <E> EnumSet<E>.toFlags(): Int where E : Enum<E>, E : Flag =
    this
        .map { it.value }
        .fold(0) { acc, value -> acc or (1 shl value) }

fun <E> Int.toEnumSet(enumClass: Class<E>): EnumSet<E> where E : Enum<E>, E : Flag {
    val enumConstants = enumClass.enumConstants ?: throw IllegalArgumentException("Enum class $enumClass has no constants")
    return enumConstants
        .filter { (this and it.value) != 0 }
        .let {
            if (it.isEmpty()) {
                EnumSet.noneOf(enumClass)
            } else {
                EnumSet.copyOf(it)
            }
        }
}

inline fun <reified E> Int.toEnumSet(): EnumSet<E> where E : Enum<E>, E : Flag = this.toEnumSet(E::class.java)
