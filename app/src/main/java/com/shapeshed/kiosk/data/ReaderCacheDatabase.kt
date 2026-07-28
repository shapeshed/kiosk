package com.shapeshed.kiosk.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Entity(tableName = "reader_extractions")
data class ReaderExtractionEntity(
    @PrimaryKey val storyId: Long,
    val url: String,
    val title: String?,
    val contentHtml: String,
    val textContent: String,
    val extractedAtMillis: Long,
    val lastAccessedMillis: Long,
    val byteSize: Int,
    val readerVersion: Int,
)

@Dao
interface ReaderExtractionDao {
    @Query("SELECT * FROM reader_extractions WHERE storyId = :storyId AND readerVersion = :readerVersion LIMIT 1")
    suspend fun get(storyId: Long, readerVersion: Int): ReaderExtractionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: ReaderExtractionEntity)

    @Query("UPDATE reader_extractions SET lastAccessedMillis = :lastAccessedMillis WHERE storyId = :storyId")
    suspend fun touch(storyId: Long, lastAccessedMillis: Long)

    @Query("SELECT COALESCE(SUM(byteSize), 0) FROM reader_extractions")
    suspend fun totalByteSize(): Long

    @Query("SELECT storyId FROM reader_extractions ORDER BY lastAccessedMillis ASC LIMIT :limit")
    suspend fun oldestStoryIds(limit: Int): List<Long>

    @Query("DELETE FROM reader_extractions WHERE storyId IN (:storyIds)")
    suspend fun deleteByStoryIds(storyIds: List<Long>)
}

@Database(
    entities = [ReaderExtractionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ReaderCacheDatabase : RoomDatabase() {
    abstract fun readerExtractionDao(): ReaderExtractionDao

    companion object {
        fun create(context: Context): ReaderCacheDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ReaderCacheDatabase::class.java,
                "reader_cache.db",
            ).build()
    }
}

class ReaderExtractionStore(
    private val dao: ReaderExtractionDao,
) {
    suspend fun get(storyId: Long): ReaderExtractionEntity? =
        withContext(Dispatchers.IO) {
            dao.get(storyId, ReaderCacheVersion)?.also {
                dao.touch(storyId, System.currentTimeMillis())
            }
        }

    suspend fun put(
        storyId: Long,
        url: String,
        title: String?,
        contentHtml: String,
        textContent: String,
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.put(
            ReaderExtractionEntity(
                storyId = storyId,
                url = url,
                title = title,
                contentHtml = contentHtml,
                textContent = textContent,
                extractedAtMillis = now,
                lastAccessedMillis = now,
                byteSize = url.utf8Size() +
                    title.orEmpty().utf8Size() +
                    contentHtml.utf8Size() +
                    textContent.utf8Size(),
                readerVersion = ReaderCacheVersion,
            ),
        )
        evictIfNeeded()
    }

    private suspend fun evictIfNeeded() {
        while (dao.totalByteSize() > MaxCacheBytes) {
            val oldest = dao.oldestStoryIds(EvictionBatchSize)
            if (oldest.isEmpty()) return
            dao.deleteByStoryIds(oldest)
        }
    }

    private companion object {
        const val ReaderCacheVersion = 1
        const val MaxCacheBytes = 40L * 1024L * 1024L
        const val EvictionBatchSize = 20
    }
}

private fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size
