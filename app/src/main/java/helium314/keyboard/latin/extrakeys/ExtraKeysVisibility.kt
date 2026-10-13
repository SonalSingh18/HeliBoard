// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.extrakeys

/**
 *  In-memory override for showing / hiding the extra keys via the toolbar key, independent of settings.
 *  The override applies to the app where it was toggled, and ends when input moves to another app.
 */
object ExtraKeysVisibility {
    private var hasOverride = false
    private var overridePackage: String? = null
    private var overrideShown = false

    /** whether the extra keys are currently shown, for the toolbar key state */
    var isShown = false
        internal set

    @JvmStatic
    fun resolve(packageName: String?, shownBySettings: Boolean): Boolean {
        if (hasOverride && overridePackage != packageName)
            hasOverride = false
        return if (hasOverride) overrideShown else shownBySettings
    }

    fun toggle(packageName: String?, currentlyShown: Boolean) {
        hasOverride = true
        overridePackage = packageName
        overrideShown = !currentlyShown
    }

    internal fun clear() {
        hasOverride = false
    }
}
