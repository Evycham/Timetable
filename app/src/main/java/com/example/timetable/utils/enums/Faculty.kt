package com.example.timetable.utils.enums

import androidx.compose.ui.graphics.Color

enum class Faculty(val label: String, val prefix: String, val color: Color) {
    ETI("Elektrotechnik & Informatik", "eti-", Color(0xff00bfff)),
    MB("Maschinenbau", "mb-", Color(0xffffd700)),
    WS("Wirtschaft", "ws-", Color(0xff008080))
}