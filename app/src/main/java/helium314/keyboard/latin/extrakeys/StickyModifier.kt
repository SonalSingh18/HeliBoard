// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.extrakeys

/**
 *  State of a sticky modifier key like Ctrl or Alt in the extra keys rows.
 *  Tap: armed for the next key. Double-tap or long press: locked until tapped again.
 */
class StickyModifier(private val doubleTapTimeoutMillis: Long = DEFAULT_DOUBLE_TAP_TIMEOUT) {
    enum class State { OFF, ARMED, LOCKED }

    var state = State.OFF
        private set
    private var lastTapTime = 0L

    val isActive get() = state != State.OFF

    fun onTap(now: Long) {
        state = when (state) {
            State.OFF -> State.ARMED
            State.ARMED -> if (now - lastTapTime <= doubleTapTimeoutMillis) State.LOCKED else State.OFF
            State.LOCKED -> State.OFF
        }
        lastTapTime = now
    }

    fun onLongPress() {
        state = State.LOCKED
    }

    /** Called after a key used this modifier. Returns whether the state changed. */
    fun consume(): Boolean {
        if (state != State.ARMED) return false
        state = State.OFF
        return true
    }

    fun reset() {
        state = State.OFF
    }

    companion object {
        const val DEFAULT_DOUBLE_TAP_TIMEOUT = 300L
    }
}
