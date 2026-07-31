package com.programmerkeyboard.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.programmerkeyboard.R

class SettingsActivity : AppCompatActivity() {

    private var isUpdatingHeightFromText = false
    private var isUpdatingTimeoutFromText = false
    private var isUpdatingAutoRepeatFromText = false
    private var isUpdatingAspectRatioFromText = false

    private lateinit var importFileLauncher: ActivityResultLauncher<String>
    private lateinit var exportFileLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)

        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
        val panelEditor = findViewById<View>(R.id.panelEditor)
        val panelLayout = findViewById<View>(R.id.panelLayout)
        val panelBehavior = findViewById<View>(R.id.panelBehavior)
        val panelHaptics = findViewById<View>(R.id.panelHaptics)
        val panelThemes = findViewById<View>(R.id.panelThemes)

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val position = tab?.position ?: 0
                panelEditor.visibility = if (position == 0) View.VISIBLE else View.GONE
                panelLayout.visibility = if (position == 1) View.VISIBLE else View.GONE
                panelBehavior.visibility = if (position == 2) View.VISIBLE else View.GONE
                panelHaptics.visibility = if (position == 3) View.VISIBLE else View.GONE
                panelThemes.visibility = if (position == 4) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // 1. Keyboard Height Slider + Editable Text Input (15% - 60%)
        val sbHeight = findViewById<SeekBar>(R.id.sbHeight)
        val etHeightValue = findViewById<EditText>(R.id.etHeightValue)
        val currentHeight = prefs.getInt("pref_keyboard_height_percent", 30).coerceIn(15, 60)

        sbHeight.progress = currentHeight - 15
        etHeightValue.setText("$currentHeight")

        sbHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val heightPct = 15 + progress
                if (fromUser) {
                    isUpdatingHeightFromText = true
                    etHeightValue.setText("$heightPct")
                    isUpdatingHeightFromText = false
                }
                prefs.edit().putInt("pref_keyboard_height_percent", heightPct).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etHeightValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingHeightFromText) return
                val inputStr = s?.toString() ?: ""
                val inputVal = inputStr.toIntOrNull()
                if (inputVal != null) {
                    val clamped = inputVal.coerceIn(15, 60)
                    sbHeight.progress = clamped - 15
                    prefs.edit().putInt("pref_keyboard_height_percent", clamped).apply()
                }
            }
        })

        // 2. Long Press Timeout Slider + Editable Text Input (150 ms - 700 ms)
        val sbLongPress = findViewById<SeekBar>(R.id.sbLongPress)
        val etLongPressValue = findViewById<EditText>(R.id.etLongPressValue)
        val currentTimeout = prefs.getLong("pref_long_press_timeout_ms", 350L).coerceIn(150L, 700L)

        val initialProgress = ((currentTimeout - 150L) / 10L).toInt()
        sbLongPress.progress = initialProgress
        etLongPressValue.setText("$currentTimeout")

        sbLongPress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val timeoutMs = 150L + (progress * 10L)
                if (fromUser) {
                    isUpdatingTimeoutFromText = true
                    etLongPressValue.setText("$timeoutMs")
                    isUpdatingTimeoutFromText = false
                }
                prefs.edit().putLong("pref_long_press_timeout_ms", timeoutMs).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etLongPressValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingTimeoutFromText) return
                val inputStr = s?.toString() ?: ""
                val inputVal = inputStr.toLongOrNull()
                if (inputVal != null) {
                    val clamped = inputVal.coerceIn(100L, 1000L)
                    val prog = ((clamped - 150L) / 10L).toInt().coerceIn(0, 55)
                    sbLongPress.progress = prog
                    prefs.edit().putLong("pref_long_press_timeout_ms", clamped).apply()
                }
            }
        })

        // 3. Auto-Repeat Interval Slider + Editable Text Input (20 ms - 200 ms)
        val sbAutoRepeat = findViewById<SeekBar>(R.id.sbAutoRepeat)
        val etAutoRepeatValue = findViewById<EditText>(R.id.etAutoRepeatValue)
        val currentAutoRepeat = prefs.getLong("pref_auto_repeat_interval_ms", 50L).coerceIn(20L, 200L)

        val initialRepeatProgress = (currentAutoRepeat - 20L).toInt().coerceIn(0, 180)
        sbAutoRepeat.progress = initialRepeatProgress
        etAutoRepeatValue.setText("$currentAutoRepeat")

        sbAutoRepeat.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val intervalMs = 20L + progress
                if (fromUser) {
                    isUpdatingAutoRepeatFromText = true
                    etAutoRepeatValue.setText("$intervalMs")
                    isUpdatingAutoRepeatFromText = false
                }
                prefs.edit().putLong("pref_auto_repeat_interval_ms", intervalMs).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etAutoRepeatValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingAutoRepeatFromText) return
                val inputStr = s?.toString() ?: ""
                val inputVal = inputStr.toLongOrNull()
                if (inputVal != null) {
                    val clamped = inputVal.coerceIn(10L, 500L)
                    val prog = (clamped - 20L).toInt().coerceIn(0, 180)
                    sbAutoRepeat.progress = prog
                    prefs.edit().putLong("pref_auto_repeat_interval_ms", clamped).apply()
                }
            }
        })

        // 4. Aspect Ratio Slider + Editable Text Input (1.5 to 2.5, default 1.75)
        val sbAspectRatio = findViewById<SeekBar>(R.id.sbAspectRatio)
        val etAspectRatioValue = findViewById<EditText>(R.id.etAspectRatioValue)
        val currentRatio = prefs.getFloat("pref_keyboard_aspect_ratio", 1.75f).coerceIn(1.5f, 2.5f)

        val initialRatioProg = ((currentRatio - 1.5f) * 20f).toInt().coerceIn(0, 20)
        sbAspectRatio.progress = initialRatioProg
        etAspectRatioValue.setText(String.format(java.util.Locale.US, "%.2f", currentRatio))

        sbAspectRatio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val ratioVal = 1.5f + (progress / 20f)
                if (fromUser) {
                    isUpdatingAspectRatioFromText = true
                    etAspectRatioValue.setText(String.format(java.util.Locale.US, "%.2f", ratioVal))
                    isUpdatingAspectRatioFromText = false
                }
                prefs.edit().putFloat("pref_keyboard_aspect_ratio", ratioVal).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etAspectRatioValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingAspectRatioFromText) return
                val inputStr = s?.toString() ?: ""
                val inputVal = inputStr.toFloatOrNull()
                if (inputVal != null) {
                    val clamped = inputVal.coerceIn(1.5f, 2.5f)
                    val prog = ((clamped - 1.5f) * 20f).toInt().coerceIn(0, 20)
                    sbAspectRatio.progress = prog
                    prefs.edit().putFloat("pref_keyboard_aspect_ratio", clamped).apply()
                }
            }
        })

        // 4. Form Factor Spinner (Docked, Split, Left-Docked, Right-Docked)
        val spFormFactor = findViewById<Spinner>(R.id.spFormFactor)
        val formFactorOptions = listOf(
            getString(R.string.setting_docked),
            getString(R.string.setting_split),
            getString(R.string.setting_left_docked),
            getString(R.string.setting_right_docked)
        )
        val formFactorAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, formFactorOptions)
        spFormFactor.adapter = formFactorAdapter

        val currentFormFactor = prefs.getString("pref_form_factor", "FULL_WIDTH_DOCKED") ?: "FULL_WIDTH_DOCKED"
        val initialFormIdx = when (currentFormFactor) {
            "SPLIT" -> 1
            "LEFT_DOCKED", "SIDE_DOCKED" -> 2
            "RIGHT_DOCKED" -> 3
            else -> 0
        }
        spFormFactor.setSelection(initialFormIdx)

        spFormFactor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mode = when (position) {
                    1 -> "SPLIT"
                    2 -> "LEFT_DOCKED"
                    3 -> "RIGHT_DOCKED"
                    else -> "FULL_WIDTH_DOCKED"
                }
                prefs.edit().putString("pref_form_factor", mode).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 5. Shift Double-Tap Lock Mode Spinner
        val spShiftLock = findViewById<Spinner>(R.id.spShiftLock)
        val shiftLockOptions = listOf(
            getString(R.string.setting_caps_lock),
            getString(R.string.setting_shift_lock)
        )
        val shiftLockAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, shiftLockOptions)
        spShiftLock.adapter = shiftLockAdapter

        val isShiftLock = prefs.getBoolean("pref_is_shift_lock", false)
        spShiftLock.setSelection(if (isShiftLock) 1 else 0)

        spShiftLock.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isShift = (position == 1)
                prefs.edit().putBoolean("pref_is_shift_lock", isShift).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 6. Haptic Feedback CheckBox
        val cbHapticFeedback = findViewById<android.widget.CheckBox>(R.id.cbHapticFeedback)
        val isHapticEnabled = prefs.getBoolean("pref_haptic_feedback_enabled", true)
        cbHapticFeedback.isChecked = isHapticEnabled
        cbHapticFeedback.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_haptic_feedback_enabled", isChecked).apply()
        }

        // 7. Vibration Style Spinner
        val spVibrationStyle = findViewById<Spinner>(R.id.spVibrationStyle)
        val vibrationStyleOptions = listOf(
            getString(R.string.setting_vib_sharp_click),
            getString(R.string.setting_vib_heavy_click),
            getString(R.string.setting_vib_crisp_tick),
            getString(R.string.setting_vib_double_click),
            getString(R.string.setting_vib_custom_pulse)
        )
        val vibrationStyleAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, vibrationStyleOptions)
        spVibrationStyle.adapter = vibrationStyleAdapter

        val currentVibStyle = prefs.getString("pref_vibration_style", "SHARP_CLICK") ?: "SHARP_CLICK"
        val initialVibStyleIdx = when (currentVibStyle) {
            "HEAVY_CLICK" -> 1
            "CRISP_TICK" -> 2
            "DOUBLE_CLICK" -> 3
            "CUSTOM_PULSE" -> 4
            else -> 0
        }
        spVibrationStyle.setSelection(initialVibStyleIdx)

        spVibrationStyle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val styleKey = when (position) {
                    1 -> "HEAVY_CLICK"
                    2 -> "CRISP_TICK"
                    3 -> "DOUBLE_CLICK"
                    4 -> "CUSTOM_PULSE"
                    else -> "SHARP_CLICK"
                }
                prefs.edit().putString("pref_vibration_style", styleKey).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 7. Vibration Duration Setting (5ms .. 100ms)
        val sbVibrationDuration = findViewById<SeekBar>(R.id.sbVibrationDuration)
        val etVibrationDuration = findViewById<EditText>(R.id.etVibrationDuration)
        val currentDuration = prefs.getLong("pref_vibration_duration_ms", 40L).toInt().coerceIn(5, 100)

        etVibrationDuration.setText(currentDuration.toString())
        sbVibrationDuration.progress = currentDuration - 5

        sbVibrationDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = progress + 5
                    etVibrationDuration.setText(duration.toString())
                    prefs.edit().putLong("pref_vibration_duration_ms", duration.toLong()).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etVibrationDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().toIntOrNull()
                if (input != null) {
                    val clamped = input.coerceIn(5, 100)
                    if (clamped != (sbVibrationDuration.progress + 5)) {
                        sbVibrationDuration.progress = clamped - 5
                    }
                    prefs.edit().putLong("pref_vibration_duration_ms", clamped.toLong()).apply()
                }
            }
        })

        // 8. Vibration Intensity Setting (1% .. 100%)
        val sbVibrationIntensity = findViewById<SeekBar>(R.id.sbVibrationIntensity)
        val etVibrationIntensity = findViewById<EditText>(R.id.etVibrationIntensity)
        val currentIntensity = prefs.getInt("pref_vibration_amplitude", 100).coerceIn(1, 100)

        etVibrationIntensity.setText(currentIntensity.toString())
        sbVibrationIntensity.progress = currentIntensity - 1

        sbVibrationIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val intensity = progress + 1
                    etVibrationIntensity.setText(intensity.toString())
                    prefs.edit().putInt("pref_vibration_amplitude", intensity).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etVibrationIntensity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().toIntOrNull()
                if (input != null) {
                    val clamped = input.coerceIn(1, 100)
                    if (clamped != (sbVibrationIntensity.progress + 1)) {
                        sbVibrationIntensity.progress = clamped - 1
                    }
                    prefs.edit().putInt("pref_vibration_amplitude", clamped).apply()
                }
            }
        })

        // 9. Mechanical Key Click Sound CheckBox & Volume
        val cbKeyClickSound = findViewById<android.widget.CheckBox>(R.id.cbKeyClickSound)
        val isSoundEnabled = prefs.getBoolean("pref_key_click_sound_enabled", true)
        cbKeyClickSound.isChecked = isSoundEnabled
        cbKeyClickSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_key_click_sound_enabled", isChecked).apply()
        }

        // 10. Mechanical Switch Type Spinner
        val spSwitchType = findViewById<Spinner>(R.id.spSwitchType)
        val switchTypeOptions = listOf(
            getString(R.string.setting_switch_rec_blue_pbt),
            getString(R.string.setting_switch_rec_blue_abs),
            getString(R.string.setting_switch_rec_brown_pbt),
            getString(R.string.setting_switch_rec_brown_abs),
            getString(R.string.setting_switch_rec_red_pbt),
            getString(R.string.setting_switch_rec_red_abs),
            getString(R.string.setting_switch_rec_black_pbt),
            getString(R.string.setting_switch_rec_black_abs),
            getString(R.string.setting_switch_rec_nk_cream),
            getString(R.string.setting_switch_rec_eg_oreo),
            getString(R.string.setting_switch_rec_eg_purple),
            getString(R.string.setting_switch_rec_topre),
            getString(R.string.setting_switch_synth_clicky),
            getString(R.string.setting_switch_synth_tactile),
            getString(R.string.setting_switch_synth_linear),
            getString(R.string.setting_switch_synth_thock),
            getString(R.string.setting_switch_synth_spring)
        )
        val switchTypeAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, switchTypeOptions)
        spSwitchType.adapter = switchTypeAdapter

        val currentSwitchType = prefs.getString("pref_switch_type", "REC_EG_PURPLE") ?: "REC_EG_PURPLE"
        val initialSwitchIdx = when (currentSwitchType) {
            "REC_BLUE_PBT" -> 0
            "REC_BLUE_ABS" -> 1
            "REC_BROWN_PBT" -> 2
            "REC_BROWN_ABS" -> 3
            "REC_RED_PBT" -> 4
            "REC_RED_ABS" -> 5
            "REC_BLACK_PBT" -> 6
            "REC_BLACK_ABS" -> 7
            "REC_NK_CREAM" -> 8
            "REC_EG_OREO" -> 9
            "REC_EG_PURPLE" -> 10
            "REC_TOPRE" -> 11
            "SYNTH_CLICKY" -> 12
            "SYNTH_TACTILE" -> 13
            "SYNTH_LINEAR" -> 14
            "SYNTH_THOCK" -> 15
            "SYNTH_SPRING" -> 16
            else -> 10
        }
        spSwitchType.setSelection(initialSwitchIdx)

        spSwitchType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val typeKey = when (position) {
                    0 -> "REC_BLUE_PBT"
                    1 -> "REC_BLUE_ABS"
                    2 -> "REC_BROWN_PBT"
                    3 -> "REC_BROWN_ABS"
                    4 -> "REC_RED_PBT"
                    5 -> "REC_RED_ABS"
                    6 -> "REC_BLACK_PBT"
                    7 -> "REC_BLACK_ABS"
                    8 -> "REC_NK_CREAM"
                    9 -> "REC_EG_OREO"
                    10 -> "REC_EG_PURPLE"
                    11 -> "REC_TOPRE"
                    12 -> "SYNTH_CLICKY"
                    13 -> "SYNTH_TACTILE"
                    14 -> "SYNTH_LINEAR"
                    15 -> "SYNTH_THOCK"
                    16 -> "SYNTH_SPRING"
                    else -> "REC_EG_PURPLE"
                }
                prefs.edit().putString("pref_switch_type", typeKey).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val sbKeyClickVolume = findViewById<SeekBar>(R.id.sbKeyClickVolume)
        val etKeyClickVolume = findViewById<EditText>(R.id.etKeyClickVolume)
        val currentVolume = prefs.getInt("pref_key_click_volume", 80).coerceIn(0, 100)

        etKeyClickVolume.setText(currentVolume.toString())
        sbKeyClickVolume.progress = currentVolume

        sbKeyClickVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    etKeyClickVolume.setText(progress.toString())
                    prefs.edit().putInt("pref_key_click_volume", progress).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etKeyClickVolume.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().toIntOrNull()
                if (input != null) {
                    val clamped = input.coerceIn(0, 100)
                    if (clamped != sbKeyClickVolume.progress) {
                        sbKeyClickVolume.progress = clamped
                    }
                    prefs.edit().putInt("pref_key_click_volume", clamped).apply()
                }
            }
        })

        // 8. Tab 5: Theme Presets & Color Customization
        val spThemePreset = findViewById<Spinner>(R.id.spThemePreset)
        val etBgColorHex = findViewById<EditText>(R.id.etBgColorHex)
        val spKeyCategory = findViewById<Spinner>(R.id.spKeyCategory)
        val etCatBgHex = findViewById<EditText>(R.id.etCatBgHex)
        val etCatFgHex = findViewById<EditText>(R.id.etCatFgHex)
        val etCatPressedBgHex = findViewById<EditText>(R.id.etCatPressedBgHex)

        val btnPickBgColor = findViewById<android.widget.Button>(R.id.btnPickBgColor)
        val btnPickCatBgColor = findViewById<android.widget.Button>(R.id.btnPickCatBgColor)
        val btnPickCatFgColor = findViewById<android.widget.Button>(R.id.btnPickCatFgColor)
        val btnPickCatPressedBgColor = findViewById<android.widget.Button>(R.id.btnPickCatPressedBgColor)

        val btnExportTheme = findViewById<android.widget.Button>(R.id.btnExportTheme)
        val btnImportTheme = findViewById<android.widget.Button>(R.id.btnImportTheme)

        fun updateButtonTint(button: android.widget.Button, hexStr: String) {
            try {
                if (hexStr.startsWith("#") && hexStr.length in 4..9) {
                    button.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(hexStr))
                }
            } catch (_: Exception) {}
        }

        val themePresets = listOf(
            getString(R.string.setting_theme_system_auto),
            getString(R.string.setting_theme_slate),
            getString(R.string.setting_theme_cyberpunk),
            getString(R.string.setting_theme_oled),
            getString(R.string.setting_theme_matrix),
            getString(R.string.setting_theme_retro),
            getString(R.string.setting_theme_muted_slate),
            getString(R.string.setting_theme_custom)
        )
        val themeAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, themePresets)
        spThemePreset.adapter = themeAdapter

        val savedPresetIdx = prefs.getInt("pref_theme_preset_idx", 0).coerceIn(0, 7)
        spThemePreset.setSelection(savedPresetIdx)

        val keyCategories = listOf(
            "🔤 Alpha Keys (alphaKey)",
            "🔢 Number Keys (numberKey)",
            "⚡ Modifier Keys (modifierKey)",
            "🛠️ Function Keys (functionKey)",
            "🎯 Action Keys (actionKey)",
            "🧭 Navigation Keys (navigationKey)",
            "📝 Editing Keys (editingKey)"
        )
        val categoryKeyNames = listOf("alphaKey", "numberKey", "modifierKey", "functionKey", "actionKey", "navigationKey", "editingKey")
        val categoryAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, keyCategories)
        spKeyCategory.adapter = categoryAdapter

        var isInternalUpdating = false

        fun getActiveThemeJson(): String {
            val presetIdx = prefs.getInt("pref_theme_preset_idx", 0).coerceIn(0, 7)
            val presetNames = listOf("system_auto", "slate", "cyberpunk", "oled", "matrix", "retro", "muted_slate")

            if (presetIdx in 0..6) {
                val targetPreset = if (presetIdx == 0) {
                    val isNight = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    if (isNight) "slate" else "system_light"
                } else presetNames[presetIdx]
                try {
                    val jsonStr = assets.open("themes.json").bufferedReader().use { it.readText() }
                    val root = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
                    val themeEntry = root.getAsJsonObject(targetPreset)
                    if (themeEntry != null) {
                        return formatPrettyJson(themeEntry.toString())
                    }
                } catch (_: Exception) {}
            }
            val customJson = prefs.getString("pref_custom_theme_json", null)
            return if (!customJson.isNullOrEmpty()) formatPrettyJson(customJson) else getDefaultThemeJson()
        }

        fun loadCategoryStyleValues(categoryName: String) {
            isInternalUpdating = true
            val currentJson = getActiveThemeJson()
            try {
                val rootObj = com.google.gson.JsonParser.parseString(currentJson).asJsonObject
                val stylesObj = rootObj.getAsJsonObject("styles")
                val catObj = stylesObj?.getAsJsonObject(categoryName)

                val catBg = catObj?.get("bgColor")?.asString ?: "#161923"
                val catFg = catObj?.get("fgColor")?.asString ?: "#F8FAFC"
                val catPressed = catObj?.get("pressedBgColor")?.asString ?: "#252B3C"

                etCatBgHex.setText(catBg)
                etCatFgHex.setText(catFg)
                etCatPressedBgHex.setText(catPressed)

                updateButtonTint(btnPickCatBgColor, catBg)
                updateButtonTint(btnPickCatFgColor, catFg)
                updateButtonTint(btnPickCatPressedBgColor, catPressed)

                val themeObj = rootObj.getAsJsonObject("theme")
                val bgHex = themeObj?.get("backgroundColor")?.asString ?: "#0F172A"
                etBgColorHex.setText(bgHex)
                updateButtonTint(btnPickBgColor, bgHex)
            } catch (_: Exception) {}
            isInternalUpdating = false
        }

        fun saveCategoryStyleValues() {
            if (isInternalUpdating) return
            val categoryIdx = spKeyCategory.selectedItemPosition.coerceIn(0, 6)
            val categoryName = categoryKeyNames[categoryIdx]

            val currentJson = getActiveThemeJson()
            try {
                val rootObj = try {
                    com.google.gson.JsonParser.parseString(currentJson).asJsonObject
                } catch (_: Exception) {
                    com.google.gson.JsonObject()
                }

                val themeObj = rootObj.getAsJsonObject("theme") ?: com.google.gson.JsonObject().also { rootObj.add("theme", it) }
                val bgHex = etBgColorHex.text.toString().trim()
                if (bgHex.startsWith("#") && bgHex.length in 4..9) {
                    themeObj.addProperty("backgroundColor", bgHex)
                    updateButtonTint(btnPickBgColor, bgHex)
                }

                val stylesObj = rootObj.getAsJsonObject("styles") ?: com.google.gson.JsonObject().also { rootObj.add("styles", it) }
                val catObj = stylesObj.getAsJsonObject(categoryName) ?: com.google.gson.JsonObject().also { stylesObj.add(categoryName, it) }

                val catBg = etCatBgHex.text.toString().trim()
                val catFg = etCatFgHex.text.toString().trim()
                val catPressed = etCatPressedBgHex.text.toString().trim()

                if (catBg.startsWith("#") && catBg.length in 4..9) {
                    catObj.addProperty("bgColor", catBg)
                    updateButtonTint(btnPickCatBgColor, catBg)
                }
                if (catFg.startsWith("#") && catFg.length in 4..9) {
                    catObj.addProperty("fgColor", catFg)
                    updateButtonTint(btnPickCatFgColor, catFg)
                }
                if (catPressed.startsWith("#") && catPressed.length in 4..9) {
                    catObj.addProperty("pressedBgColor", catPressed)
                    updateButtonTint(btnPickCatPressedBgColor, catPressed)
                }

                val newJsonStr = rootObj.toString()
                prefs.edit().putString("pref_custom_theme_json", newJsonStr).putInt("pref_theme_preset_idx", 5).apply()
                if (spThemePreset.selectedItemPosition != 5) {
                    spThemePreset.setSelection(5)
                }
            } catch (_: Exception) {}
        }

        fun showColorPickerDialog(targetEditText: EditText, targetButton: android.widget.Button) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
            val vLivePreview = dialogView.findViewById<View>(R.id.vLiveColorPreview)!!
            val tvLiveHex = dialogView.findViewById<TextView>(R.id.tvLiveHexCode)!!
            val tvLiveRgb = dialogView.findViewById<TextView>(R.id.tvLiveRgbCode)!!

            val sbHue = dialogView.findViewById<SeekBar>(R.id.sbHue)!!
            val sbSat = dialogView.findViewById<SeekBar>(R.id.sbSaturation)!!
            val sbVal = dialogView.findViewById<SeekBar>(R.id.sbValue)!!
            val tvHueVal = dialogView.findViewById<TextView>(R.id.tvHueVal)!!
            val tvSatVal = dialogView.findViewById<TextView>(R.id.tvSatVal)!!
            val tvValVal = dialogView.findViewById<TextView>(R.id.tvValVal)!!

            val sbRed = dialogView.findViewById<SeekBar>(R.id.sbRed)!!
            val sbGreen = dialogView.findViewById<SeekBar>(R.id.sbGreen)!!
            val sbBlue = dialogView.findViewById<SeekBar>(R.id.sbBlue)!!
            val tvRedVal = dialogView.findViewById<TextView>(R.id.tvRedVal)!!
            val tvGreenVal = dialogView.findViewById<TextView>(R.id.tvGreenVal)!!
            val tvBlueVal = dialogView.findViewById<TextView>(R.id.tvBlueVal)!!

            val layoutTints = dialogView.findViewById<LinearLayout>(R.id.layoutTintsContainer)!!
            val layoutShades = dialogView.findViewById<LinearLayout>(R.id.layoutShadesContainer)!!
            val layoutHarmonies = dialogView.findViewById<LinearLayout>(R.id.layoutHarmoniesContainer)!!
            val layoutActiveThemeColors = dialogView.findViewById<LinearLayout>(R.id.layoutActiveThemeColorsContainer)!!

            val initialHex = targetEditText.text.toString().trim()
            var currentColorInt = try {
                if (initialHex.startsWith("#") && initialHex.length in 4..9) {
                    android.graphics.Color.parseColor(initialHex)
                } else android.graphics.Color.parseColor("#38BDF8")
            } catch (_: Exception) {
                android.graphics.Color.parseColor("#38BDF8")
            }

            val currentHsv = FloatArray(3)
            android.graphics.Color.colorToHSV(currentColorInt, currentHsv)

            var isUpdatingSeekBars = false

            fun updateColorState(newColorInt: Int) {
                currentColorInt = newColorInt
                android.graphics.Color.colorToHSV(currentColorInt, currentHsv)

                val hexString = String.format("#%06X", (0xFFFFFF and currentColorInt))
                val r = android.graphics.Color.red(currentColorInt)
                val g = android.graphics.Color.green(currentColorInt)
                val b = android.graphics.Color.blue(currentColorInt)

                vLivePreview.setBackgroundColor(currentColorInt)
                tvLiveHex.text = hexString
                tvLiveRgb.text = "RGB: $r, $g, $b"

                tvHueVal.text = "${currentHsv[0].toInt()}°"
                tvSatVal.text = "${(currentHsv[1] * 100).toInt()}%"
                tvValVal.text = "${(currentHsv[2] * 100).toInt()}%"

                tvRedVal.text = "$r"
                tvGreenVal.text = "$g"
                tvBlueVal.text = "$b"

                if (!isUpdatingSeekBars) {
                    isUpdatingSeekBars = true
                    sbHue.progress = currentHsv[0].toInt()
                    sbSat.progress = (currentHsv[1] * 100).toInt()
                    sbVal.progress = (currentHsv[2] * 100).toInt()

                    sbRed.progress = r
                    sbGreen.progress = g
                    sbBlue.progress = b
                    isUpdatingSeekBars = false
                }

                // Render Dynamic Brighter Tints & Pastels Palette
                layoutTints.removeAllViews()
                val tintSatSteps = listOf(0.12f, 0.32f, 0.52f, 0.72f, 0.92f)
                for (s in tintSatSteps) {
                    val tintHsv = floatArrayOf(currentHsv[0], s, 1.0f)
                    val tintColor = android.graphics.Color.HSVToColor(tintHsv)
                    val swatch = View(this).apply {
                        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                        params.setMargins(4, 0, 4, 0)
                        layoutParams = params
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(tintColor)
                            cornerRadius = 8f * resources.displayMetrics.density
                            setStroke((1f * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#475569"))
                        }
                        setOnClickListener { updateColorState(tintColor) }
                    }
                    layoutTints.addView(swatch)
                }

                // Render Dynamic Shades & Tints Palette
                layoutShades.removeAllViews()
                val shadeSteps = listOf(0.95f, 0.75f, 0.55f, 0.35f, 0.20f)
                for (v in shadeSteps) {
                    val shadeHsv = floatArrayOf(currentHsv[0], currentHsv[1], v)
                    val shadeColor = android.graphics.Color.HSVToColor(shadeHsv)
                    val swatch = View(this).apply {
                        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                        params.setMargins(4, 0, 4, 0)
                        layoutParams = params
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(shadeColor)
                            cornerRadius = 8f * resources.displayMetrics.density
                            setStroke((1f * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#475569"))
                        }
                        setOnClickListener { updateColorState(shadeColor) }
                    }
                    layoutShades.addView(swatch)
                }

                // Render Dynamic Harmonies Palette (Complementary, Analogous, Triadic)
                layoutHarmonies.removeAllViews()
                val harmonyHues = listOf(
                    (currentHsv[0] + 180f) % 360f,
                    (currentHsv[0] + 30f) % 360f,
                    (currentHsv[0] + 330f) % 360f,
                    (currentHsv[0] + 120f) % 360f,
                    (currentHsv[0] + 240f) % 360f
                )
                for (h in harmonyHues) {
                    val harmHsv = floatArrayOf(h, currentHsv[1], currentHsv[2])
                    val harmColor = android.graphics.Color.HSVToColor(harmHsv)
                    val swatch = View(this).apply {
                        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                        params.setMargins(4, 0, 4, 0)
                        layoutParams = params
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(harmColor)
                            cornerRadius = 8f * resources.displayMetrics.density
                            setStroke((1f * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#475569"))
                        }
                        setOnClickListener { updateColorState(harmColor) }
                    }
                    layoutHarmonies.addView(swatch)
                }

                // Render Active Theme Palette
                val activeThemeColors = mutableSetOf<Int>()
                val currentThemeJson = prefs.getString("pref_custom_theme_json", null) ?: getDefaultThemeJson()
                try {
                    val rootObj = com.google.gson.JsonParser.parseString(currentThemeJson).asJsonObject
                    val themeObj = rootObj.getAsJsonObject("theme")
                    themeObj?.get("backgroundColor")?.asString?.let {
                        try { activeThemeColors.add(android.graphics.Color.parseColor(it)) } catch (_: Exception) {}
                    }

                    val stylesObj = rootObj.getAsJsonObject("styles")
                    stylesObj?.entrySet()?.forEach { (_, element) ->
                        val styleObj = element.asJsonObject
                        listOf("bgColor", "fgColor", "pressedBgColor", "activeBgColor").forEach { key ->
                            styleObj.get(key)?.asString?.let { hex ->
                                try { activeThemeColors.add(android.graphics.Color.parseColor(hex)) } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (_: Exception) {}

                if (activeThemeColors.isEmpty()) {
                    listOf("#0F172A", "#161923", "#F8FAFC", "#38BDF8", "#22C55E", "#EF4444").forEach { hex ->
                        try { activeThemeColors.add(android.graphics.Color.parseColor(hex)) } catch (_: Exception) {}
                    }
                }

                layoutActiveThemeColors.removeAllViews()
                for (themeColor in activeThemeColors) {
                    val swatch = View(this).apply {
                        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                        params.setMargins(4, 0, 4, 0)
                        layoutParams = params
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(themeColor)
                            cornerRadius = 8f * resources.displayMetrics.density
                            setStroke((1.5f * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#38BDF8"))
                        }
                        setOnClickListener { updateColorState(themeColor) }
                    }
                    layoutActiveThemeColors.addView(swatch)
                }
            }

            updateColorState(currentColorInt)

            val hsvListener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && !isUpdatingSeekBars) {
                        currentHsv[0] = sbHue.progress.toFloat()
                        currentHsv[1] = sbSat.progress / 100f
                        currentHsv[2] = sbVal.progress / 100f
                        updateColorState(android.graphics.Color.HSVToColor(currentHsv))
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
            sbHue.setOnSeekBarChangeListener(hsvListener)
            sbSat.setOnSeekBarChangeListener(hsvListener)
            sbVal.setOnSeekBarChangeListener(hsvListener)

            val rgbListener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && !isUpdatingSeekBars) {
                        val color = android.graphics.Color.rgb(sbRed.progress, sbGreen.progress, sbBlue.progress)
                        updateColorState(color)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
            sbRed.setOnSeekBarChangeListener(rgbListener)
            sbGreen.setOnSeekBarChangeListener(rgbListener)
            sbBlue.setOnSeekBarChangeListener(rgbListener)

            val dialog = AlertDialog.Builder(this)
                .setTitle("🎨 Color Studio & Harmony Chooser")
                .setView(dialogView)
                .setPositiveButton("Select Color") { _, _ ->
                    val hexString = String.format("#%06X", (0xFFFFFF and currentColorInt))
                    targetEditText.setText(hexString)
                    updateButtonTint(targetButton, hexString)
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }

        btnPickBgColor.setOnClickListener { showColorPickerDialog(etBgColorHex, btnPickBgColor) }
        btnPickCatBgColor.setOnClickListener { showColorPickerDialog(etCatBgHex, btnPickCatBgColor) }
        btnPickCatFgColor.setOnClickListener { showColorPickerDialog(etCatFgHex, btnPickCatFgColor) }
        btnPickCatPressedBgColor.setOnClickListener { showColorPickerDialog(etCatPressedBgHex, btnPickCatPressedBgColor) }

        spKeyCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadCategoryStyleValues(categoryKeyNames[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val textStyleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveCategoryStyleValues()
            }
        }

        etBgColorHex.addTextChangedListener(textStyleWatcher)
        etCatBgHex.addTextChangedListener(textStyleWatcher)
        etCatFgHex.addTextChangedListener(textStyleWatcher)
        etCatPressedBgHex.addTextChangedListener(textStyleWatcher)

        loadCategoryStyleValues("alphaKey")

        spThemePreset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("pref_theme_preset_idx", position).apply()
                if (position in 0..6) {
                    prefs.edit().remove("pref_custom_theme_json").apply()
                }
                val catName = categoryKeyNames[spKeyCategory.selectedItemPosition.coerceIn(0, 6)]
                loadCategoryStyleValues(catName)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        importFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { fileUri ->
                try {
                    val jsonStr = contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { it.readText() }
                    if (!jsonStr.isNullOrEmpty()) {
                        com.google.gson.JsonParser.parseString(jsonStr)
                        prefs.edit().putString("pref_custom_theme_json", jsonStr).putInt("pref_theme_preset_idx", 5).apply()
                        spThemePreset.setSelection(5)
                        Toast.makeText(this, "Theme JSON imported from file!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid JSON theme file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        exportFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            uri?.let { fileUri ->
                try {
                    val currentJson = prefs.getString("pref_custom_theme_json", null) ?: getDefaultThemeJson()
                    val prettyJson = formatPrettyJson(currentJson)
                    contentResolver.openOutputStream(fileUri)?.use { out ->
                        out.write(prettyJson.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(this, "Theme saved to JSON file!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to save theme file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnExportTheme.setOnClickListener {
            exportFileLauncher.launch("programmer_keyboard_theme.json")
        }

        btnExportTheme.setOnLongClickListener {
            val exportOptions = arrayOf("💾 Save Theme File (Downloads)", "📋 Copy JSON to Clipboard", "📤 Share via App")
            AlertDialog.Builder(this)
                .setTitle("Export Options")
                .setItems(exportOptions) { _, which ->
                    val currentJson = prefs.getString("pref_custom_theme_json", null) ?: getDefaultThemeJson()
                    val prettyJson = formatPrettyJson(currentJson)
                    when (which) {
                        0 -> exportFileLauncher.launch("programmer_keyboard_theme.json")
                        1 -> {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Theme JSON", prettyJson)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this, "Theme JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                        2 -> {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Programmer Keyboard Theme Configuration")
                                putExtra(Intent.EXTRA_TEXT, prettyJson)
                            }
                            startActivity(Intent.createChooser(shareIntent, "Share Theme Configuration"))
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        btnImportTheme.setOnClickListener {
            importFileLauncher.launch("*/*")
        }

        btnImportTheme.setOnLongClickListener {
            val importOptions = arrayOf("📂 Choose .json File from Storage", "📋 Paste JSON from Clipboard", "✏️ Edit JSON Text")
            AlertDialog.Builder(this)
                .setTitle("Import Options")
                .setItems(importOptions) { _, which ->
                    when (which) {
                        0 -> importFileLauncher.launch("*/*")
                        1 -> {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                            if (!clipText.isNullOrEmpty()) {
                                try {
                                    com.google.gson.JsonParser.parseString(clipText)
                                    prefs.edit().putString("pref_custom_theme_json", clipText).putInt("pref_theme_preset_idx", 5).apply()
                                    spThemePreset.setSelection(5)
                                    val catName = categoryKeyNames[spKeyCategory.selectedItemPosition.coerceIn(0, 4)]
                                    loadCategoryStyleValues(catName)
                                    Toast.makeText(this, "Theme JSON pasted & applied!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this, "Clipboard contents are not valid JSON!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        2 -> {
                            val inputEditText = EditText(this).apply {
                                hint = "Paste Theme JSON here..."
                                setPadding(32, 32, 32, 32)
                                textSize = 13f
                                val currentJson = prefs.getString("pref_custom_theme_json", null) ?: getDefaultThemeJson()
                                setText(formatPrettyJson(currentJson))
                            }
                            AlertDialog.Builder(this)
                                .setTitle("Edit Theme JSON")
                                .setView(inputEditText)
                                .setPositiveButton("Apply") { _, _ ->
                                    val text = inputEditText.text.toString().trim()
                                    if (text.isNotEmpty()) {
                                        try {
                                            com.google.gson.JsonParser.parseString(text)
                                            prefs.edit().putString("pref_custom_theme_json", text).putInt("pref_theme_preset_idx", 5).apply()
                                            spThemePreset.setSelection(5)
                                            val catName = categoryKeyNames[spKeyCategory.selectedItemPosition.coerceIn(0, 6)]
                                            loadCategoryStyleValues(catName)
                                            Toast.makeText(this, "Theme JSON updated & applied!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(this, "Invalid JSON format!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        val btnResetTheme = findViewById<android.widget.Button>(R.id.btnResetTheme)
        btnResetTheme.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🔄 Reset Theme")
                .setMessage("Reset all keyboard background and key category colors to the default Slate Midnight palette?")
                .setPositiveButton("Reset Theme") { _, _ ->
                    prefs.edit()
                        .remove("pref_custom_theme_json")
                        .putInt("pref_theme_preset_idx", 0)
                        .apply()
                    spThemePreset.setSelection(0)
                    val catName = categoryKeyNames[spKeyCategory.selectedItemPosition.coerceIn(0, 6)]
                    loadCategoryStyleValues(catName)
                    Toast.makeText(this, "Theme reset to default Slate Midnight palette!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // --- WYSIWYG LAYOUT EDITOR SETUP ---
        val editorView = findViewById<com.programmerkeyboard.view.InteractiveLayoutEditorView>(R.id.editorView)
        val btnEditorUndo = findViewById<Button>(R.id.btnEditorUndo)
        val btnEditorRedo = findViewById<Button>(R.id.btnEditorRedo)
        val btnEditorSave = findViewById<Button>(R.id.btnEditorSave)

        val undoStack = java.util.ArrayDeque<com.programmerkeyboard.model.LayoutDefinition>()
        val redoStack = java.util.ArrayDeque<com.programmerkeyboard.model.LayoutDefinition>()

        val customLayoutJson = prefs.getString("pref_custom_layout_json", null)
        var editingLayout: com.programmerkeyboard.model.LayoutDefinition? = if (!customLayoutJson.isNullOrEmpty()) {
            try { com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(customLayoutJson) } catch (_: Exception) { com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this, "main.json") }
        } else {
            com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this, "main.json")
        }

        fun updateUndoRedoButtons() {
            btnEditorUndo.isEnabled = undoStack.isNotEmpty()
            btnEditorUndo.alpha = if (undoStack.isNotEmpty()) 1.0f else 0.4f
            btnEditorRedo.isEnabled = redoStack.isNotEmpty()
            btnEditorRedo.alpha = if (redoStack.isNotEmpty()) 1.0f else 0.4f
        }

        fun pushUndoState() {
            editingLayout?.let {
                val gson = com.google.gson.Gson()
                val json = gson.toJson(it)
                val copy = gson.fromJson(json, com.programmerkeyboard.model.LayoutDefinition::class.java)
                undoStack.push(copy)
                redoStack.clear()
                updateUndoRedoButtons()
            }
        }

        editorView.layoutDefinition = editingLayout
        updateUndoRedoButtons()

        btnEditorUndo.setOnClickListener {
            if (undoStack.isNotEmpty()) {
                editingLayout?.let { curr ->
                    val gson = com.google.gson.Gson()
                    redoStack.push(gson.fromJson(gson.toJson(curr), com.programmerkeyboard.model.LayoutDefinition::class.java))
                }
                editingLayout = undoStack.pop()
                editorView.layoutDefinition = editingLayout
                updateUndoRedoButtons()
            }
        }

        btnEditorRedo.setOnClickListener {
            if (redoStack.isNotEmpty()) {
                editingLayout?.let { curr ->
                    val gson = com.google.gson.Gson()
                    undoStack.push(gson.fromJson(gson.toJson(curr), com.programmerkeyboard.model.LayoutDefinition::class.java))
                }
                editingLayout = redoStack.pop()
                editorView.layoutDefinition = editingLayout
                updateUndoRedoButtons()
            }
        }

        btnEditorSave.setOnClickListener {
            editingLayout?.let { layout ->
                try {
                    val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                    val jsonStr = gson.toJson(layout)
                    prefs.edit().putString("pref_custom_layout_json", jsonStr).apply()
                    Toast.makeText(this, "Layout configuration saved successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to save layout!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        editorView.onAddRowListener = {
            pushUndoState()
            val currentRows = editingLayout?.rows?.toMutableList() ?: mutableListOf()
            val newRowId = (currentRows.maxOfOrNull { it.id } ?: 0) + 1
            val newKey = com.programmerkeyboard.model.KeyDefinition(
                primaryLabel = "Key",
                widthWeight = com.programmerkeyboard.model.DimensionValue.Ratio(1.0f),
                styleName = "alphaKey",
                onPressAction = com.programmerkeyboard.model.KeyAction.SendText("Key")
            )
            currentRows.add(com.programmerkeyboard.model.KeyRow(id = newRowId, keys = listOf(newKey)))
            editingLayout = editingLayout?.copy(rows = currentRows)
            editorView.layoutDefinition = editingLayout
        }

        editorView.onAddKeyToRowListener = { rowIdx ->
            pushUndoState()
            editingLayout?.let { layout ->
                if (rowIdx in layout.rows.indices) {
                    val newRows = layout.rows.toMutableList()
                    val targetRow = newRows[rowIdx]
                    val updatedKeys = targetRow.keys.toMutableList()
                    updatedKeys.add(
                        com.programmerkeyboard.model.KeyDefinition(
                            primaryLabel = "Key",
                            widthWeight = com.programmerkeyboard.model.DimensionValue.Ratio(1.0f),
                            styleName = "alphaKey",
                            onPressAction = com.programmerkeyboard.model.KeyAction.SendText("Key")
                        )
                    )
                    newRows[rowIdx] = targetRow.copy(keys = updatedKeys)
                    editingLayout = layout.copy(rows = newRows)
                    editorView.layoutDefinition = editingLayout
                }
            }
        }

        editorView.onKeyReorderedListener = { fromRow, fromKey, toRow, toKey ->
            pushUndoState()
            editingLayout?.let { layout ->
                if (fromRow in layout.rows.indices && toRow in layout.rows.indices) {
                    val newRows = layout.rows.toMutableList()
                    val srcRowKeys = newRows[fromRow].keys.toMutableList()
                    if (fromKey in srcRowKeys.indices) {
                        val keyToMove = srcRowKeys.removeAt(fromKey)
                        newRows[fromRow] = newRows[fromRow].copy(keys = srcRowKeys)

                        val dstRowKeys = newRows[toRow].keys.toMutableList()
                        val clampedDst = toKey.coerceIn(0, dstRowKeys.size)
                        dstRowKeys.add(clampedDst, keyToMove)
                        newRows[toRow] = newRows[toRow].copy(keys = dstRowKeys)

                        editingLayout = layout.copy(rows = newRows)
                        editorView.layoutDefinition = editingLayout
                    }
                }
            }
        }

        editorView.onKeyTappedListener = { rIdx, kIdx, key ->
            showKeyEditorDialog(rIdx, kIdx, key, pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                editingLayout = updatedLayout
                editorView.layoutDefinition = editingLayout
            })
        }
    }

    private fun showKeyEditorDialog(
        rowIdx: Int,
        keyIdx: Int,
        key: com.programmerkeyboard.model.KeyDefinition,
        pushUndoState: () -> Unit,
        onUpdate: (com.programmerkeyboard.model.LayoutDefinition) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_key, null)
        val etPrimary = view.findViewById<EditText>(R.id.etEditKeyPrimaryLabel)
        val etSecondary = view.findViewById<EditText>(R.id.etEditKeySecondaryLabel)
        val spCategory = view.findViewById<Spinner>(R.id.spEditKeyCategoryStyle)
        val etWeight = view.findViewById<EditText>(R.id.etEditKeyWidthWeight)
        val spActionType = view.findViewById<Spinner>(R.id.spEditKeyActionType)
        val etActionParam = view.findViewById<EditText>(R.id.etEditKeyActionParam)
        val btnDelete = view.findViewById<Button>(R.id.btnDeleteKey)

        etPrimary.setText(key.primaryLabel)
        etSecondary.setText(key.secondaryLabel ?: "")

        val currentWeight = (key.widthWeight as? com.programmerkeyboard.model.DimensionValue.Ratio)?.value ?: 1.0f
        etWeight.setText("$currentWeight")

        val categories = listOf("alphaKey", "numberKey", "modifierKey", "functionKey", "actionKey", "navigationKey", "editingKey")
        val catAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = catAdapter
        val currentCatIdx = categories.indexOf(key.styleName).coerceAtLeast(0)
        spCategory.setSelection(currentCatIdx)

        val actionTypes = listOf("Send Text", "Send Code", "Toggle Modifier", "Switch Layout", "Show Widget", "Clipboard")
        val actionAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, actionTypes)
        spActionType.adapter = actionAdapter

        when (val act = key.onPressAction) {
            is com.programmerkeyboard.model.KeyAction.SendText -> {
                spActionType.setSelection(0)
                etActionParam.setText(act.text)
            }
            is com.programmerkeyboard.model.KeyAction.SendCode -> {
                spActionType.setSelection(1)
                etActionParam.setText("${act.code}")
            }
            is com.programmerkeyboard.model.KeyAction.ToggleModifier -> {
                spActionType.setSelection(2)
                etActionParam.setText(act.modifier.name)
            }
            is com.programmerkeyboard.model.KeyAction.SwitchLayout -> {
                spActionType.setSelection(3)
                etActionParam.setText(act.targetLayoutId)
            }
            is com.programmerkeyboard.model.KeyAction.ShowWidget -> {
                spActionType.setSelection(4)
                etActionParam.setText(act.widgetType)
            }
            else -> {
                spActionType.setSelection(0)
                etActionParam.setText(key.primaryLabel)
            }
        }

        var dialog: AlertDialog? = null

        btnDelete.setOnClickListener {
            pushUndoState()
            editingLayout?.let { layout ->
                if (rowIdx in layout.rows.indices) {
                    val newRows = layout.rows.toMutableList()
                    val targetKeys = newRows[rowIdx].keys.toMutableList()
                    if (keyIdx in targetKeys.indices) {
                        targetKeys.removeAt(keyIdx)
                        newRows[rowIdx] = newRows[rowIdx].copy(keys = targetKeys)
                        onUpdate(layout.copy(rows = newRows))
                    }
                }
            }
            dialog?.dismiss()
        }

        dialog = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Save Changes") { _, _ ->
                pushUndoState()
                val newPrimary = etPrimary.text.toString().ifEmpty { "Key" }
                val newSecondary = etSecondary.text.toString().ifEmpty { null }
                val newCat = categories[spCategory.selectedItemPosition.coerceIn(0, categories.size - 1)]
                val newWeightVal = etWeight.text.toString().toFloatOrNull() ?: 1.0f

                val paramStr = etActionParam.text.toString().trim()
                val newAction = when (spActionType.selectedItemPosition) {
                    0 -> com.programmerkeyboard.model.KeyAction.SendText(paramStr.ifEmpty { newPrimary })
                    1 -> com.programmerkeyboard.model.KeyAction.SendCode(paramStr.toIntOrNull() ?: 66)
                    2 -> com.programmerkeyboard.model.KeyAction.ToggleModifier(
                        try { com.programmerkeyboard.model.ModifierType.valueOf(paramStr.uppercase()) } catch (_: Exception) { com.programmerkeyboard.model.ModifierType.SHIFT }
                    )
                    3 -> com.programmerkeyboard.model.KeyAction.SwitchLayout(paramStr.ifEmpty { "main" })
                    4 -> com.programmerkeyboard.model.KeyAction.ShowWidget(paramStr.ifEmpty { "EMOJI_PICKER" })
                    5 -> when (paramStr.uppercase()) {
                        "COPY" -> com.programmerkeyboard.model.KeyAction.Copy
                        "CUT" -> com.programmerkeyboard.model.KeyAction.Cut
                        "PASTE" -> com.programmerkeyboard.model.KeyAction.Paste
                        else -> com.programmerkeyboard.model.KeyAction.SelectAll
                    }
                    else -> com.programmerkeyboard.model.KeyAction.SendText(newPrimary)
                }

                editingLayout?.let { layout ->
                    if (rowIdx in layout.rows.indices) {
                        val newRows = layout.rows.toMutableList()
                        val targetKeys = newRows[rowIdx].keys.toMutableList()
                        if (keyIdx in targetKeys.indices) {
                            targetKeys[keyIdx] = targetKeys[keyIdx].copy(
                                primaryLabel = newPrimary,
                                secondaryLabel = newSecondary,
                                styleName = newCat,
                                widthWeight = com.programmerkeyboard.model.DimensionValue.Ratio(newWeightVal),
                                onPressAction = newAction
                            )
                            newRows[rowIdx] = newRows[rowIdx].copy(keys = targetKeys)
                            onUpdate(layout.copy(rows = newRows))
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatPrettyJson(jsonStr: String): String {
        return try {
            val jsonElement = com.google.gson.JsonParser.parseString(jsonStr)
            com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(jsonElement)
        } catch (e: Exception) {
            jsonStr
        }
    }

    private fun getDefaultThemeJson(): String {
        return """
        {
          "theme": {
            "backgroundColor": "#0F172A",
            "modifierLatchedDotColor": "#38BDF8",
            "modifierLockedDotColor": "#EF4444"
          },
          "styles": {
            "alphaKey": { "bgColor": "#161923", "fgColor": "#F8FAFC", "pressedBgColor": "#252B3C" },
            "numberKey": { "bgColor": "#141722", "fgColor": "#38BDF8", "pressedBgColor": "#222736" },
            "modifierKey": { "bgColor": "#1D212F", "fgColor": "#F59E0B", "activeBgColor": "#1E3A5F" },
            "functionKey": { "bgColor": "#1B2030", "fgColor": "#A855F7", "pressedBgColor": "#2A324B" },
            "actionKey": { "bgColor": "#1E293B", "fgColor": "#38BDF8", "pressedBgColor": "#334155" },
            "navigationKey": { "bgColor": "#1E2638", "fgColor": "#34D399", "pressedBgColor": "#2D374D" },
            "editingKey": { "bgColor": "#221D36", "fgColor": "#F472B6", "pressedBgColor": "#362C54" }
          }
        }
        """.trimIndent()
    }
}
