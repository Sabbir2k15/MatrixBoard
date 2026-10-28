package com.matrixboard.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("matrixboard", Context.MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        fun title(t: String) = TextView(this).apply {
            text = t; textSize = 22f
        }
        root.addView(title("MatrixBoard Settings"))
        root.addView(TextView(this).apply {
            text = "English + Bangla Avro keyboard. Glass UI, Matrix colours, mechanical sounds."
            textSize = 15f
        })
        root.addView(TextView(this).apply { text = "Mechanical sound:"; textSize = 17f })
        val group = RadioGroup(this)
        MechanicalSound.NAMES.forEachIndexed { idx, name ->
            group.addView(RadioButton(this).apply {
                text = name
                isChecked = prefs.getInt("sound_profile", MechanicalSound.CLICKY) == idx
                setOnClickListener {
                    prefs.edit().putInt("sound_profile", idx).apply()
                    MechanicalSound.play(idx)
                }
            })
        }
        root.addView(group)
        val soundSwitch = Switch(this).apply {
            text = "Key sound ON"
            isChecked = prefs.getBoolean("sound_on", true)
            setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("sound_on", v).apply() }
        }
        root.addView(soundSwitch)
        val vibSwitch = Switch(this).apply {
            text = "Vibration ON"
            isChecked = prefs.getBoolean("vibrate_on", true)
            setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("vibrate_on", v).apply() }
        }
        root.addView(vibSwitch)
        root.addView(Button(this).apply {
            text = "Open Keyboard Settings (Enable MatrixBoard)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "Choose Keyboard"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        root.addView(TextView(this).apply {
            text = "Avro examples: ami = আমি, tumi = তুমি, bhalo = ভালো, bangla = বাংলা. Tap EN/BN on the keyboard to switch."
            textSize = 15f
        })
        setContentView(root)
    }
}
