package com.example.datn.core.utils.extensions

fun String.capitalizeFirstLetter(): String =
    this.trim().replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

fun String.capitalizeEachWord(): String =
    this.split(" ").joinToString(" ") { it.lowercase().capitalizeFirstLetter() }