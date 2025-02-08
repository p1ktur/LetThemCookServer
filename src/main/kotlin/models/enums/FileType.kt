package models.enums

enum class FileType(val value: String) {
    IMAGE("image"),
    VIDEO("video");

    companion object {
        fun fromString(string: String): FileType {
            return when (string) {
                "image" -> IMAGE
                else -> VIDEO
            }
        }
    }
}