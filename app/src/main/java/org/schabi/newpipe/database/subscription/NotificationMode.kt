/*
 * SPDX-FileCopyrightText: 2021 NewPipe contributors <>
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.database.subscription

import androidx.annotation.IntDef

@IntDef(NotificationMode.Companion.DISABLED, NotificationMode.Companion.ENABLED)
@Retention(AnnotationRetention.SOURCE)
annotation class NotificationMode {
    companion object {
        const val DISABLED = 0
        const val ENABLED = 1 // other values reserved for the future
    }
}
