// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.extrakeys

import android.view.KeyEvent
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.isModifier

/**
 *  Ctrl and Alt state of the extra keys, shared with the main keyboard.
 *  KeyboardActionListenerImpl adds [metaState] to every code input and calls [afterCodeInput] afterwards,
 *  so Ctrl from the extra keys followed by c on the main keyboard is sent as a Ctrl+C key event.
 */
object ExtraKeysModifiers {
    val ctrl = StickyModifier()
    val alt = StickyModifier()

    /** called whenever modifier states change, so the view can update */
    var onChange: (() -> Unit)? = null

    // while a repeating key is held, armed modifiers apply to every repetition and are consumed on release
    private var holdCount = 0
    private var consumePending = false

    val metaState: Int get() =
        (if (ctrl.isActive) KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON else 0) or
        (if (alt.isActive) KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON else 0)

    /**
     *  Combines the given (HeliBoard) meta state with the extra keys modifiers.
     *  Adds shift if the main keyboard is manually shifted and Ctrl or Alt are active, so Ctrl+Shift+letter works.
     */
    @JvmStatic
    fun adjustMetaState(metaState: Int, primaryCode: Int, shiftedManually: Boolean): Int {
        val combined = metaState or this.metaState
        if (combined and (KeyEvent.META_CTRL_MASK or KeyEvent.META_ALT_MASK) == 0) return combined
        return if (shiftedManually && primaryCode > 0 && Character.isUpperCase(primaryCode))
            combined or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        else combined
    }

    @JvmStatic
    fun afterCodeInput(primaryCode: Int) {
        if (primaryCode.isModifier()) return
        if (holdCount > 0) consumePending = true
        else consume()
    }

    fun beginHold() {
        holdCount++
    }

    fun endHold() {
        if (holdCount == 0) return
        holdCount--
        if (holdCount == 0 && consumePending) consume()
    }

    fun onModifierTap(modifier: StickyModifier, now: Long) {
        modifier.onTap(now)
        onChange?.invoke()
    }

    fun onModifierLongPress(modifier: StickyModifier) {
        modifier.onLongPress()
        onChange?.invoke()
    }

    fun reset() {
        ctrl.reset()
        alt.reset()
        holdCount = 0
        consumePending = false
        onChange?.invoke()
    }

    private fun consume() {
        consumePending = false
        val changed = ctrl.consume() or alt.consume() // no short-circuit, both need to be consumed
        if (changed) onChange?.invoke()
    }
}
