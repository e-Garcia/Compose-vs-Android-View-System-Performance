package dev.egarcia.andperf.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.egarcia.andperf.shared.FakeRepo
import dev.egarcia.andperf.shared.Item


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ItemList(FakeRepo.items()) } }
    }
}


@Composable
fun ItemList(data: List<Item>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(data, key = { it.id }) { item -> RowItem(item) }
    }
}


@Composable
fun RowItem(item: Item) {
    Text(text = "${item.title} — ${item.subtitle}")
}