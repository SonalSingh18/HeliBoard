// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.extrakeys

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.TextView
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.KeyboardActionListener
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.settings.Settings

/** Rows of terminal keys (Esc, Tab, Ctrl, Alt, arrows, ...) shown above the main keyboard. */
class ExtraKeysView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var listener: KeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER
    private val handler = Handler(Looper.getMainLooper())
    private var pages: List<ExtraKeysPage> = ExtraKeysParser.parse(ExtraKeysParser.DEFAULT_CONFIG)
    private var pageIndex = 0
    private val modifierViews = mutableListOf<Pair<TextView, StickyModifier>>()
    private var holdingRepeatKey = false
    private val modifierChangeListener = { updateModifierViews() }

    init {
        orientation = VERTICAL
        buildKeys()
    }

    fun setKeyboardActionListener(listener: KeyboardActionListener) {
        this.listener = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ExtraKeysModifiers.onChange = modifierChangeListener
        buildKeys() // settings like colors or height scale may have changed
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRepeat()
        if (ExtraKeysModifiers.onChange === modifierChangeListener) // there may already be a new view
            ExtraKeysModifiers.onChange = null
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != VISIBLE) stopRepeat()
    }

    private fun buildKeys() {
        removeAllViews()
        modifierViews.clear()
        val sv = Settings.getValues()
        val rowHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ROW_HEIGHT_DP, resources.displayMetrics) * sv.mKeyboardHeightScale
        pages.getOrNull(pageIndex)?.forEach { row ->
            val rowLayout = LinearLayout(context)
            rowLayout.orientation = HORIZONTAL
            row.forEach { key -> rowLayout.addView(createKeyView(key), LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)) }
            addView(rowLayout, LayoutParams(LayoutParams.MATCH_PARENT, rowHeight.toInt()))
        }
        updateModifierViews()
    }

    @SuppressLint("ClickableViewAccessibility") // performClick is called on ACTION_UP
    private fun createKeyView(key: ExtraKey): TextView {
        val colors = Settings.getValues().mColors
        val view = TextView(context)
        view.text = key.label
        view.gravity = Gravity.CENTER
        view.maxLines = 1
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.label.length > 2) 13f else 15f)
        view.setTextColor(colors.get(ColorType.KEY_TEXT))
        view.background = keyBackground(ColorType.FUNCTIONAL_KEY_BACKGROUND, pressed = true)
        view.isClickable = true
        when (key.type) {
            ExtraKeyType.MODIFIER -> {
                val modifier = if (key.code == KeyCode.ALT) ExtraKeysModifiers.alt else ExtraKeysModifiers.ctrl
                modifierViews.add(view to modifier)
                view.setOnTouchListener(ModifierTouchListener(modifier))
            }
            else -> view.setOnTouchListener(KeyTouchListener(key))
        }
        return view
    }

    private fun updateModifierViews() {
        val colors = Settings.getValues().mColors
        modifierViews.forEach { (view, modifier) ->
            when (modifier.state) {
                StickyModifier.State.OFF -> {
                    view.background = keyBackground(ColorType.FUNCTIONAL_KEY_BACKGROUND, pressed = true)
                    view.setTextColor(colors.get(ColorType.KEY_TEXT))
                    view.setTypeface(null, Typeface.NORMAL)
                }
                StickyModifier.State.ARMED -> { // outlined in accent color
                    view.background = keyBackground(ColorType.KEY_BACKGROUND, pressed = false, strokeColor = ColorType.ACTION_KEY_BACKGROUND)
                    view.setTextColor(colors.get(ColorType.KEY_TEXT))
                    view.setTypeface(null, Typeface.BOLD)
                }
                StickyModifier.State.LOCKED -> { // filled with accent color
                    view.background = keyBackground(ColorType.ACTION_KEY_BACKGROUND, pressed = false)
                    view.setTextColor(colors.get(ColorType.ACTION_KEY_ICON))
                    view.setTypeface(null, Typeface.BOLD)
                }
            }
        }
    }

    /** normal state is transparent if [pressed], otherwise filled with [color] */
    private fun keyBackground(color: ColorType, pressed: Boolean, strokeColor: ColorType? = null): StateListDrawable {
        val colors = Settings.getValues().mColors
        val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics)
        val inset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
        fun shape(fill: Int?) = GradientDrawable().apply {
            cornerRadius = radius
            setColor(fill ?: 0)
            if (strokeColor != null) setStroke(inset, colors.get(strokeColor))
        }
        val normal = shape(if (pressed) null else colors.get(color))
        val down = shape(colors.get(color)).apply { alpha = if (pressed) 255 else 180 }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), android.graphics.drawable.InsetDrawable(down, inset))
            addState(intArrayOf(), android.graphics.drawable.InsetDrawable(normal, inset))
        }
    }

    private fun sendKey(code: Int, repeatCount: Int) {
        listener.onPressKey(code, repeatCount, 1, HapticEvent.KEY_PRESS)
        listener.onCodeInput(code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, repeatCount > 0)
        listener.onReleaseKey(code, false)
    }

    private fun nextPage(view: View) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, view, HapticEvent.KEY_PRESS)
        if (pages.size < 2) return
        pageIndex = (pageIndex + 1) % pages.size
        post { buildKeys() } // not while handling the touch event of a view that is removed
    }

    private fun stopRepeat() {
        handler.removeCallbacksAndMessages(null)
        if (holdingRepeatKey) {
            holdingRepeatKey = false
            ExtraKeysModifiers.endHold()
        }
    }

    private inner class KeyTouchListener(private val key: ExtraKey) : OnTouchListener {
        private var repeatCount = 0
        private val repeat = object : Runnable {
            override fun run() {
                sendKey(key.code, ++repeatCount)
                handler.postDelayed(this, REPEAT_INTERVAL_MILLIS)
            }
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                    if (key.type == ExtraKeyType.REPEATABLE) {
                        stopRepeat()
                        holdingRepeatKey = true
                        ExtraKeysModifiers.beginHold()
                        repeatCount = 0
                        sendKey(key.code, 0)
                        handler.postDelayed(repeat, REPEAT_START_DELAY_MILLIS)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.isPressed = false
                    when (key.type) {
                        ExtraKeyType.REPEATABLE -> stopRepeat()
                        ExtraKeyType.PAGE -> nextPage(view)
                        else -> sendKey(key.code, 0)
                    }
                    view.performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.isPressed = false
                    if (key.type == ExtraKeyType.REPEATABLE) stopRepeat()
                }
            }
            return true
        }
    }

    private inner class ModifierTouchListener(private val modifier: StickyModifier) : OnTouchListener {
        private var longPressed = false
        private var pressedView: View? = null
        private val longPress = Runnable {
            longPressed = true
            pressedView?.let { AudioAndHapticFeedbackManager.getInstance().performHapticFeedback(it, HapticEvent.KEY_LONG_PRESS) }
            ExtraKeysModifiers.onModifierLongPress(modifier)
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                    pressedView = view
                    longPressed = false
                    AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, view, HapticEvent.KEY_PRESS)
                    handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_UP -> {
                    view.isPressed = false
                    handler.removeCallbacks(longPress)
                    if (!longPressed) ExtraKeysModifiers.onModifierTap(modifier, SystemClock.uptimeMillis())
                    view.performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.isPressed = false
                    handler.removeCallbacks(longPress)
                }
            }
            return true
        }
    }

    companion object {
        private const val ROW_HEIGHT_DP = 36f
        private const val REPEAT_START_DELAY_MILLIS = 400L
        private const val REPEAT_INTERVAL_MILLIS = 50L
    }
}
