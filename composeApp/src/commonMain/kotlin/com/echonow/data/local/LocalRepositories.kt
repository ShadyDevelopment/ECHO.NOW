package com.echonow.data.local

import com.echonow.domain.repository.FavoritesRepository
import com.echonow.domain.repository.PreferencesRepository
import com.echonow.domain.repository.ProfileRepository
import com.echonow.domain.repository.ProfileState
import com.echonow.domain.repository.ProfileAvatars
import com.echonow.domain.repository.UserProfile
import com.echonow.domain.repository.UserPreferences
import com.echonow.domain.repository.WatchProgress
import com.echonow.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsFavoritesRepository(
    private val store: KeyValueStore,
    private val profiles: ProfileRepository,
) : FavoritesRepository {
    private val mutex = Mutex()

    override fun observe(): Flow<Set<String>> =
        profiles.observe().map { it.activeProfileId }.distinctUntilChanged().flatMapLatest { profileId ->
            if (profileId == null) flowOf(emptySet()) else {
                store.observeString(profileKey(profileId, KEY_FAVORITES), "").map { value ->
                    value.split('|').filter(String::isNotBlank).toSet()
                }
            }
        }

    override suspend fun toggle(mediaId: String) {
        require(mediaId.startsWith("movie:") || mediaId.startsWith("series:")) {
            "Favorite id must identify a TMDB movie or series."
        }
        mutex.withLock {
            val profileId = requireActiveProfile()
            val key = profileKey(profileId, KEY_FAVORITES)
            val current = store.observeString(key, "").first()
                .split('|').filter(String::isNotBlank).toSet()
            val updated = current.toMutableSet().apply {
                if (!add(mediaId)) remove(mediaId)
            }
            store.putString(key, updated.sorted().joinToString("|"))
        }
    }

    override suspend fun clear() {
        store.remove(profileKey(requireActiveProfile(), KEY_FAVORITES))
    }

    private suspend fun requireActiveProfile(): String =
        profiles.observe().first().activeProfileId
            ?: error("Cannot access favorites without an active profile.")

    private companion object { const val KEY_FAVORITES = "favorites" }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPreferencesRepository(
    private val store: KeyValueStore,
    private val profiles: ProfileRepository,
) : PreferencesRepository {
    private val defaults = Json.encodeToString(UserPreferences())

    override fun observe(): Flow<UserPreferences> =
        profiles.observe().map { it.activeProfileId }.distinctUntilChanged().flatMapLatest { profileId ->
            if (profileId == null) flowOf(UserPreferences()) else {
                store.observeString(profileKey(profileId, KEY_PREFERENCES), defaults)
                    .map { Json.decodeFromString<UserPreferences>(it) }
            }
        }

    override suspend fun update(preferences: UserPreferences) {
        val profileId = requireActiveProfile()
        store.putString(profileKey(profileId, KEY_PREFERENCES), Json.encodeToString(preferences))
    }

    override suspend fun reset() = update(UserPreferences())

    private suspend fun requireActiveProfile(): String =
        profiles.observe().first().activeProfileId
            ?: error("Cannot save settings without an active profile.")

    private companion object { const val KEY_PREFERENCES = "preferences" }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocalWatchProgressRepository(
    private val store: KeyValueStore,
    private val profiles: ProfileRepository,
) : WatchProgressRepository {
    private val mutex = Mutex()

    override fun observe(): Flow<List<WatchProgress>> =
        profiles.observe().map { it.activeProfileId }.distinctUntilChanged().flatMapLatest { profileId ->
            if (profileId == null) flowOf(emptyList()) else {
                store.observeString(profileKey(profileId, KEY_PROGRESS), "[]")
                    .map { Json.decodeFromString<List<WatchProgress>>(it) }
            }
        }

    override suspend fun update(progress: WatchProgress) {
        require(progress.positionMillis >= 0 && progress.durationMillis > 0) {
            "Watch progress requires a non-negative position and a positive duration."
        }
        mutex.withLock {
            val key = profileKey(requireActiveProfile(), KEY_PROGRESS)
            val current = store.observeString(key, "[]").first()
                .let { Json.decodeFromString<List<WatchProgress>>(it) }
                .filterNot { it.entryId == progress.entryId }
            val updated = listOf(progress) + current
            store.putString(key, Json.encodeToString(updated.take(MAX_ITEMS)))
        }
    }

    override suspend fun remove(mediaId: String) {
        mutex.withLock {
            val key = profileKey(requireActiveProfile(), KEY_PROGRESS)
            val updated = store.observeString(key, "[]").first()
                .let { Json.decodeFromString<List<WatchProgress>>(it) }
                .filterNot { it.mediaId == mediaId }
            store.putString(key, Json.encodeToString(updated))
        }
    }

    private suspend fun requireActiveProfile(): String =
        profiles.observe().first().activeProfileId
            ?: error("Cannot save watch progress without an active profile.")

    private companion object {
        const val KEY_PROGRESS = "watch_progress"
        const val MAX_ITEMS = 30
    }
}

class LocalProfileRepository(private val store: KeyValueStore) : ProfileRepository {
    private val mutex = Mutex()

    override fun observe(): Flow<ProfileState> = combine(
        store.observeString(KEY_PROFILES, "[]"),
        store.observeString(KEY_ACTIVE_PROFILE, ""),
    ) { encodedProfiles, activeId ->
        val profiles = Json.decodeFromString<List<UserProfile>>(encodedProfiles)
        ProfileState(
            isLoading = false,
            profiles = profiles,
            activeProfileId = activeId.takeIf { id -> profiles.any { it.id == id } }
                ?: profiles.firstOrNull()?.id,
        )
    }

    override suspend fun create(nickname: String, avatarId: String): UserProfile = mutex.withLock {
        val normalizedNickname = nickname.trim()
        require(normalizedNickname.isNotEmpty()) { "Profile nickname cannot be empty." }
        require(normalizedNickname.length <= MAX_NICKNAME_LENGTH) {
            "Profile nickname cannot exceed $MAX_NICKNAME_LENGTH characters."
        }
        require(ProfileAvatars.isValid(avatarId)) { "Choose a valid profile avatar." }

        val current = observe().first()
        require(current.profiles.size < ProfileAvatars.MAX_PROFILES) {
            "You can create up to ${ProfileAvatars.MAX_PROFILES} profiles."
        }
        val profileId = if (current.profiles.isEmpty()) {
            store.observeString(KEY_PENDING_PROFILE_ID, "").first().ifBlank {
                newProfileId(current.profiles).also { store.putString(KEY_PENDING_PROFILE_ID, it) }
            }
        } else {
            newProfileId(current.profiles)
        }
        val profile = UserProfile(
            id = profileId,
            nickname = normalizedNickname,
            avatarId = avatarId,
        )
        if (current.profiles.isEmpty()) migrateLegacyData(profile.id)
        val updated = current.profiles + profile
        store.putString(KEY_PROFILES, Json.encodeToString(updated))
        store.putString(KEY_ACTIVE_PROFILE, profile.id)
        store.remove(KEY_PENDING_PROFILE_ID)
        profile
    }

    override suspend fun update(profile: UserProfile) = mutex.withLock {
        require(profile.nickname.trim().isNotEmpty()) { "Profile nickname cannot be empty." }
        require(profile.nickname.trim().length <= MAX_NICKNAME_LENGTH) {
            "Profile nickname cannot exceed $MAX_NICKNAME_LENGTH characters."
        }
        require(ProfileAvatars.isValid(profile.avatarId)) { "Choose a valid profile avatar." }
        val current = observe().first()
        require(current.profiles.any { it.id == profile.id }) { "Profile no longer exists." }
        store.putString(
            KEY_PROFILES,
            Json.encodeToString(current.profiles.map {
                if (it.id == profile.id) profile.copy(nickname = profile.nickname.trim()) else it
            }),
        )
    }

    override suspend fun remove(profileId: String) = mutex.withLock {
        val current = observe().first()
        require(current.profiles.any { it.id == profileId }) { "Profile no longer exists." }
        val remaining = current.profiles.filterNot { it.id == profileId }
        listOf(KEY_PREFERENCES, KEY_FAVORITES, KEY_PROGRESS).forEach { dataKey ->
            store.remove(profileKey(profileId, dataKey))
        }
        store.putString(KEY_PROFILES, Json.encodeToString(remaining))
        if (current.activeProfileId == profileId) {
            store.putString(KEY_ACTIVE_PROFILE, remaining.firstOrNull()?.id.orEmpty())
        }
    }

    override suspend fun select(profileId: String) = mutex.withLock {
        require(observe().first().profiles.any { it.id == profileId }) { "Profile no longer exists." }
        store.putString(KEY_ACTIVE_PROFILE, profileId)
    }

    private suspend fun migrateLegacyData(firstProfileId: String) {
        val keysToMigrate = listOf(KEY_PREFERENCES, KEY_FAVORITES, KEY_PROGRESS)
        if (store.observeString(KEY_MIGRATION_COMPLETE, "").first() != "true") {
            keysToMigrate.forEach { dataKey ->
                val legacyValue = store.observeString(dataKey, MISSING_VALUE).first()
                if (legacyValue != MISSING_VALUE) {
                    val targetKey = profileKey(firstProfileId, dataKey)
                    if (store.observeString(targetKey, MISSING_VALUE).first() == MISSING_VALUE) {
                        store.putString(targetKey, legacyValue)
                    }
                }
            }
            store.putString(KEY_MIGRATION_COMPLETE, "true")
        }
        keysToMigrate.forEach { store.remove(it) }
    }

    private fun newProfileId(profiles: List<UserProfile>): String {
        var id: String
        do {
            id = Random.nextBytes(PROFILE_ID_BYTES)
                .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
                .let { "profile-$it" }
        } while (profiles.any { it.id == id })
        return id
    }

    private companion object {
        const val KEY_PROFILES = "profiles"
        const val KEY_ACTIVE_PROFILE = "active_profile"
        const val KEY_MIGRATION_COMPLETE = "profile_data_migration_v1"
        const val KEY_PENDING_PROFILE_ID = "profile_data_migration_pending_id"
        const val KEY_PREFERENCES = "preferences"
        const val KEY_FAVORITES = "favorites"
        const val KEY_PROGRESS = "watch_progress"
        const val MISSING_VALUE = "\u0000missing-profile-data\u0000"
        const val MAX_NICKNAME_LENGTH = 24
        const val PROFILE_ID_BYTES = 16
    }
}

private fun profileKey(profileId: String, key: String) = "profile.$profileId.$key"
