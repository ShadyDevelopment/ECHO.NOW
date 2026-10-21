package com.echonow.domain.repository

import com.echonow.domain.model.Media
import com.echonow.domain.model.MediaType
import com.echonow.domain.model.CastMember
import com.echonow.domain.model.VideoQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface CatalogRepository {
    suspend fun page(type: MediaType, page: Int, pageSize: Int = 20): List<Media>
    suspend fun search(query: String, page: Int = 1): CatalogSearchPage
    suspend fun find(id: String): Media?
    suspend fun seasonCount(id: String): Int?
    suspend fun trailer(id: String): String?
    suspend fun cast(id: String): List<CastMember>
    suspend fun streams(media: Media, season: Int? = null, episode: Int? = null): List<VideoQuality>
}

data class CatalogSearchPage(
    val results: List<Media>,
    val page: Int,
    val totalPages: Int,
)

@Serializable
data class UserProfile(
    val id: String,
    val nickname: String,
    val avatarId: String,
)

data class ProfileState(
    val isLoading: Boolean = true,
    val profiles: List<UserProfile> = emptyList(),
    val activeProfileId: String? = null,
) {
    val activeProfile: UserProfile?
        get() = profiles.firstOrNull { it.id == activeProfileId }
}

object ProfileAvatars {
    val categories = listOf("anime", "girls", "boys", "couples", "server-icons")
    private val legacyChoices = setOf("film", "rocket", "star", "panda", "fox", "frog", "music", "ball")

    fun id(category: String, index: Int): String = "piccdn:$category:$index"

    fun imageUrl(avatarId: String): String? {
        val parts = avatarId.split(':')
        if (parts.size != 3 || parts[0] != "piccdn") return null
        val category = parts[1]
        val index = parts[2].toIntOrNull() ?: return null
        if (category !in categories || index !in 0..999) return null
        return "https://raw.githubusercontent.com/itsfizys/piccdn/main/$category/$index.jpg"
    }

    fun isValid(avatarId: String): Boolean =
        avatarId in legacyChoices || imageUrl(avatarId) != null

    val defaultId: String = id("anime", 0)
    const val MAX_PROFILES = 4
}

interface ProfileRepository {
    fun observe(): Flow<ProfileState>
    suspend fun create(nickname: String, avatarId: String): UserProfile
    suspend fun update(profile: UserProfile)
    suspend fun remove(profileId: String)
    suspend fun select(profileId: String)
}

interface FavoritesRepository {
    fun observe(): Flow<Set<String>>
    suspend fun toggle(mediaId: String)
    suspend fun clear()
}

@Serializable
data class SubtitleStyle(
    val background: Long = 0xCC000000,
    val text: Long = 0xFFFFFFFF,
    val border: Long = 0xFF000000,
    val sizeSp: Int = 22,
    val boxEnabled: Boolean = true,
    val boxTransparency: Int = 20,
    val fontFamily: String = "SansSerif",
)

@Serializable
data class UserPreferences(
    val theme: String = "Dark",
    val primaryColor: Long = 0xFF607D8B,
    val secondaryColor: Long = 0xFF00838F,
    val tertiaryColor: Long = 0xFF31A7A0,
    val buttonShape: String = "Rounded",
    val buttonFillColor: Long? = 0xFF37474F,
    val buttonTextColor: Long? = 0xFFFFFFFF,
    val buttonBorderColor: Long? = 0xFF607D8B,
    val showMenuLabels: Boolean = false,
    val navigationBarCollapsed: Boolean = true,
    val interfaceMode: String = "AUTO",
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val subtitleCountry: String = "US",
    val subtitleLanguage: String = "EN",
    val autoApplySubtitles: Boolean = true,
    val openSubtitlesApiKey: String = "",
    val subdlApiKey: String = "",
    val subtitleOffsetMillis: Long = 0L,
    val subtitleVerticalPositionPercent: Int = 0,
    val deviceName: String = "ECHO NOW",
    val clockMode: String = "AUTO",
    val manualClockValue: String = "",
    val timeZoneId: String = "AUTO",
    val timeFormat24Hour: Boolean = false,
    val showClockOnHome: Boolean = false,
    val showClockOnOtherPages: Boolean = false,
    val clockPattern: String = "EEE, MMM d · h:mm a",
    val clockColor: Long = 0xFFFFFFFF,
    val clockSizeSp: Int = 14,
    val languageTag: String = "SYSTEM",
)

@Serializable
data class PairedDevice(
    val id: String,
    val name: String,
    val pairingToken: String = "",
)

@Serializable
data class PlaybackRequest(
    val senderId: String,
    val mediaId: String,
    val season: Int = 0,
    val episode: Int = 0,
)

interface PreferencesRepository {
    fun observe(): Flow<UserPreferences>
    suspend fun update(preferences: UserPreferences)
    suspend fun reset()
}

@Serializable
data class WatchProgress(
    val mediaId: String,
    val title: String,
    val posterUrl: String,
    val type: MediaType,
    val positionMillis: Long,
    val durationMillis: Long,
    val season: Int = 0,
    val episode: Int = 0,
    val episodeTitle: String = "",
) {
    val watchedPercentage: Float
        get() = if (durationMillis <= 0L) 0f
        else (positionMillis.toDouble() / durationMillis).coerceIn(0.0, 1.0).toFloat()

    val isComplete: Boolean
        get() = watchedPercentage >= SERIES_CONTINUE_WATCHING_CUTOFF

    val seriesId: String?
        get() = mediaId.takeIf { type == MediaType.SERIES }

    val episodeId: String?
        get() = if (type == MediaType.SERIES && season > 0 && episode > 0) "$mediaId:$season:$episode" else null

    val entryId: String
        get() = if (type == MediaType.MOVIE) mediaId else "$mediaId:$season:$episode"

    companion object {
        const val SERIES_CONTINUE_WATCHING_CUTOFF = 0.9f
    }
}

interface WatchProgressRepository {
    fun observe(): Flow<List<WatchProgress>>
    suspend fun update(progress: WatchProgress)
    suspend fun remove(mediaId: String)
}
