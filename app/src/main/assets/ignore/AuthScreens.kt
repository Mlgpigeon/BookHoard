package com.example.mybookhoard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.api.AuthState
import com.example.mybookhoard.ui.components.auth.*
import kotlinx.coroutines.delay
import com.example.mybookhoard.ui.components.auth.LoginForm
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onClearError: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    var isLogin by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    // Auto-clear errors after some time
    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            delay(10_000) // Clear error after 10 seconds
            onClearError?.invoke()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App branding
        AuthHeader()

        Spacer(Modifier.height(48.dp))

        // Error handling with improved UX
        if (authState is AuthState.Error) {
            ErrorCard(
                message = authState.message,
                onDismiss = onClearError,
                onRetry = onRetry
            )
            Spacer(Modifier.height(16.dp))
        }

        // Form card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isLogin) "Sign In" else "Create Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                if (isLogin) {
                    LoginForm(
                        authState = authState,
                        onLogin = onLogin
                    )
                } else {
                    RegisterForm(
                        authState = authState,
                        onRegister = onRegister
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Switch between login/register
                AuthModeToggle(
                    isLogin = isLogin,
                    onToggle = { newIsLogin ->
                        isLogin = newIsLogin
                        onClearError?.invoke() // Clear errors when switching
                    }
                )
            }
        }

        // Connection info for debugging
        if (authState is AuthState.Error && authState.message.contains("connect", ignoreCase = true)) {
            Spacer(Modifier.height(16.dp))
            TroubleshootingCard()
        }
    }
}