/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.model

import kotlinx.serialization.Serializable

/**
 * Class to hold information about a license shown to user
 */
@Serializable
data class License(
    val name: String = "",
    val spdxID: String = "",
    val website: String? = null
)
