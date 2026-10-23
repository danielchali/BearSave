/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.serialization.json.Json
import net.newpipe.Constants
import net.newpipe.app.navigation.Destination
import net.newpipe.app.theme.currentService

/**
 * Entry point for compose-related UI components on Android
 */
class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val startDestination = intent.getStringExtra(Constants.INTENT_SCREEN_KEY)
            ?.let { encodedDestination ->
                runCatching {
                    Json.decodeFromString<Destination>(encodedDestination)
                }.getOrNull()
            }
            ?: Destination.About

        setContent {
            App(
                // TODO: Change when everything is in compose and this is the primary activity
                startDestination = startDestination,
                onCloseRequest = ::finish
            ) {
                val view = LocalView.current
                val service = currentService()

                DisposableEffect(service) {
                    val windowController = WindowCompat.getInsetsController(window, view)
                    windowController.isAppearanceLightStatusBars = service.isSchemeColorDensityLight
                    onDispose {
                        windowController.isAppearanceLightStatusBars = false
                    }
                }
            }
        }
    }
}
