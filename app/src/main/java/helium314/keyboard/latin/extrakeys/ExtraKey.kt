// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.extrakeys

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.Constants

enum class ExtraKeyType {
    /** sent once on release */
    NORMAL,
    /** sticky modifier (Ctrl / Alt), never sent as a key by itself */
    MODIFIER,
    /** sent on press, and repeated while held */
    REPEATABLE,
    /** handled by the extra keys view itself, e.g. switching to the next page */
    PAGE,
}

/** A key in the extra keys rows. [code] is a HeliBoard [KeyCode] or a positive code point. */
data class ExtraKey(val label: String, val code: Int, val type: ExtraKeyType = ExtraKeyType.NORMAL)

typealias ExtraKeysPage = List<List<ExtraKey>>

/**
 *  Extra keys are configured as text, similar to Termux extra keys:
 *  keys are separated by spaces, rows by line breaks, and pages by empty lines.
 *  A key is either one of the names in [namedKeys] (case-insensitive), or a single character that is typed as-is.
 */
object ExtraKeysParser {
    const val DEFAULT_CONFIG = """ESC TAB PGUP HOME UP END MENU
CTRL ALT PGDN LEFT DOWN RIGHT SWITCH

/ - | ~ _ \ MENU
F1 F2 F3 F5 F10 : SWITCH"""

    val ctrl = ExtraKey("Ctrl", KeyCode.CTRL, ExtraKeyType.MODIFIER)
    val alt = ExtraKey("Alt", KeyCode.ALT, ExtraKeyType.MODIFIER)

    private val namedKeys: Map<String, ExtraKey> = mapOf(
        "CTRL" to ctrl,
        "ALT" to alt,
        "ESC" to ExtraKey("Esc", KeyCode.ESCAPE),
        "TAB" to ExtraKey("Tab", KeyCode.TAB),
        "HOME" to ExtraKey("Home", KeyCode.MOVE_START_OF_LINE),
        "END" to ExtraKey("End", KeyCode.MOVE_END_OF_LINE),
        "INS" to ExtraKey("Ins", KeyCode.INSERT),
        "ENTER" to ExtraKey("⏎", Constants.CODE_ENTER),
        "SPACE" to ExtraKey("␣", Constants.CODE_SPACE),
        "PGUP" to ExtraKey("PgUp", KeyCode.PAGE_UP, ExtraKeyType.REPEATABLE),
        "PGDN" to ExtraKey("PgDn", KeyCode.PAGE_DOWN, ExtraKeyType.REPEATABLE),
        "UP" to ExtraKey("↑", KeyCode.ARROW_UP, ExtraKeyType.REPEATABLE),
        "DOWN" to ExtraKey("↓", KeyCode.ARROW_DOWN, ExtraKeyType.REPEATABLE),
        "LEFT" to ExtraKey("←", KeyCode.ARROW_LEFT, ExtraKeyType.REPEATABLE),
        "RIGHT" to ExtraKey("→", KeyCode.ARROW_RIGHT, ExtraKeyType.REPEATABLE),
        "BKSP" to ExtraKey("⌫", KeyCode.DELETE, ExtraKeyType.REPEATABLE),
        "SWITCH" to ExtraKey("⇄", KeyCode.SYMBOL_ALPHA),
        "MENU" to ExtraKey("☰", KeyCode.UNSPECIFIED, ExtraKeyType.PAGE),
    ) + (1..12).associate { "F$it" to ExtraKey("F$it", KeyCode.F1 - (it - 1)) } // F-key codes are descending

    fun parse(config: String): List<ExtraKeysPage> =
        config.replace("\r", "").split(Regex("\n\\s*\n")).mapNotNull { pageText ->
            val rows = pageText.lines().mapNotNull { line ->
                line.split(' ', '\t').filter { it.isNotEmpty() }.mapNotNull { parseKey(it) }.takeIf { it.isNotEmpty() }
            }
            rows.takeIf { it.isNotEmpty() }
        }

    fun parseKey(token: String): ExtraKey? {
        namedKeys[token.uppercase()]?.let { return it }
        if (token.codePointCount(0, token.length) != 1) return null
        return ExtraKey(token, token.codePointAt(0))
    }
}
