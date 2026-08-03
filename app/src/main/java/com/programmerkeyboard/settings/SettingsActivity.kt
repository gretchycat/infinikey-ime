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
import android.widget.Button
import android.widget.CheckBox
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
import com.programmerkeyboard.model.DimensionValue
import com.programmerkeyboard.model.KeyAction
import com.programmerkeyboard.model.KeyDefinition
import com.programmerkeyboard.model.KeyRow
import com.programmerkeyboard.model.LayoutDefinition

class SettingsActivity : AppCompatActivity() {

    private var isUpdatingHeightFromText = false
    private var isUpdatingTimeoutFromText = false
    private var isUpdatingAutoRepeatFromText = false
    private var isUpdatingAspectRatioFromText = false

    private var editingLayout: LayoutDefinition? = null

    private lateinit var importFileLauncher: ActivityResultLauncher<String>
    private lateinit var exportFileLauncher: ActivityResultLauncher<String>
    private lateinit var importLayoutLauncher: ActivityResultLauncher<String>
    private lateinit var exportLayoutLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)

        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
        val panelEditor = findViewById<View>(R.id.panelEditor)
        val panelLayout = findViewById<View>(R.id.panelLayout)
        val panelBehavior = findViewById<View>(R.id.panelBehavior)
        val panelHaptics = findViewById<View>(R.id.panelHaptics)
        val panelAudio = findViewById<View>(R.id.panelAudio)
        val panelThemes = findViewById<View>(R.id.panelThemes)

        val cardThemeImportExport = findViewById<View>(R.id.cardThemeImportExport)
        val btnGrantOverlayPermission = findViewById<Button>(R.id.btnGrantOverlayPermission)

        if (!com.programmerkeyboard.BuildConfig.DEBUG) {
            cardThemeImportExport?.visibility = View.GONE
            btnGrantOverlayPermission?.visibility = View.GONE
            if (tabLayout.tabCount > 5) {
                tabLayout.removeTabAt(5)
            }
        }

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val position = tab?.position ?: 0
                panelLayout.visibility = if (position == 0) View.VISIBLE else View.GONE
                panelBehavior.visibility = if (position == 1) View.VISIBLE else View.GONE
                panelHaptics.visibility = if (position == 2) View.VISIBLE else View.GONE
                panelAudio.visibility = if (position == 3) View.VISIBLE else View.GONE
                panelThemes.visibility = if (position == 4) View.VISIBLE else View.GONE
                panelEditor.visibility = if (position == 5) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Required Permissions & Setup Buttons
        val btnEnableIme = findViewById<Button>(R.id.btnEnableIme)
        val btnSelectIme = findViewById<Button>(R.id.btnSelectIme)
        val btnGrantMicPermission = findViewById<Button>(R.id.btnGrantMicPermission)

        btnEnableIme?.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        btnSelectIme?.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showInputMethodPicker()
        }

        btnGrantMicPermission?.setOnClickListener {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
            } else {
                Toast.makeText(this, "Microphone permission is already granted!", Toast.LENGTH_SHORT).show()
            }
        }

        btnGrantOverlayPermission?.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Display over apps permission is already granted!", Toast.LENGTH_SHORT).show()
            }
        }

        // 1. Keyboard Height Slider + Editable Text Input (20% - 35%)
        val sbHeight = findViewById<SeekBar>(R.id.sbHeight)
        val etHeightValue = findViewById<EditText>(R.id.etHeightValue)
        val currentHeight = prefs.getInt("pref_keyboard_height_percent", 30).coerceIn(20, 35)

        sbHeight.max = 15
        sbHeight.progress = currentHeight - 20
        etHeightValue.setText("$currentHeight")

        sbHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val heightPct = 20 + progress
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
                    val clamped = inputVal.coerceIn(20, 35)
                    sbHeight.progress = clamped - 20
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
        val formFactorOptions = mutableListOf(
            getString(R.string.setting_docked),
            getString(R.string.setting_split),
            getString(R.string.setting_left_docked),
            getString(R.string.setting_right_docked)
        )
        if (com.programmerkeyboard.BuildConfig.DEBUG) {
            formFactorOptions.add("Floating Window Mode [Debug]")
        }
        val formFactorAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, formFactorOptions)
        spFormFactor.adapter = formFactorAdapter

        val currentFormFactor = prefs.getString("pref_form_factor", "FULL_WIDTH_DOCKED") ?: "FULL_WIDTH_DOCKED"
        val initialFormIdx = when (currentFormFactor) {
            "SPLIT" -> 1
            "LEFT_DOCKED", "SIDE_DOCKED" -> 2
            "RIGHT_DOCKED" -> 3
            "FLOATING" -> if (com.programmerkeyboard.BuildConfig.DEBUG) 4 else 0
            else -> 0
        }
        spFormFactor.setSelection(initialFormIdx)

        spFormFactor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mode = when (position) {
                    1 -> "SPLIT"
                    2 -> "LEFT_DOCKED"
                    3 -> "RIGHT_DOCKED"
                    4 -> if (com.programmerkeyboard.BuildConfig.DEBUG) "FLOATING" else "FULL_WIDTH_DOCKED"
                    else -> "FULL_WIDTH_DOCKED"
                }

                if (mode == "FLOATING") {
                    if (!com.programmerkeyboard.util.OverlayPermissionUtil.hasOverlayPermission(this@SettingsActivity)) {
                        com.programmerkeyboard.util.OverlayPermissionUtil.requestOverlayPermission(this@SettingsActivity)
                    }
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

        val cbMinimalVoiceFeedback = findViewById<android.widget.CheckBox>(R.id.cbMinimalVoiceFeedback)
        cbMinimalVoiceFeedback.isChecked = prefs.getBoolean("pref_minimal_voice_feedback", true)
        cbMinimalVoiceFeedback.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_minimal_voice_feedback", isChecked).apply()
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

        val themePresetFileNames = listOf(
            "theme_system_auto.json",
            "theme_slate.json",
            "theme_cyberpunk.json",
            "theme_oled.json",
            "theme_matrix.json",
            "theme_retro.json",
            "theme_muted_slate.json",
            "theme_custom.json"
        )

        fun getExportedThemeFileName(): String {
            val idx = spThemePreset.selectedItemPosition.coerceIn(0, 7)
            return themePresetFileNames[idx]
        }

        btnExportTheme.setOnClickListener {
            exportFileLauncher.launch(getExportedThemeFileName())
        }

        btnExportTheme.setOnLongClickListener {
            val exportOptions = arrayOf("💾 Save Theme File (Downloads)", "📋 Copy JSON to Clipboard", "📤 Share via App")
            AlertDialog.Builder(this)
                .setTitle("Export Theme Options")
                .setItems(exportOptions) { _, which ->
                    val currentJson = prefs.getString("pref_custom_theme_json", null) ?: getDefaultThemeJson()
                    val prettyJson = formatPrettyJson(currentJson)
                    when (which) {
                        0 -> exportFileLauncher.launch(getExportedThemeFileName())
                        1 -> {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Theme JSON", prettyJson)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this, "Theme JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                        2 -> {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Programmer Keyboard Theme Configuration (${getExportedThemeFileName()})")
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
                                setHintTextColor(android.graphics.Color.parseColor("#64748B"))
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
        val editorKeyboardView = findViewById<com.programmerkeyboard.view.KeyboardView>(R.id.editorKeyboardView)
        editorKeyboardView.isEditorPreviewMode = true

        val btnPreviewFullWidth = findViewById<Button>(R.id.btnPreviewFullWidth)
        val btnPreviewLeftDocked = findViewById<Button>(R.id.btnPreviewLeftDocked)
        val btnPreviewRightDocked = findViewById<Button>(R.id.btnPreviewRightDocked)
        val btnPreviewSplitScreen = findViewById<Button>(R.id.btnPreviewSplitScreen)
        val btnAddRowEditor = findViewById<Button>(R.id.btnAddRowEditor)
        val btnEditorUndo = findViewById<Button>(R.id.btnEditorUndo)
        val btnEditorRedo = findViewById<Button>(R.id.btnEditorRedo)
        val btnEditorSave = findViewById<Button>(R.id.btnEditorSave)

        val undoStack = java.util.ArrayDeque<LayoutDefinition>()
        val redoStack = java.util.ArrayDeque<LayoutDefinition>()

        fun updateUndoRedoButtons() {
            btnEditorUndo.isEnabled = undoStack.isNotEmpty()
            btnEditorUndo.alpha = if (undoStack.isNotEmpty()) 1.0f else 0.4f
            btnEditorRedo.isEnabled = redoStack.isNotEmpty()
            btnEditorRedo.alpha = if (redoStack.isNotEmpty()) 1.0f else 0.4f
        }

        val spEditorLayoutSelector = findViewById<Spinner>(R.id.spEditorLayoutSelector)
        val layoutOptions = listOf(
            "⌨️ Main / Terminal Layout (main.json)",
            "📱 Mobile Layout (mobile.json)",
            "🔢 Mobile Numbers (mobile_number.json)",
            "🔣 Mobile Symbols (mobile_symbol.json)",
            "⚡ Function / Fn Layer (function.json)",
            "✏️ Custom Active Layout"
        )
        val layoutAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, layoutOptions)
        spEditorLayoutSelector.adapter = layoutAdapter

        val activeTarget = prefs.getString("pref_keyboard_layout_target", "main")
        val initialPosition = when (activeTarget) {
            "main" -> 0
            "mobile" -> 1
            "mobile_number" -> 2
            "mobile_symbol" -> 3
            "function" -> 4
            else -> 0
        }
        spEditorLayoutSelector.setSelection(initialPosition)

        spEditorLayoutSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val (targetId, targetFile) = when (position) {
                    0 -> Pair("main", "main.json")
                    1 -> Pair("mobile", "mobile.json")
                    2 -> Pair("mobile_number", "mobile_number.json")
                    3 -> Pair("mobile_symbol", "mobile_symbol.json")
                    4 -> Pair("function", "function.json")
                    else -> Pair("custom", null)
                }

                if (targetId != "custom") {
                    prefs.edit().putString("pref_keyboard_layout_target", targetId).apply()
                }

                val customJson = if (targetId != "custom") {
                    prefs.getString("pref_custom_layout_json_$targetId", null)
                        ?: if (targetId == "main") prefs.getString("pref_custom_layout_json", null) else null
                } else {
                    prefs.getString("pref_custom_layout_json", null)
                }

                val rawLayout = if (!customJson.isNullOrEmpty()) {
                    try { com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(customJson) }
                    catch (_: Exception) { com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, targetFile ?: "main.json") }
                } else {
                    com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, targetFile ?: "main.json")
                }
                editingLayout = com.programmerkeyboard.engine.LayoutParser.applyThemeOverrides(this@SettingsActivity, rawLayout)

                undoStack.clear()
                redoStack.clear()
                updateUndoRedoButtons()
                editingLayout?.let { editorKeyboardView.setLayout(it) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        fun updateFormFactorButtons(selectedMode: com.programmerkeyboard.model.FormFactorMode) {
            val activeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0F766E"))
            val inactiveColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E293B"))
            btnPreviewFullWidth.backgroundTintList = if (selectedMode == com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED) activeColor else inactiveColor
            btnPreviewLeftDocked.backgroundTintList = if (selectedMode == com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED) activeColor else inactiveColor
            btnPreviewRightDocked.backgroundTintList = if (selectedMode == com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED) activeColor else inactiveColor
            btnPreviewSplitScreen.backgroundTintList = if (selectedMode == com.programmerkeyboard.model.FormFactorMode.SPLIT) activeColor else inactiveColor

            editorKeyboardView.keyboardState.formFactorMode = selectedMode
            editorKeyboardView.recalculateKeyBounds()
            editorKeyboardView.requestLayout()
            editorKeyboardView.invalidate()
        }

        btnPreviewFullWidth.setOnClickListener { updateFormFactorButtons(com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED) }
        btnPreviewLeftDocked.setOnClickListener { updateFormFactorButtons(com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED) }
        btnPreviewRightDocked.setOnClickListener { updateFormFactorButtons(com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED) }
        btnPreviewSplitScreen.setOnClickListener { updateFormFactorButtons(com.programmerkeyboard.model.FormFactorMode.SPLIT) }

        editorKeyboardView.onFormFactorModeChangeListener = { mode ->
            updateFormFactorButtons(mode)
        }

        fun pushUndoState() {
            editingLayout?.let { curr ->
                undoStack.push(curr)
                redoStack.clear()
                updateUndoRedoButtons()
            }
        }

        val btnEditRowProperties = findViewById<Button>(R.id.btnEditRowProperties)
        btnEditRowProperties.setOnClickListener {
            showRowEditorDialog(initialRowIdx = 0, pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                editingLayout = updatedLayout
                editorKeyboardView.setLayout(updatedLayout)
            })
        }

        editorKeyboardView.onRowTapForEditingListener = { rowIdx, _ ->
            showRowEditorDialog(initialRowIdx = rowIdx, pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                editingLayout = updatedLayout
                editorKeyboardView.setLayout(updatedLayout)
            })
        }

        editingLayout?.let { editorKeyboardView.setLayout(it) }
        updateUndoRedoButtons()

        btnEditorUndo.setOnClickListener {
            if (undoStack.isNotEmpty()) {
                editingLayout?.let { curr ->
                    redoStack.push(curr)
                }
                editingLayout = undoStack.pop()
                editingLayout?.let { editorKeyboardView.setLayout(it) }
                updateUndoRedoButtons()
            }
        }

        btnEditorRedo.setOnClickListener {
            if (redoStack.isNotEmpty()) {
                editingLayout?.let { curr ->
                    undoStack.push(curr)
                }
                editingLayout = redoStack.pop()
                editingLayout?.let { editorKeyboardView.setLayout(it) }
                updateUndoRedoButtons()
            }
        }

        btnEditorSave.setOnClickListener {
            editingLayout?.let { layout ->
                try {
                    val jsonStr = serializeLayoutToJson(layout)
                    val targetId = layout.id
                    prefs.edit()
                        .putString("pref_custom_layout_json_$targetId", jsonStr)
                        .putString("pref_custom_layout_json", jsonStr)
                        .putString("pref_keyboard_layout_target", targetId)
                        .apply()
                    Toast.makeText(this, "Layout configuration for '${layout.name}' saved & set active!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to save layout: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnAddRowEditor.setOnClickListener {
            pushUndoState()
            val currentRows = editingLayout?.rows?.toMutableList() ?: mutableListOf()
            val newRowId = (currentRows.mapNotNull { (it.id as? Number)?.toInt() }.maxOrNull() ?: 0) + 1
            val newKey = KeyDefinition(
                primaryLabel = "Key",
                widthWeight = DimensionValue.Ratio(1.0f),
                styleName = "alphaKey",
                onPressAction = KeyAction.SendText("Key")
            )
            currentRows.add(KeyRow(id = newRowId, keys = listOf(newKey)))
            editingLayout = editingLayout?.copy(rows = currentRows)
            editingLayout?.let { editorKeyboardView.setLayout(it) }
        }

        editorKeyboardView.onKeyTapForEditingListener = { rIdx, kIdx, key ->
            showKeyEditorDialog(rIdx, kIdx, key, pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                editingLayout = updatedLayout
                editorKeyboardView.setLayout(updatedLayout)
            })
        }

        // Layout Import/Export/Reset Suite
        importLayoutLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { fileUri ->
                try {
                    val jsonStr = contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { it.readText() }
                    if (!jsonStr.isNullOrEmpty()) {
                        val parsed = com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(jsonStr)
                        pushUndoState()
                        editingLayout = parsed
                        editorKeyboardView.setLayout(editingLayout!!)
                        val pretty = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(parsed)
                        prefs.edit().putString("pref_custom_layout_json", pretty).apply()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to parse layout file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        fun getExportedLayoutFileName(): String {
            val layoutId = editingLayout?.id?.lowercase()?.replace("[^a-z0-9_]+".toRegex(), "_")?.trim('_') ?: "main"
            return "infinikey_${layoutId}_layout.json"
        }

        exportLayoutLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            uri?.let { fileUri ->
                try {
                    val layout = editingLayout ?: com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this, "main.json")
                    val prettyJson = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(layout)
                    contentResolver.openOutputStream(fileUri)?.use { out ->
                        out.write(prettyJson.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(this, "Layout configuration exported to file!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to export layout file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        val btnExportLayout = findViewById<Button>(R.id.btnExportLayout)
        val btnImportLayout = findViewById<Button>(R.id.btnImportLayout)
        val btnResetLayout = findViewById<Button>(R.id.btnResetLayout)

        btnExportLayout.setOnClickListener {
            exportLayoutLauncher.launch(getExportedLayoutFileName())
        }

        btnExportLayout.setOnLongClickListener {
            val layout = editingLayout ?: com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this, "main.json")
            val prettyJson = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(layout)
            val exportOptions = arrayOf("💾 Save Layout File (Downloads)", "📋 Copy Layout JSON to Clipboard", "📤 Share via App")
            AlertDialog.Builder(this)
                .setTitle("Export Layout Options")
                .setItems(exportOptions) { _, which ->
                    when (which) {
                        0 -> exportLayoutLauncher.launch(getExportedLayoutFileName())
                        1 -> {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Layout JSON", prettyJson)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this, "Layout JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                        2 -> {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Programmer Keyboard Layout Configuration")
                                putExtra(Intent.EXTRA_TEXT, prettyJson)
                            }
                            startActivity(Intent.createChooser(shareIntent, "Share Layout Configuration"))
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        btnImportLayout.setOnClickListener {
            importLayoutLauncher.launch("*/*")
        }

        btnImportLayout.setOnLongClickListener {
            val importOptions = arrayOf("📂 Choose .json File from Storage", "📋 Paste Layout JSON from Clipboard", "✏️ Edit Layout JSON Text")
            AlertDialog.Builder(this)
                .setTitle("Import Layout Options")
                .setItems(importOptions) { _, which ->
                    when (which) {
                        0 -> importLayoutLauncher.launch("*/*")
                        1 -> {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                            if (!clipText.isNullOrEmpty()) {
                                try {
                                    val parsed = com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(clipText)
                                    pushUndoState()
                                    editingLayout = parsed
                                    editorKeyboardView.setLayout(editingLayout!!)
                                    val pretty = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(parsed)
                                    prefs.edit().putString("pref_custom_layout_json", pretty).apply()
                                    Toast.makeText(this, "Layout JSON pasted & applied!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this, "Clipboard contents are not valid layout JSON!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        2 -> {
                            val layout = editingLayout ?: com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this, "main.json")
                            val prettyJson = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(layout)
                            val inputEditText = EditText(this).apply {
                                hint = "Paste Layout JSON here..."
                                setHintTextColor(android.graphics.Color.parseColor("#64748B"))
                                setPadding(32, 32, 32, 32)
                                textSize = 13f
                                setText(prettyJson)
                            }
                            AlertDialog.Builder(this)
                                .setTitle("Edit Layout JSON")
                                .setView(inputEditText)
                                .setPositiveButton("Apply") { _, _ ->
                                    val text = inputEditText.text.toString().trim()
                                    if (text.isNotEmpty()) {
                                        try {
                                            val parsed = com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(text)
                                            pushUndoState()
                                            editingLayout = parsed
                                            editorKeyboardView.setLayout(editingLayout!!)
                                            val pretty = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(parsed)
                                            prefs.edit().putString("pref_custom_layout_json", pretty).apply()
                                            Toast.makeText(this, "Layout JSON updated & applied!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(this, "Invalid Layout JSON format!", Toast.LENGTH_SHORT).show()
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

        btnResetLayout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Keyboard Layout")
                .setMessage("Reset to default factory Programmer keyboard layout?")
                .setPositiveButton("Reset Layout") { _, _ ->
                    pushUndoState()
                    prefs.edit().remove("pref_custom_layout_json").apply()
                    editingLayout = com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this, "main.json")
                    editorKeyboardView.setLayout(editingLayout!!)
                    Toast.makeText(this, "Layout reset to default!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun getActionTypeIndex(action: KeyAction): Int {
        return when (action) {
            is KeyAction.None -> 0
            is KeyAction.SendText -> 1
            is KeyAction.SendCode -> 2
            is KeyAction.AutoRepeat -> 3
            is KeyAction.ToggleModifier -> 4
            is KeyAction.SwitchLayout -> 5
            is KeyAction.ShowWidget -> 6
            is KeyAction.SetScreenMode -> 7
            is KeyAction.Copy, is KeyAction.Cut, is KeyAction.Paste, is KeyAction.SelectAll -> 8
            else -> 0
        }
    }

    private fun getActionParamString(action: KeyAction): String {
        return when (action) {
            is KeyAction.SendText -> action.text
            is KeyAction.SendCode -> "${action.code}"
            is KeyAction.AutoRepeat -> "${action.code}"
            is KeyAction.ToggleModifier -> action.modifier
            is KeyAction.SwitchLayout -> action.target
            is KeyAction.ShowWidget -> action.widget
            is KeyAction.SetScreenMode -> action.mode
            is KeyAction.Copy -> "COPY"
            is KeyAction.Cut -> "CUT"
            is KeyAction.Paste -> "PASTE"
            is KeyAction.SelectAll -> "SELECT_ALL"
            else -> ""
        }
    }

    private fun parseKeyActionFromInputs(typeIdx: Int, paramStr: String, defaultText: String): KeyAction {
        val param = paramStr.trim()
        return when (typeIdx) {
            0 -> KeyAction.None
            1 -> KeyAction.SendText(param.ifEmpty { defaultText })
            2 -> KeyAction.SendCode(param.toIntOrNull() ?: 66)
            3 -> KeyAction.AutoRepeat(param.toIntOrNull() ?: 67)
            4 -> KeyAction.ToggleModifier(param.uppercase().ifEmpty { "SHIFT" })
            5 -> KeyAction.SwitchLayout(param.ifEmpty { "main" })
            6 -> KeyAction.ShowWidget(param.ifEmpty { "VOICE_INPUT" })
            7 -> KeyAction.SetScreenMode(param.uppercase().ifEmpty { "SPLIT" })
            8 -> when (param.uppercase()) {
                "COPY" -> KeyAction.Copy
                "CUT" -> KeyAction.Cut
                "PASTE" -> KeyAction.Paste
                else -> KeyAction.SelectAll
            }
            else -> KeyAction.SendText(defaultText)
        }
    }

    private fun showKeyEditorDialog(
        rowIdx: Int,
        keyIdx: Int,
        key: KeyDefinition,
        pushUndoState: () -> Unit,
        onUpdate: (LayoutDefinition) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_key, null)
        val etPrimary = view.findViewById<EditText>(R.id.etEditKeyPrimaryLabel)
        val etSecondary = view.findViewById<EditText>(R.id.etEditKeySecondaryLabel)
        val etTopLeft = view.findViewById<EditText>(R.id.etEditKeyTopLeftLabel)
        val etTopRight = view.findViewById<EditText>(R.id.etEditKeyShiftLabel)
        val spCategory = view.findViewById<Spinner>(R.id.spEditKeyCategoryStyle)
        val etWeight = view.findViewById<EditText>(R.id.etEditKeyWidthWeight)

        val spActionType = view.findViewById<Spinner>(R.id.spEditKeyActionType)
        val etActionParam = view.findViewById<EditText>(R.id.etEditKeyActionParam)

        val spLongPressType = view.findViewById<Spinner>(R.id.spEditKeyLongPressType)
        val etLongPressParam = view.findViewById<EditText>(R.id.etEditKeyLongPressParam)

        val spSwipeUpType = view.findViewById<Spinner>(R.id.spEditKeySwipeUpType)
        val etSwipeUpParam = view.findViewById<EditText>(R.id.etEditKeySwipeUpParam)

        val spSwipeDownType = view.findViewById<Spinner>(R.id.spEditKeySwipeDownType)
        val etSwipeDownParam = view.findViewById<EditText>(R.id.etEditKeySwipeDownParam)

        val btnDelete = view.findViewById<Button>(R.id.btnDeleteKey)

        etPrimary.setText(key.primaryLabel)
        etSecondary.setText(key.secondaryLabel ?: "")
        etTopLeft.setText(key.topLeftLabel ?: "")
        etTopRight.setText(key.topRightLabel ?: "")

        val currentWeight = (key.widthWeight as? DimensionValue.Ratio)?.value ?: 1.0f
        etWeight.setText("$currentWeight")

        val availableStyles = mutableListOf("alphaKey", "numberKey", "modifierKey", "functionKey", "actionKey", "navigationKey", "editingKey")
        editingLayout?.styles?.keys?.forEach { s ->
            if (!availableStyles.contains(s)) availableStyles.add(s)
        }
        key.styleName?.let { if (!availableStyles.contains(it)) availableStyles.add(it) }

        val catAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, availableStyles)
        spCategory.adapter = catAdapter
        val currentCatIdx = availableStyles.indexOf(key.styleName).coerceAtLeast(0)
        spCategory.setSelection(currentCatIdx)

        val actionTypes = listOf(
            "None",
            "Send Text",
            "Send Keycode",
            "Auto-Repeat Keycode",
            "Toggle Modifier",
            "Switch Layout",
            "Show Widget",
            "Set Screen Mode",
            "Clipboard (COPY/CUT/PASTE/SELECT_ALL)"
        )
        val actionAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, actionTypes)
        spActionType.adapter = actionAdapter
        spLongPressType.adapter = actionAdapter
        spSwipeUpType.adapter = actionAdapter
        spSwipeDownType.adapter = actionAdapter

        spActionType.setSelection(getActionTypeIndex(key.onPressAction))
        etActionParam.setText(getActionParamString(key.onPressAction))

        spLongPressType.setSelection(getActionTypeIndex(key.onLongPressAction))
        etLongPressParam.setText(getActionParamString(key.onLongPressAction))

        spSwipeUpType.setSelection(getActionTypeIndex(key.onSwipeUpAction))
        etSwipeUpParam.setText(getActionParamString(key.onSwipeUpAction))

        spSwipeDownType.setSelection(getActionTypeIndex(key.onSwipeDownAction))
        etSwipeDownParam.setText(getActionParamString(key.onSwipeDownAction))

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
            .setPositiveButton("Save Key Properties") { _, _ ->
                pushUndoState()
                val newPrimary = etPrimary.text.toString().ifEmpty { "Key" }
                val newSecondary = etSecondary.text.toString().ifEmpty { null }
                val newTopLeft = etTopLeft.text.toString().ifEmpty { null }
                val newTopRight = etTopRight.text.toString().ifEmpty { null }
                val newCat = availableStyles[spCategory.selectedItemPosition.coerceIn(0, availableStyles.size - 1)]
                val newWeightVal = etWeight.text.toString().toFloatOrNull() ?: 1.0f

                val newOnPress = parseKeyActionFromInputs(spActionType.selectedItemPosition, etActionParam.text.toString(), newPrimary)
                val newLongPress = parseKeyActionFromInputs(spLongPressType.selectedItemPosition, etLongPressParam.text.toString(), "")
                val newSwipeUp = parseKeyActionFromInputs(spSwipeUpType.selectedItemPosition, etSwipeUpParam.text.toString(), "")
                val newSwipeDown = parseKeyActionFromInputs(spSwipeDownType.selectedItemPosition, etSwipeDownParam.text.toString(), "")

                editingLayout?.let { layout ->
                    if (rowIdx in layout.rows.indices) {
                        val newRows = layout.rows.toMutableList()
                        val targetKeys = newRows[rowIdx].keys.toMutableList()
                        if (keyIdx in targetKeys.indices) {
                            targetKeys[keyIdx] = targetKeys[keyIdx].copy(
                                primaryLabel = newPrimary,
                                secondaryLabel = newSecondary,
                                topLeftLabel = newTopLeft,
                                topRightLabel = newTopRight,
                                styleName = newCat,
                                widthWeight = DimensionValue.Ratio(newWeightVal),
                                onPressAction = newOnPress,
                                onLongPressAction = newLongPress,
                                onSwipeUpAction = newSwipeUp,
                                onSwipeDownAction = newSwipeDown
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

    private fun showRowEditorDialog(initialRowIdx: Int = 0, pushUndoState: () -> Unit, onUpdate: (LayoutDefinition) -> Unit) {
        val layout = editingLayout ?: return
        val view = layoutInflater.inflate(R.layout.dialog_edit_row, null)
        val spRowSelect = view.findViewById<Spinner>(R.id.spEditRowSelect)
        val cbHidden = view.findViewById<CheckBox>(R.id.cbEditRowHidden)
        val etSplitIndex = view.findViewById<EditText>(R.id.etEditRowSplitIndex)
        val cbSplitKey = view.findViewById<CheckBox>(R.id.cbEditRowSplitKey)
        val btnAddKeyToRow = view.findViewById<Button>(R.id.btnAddKeyToRow)
        val btnDeleteRow = view.findViewById<Button>(R.id.btnDeleteRow)

        val rowOptions = layout.rows.mapIndexed { idx, row ->
            val status = if (row.hidden) " (Hidden)" else ""
            "Row ${idx + 1}$status (ID: ${row.id}, Keys: ${row.keys.size})"
        }
        val rowAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, rowOptions)
        spRowSelect.adapter = rowAdapter

        fun updateRowFields(idx: Int) {
            if (idx in layout.rows.indices) {
                val row = layout.rows[idx]
                cbHidden.isChecked = row.hidden
                etSplitIndex.setText("${row.splitIndex ?: 5}")
                cbSplitKey.isChecked = row.splitKey
            }
        }

        spRowSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                updateRowFields(pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val safeInitialIdx = initialRowIdx.coerceIn(0, maxOf(0, layout.rows.size - 1))
        spRowSelect.setSelection(safeInitialIdx)
        updateRowFields(safeInitialIdx)

        var dialogRef: AlertDialog? = null

        btnAddKeyToRow.setOnClickListener {
            val selectedIdx = spRowSelect.selectedItemPosition
            if (selectedIdx in layout.rows.indices) {
                pushUndoState()
                val newRows = layout.rows.toMutableList()
                val targetRow = newRows[selectedIdx]
                val updatedKeys = targetRow.keys.toMutableList()
                updatedKeys.add(
                    KeyDefinition(
                        primaryLabel = "Key",
                        widthWeight = DimensionValue.Ratio(1.0f),
                        styleName = "alphaKey",
                        onPressAction = KeyAction.SendText("Key")
                    )
                )
                newRows[selectedIdx] = targetRow.copy(keys = updatedKeys)
                onUpdate(layout.copy(rows = newRows))
                dialogRef?.dismiss()
            }
        }

        btnDeleteRow.setOnClickListener {
            val selectedIdx = spRowSelect.selectedItemPosition
            if (selectedIdx in layout.rows.indices && layout.rows.size > 1) {
                AlertDialog.Builder(this)
                    .setTitle("🗑️ Confirm Delete Row")
                    .setMessage("Are you sure you want to delete Row ${selectedIdx + 1}? This will remove all ${layout.rows[selectedIdx].keys.size} keys in this row and cannot be undone.")
                    .setPositiveButton("Delete Row") { _, _ ->
                        pushUndoState()
                        val newRows = layout.rows.toMutableList()
                        newRows.removeAt(selectedIdx)
                        onUpdate(layout.copy(rows = newRows))
                        dialogRef?.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else if (layout.rows.size <= 1) {
                Toast.makeText(this, "Cannot delete the only remaining row!", Toast.LENGTH_SHORT).show()
            }
        }

        dialogRef = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Save Row Settings") { _, _ ->
                val selectedIdx = spRowSelect.selectedItemPosition
                if (selectedIdx in layout.rows.indices) {
                    pushUndoState()
                    val newRows = layout.rows.toMutableList()
                    val targetRow = newRows[selectedIdx]
                    val newHidden = cbHidden.isChecked
                    val newSplitIdx = etSplitIndex.text.toString().toIntOrNull() ?: 5
                    val newSplitKey = cbSplitKey.isChecked
                    newRows[selectedIdx] = targetRow.copy(hidden = newHidden, splitIndex = newSplitIdx, splitKey = newSplitKey)
                    onUpdate(layout.copy(rows = newRows))
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

    private fun serializeAction(action: KeyAction): com.google.gson.JsonElement {
        val obj = com.google.gson.JsonObject()
        when (action) {
            is KeyAction.None -> obj.addProperty("type", "NONE")
            is KeyAction.SendText -> {
                obj.addProperty("type", "SEND_TEXT")
                obj.addProperty("text", action.text)
            }
            is KeyAction.SendCode -> {
                obj.addProperty("type", "SEND_CODE")
                obj.addProperty("code", action.code)
            }
            is KeyAction.AutoRepeat -> {
                obj.addProperty("type", "AUTO_REPEAT")
                obj.addProperty("code", action.code)
            }
            is KeyAction.ToggleModifier -> {
                obj.addProperty("type", "TOGGLE_MODIFIER")
                obj.addProperty("modifier", action.modifier)
            }
            is KeyAction.SwitchLayout -> {
                obj.addProperty("type", "SWITCH_LAYOUT")
                obj.addProperty("target", action.target)
            }
            is KeyAction.ShowWidget -> {
                obj.addProperty("type", "SHOW_WIDGET")
                obj.addProperty("widget", action.widget)
            }
            is KeyAction.SetScreenMode -> {
                obj.addProperty("type", "SET_SCREEN_MODE")
                obj.addProperty("mode", action.mode)
            }
            is KeyAction.Copy -> obj.addProperty("type", "COPY")
            is KeyAction.Cut -> obj.addProperty("type", "CUT")
            is KeyAction.Paste -> obj.addProperty("type", "PASTE")
            is KeyAction.SelectAll -> obj.addProperty("type", "SELECT_ALL")
            is KeyAction.SwitchIme -> obj.addProperty("type", "SWITCH_IME")
            is KeyAction.ToggleRow -> {
                obj.addProperty("type", "TOGGLE_ROW")
                obj.addProperty("rowId", action.rowId.toString())
            }
            is KeyAction.ShowPopup -> {
                obj.addProperty("type", "SHOW_POPUP")
                val arr = com.google.gson.JsonArray()
                action.options.forEach { arr.add(it) }
                obj.add("options", arr)
            }
            else -> obj.addProperty("type", "NONE")
        }
        return obj
    }

    private fun serializeKey(key: KeyDefinition): com.google.gson.JsonObject {
        val obj = com.google.gson.JsonObject()
        obj.addProperty("label", key.primaryLabel)
        key.secondaryLabel?.let { obj.addProperty("secondaryLabel", it) }
        key.topLeftLabel?.let { obj.addProperty("topLeftLabel", it) }
        key.topRightLabel?.let { obj.addProperty("topRightLabel", it) }
        key.styleName?.let { obj.addProperty("style", it) }
        
        when (val w = key.widthWeight) {
            is DimensionValue.Ratio -> obj.addProperty("weight", w.value)
            is DimensionValue.Absolute -> obj.addProperty("weight", "${w.value}dp")
            null -> {}
        }
        when (val h = key.heightRatio) {
            is DimensionValue.Ratio -> obj.addProperty("height", h.value)
            is DimensionValue.Absolute -> obj.addProperty("height", "${h.value}dp")
            null -> {}
        }
        if (key.isSplitKey) obj.addProperty("isSplitKey", true)
        if (key.isFlexible) obj.addProperty("flexible", true)
        if (key.isSpacer) obj.addProperty("spacer", true)

        if (key.onPressAction !is KeyAction.None) obj.add("onPress", serializeAction(key.onPressAction))
        if (key.onLongPressAction !is KeyAction.None) obj.add("onLongPress", serializeAction(key.onLongPressAction))
        if (key.onSwipeUpAction !is KeyAction.None) obj.add("onSwipeUp", serializeAction(key.onSwipeUpAction))
        if (key.onSwipeDownAction !is KeyAction.None) obj.add("onSwipeDown", serializeAction(key.onSwipeDownAction))
        return obj
    }

    private fun serializeRow(row: KeyRow): com.google.gson.JsonObject {
        val obj = com.google.gson.JsonObject()
        val numId = (row.id as? Number)?.toInt()
        if (numId != null) {
            obj.addProperty("id", numId)
        } else {
            val strId = row.id.toString()
            val parsedInt = strId.toIntOrNull()
            if (parsedInt != null) {
                obj.addProperty("id", parsedInt)
            } else {
                obj.addProperty("id", strId)
            }
        }
        if (row.hidden) obj.addProperty("hidden", true)
        row.splitIndex?.let { obj.addProperty("splitIndex", it) }
        if (row.splitKey) obj.addProperty("splitKey", true)
        val keysArr = com.google.gson.JsonArray()
        row.keys.forEach { keysArr.add(serializeKey(it)) }
        obj.add("keys", keysArr)
        return obj
    }

    private fun serializeStyle(style: com.programmerkeyboard.model.KeyStyle): com.google.gson.JsonObject {
        val obj = com.google.gson.JsonObject()
        style.bgColor?.let { obj.addProperty("bgColor", it) }
        style.fgColor?.let { obj.addProperty("fgColor", it) }
        style.pressedBgColor?.let { obj.addProperty("pressedBgColor", it) }
        style.activeBgColor?.let { obj.addProperty("activeBgColor", it) }
        style.secondaryFgColor?.let { obj.addProperty("secondaryFgColor", it) }
        style.borderColor?.let { obj.addProperty("borderColor", it) }
        style.showPreview?.let { obj.addProperty("showPreview", it) }
        return obj
    }

    private fun serializeLayoutToJson(layout: LayoutDefinition): String {
        val root = com.google.gson.JsonObject()
        root.addProperty("id", layout.id)
        root.addProperty("name", layout.name)

        // Metadata
        val metaObj = com.google.gson.JsonObject()
        metaObj.addProperty("defaultScreenMode", layout.metadata.defaultScreenMode)
        metaObj.addProperty("defaultHeightPercentage", layout.metadata.defaultHeightPercentage)
        metaObj.addProperty("showKeyPreview", layout.metadata.showKeyPreview)
        root.add("metadata", metaObj)

        // Theme
        val themeObj = com.google.gson.JsonObject()
        fun formatColor(colorInt: Int?): String? {
            return colorInt?.let { String.format("#%06X", (0xFFFFFF and it)) }
        }
        layout.theme.backgroundColor?.let { themeObj.addProperty("backgroundColor", formatColor(it)) }
        layout.theme.modifierOffDotColor?.let { themeObj.addProperty("modifierOffDotColor", formatColor(it)) }
        layout.theme.modifierLatchedDotColor?.let { themeObj.addProperty("modifierLatchedDotColor", formatColor(it)) }
        layout.theme.modifierLockedDotColor?.let { themeObj.addProperty("modifierLockedDotColor", formatColor(it)) }
        root.add("theme", themeObj)

        // Styles
        val stylesObj = com.google.gson.JsonObject()
        layout.styles.forEach { (styleName, keyStyle) ->
            stylesObj.add(styleName, serializeStyle(keyStyle))
        }
        root.add("styles", stylesObj)

        // Rows
        val rowsArr = com.google.gson.JsonArray()
        layout.rows.forEach { rowsArr.add(serializeRow(it)) }
        root.add("rows", rowsArr)

        return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatusUI()
    }

    private fun updatePermissionStatusUI() {
        val tvStatusEnableIme = findViewById<TextView>(R.id.tvStatusEnableIme) ?: return
        val tvStatusSelectIme = findViewById<TextView>(R.id.tvStatusSelectIme) ?: return
        val tvStatusMicPermission = findViewById<TextView>(R.id.tvStatusMicPermission) ?: return
        val tvStatusOverlayPermission = findViewById<TextView>(R.id.tvStatusOverlayPermission) ?: return

        // 1. IME Enabled Check
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val isEnabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        if (isEnabled) {
            tvStatusEnableIme.text = "✅ Enabled in System Settings"
            tvStatusEnableIme.setTextColor(android.graphics.Color.parseColor("#10B981"))
        } else {
            tvStatusEnableIme.text = "⚠️ Disabled in System Settings"
            tvStatusEnableIme.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
        }

        // 2. IME Selected Check
        val currentIme = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
        val isSelected = currentIme?.contains(packageName) == true
        if (isSelected) {
            tvStatusSelectIme.text = "✅ Selected as Active Keyboard"
            tvStatusSelectIme.setTextColor(android.graphics.Color.parseColor("#10B981"))
        } else {
            tvStatusSelectIme.text = "⚠️ Not Selected as Active Keyboard"
            tvStatusSelectIme.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
        }

        // 3. Mic Permission Check
        val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            tvStatusMicPermission.text = "✅ Granted (Voice Input Ready)"
            tvStatusMicPermission.setTextColor(android.graphics.Color.parseColor("#10B981"))
        } else {
            tvStatusMicPermission.text = "⚠️ Permission Not Granted"
            tvStatusMicPermission.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
        }

        // 4. Overlay Permission Check
        val hasOverlay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else true
        if (hasOverlay) {
            tvStatusOverlayPermission.text = "✅ Granted (Overlay Widgets Enabled)"
            tvStatusOverlayPermission.setTextColor(android.graphics.Color.parseColor("#10B981"))
        } else {
            tvStatusOverlayPermission.text = "⚠️ Permission Not Granted"
            tvStatusOverlayPermission.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
        }
    }
}
