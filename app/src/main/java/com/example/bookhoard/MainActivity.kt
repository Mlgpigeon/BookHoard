package com.example.bookhoard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookhoard.backup.exportDbZip
import com.example.bookhoard.backup.importDbZip
import com.example.bookhoard.data.Book
import com.example.bookhoard.data.ReadingStatus
import com.example.bookhoard.data.WishlistStatus
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: BooksVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.importFromAssetsOnce(this)
        setContent { MaterialTheme { MainScreen(vm) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(vm: BooksVm) {
    val list by vm.items.collectAsState(initial = emptyList())
    var viewMode by remember { mutableStateOf("books") }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        ViewModeSelector(viewMode) { viewMode = it }
        Spacer(Modifier.height(16.dp))

        if (viewMode == "books") {
            // Vista normal por estados
            val total = list.size
            val reading = list.filter { it.status == ReadingStatus.READING }
            val unread = list.filter { it.status == ReadingStatus.NOT_STARTED }
            val read = list.filter { it.status == ReadingStatus.READ }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Total", total, Modifier.weight(1f))
                Stat("Unread", unread.size, Modifier.weight(1f))
                Stat("Reading", reading.size, Modifier.weight(1f))
                Stat("Read", read.size, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            CategorySection("📕 Unread", unread, vm)
            CategorySection("📖 Reading", reading, vm)
            CategorySection("✅ Read", read, vm)
        } else {
            // Vista agrupada por autor
            AuthorsView(list)
        }
    }
}


@Composable
fun CategorySection(title: String, books: List<Book>, vm: BooksVm) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }

            if (expanded) {
                Spacer(Modifier.height(4.dp))
                books.forEach { b ->
                    BookRow(b, vm)
                }
            }
        }
    }
}

@Composable
fun BookRow(book: Book, vm: BooksVm) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("• ${book.title}", Modifier.weight(1f))

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Change status"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Unread") },
                    onClick = {
                        vm.updateStatus(book, ReadingStatus.NOT_STARTED)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Reading") },
                    onClick = {
                        vm.updateStatus(book, ReadingStatus.READING)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Read") },
                    onClick = {
                        vm.updateStatus(book, ReadingStatus.READ)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}


@Composable
private fun Stat(label: String, value: Int, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = "$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: BooksVm) {
    var selectedTab by remember { mutableStateOf("profile") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("BookHoard") }) },
        bottomBar = {
            BottomAppBar {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == "profile",
                        onClick = { selectedTab = "profile" },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "add",
                        onClick = { selectedTab = "add" },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                        label = { Text("Add") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "wishlist",
                        onClick = { selectedTab = "wishlist" },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Wishlist") },
                        label = { Text("Wishlist") }
                    )
                }
            }
        }
    ) { paddings ->
        Box(Modifier.padding(paddings)) {
            when (selectedTab) {
                "profile" -> BooksScreen(vm)   // lo que tenías antes
                "add" -> AddBookScreen(vm)       // nueva pantalla
                "wishlist" -> WishlistScreen(vm)
            }
        }
    }
}

@Composable
fun AddBookScreen(vm: BooksVm) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var wishlistStatus by remember { mutableStateOf<WishlistStatus?>(null) }

    Column(
        Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text("Author") },
            modifier = Modifier.fillMaxWidth()
        )

        // Dropdown para wishlist
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(
                    wishlistStatus?.name ?: "Select wishlist status",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("⭐ Wish") },
                    onClick = {
                        wishlistStatus = WishlistStatus.WISH
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📦 On the way") },
                    onClick = {
                        wishlistStatus = WishlistStatus.ON_THE_WAY
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📚 Obtained") },
                    onClick = {
                        wishlistStatus = WishlistStatus.OBTAINED
                        expanded = false
                    }
                )
            }
        }

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    val book = Book(
                        title = title.trim(),
                        author = author.trim().ifBlank { null },
                        wishlist = wishlistStatus
                    )
                    vm.replaceAll(listOf(book))
                    title = ""
                    author = ""
                    wishlistStatus = null
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Book")
        }
    }
}


@Composable
fun WishlistScreen(vm: BooksVm) {
    val list by vm.items.collectAsState(initial = emptyList())
    var viewMode by remember { mutableStateOf("books") }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        ViewModeSelector(viewMode) { viewMode = it }
        Spacer(Modifier.height(16.dp))

        if (viewMode == "books") {
            val wish = list.filter { it.wishlist == WishlistStatus.WISH }
            val onTheWay = list.filter { it.wishlist == WishlistStatus.ON_THE_WAY }

            WishlistSection("⭐ Wish", wish, vm)
            WishlistSection("📦 On the way", onTheWay, vm)
        } else {
            // Vista agrupada por autor
            AuthorsView(list.filter { it.wishlist == WishlistStatus.WISH || it.wishlist == WishlistStatus.ON_THE_WAY })
        }
    }
}


@Composable
fun WishlistSection(title: String, books: List<Book>, vm: BooksVm) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }

            if (expanded) {
                books.forEach { book ->
                    WishlistRow(book, vm)
                }
            }
        }
    }
}

@Composable
fun WishlistRow(book: Book, vm: BooksVm) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("• ${book.title}", Modifier.weight(1f))

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.Star, contentDescription = "Change wishlist status")
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("⭐ Wish") },
                    onClick = {
                        vm.updateWishlist(book, WishlistStatus.WISH)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📦 On the way") },
                    onClick = {
                        vm.updateWishlist(book, WishlistStatus.ON_THE_WAY)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📚 Obtained (move to collection)") },
                    onClick = {
                        vm.updateWishlist(book, WishlistStatus.OBTAINED)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Remove from wishlist") },
                    onClick = {
                        vm.updateWishlist(book, null)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ViewModeSelector(viewMode: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { onChange("books") },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (viewMode == "books") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
            )
        ) {
            Text("Books")
        }
        OutlinedButton(
            onClick = { onChange("authors") },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (viewMode == "authors") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
            )
        ) {
            Text("Authors")
        }
    }
}


@Composable
fun AuthorsView(all: List<Book>) {
    val sections = listOf(
        "Read" to ReadingStatus.READ,
        "Reading" to ReadingStatus.READING,
        "Unread" to ReadingStatus.NOT_STARTED
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sections.forEach { (label, status) ->
            val bucket = all.filter { it.status == status }
            if (bucket.isNotEmpty()) {
                item { StatusSection(label, bucket) }
            }
        }
    }
}

@Composable
private fun StatusSection(title: String, books: List<Book>) {
    var expanded by remember { mutableStateOf(true) }
    val byAuthor = books
        .groupBy { it.author?.ifBlank { "Unknown" } ?: "Unknown" }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Show") }
            }
            if (expanded) {
                Spacer(Modifier.height(4.dp))
                byAuthor.forEach { (author, items) ->
                    AuthorRow(author = author, items = items)
                }
            }
        }
    }
}

@Composable
private fun AuthorRow(author: String, items: List<Book>) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(author, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(items.size.toString())
        }
        if (open) {
            Spacer(Modifier.height(2.dp))
            items.forEach { b ->
                Text("• ${b.title}", Modifier.padding(start = 12.dp, bottom = 2.dp))
            }
        }
    }
}



