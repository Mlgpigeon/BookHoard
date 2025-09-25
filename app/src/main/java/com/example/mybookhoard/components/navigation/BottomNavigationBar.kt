package com.example.mybookhoard.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun BottomNavigationBar(
    isSearchSelected: Boolean = false,
    isProfileSelected: Boolean = false,
    onSearchClick: () -> Unit,
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