package com.gatemaster.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.data.auth.AuthState
import com.gatemaster.app.ui.AppViewModelProvider

/**
 * The account screen.
 *
 * Signing in is optional and the app says so rather than implying it is
 * required: everything on the device already works, and an account adds one
 * thing, which is that it follows you to another phone. A sign-in wall in front
 * of offline study notes would be the wrong trade for a student on a train.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Without this the keyboard covers the submit button, and on a
                // form whose last field is a password that is every time: the
                // keyboard is already open when the user wants to press it.
                .imePadding()
                .padding(PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp)),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (val auth = state.auth) {
                is AuthState.SignedIn -> SignedInCard(
                    displayName = auth.displayName,
                    email = auth.email,
                    busy = state.busy,
                    syncing = state.syncing,
                    syncMessage = state.syncMessage,
                    onSyncNow = viewModel::syncNow,
                    onSignOut = viewModel::signOut,
                )

                AuthState.Unavailable -> UnavailableCard()

                AuthState.SignedOut -> SignInForm(state = state, viewModel = viewModel)
            }

            ServerCard(
                url = state.serverUrl,
                editing = state.editingServer,
                onEdit = viewModel::startEditingServer,
                onChange = viewModel::setServerUrl,
                onSave = viewModel::saveServerUrl,
            )
        }
    }
}

@Composable
private fun SignedInCard(
    displayName: String,
    email: String,
    busy: Boolean,
    syncing: Boolean,
    syncMessage: String?,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
) {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(displayName.ifBlank { "Signed in" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            "What you have read and every paper you have sat now follow you to " +
                "any phone you sign in on.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )

        Button(
            onClick = onSyncNow,
            enabled = !syncing && !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            if (syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Sync now")
            }
        }

        syncMessage?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Text(
            "It also syncs on its own every few hours, and after you finish a paper.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        OutlinedButton(
            onClick = onSignOut,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            Text("Sign out")
        }

        Text(
            "Signing out leaves everything on this phone exactly where it is.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun UnavailableCard() {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Sync is not set up", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "This build has no sync server, so there is nothing to sign in to. " +
                "Everything works without one — notes, practice and progress all " +
                "live on this phone. Point it at a server below to turn sync on.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun SignInForm(state: AccountUiState, viewModel: AccountViewModel) {
    Card {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AccountMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, AccountMode.entries.size),
                ) {
                    Text(if (mode == AccountMode.SIGN_IN) "Sign in" else "Create account")
                }
            }
        }

        Text(
            text = "Optional. The app works fully without an account; signing in " +
                "makes your reading and your scores follow you to another phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )

        if (state.mode == AccountMode.CREATE) {
            OutlinedTextField(
                value = state.displayName,
                onValueChange = viewModel::setDisplayName,
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::setEmail,
            label = { Text("Email") },
            singleLine = true,
            isError = state.errorField == "email",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::setPassword,
            label = { Text("Password") },
            singleLine = true,
            isError = state.errorField == "password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )

        state.error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Button(
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            if (state.busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(if (state.mode == AccountMode.SIGN_IN) "Sign in" else "Create account")
            }
        }
    }
}

@Composable
private fun ServerCard(
    url: String,
    editing: Boolean,
    onEdit: () -> Unit,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card {
        Text("Sync server", style = MaterialTheme.typography.titleSmall)

        if (editing) {
            OutlinedTextField(
                value = url,
                onValueChange = onChange,
                label = { Text("https://…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onSave) { Text("Save") }
            }
        } else {
            Text(
                text = url.ifBlank { "Not set — sync is off" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) { Text(if (url.isBlank()) "Set a server" else "Change") }
            }
        }

        Text(
            text = "Your own instance of the GateMaster sync API. Nothing leaves " +
                "this phone until you set one and sign in.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The one surface every block on this screen sits on. */
@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}
