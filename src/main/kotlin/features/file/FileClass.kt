package features.file;

sealed interface FileClass {
    data object Profile : FileClass
    data class Recipe(val recipeId: String) : FileClass
    data class RecipeAttachment(val recipeId: String) : FileClass
    data class Block(val recipeId: String, val blockId: String) : FileClass
}