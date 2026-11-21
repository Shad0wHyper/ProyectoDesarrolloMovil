package com.developers.core.ui

fun <T> tabSlideDirection(tabs: List<T>, from: T?, to: T?, fallback: Int): Int {
    val fromIndex = tabs.indexOf(from)
    val toIndex = tabs.indexOf(to)
    return when {
        fromIndex < 0 || toIndex < 0 || fromIndex == toIndex -> fallback
        toIndex > fromIndex -> 1
        else -> -1
    }
}
