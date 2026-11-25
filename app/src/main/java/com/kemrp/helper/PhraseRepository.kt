package com.kemrp.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PhraseRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("kemrp_prefs", Context.MODE_PRIVATE)
    private val localFile = File(context.filesDir, "phrases.json")

    private fun loadJson(): JSONObject {
        if (!localFile.exists()) {
            copyAssetToLocal()
        }
        return try {
            JSONObject(localFile.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            copyAssetToLocal()
            JSONObject(localFile.readText(Charsets.UTF_8))
        }
    }

    private fun saveJson(obj: JSONObject) {
        localFile.writeText(obj.toString(2), Charsets.UTF_8)
    }

    private fun copyAssetToLocal() {
        context.assets.open("phrases.json").use { input ->
            localFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun resetToDefaults() {
        copyAssetToLocal()
    }

    fun getPhrases(sectionKey: String): MutableList<PhraseItem> {
        val root = loadJson()
        val arr = root.optJSONArray(sectionKey) ?: return mutableListOf()
        val list = mutableListOf<PhraseItem>()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONArray(i) ?: continue
            if (row.length() >= 3) {
                list.add(PhraseItem(row.optString(0), row.optString(1), row.optString(2)))
            }
        }
        return list
    }

    fun updatePhrase(sectionKey: String, index: Int, newText: String) {
        val root = loadJson()
        val arr = root.optJSONArray(sectionKey) ?: return
        val row = arr.optJSONArray(index) ?: return
        row.put(2, newText)
        saveJson(root)
    }

    fun getMegaphone(key: String): String {
        val root = loadJson()
        return root.optString(key, "")
    }

    fun getProfile(fid: String): Pair<String, String> {
        val def = AppData.factionById(fid)
        val root = loadJson()
        val profiles = root.optJSONObject("_profiles")
        val arr = profiles?.optJSONArray(fid)
        val rank = arr?.optString(0)?.takeIf { it.isNotBlank() } ?: def.defaultRank
        val name = arr?.optString(1)?.takeIf { it.isNotBlank() } ?: def.defaultName
        return rank to name
    }

    fun saveProfile(fid: String, rank: String, name: String) {
        val root = loadJson()
        val profiles = root.optJSONObject("_profiles") ?: JSONObject().also { root.put("_profiles", it) }
        val arr = JSONArray().apply {
            put(rank)
            put(name)
        }
        profiles.put(fid, arr)
        saveJson(root)
    }

    fun formatPhrase(rawText: String, fid: String): String {
        val (rank, name) = getProfile(fid)
        return rawText.replace("{rank}", rank).replace("{name}", name)
    }

    fun copyPhrase(rawText: String, fid: String, showToast: Boolean = true): String {
        val formatted = formatPhrase(rawText, fid)
        copyRaw(formatted, showToast)
        incrementSentCount()
        return formatted
    }

    fun copyRaw(text: String, showToast: Boolean = true) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("KemRPhelper", text))
        if (showToast) {
            Toast.makeText(context, "Скопировано в буфер!", Toast.LENGTH_SHORT).show()
        }
    }

    fun getSelectedFaction(): String = prefs.getString("selected_faction", "dps") ?: "dps"
    fun setSelectedFaction(fid: String) = prefs.edit().putString("selected_faction", fid).apply()

    fun getOverlayAlpha(): Float = prefs.getFloat("overlay_alpha", 0.85f)
    fun setOverlayAlpha(alpha: Float) = prefs.edit().putFloat("overlay_alpha", alpha.coerceIn(0.3f, 1.0f)).apply()

    fun getAutoMinimize(): Boolean = prefs.getBoolean("auto_minimize", true)
    fun setAutoMinimize(enabled: Boolean) = prefs.edit().putBoolean("auto_minimize", enabled).apply()

    fun getSentCount(): Int = prefs.getInt("sent_count", 0)
    fun incrementSentCount(): Int {
        val next = getSentCount() + 1
        prefs.edit().putInt("sent_count", next).apply()
        return next
    }
}
