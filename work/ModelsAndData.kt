package com.example.mediajournal

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ContentType(val label: String) {
    SERIES("Series"),
    ANIME("Anime"),
    MOVIE("Peliculas"),
    BOOK("Libros")
}

enum class ContentStatus(val label: String) {
    PENDING("Pendiente"),
    IN_PROGRESS("En progreso"),
    FINISHED("Terminado"),
    ABANDONED("Abandonado")
}

data class TrackedContent(
    val id: Long = 0,
    val title: String,
    val type: ContentType,
    val status: ContentStatus,
    val rating: Int? = null,
    val genre: String? = null,
    val notes: String? = null,
    val startDate: LocalDate? = null,
    val finishedDate: LocalDate? = null,
    val currentProgress: Int? = null,
    val totalProgress: Int? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class PieChartItem(
    val type: ContentType,
    val count: Int,
    val percentage: Float
)

data class MonthlyStats(
    val year: Int,
    val month: Int,
    val items: List<PieChartItem>,
    val totalFinished: Int,
    val averageRating: Double?,
    val finishedContents: List<TrackedContent>
)

data class HistoricalStats(
    val items: List<PieChartItem>,
    val totalFinished: Int,
    val averageRating: Double?,
    val bestRated: TrackedContent?
)

@Entity(tableName = "tracked_content")
data class TrackedContentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: ContentType,
    val status: ContentStatus,
    val rating: Int?,
    val genre: String?,
    val notes: String?,
    val startDate: LocalDate?,
    val finishedDate: LocalDate?,
    val currentProgress: Int?,
    val totalProgress: Int?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

class Converters {
    @TypeConverter
    fun fromType(value: ContentType): String = value.name

    @TypeConverter
    fun toType(value: String): ContentType = ContentType.valueOf(value)

    @TypeConverter
    fun fromStatus(value: ContentStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): ContentStatus = ContentStatus.valueOf(value)

    @TypeConverter
    fun fromDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromDateTime(value: LocalDateTime): String = value.toString()

    @TypeConverter
    fun toDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)
}

@Dao
interface TrackedContentDao {
    @Query("SELECT * FROM tracked_content ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TrackedContentEntity>>

    @Query("SELECT * FROM tracked_content WHERE id = :id")
    fun observeById(id: Long): Flow<TrackedContentEntity?>

    @Query("""
        SELECT * FROM tracked_content
        WHERE status = 'FINISHED'
        AND finishedDate BETWEEN :startDate AND :endDate
        ORDER BY finishedDate DESC
    """)
    fun observeFinishedBetween(startDate: String, endDate: String): Flow<List<TrackedContentEntity>>

    @Query("SELECT COUNT(*) FROM tracked_content")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(content: TrackedContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contents: List<TrackedContentEntity>)

    @Delete
    suspend fun delete(content: TrackedContentEntity)
}

@Database(entities = [TrackedContentEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackedContentDao(): TrackedContentDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "media_journal.db"
                ).build().also { instance = it }
            }
        }
    }
}

interface ContentRepository {
    fun observeContents(): Flow<List<TrackedContent>>
    fun observeContent(id: Long): Flow<TrackedContent?>
    fun observeMonthlyStats(year: Int, month: Int): Flow<MonthlyStats>
    fun observeHistoricalStats(): Flow<HistoricalStats>
    suspend fun save(content: TrackedContent)
    suspend fun delete(content: TrackedContent)
    suspend fun seedIfEmpty()
}

class RoomContentRepository(
    private val dao: TrackedContentDao
) : ContentRepository {
    override fun observeContents(): Flow<List<TrackedContent>> {
        return dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeContent(id: Long): Flow<TrackedContent?> {
        return dao.observeById(id).map { it?.toDomain() }
    }

    override fun observeMonthlyStats(year: Int, month: Int): Flow<MonthlyStats> {
        val start = LocalDate.of(year, month, 1)
        val end = start.withDayOfMonth(start.lengthOfMonth())
        return dao.observeFinishedBetween(start.toString(), end.toString()).map { rows ->
            val contents = rows.map { it.toDomain() }
            val total = contents.size
            MonthlyStats(
                year = year,
                month = month,
                items = ContentType.entries.map { type ->
                    val count = contents.count { it.type == type }
                    PieChartItem(type, count, if (total == 0) 0f else count.toFloat() / total)
                },
                totalFinished = total,
                averageRating = contents.mapNotNull { it.rating }.average().takeIf { !it.isNaN() },
                finishedContents = contents
            )
        }
    }

    override fun observeHistoricalStats(): Flow<HistoricalStats> {
        return observeContents().map { contents ->
            val finished = contents.filter { it.status == ContentStatus.FINISHED }
            val total = finished.size
            HistoricalStats(
                items = ContentType.entries.map { type ->
                    val count = finished.count { it.type == type }
                    PieChartItem(type, count, if (total == 0) 0f else count.toFloat() / total)
                },
                totalFinished = total,
                averageRating = finished.mapNotNull { it.rating }.average().takeIf { !it.isNaN() },
                bestRated = finished
                    .filter { it.rating != null }
                    .maxByOrNull { it.rating ?: 0 }
            )
        }
    }

    override suspend fun save(content: TrackedContent) {
        dao.upsert(content.copy(updatedAt = LocalDateTime.now()).toEntity())
    }

    override suspend fun delete(content: TrackedContent) {
        dao.delete(content.toEntity())
    }

    override suspend fun seedIfEmpty() {
        if (dao.count() > 0) return

        val today = LocalDate.now()
        val now = LocalDateTime.now()
        dao.insertAll(
            listOf(
                TrackedContent(
                    title = "The Bear",
                    type = ContentType.SERIES,
                    status = ContentStatus.IN_PROGRESS,
                    rating = 4,
                    genre = "Drama",
                    notes = "Temporada actual en progreso.",
                    startDate = today.minusDays(18),
                    currentProgress = 5,
                    totalProgress = 10,
                    createdAt = now.minusDays(18),
                    updatedAt = now.minusDays(1)
                ).toEntity(),
                TrackedContent(
                    title = "Dune: Part Two",
                    type = ContentType.MOVIE,
                    status = ContentStatus.FINISHED,
                    rating = 5,
                    genre = "Ciencia ficcion",
                    notes = "Gran cierre visual.",
                    startDate = today.minusDays(7),
                    finishedDate = today.minusDays(7),
                    createdAt = now.minusDays(7),
                    updatedAt = now.minusDays(7)
                ).toEntity(),
                TrackedContent(
                    title = "Proyecto Hail Mary",
                    type = ContentType.BOOK,
                    status = ContentStatus.FINISHED,
                    rating = 5,
                    genre = "Sci-fi",
                    notes = "Lectura muy agil.",
                    startDate = today.minusDays(21),
                    finishedDate = today.minusDays(3),
                    currentProgress = 496,
                    totalProgress = 496,
                    createdAt = now.minusDays(21),
                    updatedAt = now.minusDays(3)
                ).toEntity(),
                TrackedContent(
                    title = "Severance",
                    type = ContentType.SERIES,
                    status = ContentStatus.PENDING,
                    genre = "Thriller",
                    notes = "Pendiente para ver despues.",
                    createdAt = now.minusDays(2),
                    updatedAt = now.minusDays(2)
                ).toEntity(),
                TrackedContent(
                    title = "Frieren",
                    type = ContentType.ANIME,
                    status = ContentStatus.FINISHED,
                    rating = 5,
                    genre = "Fantasia",
                    notes = "Anime terminado.",
                    startDate = today.minusDays(30),
                    finishedDate = today.minusDays(5),
                    currentProgress = 28,
                    totalProgress = 28,
                    createdAt = now.minusDays(30),
                    updatedAt = now.minusDays(5)
                ).toEntity()
            )
        )
    }
}

fun TrackedContentEntity.toDomain(): TrackedContent {
    return TrackedContent(
        id = id,
        title = title,
        type = type,
        status = status,
        rating = rating,
        genre = genre,
        notes = notes,
        startDate = startDate,
        finishedDate = finishedDate,
        currentProgress = currentProgress,
        totalProgress = totalProgress,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TrackedContent.toEntity(): TrackedContentEntity {
    return TrackedContentEntity(
        id = id,
        title = title,
        type = type,
        status = status,
        rating = rating,
        genre = genre,
        notes = notes,
        startDate = startDate,
        finishedDate = finishedDate,
        currentProgress = currentProgress,
        totalProgress = totalProgress,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

object RepositoryProvider {
    @Volatile
    private var repository: ContentRepository? = null

    fun get(context: Context): ContentRepository {
        return repository ?: synchronized(this) {
            repository ?: RoomContentRepository(
                AppDatabase.getInstance(context).trackedContentDao()
            ).also { repository = it }
        }
    }
}
