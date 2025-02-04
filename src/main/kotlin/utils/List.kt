package utils

fun <T> List<T>.selectPage(page: Int?, perPage: Int?): List<T> {
    if (page == null) return this
    if (page * (perPage ?: 10) > size) return this

    val listOfSelected = mutableListOf<T>()
    val startIndex = page * (perPage ?: 10)
    var currentIndex = startIndex

    while (currentIndex <= lastIndex && currentIndex < startIndex + (perPage ?: 10)) {
        listOfSelected.add(get(currentIndex))
        currentIndex += 1
    }

    return listOfSelected
}