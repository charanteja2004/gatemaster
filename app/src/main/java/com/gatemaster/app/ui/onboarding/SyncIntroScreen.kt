package com.gatemaster.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.ui.AppViewModelProvider

/**
 * The first-run offer of an account.
 *
 * Sync is the one thing in this app that an account buys, and it was reachable
 * only from a row inside Settings, which meant most people never learned it
 * existed. This says so once, on the way in, and takes no for an answer: the
 * notes, the practice and the tests all work signed out, so a sign-in wall
 * here would cost a first-time reader everything and buy them nothing.
 */
@Composable
fun SyncIntroScreen(
    onSignIn: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncIntroViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // A build with no server has nothing to put on this screen, so it never
    // draws: the step forwards to home before the first frame is composed.
    LaunchedEffect(state.nothingToOffer) {
        if (state.nothingToOffer) onContinue()
    }

    Surface(modifier = modifier.fillMaxSize()) {
        if (!state.ready || state.nothingToOffer) return@Surface

        val signedInAs = state.signedInAs

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CloudSync,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.size(24.dp))

            Text(
                text = if (signedInAs == null) "Study on more than one device" else "You're all set",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.size(12.dp))

            Text(
                text = if (signedInAs == null) {
                    "An account keeps what you have read and every test you have sat in step " +
                        "across your phone and anything else you sign in on.\n\n" +
                        "Everything else works without one."
                } else {
                    "Signed in as $signedInAs. Your reading and your attempts will follow you " +
                        "to any device you sign in on."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.size(32.dp))

            if (signedInAs == null) {
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text("Create an account or sign in")
                }
                Spacer(Modifier.size(8.dp))
                // Deliberately a plain text button, and deliberately not phrased
                // as a refusal. Skipping is a normal choice here, not a lesser
                // one, and the offer stays in Settings for whenever it is not.
                TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text("Not now")
                }
            } else {
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text("Start studying")
                }
            }
        }
    }
}
