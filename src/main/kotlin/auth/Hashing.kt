package auth

import java.security.MessageDigest

fun String.hash(): String {
    val hashedBytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return hashedBytes.joinToString("") { "%02x".format(it) }
}