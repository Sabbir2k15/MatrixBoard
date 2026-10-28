package com.matrixboard.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class MatrixKeyboardService : InputMethodService() {

    enum class Mode { ENGLISH, BANGLA_AVRO }

    private var mode = Mode.ENGLISH
    private var shift = false
    private var symbols = false
    private val romanBuffer = StringBuilder()
    private var keyboardRoot: LinearLayout? = null
    private var modeButton: Button? = null

    // Matrix palette from the screenshot: yellow / blue / black / white / red
    private val palette = intArrayOf(
        Color.parseColor("#FFC93C"), // yellow
        Color.parseColor("#2F6FED"), // blue
        Color.parseColor("#151515"), // black
        Color.parseColor("#F2F2F2"), // white
        Color.parseColor("#E53935")  // red
    )

    private val letterRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m")
    )
    private val symbolRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")"),
        listOf("!", "?", ".", ",", ":", ";", "'", "\"")
    )

    private fun prefs() = getSharedPreferences("matrixboard", Context.MODE_PRIVATE)
    private fun soundOn() = prefs().getBoolean("sound_on", true)
    private fun soundProfile() = prefs().getInt("sound_profile", MechanicalSound.CLICKY)
    private fun vibrateOn() = prefs().getBoolean("vibrate_on", true)

    private fun clickFeedback() {
        if (soundOn()) MechanicalSound.play(soundProfile())
        if (vibrateOn()) {
            try {
                val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    v?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(15)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Glass: dark translucent panel, like frosted glass over the app behind it.
            setBackgroundColor(Color.parseColor("#CC101418"))
            setPadding(10, 12, 10, 12)
        }
        keyboardRoot = root
        rebuildKeys(root)
        return root
    }

    private fun rebuildKeys(root: LinearLayout) {
        root.removeAllViews()
        var colorIndex = 0
        val rows = if (symbols) symbolRows else letterRows
        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (key in row) {
                val label = if (shift && !symbols) key.uppercase() else key
                val btn = makeKey(label, palette[colorIndex % palette.size], 1f)
                colorIndex++
                btn.setOnClickListener { onKey(label) }
                rowLayout.addView(btn)
            }
            root.addView(rowLayout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 })
        }
        // Bottom control row: EN/BN | ?123/ABC | Shift | Space | Delete | Enter
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        modeButton = makeKey(if (mode == Mode.ENGLISH) "EN" else "BN", Color.parseColor("#00BFA5"), 1f).apply {
            setOnClickListener {
                clickFeedback()
                mode = if (mode == Mode.ENGLISH) Mode.BANGLA_AVRO else Mode.ENGLISH
                romanBuffer.clear()
                currentInputConnection?.finishComposingText()
                text = if (mode == Mode.ENGLISH) "EN" else "BN"
                rebuildKeys(keyboardRoot ?: return@setOnClickListener)
            }
        }
        bottom.addView(modeButton)
        val symBtn = makeKey(if (symbols) "ABC" else "?123", Color.parseColor("#7C4DFF"), 1f).apply {
            setOnClickListener {
                clickFeedback(); symbols = !symbols
                rebuildKeys(keyboardRoot ?: return@setOnClickListener)
            }
        }
        bottom.addView(symBtn)
        val shiftBtn = makeKey(if (shift) "⇧✓" else "⇧", Color.parseColor("#FF6D00"), 1f).apply {
            setOnClickListener {
                clickFeedback(); shift = !shift
                rebuildKeys(keyboardRoot ?: return@setOnClickListener)
            }
        }
        bottom.addView(shiftBtn)
        val spaceBtn = makeKey("Space", Color.parseColor("#37474F"), 3f).apply {
            setOnClickListener { onKey(" ") }
        }
        bottom.addView(spaceBtn)
        val delBtn = makeKey("⌫", Color.parseColor("#E53935"), 1f).apply {
            setOnClickListener { onKey("⌫") }
        }
        bottom.addView(delBtn)
        val enterBtn = makeKey("⏎", Color.parseColor("#00C853"), 1f).apply {
            setOnClickListener { onKey("⏎") }
        }
        bottom.addView(enterBtn)
        root.addView(bottom, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 10 })
    }

    private fun makeKey(label: String, bg: Int, weight: Float): Button {
        return Button(this).apply {
            text = label
            textSize = 18f
            // White keys get dark text, all others get white text.
            setTextColor(if (bg == Color.parseColor("#F2F2F2")) Color.BLACK else Color.WHITE)
            isAllCaps = false
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(bg)
                // Glass border: soft white stroke.
                setStroke(2, Color.parseColor("#55FFFFFF"))
            }
            background = drawable
            // Glass alpha: slightly translucent so the app behind shows through a little.
            alpha = 0.96f
            layoutParams = LinearLayout.LayoutParams(0, 132, weight).apply {
                leftMargin = 5; rightMargin = 5
            }
            setPadding(0, 0, 0, 0)
        }
    }

    private fun onKey(key: String) {
        clickFeedback()
        val ic = currentInputConnection ?: return
        when (key) {
            "⌫" -> {
                if (mode == Mode.BANGLA_AVRO && romanBuffer.isNotEmpty()) {
                    romanBuffer.deleteCharAt(romanBuffer.length - 1)
                    if (romanBuffer.isEmpty()) ic.finishComposingText()
                    else ic.setComposingText(AvroTransliterator.transliterate(romanBuffer.toString()), 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            "⏎" -> {
                commitBanglaWord(ic)
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
            }
            " " -> {
                commitBanglaWord(ic)
                ic.commitText(" ", 1)
            }
            else -> {
                if (symbols) {
                    commitBanglaWord(ic)
                    ic.commitText(key, 1)
                } else if (mode == Mode.BANGLA_AVRO) {
                    // Avro: keep the Roman letters in a buffer, show Bangla as composing text.
                    // Only letters feed the buffer; punctuation commits the word first.
                    if (key.length == 1 && key[0].isLetter()) {
                        romanBuffer.append(key)
                        ic.setComposingText(AvroTransliterator.transliterate(romanBuffer.toString()), 1)
                        // Shift is one-shot in Avro mode for capitals like T/D/S, then off.
                        if (shift) { shift = false; rebuildKeys(keyboardRoot ?: return) }
                    } else {
                        commitBanglaWord(ic); ic.commitText(key, 1)
                    }
                } else {
                    ic.commitText(key, 1)
                    if (shift) { shift = false; rebuildKeys(keyboardRoot ?: return) }
                }
            }
        }
    }

    private fun commitBanglaWord(ic: android.view.inputmethod.InputConnection) {
        if (mode == Mode.BANGLA_AVRO && romanBuffer.isNotEmpty()) {
            ic.commitText(AvroTransliterator.transliterate(romanBuffer.toString()), 1)
            romanBuffer.clear()
        } else {
            ic.finishComposingText()
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        romanBuffer.clear()
    }
}
