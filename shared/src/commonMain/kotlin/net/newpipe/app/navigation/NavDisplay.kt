/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

/**
 * Navigation display for compose screens
 * @param startDestination Starting destination for the app
 * @param navigator Navigator to help with navigation, injected by Koin
 * @param onCloseRequest Callback to close the app
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun NavDisplay(
    startDestination: Destination,
    onCloseRequest: () -> Unit,
    navigator: Navigator = koinInject {
        parametersOf(startDestination, onCloseRequest)
    }
) {
    val backstack = rememberSerializable(serializer = SnapshotStateListSerializer<Destination>()) {
        navigator.backstack
    }

    val saveableStateDecorator =
        rememberSaveableStateHolderNavEntryDecorator<Destination>()
    val viewModelStoreDecorator =
        rememberViewModelStoreNavEntryDecorator<Destination>()
    val entryDecorators: List<NavEntryDecorator<Destination>> =
        remember(saveableStateDecorator, viewModelStoreDecorator) {
            listOf(saveableStateDecorator, viewModelStoreDecorator)
        }

    val currentDestination = backstack.lastOrNull() ?: startDestination
    val destinations = listOf(Destination.About, Destination.Settings)

    Scaffold(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentDestination == destination
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                backstack.clear()
                                backstack.add(destination)
                            }
                        },
                        icon = {
                            Text(if (selected) "●" else "○")
                        },
                        label = {
                            Text(
                                when (destination) {
                                    Destination.About -> "About"
                                    Destination.Settings -> "Settings"
                                }
                            )
                        }
                    )
                }
            }
        }
    ) { contentPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            NavDisplay(
                backStack = backstack,
                onBack = { navigator.navigateUp() },
                entryDecorators = entryDecorators,
                entryProvider = koinEntryProvider()
            )
        }
    }
}
