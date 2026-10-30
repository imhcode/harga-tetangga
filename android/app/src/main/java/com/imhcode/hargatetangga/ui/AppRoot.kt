package com.imhcode.hargatetangga.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.imhcode.hargatetangga.data.PingResult
import com.imhcode.hargatetangga.data.Supa
import com.imhcode.hargatetangga.data.pingModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    if (!Supa.isConfigured) {
        Message(
            "Supabase belum dikonfigurasi",
            "Isi SUPABASE_URL dan SUPABASE_ANON_KEY di android/local.properties, lalu build ulang.",
        )
        return
    }
    val status by Supa.client.auth.sessionStatus.collectAsState()
    when (status) {
        is SessionStatus.Authenticated -> MainScreen()
        is SessionStatus.Initializing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> LoginScreen()
    }
}

@Composable
private fun Message(title: String, body: String) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LoginScreen() {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("HargaTetangga", style = MaterialTheme.typography.headlineMedium)
        Text("Masuk dengan akun rumah tanggamu.", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Kata sandi") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                // Captured here because the signInWith builder has its own email/password properties.
                val enteredEmail = email.trim()
                val enteredPassword = password
                scope.launch {
                    busy = true
                    error = null
                    try {
                        Supa.client.auth.signInWith(Email) {
                            this.email = enteredEmail
                            this.password = enteredPassword
                        }
                    } catch (e: Exception) {
                        error = "Gagal masuk. Periksa email dan kata sandi. (${e.message})"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "Masuk…" else "Masuk") }
    }
}

// Account tab: the F0 connection test and sign-out.
@Composable
fun AccountScreen() {
    var prompt by rememberSaveable { mutableStateOf("Sebutkan satu kebutuhan pokok dapur.") }
    var result by remember { mutableStateOf<PingResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Akun", style = MaterialTheme.typography.headlineMedium)
        Text("Tes AI", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tombol ini memanggil Muse Spark lewat server Supabase. Kunci API tidak tersimpan di HP.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Pertanyaan") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        result = pingModel(prompt)
                    } catch (e: Exception) {
                        result = null
                        error = "Panggilan gagal: ${e.message}"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "Menunggu jawaban…" else "Tes AI") }

        result?.let {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(it.reply, style = MaterialTheme.typography.bodyLarge)
                    Text("${it.model} · ${it.latencyMs} ms", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        TextButton(onClick = { scope.launch { Supa.client.auth.signOut() } }) { Text("Keluar") }
    }
}
