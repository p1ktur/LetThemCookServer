package models.database.products

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class Product(
    val id: Int = 0,
    val name: String
) {
    companion object {
        fun fromWeightedProduct(text: String): Triple<String, Int, Int> {
            val parts = text.split(", ")
            val name = parts.firstOrNull().toString()
            val weight = parts.find { it.contains("g") }?.replace("g", "")?.toIntOrNull() ?: 0
            val amount = parts.find { it.contains("pc") }?.split(" ")?.firstOrNull()?.toIntOrNull() ?: 0

            return Triple(name, weight, amount)
        }
    }
}

@Serializable
data class WeightedProduct(
    val data: Product,
    val weight: Int,
    val amount: Int
) {
    override fun toString(): String {
        return StringBuilder().apply {
            append(data.name)
            if (weight > 0) append(", ${weight}g")
            if (amount > 0) {
                val piecesSuffix = if (amount == 1) "pc" else "pcs"
                append(", $amount $piecesSuffix")
            }
        }.toString()
    }
}

object Products : Table("products") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255).uniqueIndex()
}