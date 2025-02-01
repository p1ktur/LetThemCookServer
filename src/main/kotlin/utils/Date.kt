package utils

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

fun Date.toLocal(): LocalDate {
    return toInstant()
        .atOffset(ZoneOffset.UTC)
        .toLocalDate()
}