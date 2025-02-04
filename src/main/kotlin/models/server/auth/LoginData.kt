package models.server.auth

import utils.hash
import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    val login: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    var password: String
) {
    fun hashPassword() {
        password = password.hash()
    }
}
