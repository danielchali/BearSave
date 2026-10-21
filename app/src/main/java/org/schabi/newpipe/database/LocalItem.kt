/*
 * SPDX-FileCopyrightText: 2018-2020 NewPipe contributors <>
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.database

interface LocalItem {
    val localItemType: LocalItemType

    enum class LocalItemType {
        PLAYLIST_LOCAL_ITEM,
        PLAYLIST_REMOTE_ITEM,

        PLAYLIST_STREAM_ITEM,
        STATISTIC_STREAM_ITEM
    }
}
