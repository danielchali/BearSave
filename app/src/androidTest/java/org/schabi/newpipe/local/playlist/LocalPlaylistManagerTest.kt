package org.schabi.newpipe.local.playlist

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.schabi.newpipe.database.AppDatabase
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.testUtil.TestDatabase
import org.schabi.newpipe.testUtil.TrampolineSchedulerRule

class LocalPlaylistManagerTest {

    private lateinit var manager: LocalPlaylistManager
    private lateinit var database: AppDatabase

    @get:Rule
    val trampolineScheduler = TrampolineSchedulerRule()

    @Before
    fun setup() {
        database = TestDatabase.createReplacingNewPipeDatabase()
        manager = LocalPlaylistManager(database)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun createPlaylist() {
        val BEARTUBE_URL = ""
        val stream = StreamEntity(
            serviceId = 1,
            url = BEARTUBE_URL,
            title = "title",
            streamType = StreamType.VIDEO_STREAM,
            duration = 1,
            uploader = "uploader",
            uploaderUrl = BEARTUBE_URL
        )

        val result = manager.createPlaylist("name", listOf(stream))

        // This should not behave like this.
        // Currently list of all stream ids is returned instead of playlist id
        result.test().await().assertValue(listOf(1L))
    }

    @Test
    fun createPlaylist_emptyPlaylistMustReturnEmpty() {
        val result = manager.createPlaylist("name", emptyList())

        // This should not behave like this.
        // It should throw an error because currently the result is null
        result.test().await().assertComplete()
        manager.playlists.test().awaitCount(1).assertValue(emptyList())
    }

    @Test()
    fun createPlaylist_nonExistentStreamsAreUpserted() {
        val stream = StreamEntity(
            serviceId = 1,
            url = "",
            title = "title",
            streamType = StreamType.VIDEO_STREAM,
            duration = 1,
            uploader = "uploader",
            uploaderUrl = ""
        )
        database.streamDAO().insert(stream)
        val upserted = StreamEntity(
            serviceId = 1,
            url = "",
            title = "title2",
            streamType = StreamType.VIDEO_STREAM,
            duration = 1,
            uploader = "uploader",
            uploaderUrl = ""
        )

        val result = manager.createPlaylist("name", listOf(stream, upserted))

        result.test().await().assertComplete()
        database.streamDAO().getAll().test().awaitCount(1).assertValue(listOf(stream, upserted))
    }
}
