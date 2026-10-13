// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.extrakeys

import android.content.Context
import android.view.KeyEvent
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.ShadowInputMethodManager2
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StickyModifierTest {
    private val modifier = StickyModifier(doubleTapTimeoutMillis = 300)

    @Test fun tapArms() {
        modifier.onTap(1000)
        assertEquals(StickyModifier.State.ARMED, modifier.state)
    }

    @Test fun secondSlowTapDisarms() {
        modifier.onTap(1000)
        modifier.onTap(2000)
        assertEquals(StickyModifier.State.OFF, modifier.state)
    }

    @Test fun doubleTapLocks() {
        modifier.onTap(1000)
        modifier.onTap(1200)
        assertEquals(StickyModifier.State.LOCKED, modifier.state)
    }

    @Test fun tapUnlocks() {
        modifier.onLongPress()
        assertEquals(StickyModifier.State.LOCKED, modifier.state)
        modifier.onTap(1000)
        assertEquals(StickyModifier.State.OFF, modifier.state)
    }

    @Test fun consumeOnlyReleasesArmed() {
        modifier.onTap(1000)
        assertTrue(modifier.consume())
        assertEquals(StickyModifier.State.OFF, modifier.state)
        assertFalse(modifier.consume())

        modifier.onLongPress()
        assertFalse(modifier.consume())
        assertEquals(StickyModifier.State.LOCKED, modifier.state)
    }

    @Test fun tapAfterLockedIsNotDoubleTap() {
        modifier.onTap(1000)
        modifier.onTap(1100) // locked
        modifier.onTap(1200) // off
        modifier.onTap(1300) // armed: previous tap was within timeout, but state was off
        assertEquals(StickyModifier.State.ARMED, modifier.state)
    }
}

class ExtraKeysModifiersTest {
    @Before fun reset() = ExtraKeysModifiers.reset()

    @Test fun noModifiersKeepsMetaState() {
        assertEquals(0, ExtraKeysModifiers.adjustMetaState(0, 'c'.code, false))
        assertEquals(KeyEvent.META_SHIFT_ON, ExtraKeysModifiers.adjustMetaState(KeyEvent.META_SHIFT_ON, 'C'.code, true))
    }

    @Test fun ctrlAddsMetaAndIsConsumed() {
        ExtraKeysModifiers.onModifierTap(ExtraKeysModifiers.ctrl, 1000)
        val meta = ExtraKeysModifiers.adjustMetaState(0, 'c'.code, false)
        assertTrue(meta and KeyEvent.META_CTRL_ON != 0)
        assertTrue(meta and KeyEvent.META_SHIFT_ON == 0)
        ExtraKeysModifiers.afterCodeInput('c'.code)
        assertFalse(ExtraKeysModifiers.ctrl.isActive)
    }

    @Test fun ctrlShiftOnlyWhenShiftedManually() {
        ExtraKeysModifiers.onModifierTap(ExtraKeysModifiers.ctrl, 1000)
        // auto-capitalized letter: no shift
        assertTrue(ExtraKeysModifiers.adjustMetaState(0, 'C'.code, false) and KeyEvent.META_SHIFT_ON == 0)
        assertTrue(ExtraKeysModifiers.adjustMetaState(0, 'C'.code, true) and KeyEvent.META_SHIFT_ON != 0)
        // not for non-letters
        assertTrue(ExtraKeysModifiers.adjustMetaState(0, KeyCode.ARROW_LEFT, true) and KeyEvent.META_SHIFT_ON == 0)
    }

    @Test fun modifierKeysDoNotConsume() {
        ExtraKeysModifiers.onModifierTap(ExtraKeysModifiers.alt, 1000)
        ExtraKeysModifiers.afterCodeInput(KeyCode.SHIFT)
        assertTrue(ExtraKeysModifiers.alt.isActive)
        ExtraKeysModifiers.afterCodeInput('b'.code)
        assertFalse(ExtraKeysModifiers.alt.isActive)
    }

    @Test fun lockedSurvivesInput() {
        ExtraKeysModifiers.onModifierLongPress(ExtraKeysModifiers.ctrl)
        ExtraKeysModifiers.afterCodeInput('c'.code)
        ExtraKeysModifiers.afterCodeInput('d'.code)
        assertTrue(ExtraKeysModifiers.ctrl.isActive)
    }

    @Test fun heldRepeatKeyConsumesOnRelease() {
        ExtraKeysModifiers.onModifierTap(ExtraKeysModifiers.ctrl, 1000)
        ExtraKeysModifiers.beginHold()
        repeat(5) { ExtraKeysModifiers.afterCodeInput(KeyCode.ARROW_LEFT) }
        assertTrue(ExtraKeysModifiers.ctrl.isActive)
        ExtraKeysModifiers.endHold()
        assertFalse(ExtraKeysModifiers.ctrl.isActive)
    }

    @Test fun onChangeIsCalled() {
        var calls = 0
        ExtraKeysModifiers.onChange = { calls++ }
        ExtraKeysModifiers.onModifierTap(ExtraKeysModifiers.ctrl, 1000)
        ExtraKeysModifiers.afterCodeInput('x'.code)
        ExtraKeysModifiers.afterCodeInput('y'.code) // nothing to consume, no call
        assertEquals(2, calls)
        ExtraKeysModifiers.onChange = null
    }
}

class ExtraKeysParserTest {
    @Test fun defaultConfigHasTwoPagesOfTwoRows() {
        val pages = ExtraKeysParser.parse(ExtraKeysParser.DEFAULT_CONFIG)
        assertEquals(2, pages.size)
        pages.forEach { page ->
            assertEquals(2, page.size)
            page.forEach { assertEquals(7, it.size) }
        }
        val first = pages.first()
        assertEquals(listOf("Esc", "Tab", "PgUp", "Home", "↑", "End", "☰"), first[0].map { it.label })
        assertEquals(listOf("Ctrl", "Alt", "PgDn", "←", "↓", "→", "⇄"), first[1].map { it.label })
    }

    @Test fun keyTypesAndCodes() {
        assertEquals(ExtraKey("Esc", KeyCode.ESCAPE), ExtraKeysParser.parseKey("esc"))
        assertEquals(ExtraKeyType.MODIFIER, ExtraKeysParser.parseKey("CTRL")?.type)
        assertEquals(ExtraKeyType.REPEATABLE, ExtraKeysParser.parseKey("Left")?.type)
        assertEquals(KeyCode.F1, ExtraKeysParser.parseKey("F1")?.code)
        assertEquals(KeyCode.F12, ExtraKeysParser.parseKey("F12")?.code)
        assertEquals(ExtraKey("|", '|'.code), ExtraKeysParser.parseKey("|"))
        assertNull(ExtraKeysParser.parseKey("NOPE"))
    }

    @Test fun ignoresBlankAndInvalid() {
        val pages = ExtraKeysParser.parse("ESC  NOPE\r\n\n\n  \nTAB\n")
        assertEquals(listOf(listOf(listOf("Esc")), listOf(listOf("Tab"))), pages.map { p -> p.map { r -> r.map { it.label } } })
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class])
class ExtraKeysSettingsTest {
    private val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("extra_keys_test", Context.MODE_PRIVATE)

    @Before fun clear() = prefs.edit { clear() }

    @Test fun defaultIsTerminalOnlyWithTermux() {
        assertTrue(ExtraKeysSettings.isTerminalMode(prefs, "com.termux"))
        assertFalse(ExtraKeysSettings.isTerminalMode(prefs, "com.example.chat"))
        assertFalse(ExtraKeysSettings.isTerminalMode(prefs, null))
        assertTrue(ExtraKeysSettings.showExtraKeys(prefs, true))
        assertFalse(ExtraKeysSettings.showExtraKeys(prefs, false))
    }

    @Test fun modes() {
        prefs.edit { putString(ExtraKeysSettings.PREF_MODE, ExtraKeysSettings.Mode.ALWAYS.name) }
        assertTrue(ExtraKeysSettings.showExtraKeys(prefs, false))
        assertTrue(ExtraKeysSettings.isTerminalMode(prefs, "com.termux"))
        prefs.edit { putString(ExtraKeysSettings.PREF_MODE, ExtraKeysSettings.Mode.OFF.name) }
        assertFalse(ExtraKeysSettings.showExtraKeys(prefs, true))
        assertFalse(ExtraKeysSettings.isTerminalMode(prefs, "com.termux")) // off means no overrides either
    }

    @Test fun toggleAddsAndRemovesApp() {
        ExtraKeysSettings.toggleTerminalApp(prefs, "com.example.ssh")
        assertTrue(ExtraKeysSettings.isTerminalMode(prefs, "com.example.ssh"))
        assertTrue(ExtraKeysSettings.isTerminalMode(prefs, "com.termux")) // defaults kept
        ExtraKeysSettings.toggleTerminalApp(prefs, "com.example.ssh")
        assertFalse(ExtraKeysSettings.isTerminalMode(prefs, "com.example.ssh"))
    }

    @Test fun configFollowsRowsUnlessCustomized() {
        assertEquals(ExtraKeysParser.DEFAULT_CONFIG, ExtraKeysSettings.readConfig(prefs))
        prefs.edit { putInt(ExtraKeysSettings.PREF_ROWS, 1) }
        assertEquals(ExtraKeysParser.DEFAULT_CONFIG_ONE_ROW, ExtraKeysSettings.readConfig(prefs))
        assertTrue(ExtraKeysParser.parse(ExtraKeysParser.DEFAULT_CONFIG_ONE_ROW).all { it.size == 1 })
        prefs.edit { putString(ExtraKeysSettings.PREF_CONFIG, "ESC CTRL") }
        assertEquals("ESC CTRL", ExtraKeysSettings.readConfig(prefs))
        prefs.edit { putString(ExtraKeysSettings.PREF_CONFIG, "NOT_A_KEY") } // nothing valid -> default
        assertEquals(ExtraKeysParser.DEFAULT_CONFIG_ONE_ROW, ExtraKeysSettings.readConfig(prefs))
    }
}
