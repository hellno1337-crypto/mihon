package eu.kanade.tachiyomi.extension.en.nhentai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GalleryListDto(
    val result: List<GalleryDto> = emptyList(),
    @SerialName("num_pages") val numPages: Int = 1,
    @SerialName("per_page") val perPage: Int = 25,
)

@Serializable
data class GalleryDto(
    val id: Int,
    @SerialName("media_id") val mediaId: String,
    val title: TitleDto,
    val images: ImagesDto,
    val tags: List<TagDto> = emptyList(),
    val scanlator: String = "",
    @SerialName("upload_date") val uploadDate: Long = 0L,
    @SerialName("num_pages") val numPages: Int = 0,
    @SerialName("num_favorites") val numFavorites: Int = 0,
)

@Serializable
data class TitleDto(
    val english: String = "",
    val japanese: String = "",
    val pretty: String = "",
)

@Serializable
data class ImagesDto(
    val pages: List<ImageDto> = emptyList(),
    val cover: ImageDto = ImageDto(),
    val thumbnail: ImageDto = ImageDto(),
)

@Serializable
data class ImageDto(
    @SerialName("t") val type: String = "j",  // j=jpg, p=png, g=gif
    val w: Int = 0,
    val h: Int = 0,
)

@Serializable
data class TagDto(
    val id: Int,
    val type: String,   // tag, artist, character, parody, group, language, category
    val name: String,
    val url: String = "",
    val count: Int = 0,
)

fun ImageDto.extension(): String = when (type) {
    "p"  -> "png"
    "g"  -> "gif"
    else -> "jpg"
}
