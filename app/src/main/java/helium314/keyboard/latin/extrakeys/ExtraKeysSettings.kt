// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.extrakeys

import android.content.SharedPreferences
import androidx.core.content.edit

/** Preferences for the extra keys rows. Kept here instead of Settings / Defaults to keep changes to upstream files small. */
object ExtraKeysSettings {
    const val PREF_MODE = "extra_keys_mode"
    const val PREF_ROWS = "extra_keys_rows"
    const val PREF_TERMINAL_APPS = "extra_keys_terminal_apps"
    const val PREF_CONFIG = "extra_keys_config"

    enum class Mode { OFF, ALWAYS, TERMINAL }

    val DEFAULT_MODE = Mode.TERMINAL
    const val DEFAULT_ROWS = 2
    /** Termux, ConnectBot, JuiceSSH, Termius, Android Terminal Emulator, NeoTerm */
    const val DEFAULT_TERMINAL_APPS =
        "com.termux org.connectbot com.sonelli.juicessh com.server.auditor.ssh.client jackpal.androidterm io.neoterm"

    @JvmStatic
    fun readMode(prefs: SharedPreferences): Mode =
        runCatching { Mode.valueOf(prefs.getString(PREF_MODE, null) ?: DEFAULT_MODE.name) }.getOrDefault(DEFAULT_MODE)

    fun readTerminalApps(prefs: SharedPreferences): Set<String> =
        parsePackageList(prefs.getString(PREF_TERMINAL_APPS, DEFAULT_TERMINAL_APPS) ?: "")

    fun parsePackageList(text: String): Set<String> =
        text.split(' ', ',', '\n', '\t').filter { it.isNotBlank() }.toCollection(LinkedHashSet())

    /** Terminal mode: autocorrect, suggestions, auto-caps and auto-space are off for this field (without changing user settings). */
    @JvmStatic
    fun isTerminalMode(prefs: SharedPreferences, packageName: String?): Boolean =
        packageName != null && readMode(prefs) != Mode.OFF && packageName in readTerminalApps(prefs)

    @JvmStatic
    fun showExtraKeys(prefs: SharedPreferences, terminalMode: Boolean): Boolean = when (readMode(prefs)) {
        Mode.OFF -> false
        Mode.ALWAYS -> true
        Mode.TERMINAL -> terminalMode
    }

    /** The config text the extra keys are built from: user config if set, otherwise the default for the number of rows. */
    fun readConfig(prefs: SharedPreferences): String {
        val custom = prefs.getString(PREF_CONFIG, null)
        if (!custom.isNullOrBlank() && ExtraKeysParser.parse(custom).isNotEmpty()) return custom
        return if (prefs.getInt(PREF_ROWS, DEFAULT_ROWS) == 1) ExtraKeysParser.DEFAULT_CONFIG_ONE_ROW
        else ExtraKeysParser.DEFAULT_CONFIG
    }

    /** Adds the app to the terminal apps if it's not in there, otherwise removes it. */
    fun toggleTerminalApp(prefs: SharedPreferences, packageName: String) {
        val apps = readTerminalApps(prefs).toMutableSet()
        if (!apps.remove(packageName)) apps.add(packageName)
        prefs.edit { putString(PREF_TERMINAL_APPS, apps.joinToString(" ")) }
    }
}
