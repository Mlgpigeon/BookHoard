package com.example.mybookhoard.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun BottomNavigationBar(
    isSearchSelected: Boolean = false,
    isLibrarySelected: Boolean = false,
    isProfileSelected: Boolean = false,
    onSearchClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    BottomAppBar {
        NavigationBar {
            NavigationBarItem(
                selected = isSearchSelected,
                onClick = onSearchClick,
                icon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                },
                label = { Text("Search") }
            )

            NavigationBarItem(
                selected = isLibrarySelected,
                onClick = onLibraryClick,
                icon = {
                    Icon(
                        Icons.Filled.LibraryBooks,
                        contentDescription = "Library"
                    )
                },
                label = { Text("Library") }
            )

            NavigationBarItem(
                selected = isProfileSelected,
                onClick = onProfileClick,
                icon = {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile"
                    )
                },
                label = { Text("Profile") }
            )
        }
    }
}