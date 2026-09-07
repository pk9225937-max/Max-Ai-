package com.example.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WallpaperType {
    CUTE_GIRL,
    CUSTOM_PHOTO,
    CYBER_DARK
}

enum class AssistantPersonality {
    MAX_NORMAL,
    GIRLFRIEND_MODE
}

class WallpaperThemeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("max_theme_prefs", Context.MODE_PRIVATE)

    private val _wallpaperType = MutableStateFlow(loadWallpaperType())
    val wallpaperType: StateFlow<WallpaperType> = _wallpaperType.asStateFlow()

    private val _customPhotoUri = MutableStateFlow(prefs.getString("custom_wallpaper_uri", null))
    val customPhotoUri: StateFlow<String?> = _customPhotoUri.asStateFlow()

    private val _wallpaperDim = MutableStateFlow(prefs.getFloat("wallpaper_dim", 0.20f))
    val wallpaperDim: StateFlow<Float> = _wallpaperDim.asStateFlow()

    private val _cleanMode = MutableStateFlow(prefs.getBoolean("clean_screen_mode", true))
    val cleanMode: StateFlow<Boolean> = _cleanMode.asStateFlow()

    private val _personality = MutableStateFlow(loadPersonality())
    val personality: StateFlow<AssistantPersonality> = _personality.asStateFlow()

    private val _selectedVoice = MutableStateFlow(prefs.getString("gemini_voice_name", "Aoede") ?: "Aoede")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private fun loadWallpaperType(): WallpaperType {
        val saved = prefs.getString("wallpaper_type", WallpaperType.CUTE_GIRL.name)
        return try {
            WallpaperType.valueOf(saved ?: WallpaperType.CUTE_GIRL.name)
        } catch (e: Exception) {
            WallpaperType.CUTE_GIRL
        }
    }

    private fun loadPersonality(): AssistantPersonality {
        val saved = prefs.getString("assistant_personality", AssistantPersonality.MAX_NORMAL.name)
        return try {
            AssistantPersonality.valueOf(saved ?: AssistantPersonality.MAX_NORMAL.name)
        } catch (e: Exception) {
            AssistantPersonality.MAX_NORMAL
        }
    }

    fun setWallpaperType(type: WallpaperType) {
        prefs.edit().putString("wallpaper_type", type.name).apply()
        _wallpaperType.value = type
    }

    fun setCustomPhotoUri(uriString: String?) {
        if (uriString != null) {
            prefs.edit().putString("custom_wallpaper_uri", uriString).apply()
            _customPhotoUri.value = uriString
            setWallpaperType(WallpaperType.CUSTOM_PHOTO)
        } else {
            prefs.edit().remove("custom_wallpaper_uri").apply()
            _customPhotoUri.value = null
            setWallpaperType(WallpaperType.CUTE_GIRL)
        }
    }

    fun setWallpaperDim(dim: Float) {
        val clamped = dim.coerceIn(0.0f, 0.85f)
        prefs.edit().putFloat("wallpaper_dim", clamped).apply()
        _wallpaperDim.value = clamped
    }

    fun toggleCleanMode() {
        val newMode = !_cleanMode.value
        prefs.edit().putBoolean("clean_screen_mode", newMode).apply()
        _cleanMode.value = newMode
    }

    fun setCleanMode(clean: Boolean) {
        prefs.edit().putBoolean("clean_screen_mode", clean).apply()
        _cleanMode.value = clean
    }

    fun togglePersonality(): AssistantPersonality {
        val next = if (_personality.value == AssistantPersonality.MAX_NORMAL) {
            AssistantPersonality.GIRLFRIEND_MODE
        } else {
            AssistantPersonality.MAX_NORMAL
        }
        setPersonality(next)
        return next
    }

    fun setPersonality(personality: AssistantPersonality) {
        prefs.edit().putString("assistant_personality", personality.name).apply()
        _personality.value = personality
    }

    fun setVoice(voice: String) {
        prefs.edit().putString("gemini_voice_name", voice).apply()
        _selectedVoice.value = voice
    }
}
