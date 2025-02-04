package utils

import java.net.URLDecoder
import java.net.URLEncoder

fun String?.encode(): String {
    return this?.let { URLEncoder.encode(it, "utf-8") }.toString()
}

fun String?.decode(): String {
    return this?.let { URLDecoder.decode(it, "utf-8") }.toString()
}