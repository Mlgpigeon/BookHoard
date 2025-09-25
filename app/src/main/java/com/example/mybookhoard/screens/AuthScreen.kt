package com.example.mybookhoard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.api.AuthState
import com.example.mybookhoard.components.auth.*

@Composable
fun AuthScreen(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AuthHeader()
        Spacer(Modifier.height(32.dp))


        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                if (isLogin) {
                    LoginForm(authState = authState, onLogin = onLogin)
                } else {
                    RegisterForm(authState = authState, onRegister = onRegister)
                }
                Spacer(Modifier.height(8.dp))
                AuthModeToggle(isLogin = isLogin) { isLogin = it }
            }
        }

        if (authState is AuthState.Error &&
            authState.message.contains("connect", true)) {
            Spacer(Modifier.height(16.dp))
            TroubleshootingCard()
        }
    }
}
