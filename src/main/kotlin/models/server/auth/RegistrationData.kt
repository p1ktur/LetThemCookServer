package models.server.auth

import utils.hash
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationData(
    val login: String,
    val email: String,
    var password: String
) {
    fun hashPassword() {
        password = password.hash()
    }
}
