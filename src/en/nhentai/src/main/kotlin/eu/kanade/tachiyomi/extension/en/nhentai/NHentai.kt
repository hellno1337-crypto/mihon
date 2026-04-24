package eu.kanade.tachiyomi.extension.en.nhentai

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class NHentai : HttpSource() {

    override val name    = "NHentai"
    override val baseUrl = "https://nhentai.net"
    override val lang    = "en"
    override val supportsLatest = true

    private val apiUrl   = "https://nhentai.net/api"
    private val imgUrl   = "https://i.nhentai.net/galleries"
    private val thumbUrl = "https://t.nhentai.net/galleries"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Cloudflare requires these headers on every request
    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

    // ── Popular ──────────────────────────────────────────────────────────────

    override fun popularMangaRequest(page: Int): Request =
        GET("$apiUrl/galleries/search?query=*&sort=popular&page=$page", headers)

    override fun popularMangaParse(response: Response): MangasPage =
        parseGalleryList(response)

    // ── Latest ───────────────────────────────────────────────────────────────

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$apiUrl/galleries/all?page=$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage =
        parseGalleryList(response)

    // ── Search ───────────────────────────────────────────────────────────────

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$apiUrl/galleries/search".toHttpUrl().newBuilder()

        // Build query string from text + tag filters
        val tagParts = mutableListOf<String>()
        if (query.isNotBlank()) tagParts.add(query)

        filters.forEach { filter ->
            when (filter) {
                is SortFilter -> url.addQueryParameter("sort", filter.toUriPart())
                is TagFilter  -> if (filter.state.isNotBlank()) tagParts.add(filter.state)
                is ArtistFilter -> if (filter.state.isNotBlank()) tagParts.add("artist:${filter.state}")
                is CharacterFilter -> if (filter.state.isNotBlank()) tagParts.add("character:${filter.state}")
                is ParodyFilter -> if (filter.state.isNotBlank()) tagParts.add("parody:${filter.state}")
                is GroupFilter -> if (filter.state.isNotBlank()) tagParts.add("group:${filter.state}")
                is LanguageFilter -> {
                    val lang = filter.toUriPart()
                    if (lang.isNotEmpty()) tagParts.add("language:$lang")
                }
                else -> {}
            }
        }

        val combinedQuery = if (tagParts.isEmpty()) "*" else tagParts.joinToString(" ")
        url.addQueryParameter("query", combinedQuery)
        url.addQueryParameter("page", page.toString())

        return GET(url.build(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage =
        parseGalleryList(response)

    // ── Manga details ─────────────────────────────────────────────────────────

    // mangaDetailsRequest is called with a manga whose url is "/g/{id}/"
    override fun mangaDetailsRequest(manga: SManga): Request {
        val id = manga.url.removeSurrounding("/").substringAfterLast("/")
        return GET("$apiUrl/gallery/$id", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val gallery = json.decodeFromString<GalleryDto>(response.body.string())
        return gallery.toSManga()
    }

    // ── Chapter list ──────────────────────────────────────────────────────────

    // nhentai galleries are single-chapter doujinshi — always one chapter
    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val gallery = json.decodeFromString<GalleryDto>(response.body.string())
        val chapter = SChapter.create().apply {
            name = "Chapter 1"
            url  = "/g/${gallery.id}/"
            date_upload = gallery.uploadDate * 1000L
            chapter_number = 1f
        }
        return listOf(chapter)
    }

    // ── Page list ─────────────────────────────────────────────────────────────

    // Chapter url is "/g/{id}/" — fetch the gallery API to get image list
    override fun pageListRequest(chapter: SChapter): Request {
        val id = chapter.url.removeSurrounding("/").substringAfterLast("/")
        return GET("$apiUrl/gallery/$id", headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val gallery = json.decodeFromString<GalleryDto>(response.body.string())
        return gallery.images.pages.mapIndexed { index, image ->
            val ext = image.extension()
            Page(index, imageUrl = "$imgUrl/${gallery.mediaId}/${index + 1}.$ext")
        }
    }

    override fun imageUrlParse(response: Response) = ""   // not used

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseGalleryList(response: Response): MangasPage {
        val list = json.decodeFromString<GalleryListDto>(response.body.string())
        val mangas = list.result.map { it.toSManga() }
        val hasMore = list.result.size >= list.perPage
        return MangasPage(mangas, hasMore)
    }

    private fun GalleryDto.toSManga(): SManga = SManga.create().apply {
        url           = "/g/$id/"
        title         = title.english.ifBlank { title.pretty.ifBlank { title.japanese } }
        thumbnail_url = "$thumbUrl/$mediaId/thumb.${images.thumbnail.extension()}"

        val tagsByType = tags.groupBy { it.type }
        author  = tagsByType["artist"]?.joinToString { it.name }
        artist  = author

        // Build description with metadata
        val sb = StringBuilder()
        tagsByType["parody"]?.let   { sb.appendLine("Parody: ${it.joinToString { t -> t.name }}") }
        tagsByType["character"]?.let { sb.appendLine("Characters: ${it.joinToString { t -> t.name }}") }
        tagsByType["group"]?.let    { sb.appendLine("Group: ${it.joinToString { t -> t.name }}") }
        sb.appendLine("Pages: $numPages")
        sb.append("Favorites: $numFavorites")
        description = sb.toString()

        // Combine content tags as genre list
        genre = tagsByType["tag"]?.joinToString { it.name }

        status = SManga.COMPLETED   // doujinshi are always complete
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    override fun getFilterList() = FilterList(
        Filter.Header("Filters are combined with text search"),
        Filter.Separator(),
        SortFilter(),
        LanguageFilter(),
        Filter.Separator(),
        Filter.Header("Tag filters (exact name, e.g. \"sole male\")"),
        TagFilter(),
        ArtistFilter(),
        CharacterFilter(),
        ParodyFilter(),
        GroupFilter(),
    )

    private class SortFilter : UriPartFilter(
        "Sort by",
        arrayOf(
            Pair("Recent",       "date"),
            Pair("Popular Today",  "popular-today"),
            Pair("Popular Week",   "popular-week"),
            Pair("All Time Popular", "popular"),
        )
    )

    private class LanguageFilter : UriPartFilter(
        "Language",
        arrayOf(
            Pair("All",      ""),
            Pair("English",  "english"),
            Pair("Japanese", "japanese"),
            Pair("Chinese",  "chinese"),
        )
    )

    private class TagFilter       : Filter.Text("Tag")
    private class ArtistFilter    : Filter.Text("Artist")
    private class CharacterFilter : Filter.Text("Character")
    private class ParodyFilter    : Filter.Text("Parody / Series")
    private class GroupFilter     : Filter.Text("Group / Circle")

    private open class UriPartFilter(
        displayName: String,
        private val vals: Array<Pair<String, String>>,
    ) : Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
        fun toUriPart() = vals[state].second
    }
}
