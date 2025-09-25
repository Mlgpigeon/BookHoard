package com.example.bookhoard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun BottomNavigationBar(
    isProfileSelected: Boolean,
    onProfileClick: () -> Unit
) {
    BottomAppBar {
        NavigationBar {
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