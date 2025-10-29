package dev.egarcia.andperf.shared

object FakeRepo {
    fun items(count: Int = 1000): List<Item> = List(count) { i ->
        Item(
            id = i,
            title = "Item #$i",
            subtitle = "Subtitle for item #$i"
        )
    }
}