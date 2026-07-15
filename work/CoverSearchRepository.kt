package com.example.mediajournal

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CoverSearchResult(
    val title: String,
    val subtitle: String,
    val imageUrl: String
)

class CoverSearchRepository(context: Context? = null) {
    private val preferences = context?.applicationContext
        ?.getSharedPreferences("cover_search_settings", Context.MODE_PRIVATE)

    var tmdbApiKey: String
        get() = preferences?.getString("tmdb_api_key", "").orEmpty()
            .ifBlank { BuildConfig.TMDB_API_KEY }
        set(value) {
            preferences?.edit()?.putString("tmdb_api_key", value.trim())?.apply()
        }

    suspend fun search(title: String, type: ContentType): List<CoverSearchResult> = withContext(Dispatchers.IO) {
        val query = title.trim()
        if (query.isBlank()) return@withContext emptyList()

        when (type) {
            ContentType.BOOK -> searchBooks(query)
            ContentType.MOVIE -> searchMovies(query)
            ContentType.SERIES -> searchSeries(query)
            ContentType.ANIME -> searchAnime(query)
        }
    }

    suspend fun searchBroad(title: String, preferredType: ContentType): List<CoverSearchResult> = withContext(Dispatchers.IO) {
        val query = title.trim()
        if (query.isBlank()) return@withContext emptyList()

        val preferredResults = searchByTypeSafely(query, preferredType)
        if (preferredResults.isNotEmpty()) return@withContext preferredResults.withTypeLabel(preferredType)

        ContentType.entries
            .filterNot { it == preferredType }
            .asSequence()
            .map { type -> searchByTypeSafely(query, type).withTypeLabel(type) }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
    }

    private fun searchByTypeSafely(query: String, type: ContentType): List<CoverSearchResult> {
        return runCatching {
            when (type) {
                ContentType.BOOK -> searchBooks(query)
                ContentType.MOVIE -> searchMovies(query)
                ContentType.SERIES -> searchSeries(query)
                ContentType.ANIME -> searchAnime(query)
            }
        }.getOrDefault(emptyList())
    }

    private fun List<CoverSearchResult>.withTypeLabel(type: ContentType): List<CoverSearchResult> {
        return map { result ->
            result.copy(
                subtitle = listOf(type.label, result.subtitle)
                    .filter { it.isNotBlank() }
                    .joinToString(" - ")
            )
        }
    }

    private fun searchBooks(query: String): List<CoverSearchResult> {
        val json = getJsonObject("https://openlibrary.org/search.json?title=${query.encode()}&limit=8")
        val docs = json.optJSONArray("docs") ?: return emptyList()
        return docs.objects().mapNotNull { item ->
            val coverId = item.optLong("cover_i", 0L).takeIf { it > 0 } ?: return@mapNotNull null
            val author = item.optJSONArray("author_name")?.optString(0).orEmpty()
            val year = item.optInt("first_publish_year", 0).takeIf { it > 0 }?.toString().orEmpty()
            CoverSearchResult(
                title = item.optString("title", query),
                subtitle = listOf(author, year).filter { it.isNotBlank() }.joinToString(" - "),
                imageUrl = "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
            )
        }
    }

    private fun searchMovies(query: String): List<CoverSearchResult> {
        val key = tmdbApiKey
        if (key.isNotBlank()) {
            val json = getTmdbJson("movie", query, key)
            return tmdbResults(json, "release_date")
        }

        val json = getJsonObject("https://itunes.apple.com/search?term=${query.encode()}&media=all&limit=25")
        val results = json.optJSONArray("results") ?: return emptyList()
        return results.objects()
            .filter { it.optString("kind") == "feature-movie" }
            .mapNotNull { item ->
            val image = item.optString("artworkUrl100").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            CoverSearchResult(
                title = item.optString("trackName", query),
                subtitle = item.optString("releaseDate").take(4),
                imageUrl = image.replace("100x100bb", "600x600bb")
            )
        }.take(8)
    }

    private fun searchSeries(query: String): List<CoverSearchResult> {
        val key = tmdbApiKey
        if (key.isNotBlank()) {
            val json = getTmdbJson("tv", query, key)
            return tmdbResults(json, "first_air_date")
        }

        val results = getJsonArray("https://api.tvmaze.com/search/shows?q=${query.encode()}")
        return results.objects().mapNotNull { item ->
            val show = item.optJSONObject("show") ?: return@mapNotNull null
            val image = show.optJSONObject("image")
            val imageUrl = image?.optString("original")?.takeIf { it.isNotBlank() }
                ?: image?.optString("medium")?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            CoverSearchResult(
                title = show.optString("name", query),
                subtitle = show.optString("premiered").take(4),
                imageUrl = imageUrl
            )
        }.take(8)
    }

    private fun getTmdbJson(type: String, query: String, credential: String): JSONObject {
        val baseUrl = "https://api.themoviedb.org/3/search/$type?query=${query.encode()}&language=es-AR"
        return if (credential.startsWith("eyJ")) {
            getJsonObject(baseUrl, bearerToken = credential)
        } else {
            getJsonObject("$baseUrl&api_key=${credential.encode()}")
        }
    }

    private fun searchAnime(query: String): List<CoverSearchResult> {
        val json = getJsonObject("https://kitsu.io/api/edge/anime?filter[text]=${query.encode()}&page[limit]=8")
        val data = json.optJSONArray("data") ?: return emptyList()
        return data.objects().mapNotNull { item ->
            val attributes = item.optJSONObject("attributes") ?: return@mapNotNull null
            val poster = attributes.optJSONObject("posterImage")
            val imageUrl = poster?.optString("original")?.takeIf { it.isNotBlank() }
                ?: poster?.optString("large")?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            CoverSearchResult(
                title = attributes.optString("canonicalTitle", query),
                subtitle = attributes.optString("startDate").take(4),
                imageUrl = imageUrl
            )
        }
    }

    private fun tmdbResults(json: JSONObject, dateField: String): List<CoverSearchResult> {
        val results = json.optJSONArray("results") ?: return emptyList()
        return results.objects().mapNotNull { item ->
            val posterPath = item.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }
                ?: return@mapNotNull null
            CoverSearchResult(
                title = item.optString("title").ifBlank { item.optString("name") },
                subtitle = item.optString(dateField).take(4),
                imageUrl = "https://image.tmdb.org/t/p/w500$posterPath"
            )
        }.take(8)
    }

    private fun getJsonObject(url: String, bearerToken: String? = null): JSONObject = JSONObject(get(url, bearerToken))

    private fun getJsonArray(url: String): JSONArray = JSONArray(get(url))

    private fun get(url: String, bearerToken: String? = null): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MediaJournal/1.0")
            if (!bearerToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}

private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")

private fun JSONArray.objects(): List<JSONObject> {
    return (0 until length()).mapNotNull { index -> optJSONObject(index) }
}
