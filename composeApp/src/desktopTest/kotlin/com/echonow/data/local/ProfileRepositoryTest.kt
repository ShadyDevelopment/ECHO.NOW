package com.echonow.data.local

import com.echonow.domain.model.MediaType
import com.echonow.domain.repository.ProfileAvatars
import com.echonow.domain.repository.UserPreferences
import com.echonow.domain.repository.WatchProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileRepositoryTest {
    @Test
    fun subtitleVerticalPositionDefaultsForOldPreferencesAndPersists() = runBlocking {
        assertEquals(0, Json.decodeFromString<UserPreferences>("""{"theme":"Dark"}""").subtitleVerticalPositionPercent)

        val store = MemoryKeyValueStore()
        val profiles = LocalProfileRepository(store)
        val preferences = SettingsPreferencesRepository(store, profiles)
        profiles.create("Viewer", "film")
        preferences.update(UserPreferences(subtitleVerticalPositionPercent = 18))

        assertEquals(18, preferences.observe().firstValue().subtitleVerticalPositionPercent)
    }

    @Test
    fun piccdnAvatarIdsResolveToValidSquareAvatarUrls() {
        val avatarId = ProfileAvatars.id("server-icons", 247)
        assertTrue(ProfileAvatars.isValid(avatarId))
        assertEquals(
            "https://raw.githubusercontent.com/itsfizys/piccdn/main/server-icons/247.jpg",
            ProfileAvatars.imageUrl(avatarId),
        )
        assertFalse(ProfileAvatars.isValid(ProfileAvatars.id("wallpapers", 247)))
        assertFalse(ProfileAvatars.isValid(ProfileAvatars.id("anime", 1_000)))
    }

    @Test
    fun profileDataIsIsolatedAndFollowsActiveProfile() = runBlocking {
        val store = MemoryKeyValueStore()
        val profiles = LocalProfileRepository(store)
        val preferences = SettingsPreferencesRepository(store, profiles)
        val favorites = SettingsFavoritesRepository(store, profiles)
        val watchProgress = LocalWatchProgressRepository(store, profiles)

        val alex = profiles.create("Alex", "film")
        preferences.update(UserPreferences(theme = "Light"))
        favorites.toggle("movie:101")
        watchProgress.update(progress("series:201", MediaType.SERIES))

        val sam = profiles.create("Sam", "rocket")
        assertNotEquals(alex.id, sam.id)
        assertEquals(UserPreferences(), preferences.observe().firstValue())
        assertTrue(favorites.observe().firstValue().isEmpty())
        assertTrue(watchProgress.observe().firstValue().isEmpty())

        favorites.toggle("movie:202")
        profiles.select(alex.id)
        assertEquals("Light", preferences.observe().firstValue().theme)
        assertEquals(setOf("movie:101"), favorites.observe().firstValue())
        assertEquals(listOf("series:201"), watchProgress.observe().firstValue().map { it.mediaId })
    }

    @Test
    fun firstProfileMigratesLegacyDataAndRetriesWithStableId() = runBlocking {
        val legacyProgress = listOf(progress("series:303", MediaType.SERIES))
        val store = MemoryKeyValueStore(
            initial = mapOf(
                "preferences" to Json.encodeToString(UserPreferences(theme = "Light")),
                "favorites" to "movie:302",
                "watch_progress" to Json.encodeToString(legacyProgress),
            ),
        )
        val profiles = LocalProfileRepository(store)
        store.failNextProfilesWrite = true

        val failed = runCatching { profiles.create("Taylor", "star") }.exceptionOrNull()
        assertTrue(failed is IllegalStateException)
        val retriedProfile = profiles.create("Taylor", "star")
        val preferences = SettingsPreferencesRepository(store, profiles)
        val favorites = SettingsFavoritesRepository(store, profiles)
        val watchProgress = LocalWatchProgressRepository(store, profiles)

        assertEquals("Light", preferences.observe().firstValue().theme)
        assertEquals(setOf("movie:302"), favorites.observe().firstValue())
        assertEquals(legacyProgress, watchProgress.observe().firstValue())
        assertEquals(retriedProfile.id, profiles.observe().firstValue().activeProfileId)
        assertEquals("true", store.value("profile_data_migration_v1"))
        assertNull(store.value("preferences"))
        assertNull(store.value("favorites"))
        assertNull(store.value("watch_progress"))
    }

    @Test
    fun profileLimitAndRemovalNeverLeaveInvalidActiveProfile() = runBlocking {
        val store = MemoryKeyValueStore()
        val profiles = LocalProfileRepository(store)
        val created = (1..ProfileAvatars.MAX_PROFILES).map { index ->
            profiles.create("Viewer $index", ProfileAvatars.id(ProfileAvatars.categories.first(), index - 1))
        }

        assertTrue(runCatching { profiles.create("Fifth", "film") }.exceptionOrNull() is IllegalArgumentException)
        profiles.select(created[2].id)
        profiles.remove(created[2].id)
        assertEquals(created[0].id, profiles.observe().firstValue().activeProfileId)

        profiles.remove(created[0].id)
        profiles.remove(created[1].id)
        profiles.remove(created[3].id)
        val state = profiles.observe().firstValue()
        assertTrue(state.profiles.isEmpty())
        assertNull(state.activeProfileId)
        assertFalse(state.isLoading)
    }

    private fun progress(id: String, type: MediaType) = WatchProgress(
        mediaId = id,
        title = "Title",
        posterUrl = "",
        type = type,
        positionMillis = 10_000,
        durationMillis = 100_000,
    )

    private suspend fun <T> Flow<T>.firstValue(): T =
        first()
}

private class MemoryKeyValueStore(
    initial: Map<String, String> = emptyMap(),
) : KeyValueStore {
    private val values = initial.toMutableMap()
    private val entries = mutableMapOf<String, Pair<String, MutableStateFlow<String>>>()
    var failNextProfilesWrite = false

    override fun observeString(key: String, default: String): Flow<String> =
        synchronized(entries) {
            entries.getOrPut(key) {
                default to MutableStateFlow(values[key] ?: default)
            }.second.asStateFlow()
        }

    override suspend fun putString(key: String, value: String) {
        if (key == "profiles" && failNextProfilesWrite) {
            failNextProfilesWrite = false
            throw IllegalStateException("Simulated persistence failure.")
        }
        values[key] = value
        synchronized(entries) { entries[key]?.second?.value = value }
    }

    override suspend fun remove(key: String) {
        values.remove(key)
        synchronized(entries) {
            entries[key]?.let { (default, state) -> state.value = default }
        }
    }

    fun value(key: String): String? = values[key]
}
