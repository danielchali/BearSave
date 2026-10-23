/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.splash_dredlabs_branding
import org.jetbrains.compose.resources.painterResource
import net.newpipe.app.di.KoinApp
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.NavDisplay
import net.newpipe.app.navigation.navModule
import net.newpipe.app.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

/**
 * Entry point for the multiplatform compose application
 * @param startDestination Starting destination for the app; defaults to about
 * @param onCloseRequest Callback to close the app
 * @param withKoin Additional logic to execute after initialising Koin and setting content
 */
@Composable
fun App(
    startDestination: Destination = Destination.About,
    onCloseRequest: () -> Unit,
    withKoin: @Composable () -> Unit = {}
) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1400)
        showSplash = false
    }

    KoinApplication(
        configuration = koinConfiguration<KoinApp>(
            appDeclaration = {
                modules(navModule())
            }
        )
    ) {
        AppTheme {
            if (showSplash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Image(
                        painter = painterResource(Res.drawable.splash_dredlabs_branding),
                        contentDescription = "From DredLabs+",
                        modifier = Modifier.size(width = 240.dp, height = 64.dp)
                    )
                }
            } else {
                NavDisplay(
                    startDestination = startDestination,
                    onCloseRequest = onCloseRequest
                )
                withKoin()
            }
        }
    }
}
