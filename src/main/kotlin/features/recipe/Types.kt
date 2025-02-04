package features.recipe

enum class SearchType(val value: String) {
    NAME("name"),
    CATEGORY("category"),
    PRODUCTS("products");

    companion object {
        fun fromString(string: String?): SearchType {
            return when (string) {
                NAME.value -> NAME
                CATEGORY.value -> CATEGORY
                PRODUCTS.value -> PRODUCTS
                else -> NAME
            }
        }
    }
}

enum class SortType(val value: String) {
    DATE("date"),
    REVIEWS("reviews"),
    PREPARATIONS("preparations"),
    LIKES("likes"),
    POPULARITY("popularity");

    companion object {
        fun fromString(string: String?): SortType {
            return when (string) {
                DATE.value -> DATE
                REVIEWS.value -> REVIEWS
                PREPARATIONS.value -> PREPARATIONS
                LIKES.value -> LIKES
                POPULARITY.value -> POPULARITY
                else -> DATE
            }
        }
    }
}