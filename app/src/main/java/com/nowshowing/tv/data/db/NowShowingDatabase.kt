package com.nowshowing.tv.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class WatchlistItem(
    val imdbID: String,
    val title: String,
    val poster: String,
    val year: String,
    val type: String,
    val dateAdded: Long = System.currentTimeMillis()
)

data class ContinueItem(
    val imdbID: String,
    val title: String,
    val poster: String,
    val year: String,
    val type: String,
    val season: Int = 1,
    val episode: Int = 1,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

data class WatchedEpisodeItem(
    val imdbID: String,
    val season: Int,
    val episode: Int,
    val watchedAt: Long = System.currentTimeMillis()
)

data class LibraryBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val watchlist: List<WatchlistItem> = emptyList(),
    val recentlyOpened: List<RecentItem> = emptyList(),
    val watchedEpisodes: List<WatchedEpisodeItem> = emptyList()
)

data class LibraryImportResult(
    val watchlistCount: Int,
    val recentlyOpenedCount: Int,
    val watchedEpisodesCount: Int
)

class NowShowingDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "nowshowing_tv.db"
        const val DATABASE_VERSION = 3

        private const val STALE_RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        private const val TABLE_WATCHLIST = "watchlist"
        private const val TABLE_CONTINUE = "continue_watching"
        private const val TABLE_CACHE = "metadata_cache"
        private const val TABLE_SEARCH_HISTORY = "search_history"
        private const val TABLE_RECENTLY_OPENED = "recently_opened"
        private const val TABLE_WATCHED_EPISODES = "watched_episodes"

        @Volatile
        private var instance: NowShowingDatabase? = null

        fun getInstance(context: Context): NowShowingDatabase {
            return instance ?: synchronized(this) {
                instance ?: NowShowingDatabase(context.applicationContext).also { instance = it }
            }
        }

        @androidx.annotation.VisibleForTesting
        fun resetInstanceForTesting() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        createRecentTable(db)
        createWatchedEpisodesTable(db)
        db.execSQL(
            """
            CREATE TABLE $TABLE_WATCHLIST (
                imdb_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                poster TEXT,
                year TEXT,
                type TEXT,
                date_added INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CONTINUE (
                imdb_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                poster TEXT,
                year TEXT,
                type TEXT,
                season INTEGER DEFAULT 1,
                episode INTEGER DEFAULT 1,
                position_ms INTEGER DEFAULT 0,
                duration_ms INTEGER DEFAULT 0,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CACHE (
                cache_key TEXT PRIMARY KEY,
                payload TEXT NOT NULL,
                expires_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SEARCH_HISTORY (
                query TEXT PRIMARY KEY,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Additive migrations: preserve all existing watchlist, history, and cache rows.
        if (oldVersion < 2) createRecentTable(db)
        if (oldVersion < 3) createWatchedEpisodesTable(db)
    }

    private fun createRecentTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_RECENTLY_OPENED (
                imdb_id TEXT PRIMARY KEY, title TEXT NOT NULL, poster TEXT, year TEXT,
                type TEXT NOT NULL, season INTEGER NOT NULL, episode INTEGER NOT NULL,
                source_id TEXT NOT NULL, watched INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
    }

    private fun createWatchedEpisodesTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_WATCHED_EPISODES (
                imdb_id TEXT NOT NULL,
                season INTEGER NOT NULL,
                episode INTEGER NOT NULL,
                watched_at INTEGER NOT NULL,
                PRIMARY KEY (imdb_id, season, episode)
            )
        """.trimIndent())
    }

    // =========================================================================
    // Recently Opened Operations
    // =========================================================================

    fun getRecentlyOpened(): List<RecentItem> {
        val result = mutableListOf<RecentItem>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_RECENTLY_OPENED ORDER BY updated_at DESC LIMIT 30", null).use {
            while (it.moveToNext()) {
                result += RecentItem(
                    imdbID = it.getString(it.getColumnIndexOrThrow("imdb_id")),
                    title = it.getString(it.getColumnIndexOrThrow("title")),
                    poster = it.getString(it.getColumnIndexOrThrow("poster")) ?: "",
                    year = it.getString(it.getColumnIndexOrThrow("year")) ?: "",
                    type = it.getString(it.getColumnIndexOrThrow("type")),
                    season = it.getInt(it.getColumnIndexOrThrow("season")),
                    episode = it.getInt(it.getColumnIndexOrThrow("episode")),
                    sourceId = it.getString(it.getColumnIndexOrThrow("source_id")),
                    watched = it.getInt(it.getColumnIndexOrThrow("watched")) != 0,
                    updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at"))
                )
            }
        }
        return result
    }

    fun upsertRecentlyOpened(item: RecentItem) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("imdb_id", item.imdbID); put("title", item.title)
                put("poster", item.poster); put("year", item.year); put("type", item.type)
                put("season", item.season); put("episode", item.episode)
                put("source_id", item.sourceId)
                // Manual watched is a title-level label; reopening any episode preserves it.
                val watched = db.rawQuery("SELECT watched FROM $TABLE_RECENTLY_OPENED WHERE imdb_id = ?", arrayOf(item.imdbID)).use {
                    if (it.moveToFirst()) it.getInt(0) != 0 else item.watched
                }
                put("watched", if (watched) 1 else 0)
                put("updated_at", item.updatedAt)
            }
            db.insertWithOnConflict(TABLE_RECENTLY_OPENED, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.execSQL("DELETE FROM $TABLE_RECENTLY_OPENED WHERE imdb_id NOT IN (SELECT imdb_id FROM $TABLE_RECENTLY_OPENED ORDER BY updated_at DESC LIMIT 30)")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun removeRecentlyOpened(imdbId: String) {
        writableDatabase.delete(TABLE_RECENTLY_OPENED, "imdb_id = ?", arrayOf(imdbId))
    }

    fun setRecentlyWatched(imdbId: String, watched: Boolean) {
        val values = ContentValues().apply { put("watched", if (watched) 1 else 0) }
        writableDatabase.update(TABLE_RECENTLY_OPENED, values, "imdb_id = ?", arrayOf(imdbId))
    }

    // =========================================================================
    // Per-Episode Watched Marks (Schema v3)
    // =========================================================================

    fun isEpisodeWatched(imdbId: String, season: Int, episode: Int): Boolean {
        val db = readableDatabase
        db.rawQuery(
            "SELECT 1 FROM $TABLE_WATCHED_EPISODES WHERE imdb_id = ? AND season = ? AND episode = ? LIMIT 1",
            arrayOf(imdbId, season.toString(), episode.toString())
        ).use { return it.moveToFirst() }
    }

    fun getWatchedEpisodes(imdbId: String): Set<Pair<Int, Int>> {
        val result = mutableSetOf<Pair<Int, Int>>()
        readableDatabase.rawQuery(
            "SELECT season, episode FROM $TABLE_WATCHED_EPISODES WHERE imdb_id = ?",
            arrayOf(imdbId)
        ).use {
            val sCol = it.getColumnIndexOrThrow("season")
            val eCol = it.getColumnIndexOrThrow("episode")
            while (it.moveToNext()) {
                result.add(Pair(it.getInt(sCol), it.getInt(eCol)))
            }
        }
        return result
    }

    fun getAllWatchedEpisodes(): List<WatchedEpisodeItem> {
        val list = mutableListOf<WatchedEpisodeItem>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_WATCHED_EPISODES ORDER BY watched_at DESC", null).use {
            val idCol = it.getColumnIndexOrThrow("imdb_id")
            val sCol = it.getColumnIndexOrThrow("season")
            val eCol = it.getColumnIndexOrThrow("episode")
            val tCol = it.getColumnIndexOrThrow("watched_at")
            while (it.moveToNext()) {
                list.add(
                    WatchedEpisodeItem(
                        imdbID = it.getString(idCol),
                        season = it.getInt(sCol),
                        episode = it.getInt(eCol),
                        watchedAt = it.getLong(tCol)
                    )
                )
            }
        }
        return list
    }

    fun setEpisodeWatched(imdbId: String, season: Int, episode: Int, watched: Boolean) {
        val db = writableDatabase
        if (watched) {
            val values = ContentValues().apply {
                put("imdb_id", imdbId)
                put("season", season)
                put("episode", episode)
                put("watched_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_WATCHED_EPISODES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            db.delete(
                TABLE_WATCHED_EPISODES,
                "imdb_id = ? AND season = ? AND episode = ?",
                arrayOf(imdbId, season.toString(), episode.toString())
            )
        }
    }

    fun markPreviousEpisodesWatched(
        imdbId: String,
        upToSeason: Int,
        upToEpisode: Int,
        seasonEpisodeCounts: Map<Int, Int> = emptyMap()
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            for (s in 1 until upToSeason) {
                val maxEp = seasonEpisodeCounts[s] ?: 24
                for (e in 1..maxEp) {
                    val values = ContentValues().apply {
                        put("imdb_id", imdbId)
                        put("season", s)
                        put("episode", e)
                        put("watched_at", now)
                    }
                    db.insertWithOnConflict(TABLE_WATCHED_EPISODES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            for (e in 1..upToEpisode) {
                val values = ContentValues().apply {
                    put("imdb_id", imdbId)
                    put("season", upToSeason)
                    put("episode", e)
                    put("watched_at", now)
                }
                db.insertWithOnConflict(TABLE_WATCHED_EPISODES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // =========================================================================
    // Watchlist Operations
    // =========================================================================

    fun getWatchlist(): List<WatchlistItem> {
        val list = mutableListOf<WatchlistItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_WATCHLIST ORDER BY date_added DESC", null)
        cursor.use {
            val idCol = it.getColumnIndexOrThrow("imdb_id")
            val titleCol = it.getColumnIndexOrThrow("title")
            val posterCol = it.getColumnIndexOrThrow("poster")
            val yearCol = it.getColumnIndexOrThrow("year")
            val typeCol = it.getColumnIndexOrThrow("type")
            val dateCol = it.getColumnIndexOrThrow("date_added")

            while (it.moveToNext()) {
                list.add(
                    WatchlistItem(
                        imdbID = it.getString(idCol),
                        title = it.getString(titleCol),
                        poster = it.getString(posterCol) ?: "",
                        year = it.getString(yearCol) ?: "",
                        type = it.getString(typeCol) ?: "movie",
                        dateAdded = it.getLong(dateCol)
                    )
                )
            }
        }
        return list
    }

    fun isInWatchlist(imdbID: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_WATCHLIST WHERE imdb_id = ? LIMIT 1", arrayOf(imdbID))
        cursor.use { return it.moveToFirst() }
    }

    fun addToWatchlist(item: WatchlistItem) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("imdb_id", item.imdbID)
            put("title", item.title)
            put("poster", item.poster)
            put("year", item.year)
            put("type", item.type)
            put("date_added", item.dateAdded)
        }
        db.insertWithOnConflict(TABLE_WATCHLIST, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeFromWatchlist(imdbID: String) {
        val db = writableDatabase
        db.delete(TABLE_WATCHLIST, "imdb_id = ?", arrayOf(imdbID))
    }

    // =========================================================================
    // Continue Watching Operations
    // =========================================================================

    fun getContinueWatching(): List<ContinueItem> {
        val list = mutableListOf<ContinueItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CONTINUE ORDER BY updated_at DESC LIMIT 20", null)
        cursor.use {
            val idCol = it.getColumnIndexOrThrow("imdb_id")
            val titleCol = it.getColumnIndexOrThrow("title")
            val posterCol = it.getColumnIndexOrThrow("poster")
            val yearCol = it.getColumnIndexOrThrow("year")
            val typeCol = it.getColumnIndexOrThrow("type")
            val seasonCol = it.getColumnIndexOrThrow("season")
            val epCol = it.getColumnIndexOrThrow("episode")
            val posCol = it.getColumnIndexOrThrow("position_ms")
            val durCol = it.getColumnIndexOrThrow("duration_ms")
            val updatedCol = it.getColumnIndexOrThrow("updated_at")

            while (it.moveToNext()) {
                list.add(
                    ContinueItem(
                        imdbID = it.getString(idCol),
                        title = it.getString(titleCol),
                        poster = it.getString(posterCol) ?: "",
                        year = it.getString(yearCol) ?: "",
                        type = it.getString(typeCol) ?: "movie",
                        season = it.getInt(seasonCol),
                        episode = it.getInt(epCol),
                        positionMs = it.getLong(posCol),
                        durationMs = it.getLong(durCol),
                        updatedAt = it.getLong(updatedCol)
                    )
                )
            }
        }
        return list
    }

    fun upsertContinueWatching(item: ContinueItem) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("imdb_id", item.imdbID)
            put("title", item.title)
            put("poster", item.poster)
            put("year", item.year)
            put("type", item.type)
            put("season", item.season)
            put("episode", item.episode)
            put("position_ms", item.positionMs)
            put("duration_ms", item.durationMs)
            put("updated_at", item.updatedAt)
        }
        db.insertWithOnConflict(TABLE_CONTINUE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeFromContinueWatching(imdbID: String) {
        val db = writableDatabase
        db.delete(TABLE_CONTINUE, "imdb_id = ?", arrayOf(imdbID))
    }

    // =========================================================================
    // Search History Operations
    // =========================================================================

    fun getRecentSearches(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT query FROM $TABLE_SEARCH_HISTORY ORDER BY timestamp DESC LIMIT 8", null)
        cursor.use {
            val col = it.getColumnIndexOrThrow("query")
            while (it.moveToNext()) {
                list.add(it.getString(col))
            }
        }
        return list
    }

    fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val db = writableDatabase
        val values = ContentValues().apply {
            put("query", clean)
            put("timestamp", System.currentTimeMillis())
        }
        db.beginTransaction()
        try {
            db.insertWithOnConflict(TABLE_SEARCH_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.execSQL("DELETE FROM $TABLE_SEARCH_HISTORY WHERE query NOT IN (SELECT query FROM $TABLE_SEARCH_HISTORY ORDER BY timestamp DESC, query LIMIT 8)")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun clearSearchHistory() {
        val db = writableDatabase
        db.delete(TABLE_SEARCH_HISTORY, null, null)
    }

    fun resetData(keepWatchlist: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_RECENTLY_OPENED, null, null)
            db.delete(TABLE_CONTINUE, null, null)
            db.delete(TABLE_SEARCH_HISTORY, null, null)
            db.delete(TABLE_WATCHED_EPISODES, null, null)
            db.delete(TABLE_CACHE, null, null)
            if (!keepWatchlist) {
                db.delete(TABLE_WATCHLIST, null, null)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // =========================================================================
    // Metadata Cache
    // =========================================================================

    fun pruneMetadata(db: SQLiteDatabase = writableDatabase) {
        db.delete(TABLE_CACHE, "expires_at < ?", arrayOf((System.currentTimeMillis() - STALE_RETENTION_MS).toString()))
        db.execSQL("DELETE FROM $TABLE_CACHE WHERE cache_key NOT IN (SELECT cache_key FROM $TABLE_CACHE ORDER BY expires_at DESC, cache_key LIMIT 500)")
    }

    fun clearMetadata() { writableDatabase.delete(TABLE_CACHE, null, null) }
    fun metadataBytes(): Long = readableDatabase.rawQuery("SELECT COALESCE(SUM(length(CAST(payload AS BLOB))), 0) FROM $TABLE_CACHE", null).use {
        it.moveToFirst(); it.getLong(0)
    }

    fun cachedDetailPayloads(): List<String> = readableDatabase.rawQuery(
        "SELECT payload FROM $TABLE_CACHE WHERE cache_key LIKE 'details_%' AND expires_at > ? ORDER BY expires_at DESC, cache_key LIMIT 500",
        arrayOf(System.currentTimeMillis().toString())
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    fun getCachedPayload(key: String, allowStale: Boolean = false): String? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT payload FROM $TABLE_CACHE WHERE cache_key = ? AND expires_at > ? LIMIT 1",
            arrayOf(key, (System.currentTimeMillis() - if (allowStale) STALE_RETENTION_MS else 0L).toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow("payload"))
            }
        }
        return null
    }

    fun setCachedPayload(key: String, payload: String, ttlMs: Long = 24 * 60 * 60 * 1000L) {
        if (payload.toByteArray(Charsets.UTF_8).size > 65536) return
        val db = writableDatabase
        val values = ContentValues().apply {
            put("cache_key", key)
            put("payload", payload)
            put("expires_at", System.currentTimeMillis() + ttlMs)
        }
        db.beginTransaction()
        try {
            db.insertWithOnConflict(TABLE_CACHE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            pruneMetadata(db)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    // =========================================================================
    // Backup & Restore (Local JSON Library Import/Export)
    // =========================================================================

    fun exportLibrary(): LibraryBackup {
        return LibraryBackup(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            watchlist = getWatchlist(),
            recentlyOpened = getRecentlyOpened(),
            watchedEpisodes = getAllWatchedEpisodes()
        )
    }

    fun importLibrary(backup: LibraryBackup): LibraryImportResult {
        var wCount = 0
        var rCount = 0
        var eCount = 0
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Merge watchlist
            for (item in backup.watchlist) {
                if (item.imdbID.matches(Regex("tt\\d+"))) {
                    addToWatchlist(item)
                    wCount++
                }
            }
            // Merge recently opened
            for (item in backup.recentlyOpened) {
                if (item.imdbID.matches(Regex("tt\\d+"))) {
                    upsertRecentlyOpened(item)
                    rCount++
                }
            }
            // Merge watched episodes
            for (item in backup.watchedEpisodes) {
                if (item.imdbID.matches(Regex("tt\\d+"))) {
                    setEpisodeWatched(item.imdbID, item.season, item.episode, true)
                    eCount++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return LibraryImportResult(
            watchlistCount = wCount,
            recentlyOpenedCount = rCount,
            watchedEpisodesCount = eCount
        )
    }
}
