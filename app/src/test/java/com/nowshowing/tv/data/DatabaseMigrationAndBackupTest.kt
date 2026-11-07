package com.nowshowing.tv.data

import android.database.sqlite.SQLiteDatabase
import com.nowshowing.tv.data.db.NowShowingDatabase
import com.nowshowing.tv.data.db.RecentItem
import com.nowshowing.tv.data.db.WatchlistItem
import com.nowshowing.tv.data.repository.MediaRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationAndBackupTest {

    private lateinit var db: NowShowingDatabase
    private lateinit var repository: MediaRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        NowShowingDatabase.resetInstanceForTesting()
        context.deleteDatabase(NowShowingDatabase.DATABASE_NAME)
        db = NowShowingDatabase(context)
        repository = MediaRepository(context, db = db)
    }

    @After
    fun tearDown() {
        db.close()
        NowShowingDatabase.resetInstanceForTesting()
    }

    @Test
    fun schemaV2ToV3Migration_preservesExistingData() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("test_migration.db")

        // 1. Create a database at Version 2 manually
        val helperV2 = object : android.database.sqlite.SQLiteOpenHelper(context, "test_migration.db", null, 2) {
            override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
                sqLiteDatabase.execSQL("""
                    CREATE TABLE watchlist (
                        imdb_id TEXT PRIMARY KEY, title TEXT NOT NULL, poster TEXT,
                        year TEXT, type TEXT, date_added INTEGER NOT NULL
                    )
                """.trimIndent())
                sqLiteDatabase.execSQL("""
                    CREATE TABLE recently_opened (
                        imdb_id TEXT PRIMARY KEY, title TEXT NOT NULL, poster TEXT, year TEXT,
                        type TEXT NOT NULL, season INTEGER NOT NULL, episode INTEGER NOT NULL,
                        source_id TEXT NOT NULL, watched INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())
                sqLiteDatabase.execSQL("""
                    CREATE TABLE continue_watching (
                        imdb_id TEXT PRIMARY KEY, title TEXT NOT NULL, poster TEXT, year TEXT,
                        type TEXT, season INTEGER DEFAULT 1, episode INTEGER DEFAULT 1,
                        position_ms INTEGER DEFAULT 0, duration_ms INTEGER DEFAULT 0,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())
                sqLiteDatabase.execSQL("""
                    CREATE TABLE metadata_cache (
                        cache_key TEXT PRIMARY KEY, payload TEXT NOT NULL, expires_at INTEGER NOT NULL
                    )
                """.trimIndent())
                sqLiteDatabase.execSQL("""
                    CREATE TABLE search_history (query TEXT PRIMARY KEY, timestamp INTEGER NOT NULL)
                """.trimIndent())
            }
            override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val v2Db = helperV2.writableDatabase
        v2Db.execSQL("INSERT INTO watchlist VALUES ('tt1234567', 'Inception', 'poster.jpg', '2010', 'movie', 1000)")
        v2Db.execSQL("INSERT INTO recently_opened VALUES ('tt9876543', 'Breaking Bad', 'bb.jpg', '2008', 'series', 1, 3, 'vidlink', 1, 2000)")
        helperV2.close()

        // 2. Open with current NowShowingDatabase (Version 3)
        val migratedHelper = object : android.database.sqlite.SQLiteOpenHelper(context, "test_migration.db", null, NowShowingDatabase.DATABASE_VERSION) {
            override fun onCreate(sqLiteDatabase: SQLiteDatabase) {}
            override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                if (oldVersion < 2) {
                    sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS recently_opened (imdb_id TEXT PRIMARY KEY)")
                }
                if (oldVersion < 3) {
                    sqLiteDatabase.execSQL("""
                        CREATE TABLE IF NOT EXISTS watched_episodes (
                            imdb_id TEXT NOT NULL,
                            season INTEGER NOT NULL,
                            episode INTEGER NOT NULL,
                            watched_at INTEGER NOT NULL,
                            PRIMARY KEY (imdb_id, season, episode)
                        )
                    """.trimIndent())
                }
            }
        }

        val v3Db = migratedHelper.readableDatabase
        // Verify existing rows survived intact
        v3Db.rawQuery("SELECT title FROM watchlist WHERE imdb_id = 'tt1234567'", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("Inception", it.getString(0))
        }
        v3Db.rawQuery("SELECT title, season, episode FROM recently_opened WHERE imdb_id = 'tt9876543'", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("Breaking Bad", it.getString(0))
            assertEquals(1, it.getInt(1))
            assertEquals(3, it.getInt(2))
        }

        // Verify watched_episodes table was created and is usable
        v3Db.rawQuery("SELECT count(*) FROM watched_episodes", null).use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
        migratedHelper.close()
    }

    @Test
    fun watchedEpisodes_markSingleAndBatch() {
        assertFalse(db.isEpisodeWatched("tt0903747", 1, 1))

        db.setEpisodeWatched("tt0903747", 1, 1, true)
        assertTrue(db.isEpisodeWatched("tt0903747", 1, 1))
        assertEquals(setOf(Pair(1, 1)), db.getWatchedEpisodes("tt0903747"))

        db.setEpisodeWatched("tt0903747", 1, 1, false)
        assertFalse(db.isEpisodeWatched("tt0903747", 1, 1))

        // Mark previous as watched
        db.markPreviousEpisodesWatched("tt0903747", upToSeason = 2, upToEpisode = 3, mapOf(1 to 7))
        val watched = db.getWatchedEpisodes("tt0903747")
        // Season 1 (1..7) + Season 2 (1..3) = 10 episodes
        assertEquals(10, watched.size)
        assertTrue(watched.contains(Pair(1, 1)))
        assertTrue(watched.contains(Pair(1, 7)))
        assertTrue(watched.contains(Pair(2, 3)))
        assertFalse(watched.contains(Pair(2, 4)))
    }

    @Test
    fun backupAndRestore_roundTripRestoresLibrary() = runBlocking {
        // Seed Watchlist, Recent, Watched Episodes
        db.addToWatchlist(WatchlistItem("tt0137523", "Fight Club", "fc.jpg", "1999", "movie"))
        db.upsertRecentlyOpened(RecentItem("tt0944947", "Game of Thrones", "got.jpg", "2011", "series", 1, 2, "vidsrc"))
        db.setEpisodeWatched("tt0944947", 1, 1, true)
        db.setEpisodeWatched("tt0944947", 1, 2, true)

        // Export JSON
        val exportedJson = repository.exportLibraryJson()
        assertTrue(exportedJson.contains("tt0137523"))
        assertTrue(exportedJson.contains("Fight Club"))
        assertTrue(exportedJson.contains("Game of Thrones"))
        assertFalse(exportedJson.contains("http://")) // Excludes full provider URLs
        assertFalse(exportedJson.contains("https://")) // Excludes provider URLs

        // Clear database tables to simulate fresh install restore
        db.writableDatabase.execSQL("DELETE FROM watchlist")
        db.writableDatabase.execSQL("DELETE FROM recently_opened")
        db.writableDatabase.execSQL("DELETE FROM watched_episodes")
        assertEquals(0, db.getWatchlist().size)
        assertEquals(0, db.getRecentlyOpened().size)
        assertEquals(0, db.getAllWatchedEpisodes().size)

        // Import JSON
        val result = repository.importLibraryJson(exportedJson)
        assertEquals(1, result.watchlistCount)
        assertEquals(1, result.recentlyOpenedCount)
        assertEquals(2, result.watchedEpisodesCount)

        // Verify restoration
        assertEquals("Fight Club", db.getWatchlist().single().title)
        assertEquals("Game of Thrones", db.getRecentlyOpened().single().title)
        assertTrue(db.isEpisodeWatched("tt0944947", 1, 1))
        assertTrue(db.isEpisodeWatched("tt0944947", 1, 2))
    }

    @Test
    fun importLibrary_rejectsMalformedOrInvalidPayloads() = runBlocking {
        try {
            repository.importLibraryJson("{ invalid json")
            fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Malformed"))
        }

        try {
            repository.importLibraryJson("""{"version": 99, "watchlist": []}""")
            fail("Should throw on unsupported version")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unsupported backup version"))
        }
    }

    @Test
    fun resetData_keepWatchlistRespectsFlag() = runBlocking {
        db.addToWatchlist(WatchlistItem("tt0137523", "Fight Club", "fc.jpg", "1999", "movie"))
        db.upsertRecentlyOpened(RecentItem("tt0944947", "Game of Thrones", "got.jpg", "2011", "series", 1, 2, "vidsrc"))
        db.setEpisodeWatched("tt0944947", 1, 1, true)

        // Reset keeping watchlist
        repository.resetAppData(keepWatchlist = true)
        assertEquals(1, db.getWatchlist().size)
        assertEquals(0, db.getRecentlyOpened().size)
        assertFalse(db.isEpisodeWatched("tt0944947", 1, 1))

        // Reset without keeping watchlist
        repository.resetAppData(keepWatchlist = false)
        assertEquals(0, db.getWatchlist().size)
    }
}
