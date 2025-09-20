package com.example.bookhoard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.bookhoard.ui.screens.AddBookScreen
import com.example.bookhoard.BooksVm
import com.example.bookhoard.ui.screens.BooksScreen
import com.example.bookhoard.ui.screens.WishlistScreen

class MainActivity : ComponentActivity() {
    private val vm: BooksVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.importFromAssetsOnce(this)
        setContent {
            MaterialTheme {
                MainScreen(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: BooksVm) {
    var selectedTab by remember { mutableStateOf("profile") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("BookHoard") })
        },
        bottomBar = {
            BottomAppBar {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == "profile",
                        onClick = { selectedTab = "profile" },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                        label = { Text("Books") }
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
                "profile" -> BooksScreen(vm)
                "add" -> AddBookScreen(vm)
                "wishlist" -> WishlistScreen(vm)
            }
        }
    }
}