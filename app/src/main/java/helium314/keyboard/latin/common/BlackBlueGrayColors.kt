// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.common

import android.graphics.drawable.Drawable
import androidx.core.graphics.toColorInt
import java.util.EnumMap

/** Built-in "Black blue-gray" theme: black background, dark gray keys, blue-gray functional keys, light blue accent. */
fun blackBlueGrayColors(themeStyle: String, hasKeyBorders: Boolean, backgroundImage: Drawable?): Colors {
    val background = "#000000".toColorInt()
    val key = "#202020".toColorInt()
    val functional = "#36394D".toColorInt()
    val text = "#FFFFFF".toColorInt()
    val hint = "#9AA0A6".toColorInt()
    val suggestion = "#E8EAED".toColorInt()
    val accent = "#A8C7FA".toColorInt()

    val colors = EnumMap<ColorType, Int>(ColorType::class.java)
    ColorType.entries.forEach { colors[it] = text } // text and icons, unless set below
    colors.putAll(mapOf(
        ColorType.ACTION_KEY_BACKGROUND to functional,
        ColorType.ACTION_KEY_POPUP_KEYS_BACKGROUND to functional,
        ColorType.AUTOFILL_BACKGROUND_CHIP to key,
        ColorType.CLIPBOARD_PIN to accent,
        ColorType.EMOJI_CATEGORY to hint,
        ColorType.EMOJI_CATEGORY_SELECTED to accent,
        ColorType.EMOJI_SEARCH_BACKGROUND to key,
        ColorType.FUNCTIONAL_KEY_BACKGROUND to functional,
        ColorType.GESTURE_TRAIL to accent,
        ColorType.GESTURE_PREVIEW to functional,
        ColorType.KEY_BACKGROUND to key,
        ColorType.KEY_HINT_TEXT to hint,
        ColorType.KEY_PREVIEW_BACKGROUND to functional,
        ColorType.MORE_SUGGESTIONS_HINT to hint,
        ColorType.MORE_SUGGESTIONS_BACKGROUND to key,
        ColorType.MORE_SUGGESTIONS_WORD_BACKGROUND to key,
        ColorType.POPUP_KEYS_BACKGROUND to functional,
        ColorType.NAVIGATION_BAR to background,
        ColorType.SPACE_BAR_BACKGROUND to key,
        ColorType.SPACE_BAR_TEXT to hint,
        ColorType.ONE_HANDED_MODE_BUTTON to hint,
        ColorType.REMOVE_SUGGESTION_ICON to hint,
        ColorType.STRIP_BACKGROUND to background,
        ColorType.CLIPBOARD_SUGGESTION_BACKGROUND to key,
        ColorType.SUGGESTED_WORD to suggestion,
        ColorType.SUGGESTION_TYPED_WORD to suggestion,
        ColorType.SUGGESTION_VALID_WORD to suggestion,
        ColorType.TOOL_BAR_EXPAND_KEY_BACKGROUND to key,
        ColorType.TOOL_BAR_KEY_ENABLED_BACKGROUND to functional,
        ColorType.MAIN_BACKGROUND to background,
    ))
    return AllColors(colors, themeStyle, hasKeyBorders, backgroundImage)
}
