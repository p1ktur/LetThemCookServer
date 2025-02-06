package utils

import java.net.URLDecoder

fun String?.decode(): String {
    return this?.let { URLDecoder.decode(it, "utf-8") }.toString()
}