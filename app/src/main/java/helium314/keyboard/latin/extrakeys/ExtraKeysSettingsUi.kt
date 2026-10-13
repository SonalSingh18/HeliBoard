// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.extrakeys

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.dialogs.TextInputDialog
import helium314.keyboard.settings.preferences.ListPreference
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.TextInputPreference

/** Items for the "Extra keys" category, to be inserted into a settings screen. */
fun extraKeysSettingsItems(prefs: SharedPreferences): List<Any?> {
    val enabled = ExtraKeysSettings.readMode(prefs) != ExtraKeysSettings.Mode.OFF
    val customConfig = !prefs.getString(ExtraKeysSettings.PREF_CONFIG, null).isNullOrBlank()
    return listOf(
        R.string.settings_category_extra_keys,
        ExtraKeysSettings.PREF_MODE,
        if (enabled && !customConfig) ExtraKeysSettings.PREF_ROWS else null,
        if (enabled) ExtraKeysSettings.PREF_TERMINAL_APPS else null,
        if (enabled) ExtraKeysSettings.PREF_CONFIG else null,
    )
}

fun createExtraKeysSettings(context: Context) = listOf(
    Setting(context, ExtraKeysSettings.PREF_MODE, R.string.extra_keys_mode) { setting ->
        val items = listOf(
            stringResource(R.string.extra_keys_mode_off) to ExtraKeysSettings.Mode.OFF.name,
            stringResource(R.string.extra_keys_mode_always) to ExtraKeysSettings.Mode.ALWAYS.name,
            stringResource(R.string.extra_keys_mode_terminal) to ExtraKeysSettings.Mode.TERMINAL.name,
        )
        ListPreference(setting, items, ExtraKeysSettings.DEFAULT_MODE.name)
    },
    Setting(context, ExtraKeysSettings.PREF_ROWS, R.string.extra_keys_rows) { setting ->
        ListPreference(setting, listOf("1" to 1, "2" to 2), ExtraKeysSettings.DEFAULT_ROWS)
    },
    Setting(context, ExtraKeysSettings.PREF_TERMINAL_APPS, R.string.extra_keys_terminal_apps) { setting ->
        TextInputPreference(setting, ExtraKeysSettings.DEFAULT_TERMINAL_APPS, stringResource(R.string.extra_keys_terminal_apps_info))
    },
    Setting(context, ExtraKeysSettings.PREF_CONFIG, R.string.extra_keys_config) { setting ->
        var showDialog by rememberSaveable { mutableStateOf(false) }
        val prefs = LocalContext.current.prefs()
        Preference(name = setting.title, onClick = { showDialog = true })
        if (showDialog) {
            TextInputDialog(
                onDismissRequest = { showDialog = false },
                onConfirmed = { prefs.edit { putString(setting.key, it) } },
                initialText = ExtraKeysSettings.readConfig(prefs),
                title = { Text(setting.title) },
                description = { Text(stringResource(R.string.extra_keys_config_info)) },
                singleLine = false,
                checkTextValid = { ExtraKeysParser.parse(it).isNotEmpty() },
                onNeutral = { prefs.edit { remove(setting.key) }; showDialog = false },
                neutralButtonText = stringResource(R.string.button_default),
            )
        }
    },
)
