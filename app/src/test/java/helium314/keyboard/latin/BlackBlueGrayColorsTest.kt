// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin

import android.graphics.Color
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.blackBlueGrayColors
import helium314.keyboard.latin.common.default
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
class BlackBlueGrayColorsTest {
    private val colors = blackBlueGrayColors(KeyboardTheme.STYLE_MATERIAL, true, null)

    @Test fun everyColorIsSet() {
        ColorType.entries.forEach { assertNotEquals(it.default(), colors.get(it), "$it not set") }
    }

    @Test fun mainColors() {
        assertEquals(Color.BLACK, colors.get(ColorType.MAIN_BACKGROUND))
        assertEquals(Color.parseColor("#202020"), colors.get(ColorType.KEY_BACKGROUND))
        assertEquals(Color.parseColor("#36394D"), colors.get(ColorType.FUNCTIONAL_KEY_BACKGROUND))
        assertEquals(Color.parseColor("#36394D"), colors.get(ColorType.ACTION_KEY_BACKGROUND)) // enter like shift
        assertEquals(Color.WHITE, colors.get(ColorType.KEY_TEXT))
    }
}
