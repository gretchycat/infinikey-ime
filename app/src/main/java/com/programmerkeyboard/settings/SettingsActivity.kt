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
import android.widget.ImageView
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
    private lateinit var browseKeyImageLauncher: ActivityResultLauncher<String>
    private var onKeyIconPickedListener: ((String) -> Unit)? = null

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

        val btnGrantOverlayPermission = findViewById<Button>(R.id.btnGrantOverlayPermission)

        if (!com.programmerkeyboard.BuildConfig.DEBUG) {
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

        // Display Version & Build Info on Tab 1 (Geometry)
        val tvSettingsVersionInfo = findViewById<TextView>(R.id.tvSettingsVersionInfo)
        tvSettingsVersionInfo?.text = "Version ${com.programmerkeyboard.BuildConfig.VERSION_NAME} • Build ${com.programmerkeyboard.BuildConfig.VERSION_CODE}"

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

        // 1a. Portrait Keyboard Height Slider + Editable Text Input (20% - 40%, default 30%)
        val sbHeightPortrait = findViewById<SeekBar>(R.id.sbHeightPortrait)
        val etHeightValuePortrait = findViewById<EditText>(R.id.etHeightValuePortrait)
        val currentHeightPortrait = prefs.getInt("pref_keyboard_height_percent_portrait", prefs.getInt("pref_keyboard_height_percent", 30)).coerceIn(20, 40)

        sbHeightPortrait?.max = 20
        sbHeightPortrait?.progress = (currentHeightPortrait - 20).coerceIn(0, 20)
        etHeightValuePortrait?.setText("$currentHeightPortrait")

        sbHeightPortrait?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val heightPct = 20 + progress
                if (fromUser) {
                    isUpdatingHeightFromText = true
                    etHeightValuePortrait?.setText("$heightPct")
                    isUpdatingHeightFromText = false
                }
                prefs.edit().putInt("pref_keyboard_height_percent_portrait", heightPct).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etHeightValuePortrait?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingHeightFromText) return
                val inputStr = s?.toString() ?: ""
                val inputVal = inputStr.toIntOrNull()
                if (inputVal != null) {
                    val clamped = inputVal.coerceIn(20, 40)
                    sbHeightPortrait?.progress = clamped - 20
                    prefs.edit().putInt("pref_keyboard_height_percent_portrait", clamped).apply()
                }
            }
        })

        // 1b. Landscape Keyboard Height Slider + Editable Text Input (25% - 65%, default 45%)
        val sbHeightLandscape = findViewById<SeekBar>(R.id.sbHeightLandscape)
        val etHeightValueLandscape = findViewById<EditText>(R.id.etHeightValueLandscape)
        val currentHeightLandscape = prefs.getInt("pref_keyboard_height_percent_landscape", 45).coerceIn(25, 65)

        sbHeightLandscape?.max = 40
        sbHeightLandscape?.progress = (currentHeightLandscape - 25).coerceIn(0, 40)
        etHeightValueLandscape?.setText("$currentHeightLandscape")

        sbHeightLandscape?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val heightPct = 25 + progress
                if (fromUser) {
                    isUpdatingHeightFromText = true
                    etHeightValueLandscape?.setText("$heightPct")
                    isUpdatingHeightFromText = false
                }
                prefs.edit().putInt("pref_keyboard_height_percent_landscape", heightPct).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etHeightValueLandscape?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingHeightFromText) return
                val inputStr = s?.toString() ?: ""
                val inputVal = inputStr.toIntOrNull()
                if (inputVal != null) {
                    val clamped = inputVal.coerceIn(25, 65)
                    sbHeightLandscape?.progress = clamped - 25
                    prefs.edit().putInt("pref_keyboard_height_percent_landscape", clamped).apply()
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

        // 2b. Key Tap Debounce Filter Spinner (Disabled, 15ms, 25ms, 35ms Default, 50ms, 70ms)
        val spKeyDebounce = findViewById<Spinner>(R.id.spKeyDebounce)
        val debounceOptions = arrayOf("Disabled (0ms)", "15 ms", "25 ms", "35 ms (Default)", "50 ms", "70 ms")
        val debounceValues = intArrayOf(0, 15, 25, 35, 50, 70)

        val debounceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, debounceOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spKeyDebounce?.adapter = debounceAdapter

        val savedDebounceMs = prefs.getInt("pref_key_debounce_ms", 35)
        val selectedDebounceIdx = debounceValues.indexOf(savedDebounceMs).let { if (it >= 0) it else 2 }
        spKeyDebounce?.setSelection(selectedDebounceIdx)

        spKeyDebounce?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in debounceValues.indices) {
                    val ms = debounceValues[position]
                    prefs.edit().putInt("pref_key_debounce_ms", ms).apply()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

        // 4a. Portrait Key Aspect Ratio Slider (1.2 to 2.5, default 1.75)
        val sbAspectRatioPortrait = findViewById<SeekBar>(R.id.sbAspectRatioPortrait)
        val etAspectRatioValuePortrait = findViewById<EditText>(R.id.etAspectRatioValuePortrait)
        val currentRatioPortrait = prefs.getFloat("pref_keyboard_aspect_ratio_portrait", prefs.getFloat("pref_keyboard_aspect_ratio", 1.75f)).coerceIn(1.2f, 2.5f)

        val initialRatioProgPortrait = ((currentRatioPortrait - 1.2f) * 20f).toInt().coerceIn(0, 20)
        sbAspectRatioPortrait?.max = 20
        sbAspectRatioPortrait?.progress = initialRatioProgPortrait
        etAspectRatioValuePortrait?.setText(String.format(java.util.Locale.US, "%.2f", currentRatioPortrait))

        sbAspectRatioPortrait?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val ratioVal = 1.2f + (progress / 20f)
                if (fromUser) {
                    isUpdatingAspectRatioFromText = true
                    etAspectRatioValuePortrait?.setText(String.format(java.util.Locale.US, "%.2f", ratioVal))
                    isUpdatingAspectRatioFromText = false
                }
                prefs.edit().putFloat("pref_keyboard_aspect_ratio_portrait", ratioVal).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etAspectRatioValuePortrait?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingAspectRatioFromText) return
                val inputStr = s?.toString() ?: ""
                val inputVal = inputStr.toFloatOrNull()
                if (inputVal != null) {
                    val clamped = inputVal.coerceIn(1.2f, 2.5f)
                    val prog = ((clamped - 1.2f) * 20f).toInt().coerceIn(0, 20)
                    sbAspectRatioPortrait?.progress = prog
                    prefs.edit().putFloat("pref_keyboard_aspect_ratio_portrait", clamped).apply()
                }
            }
        })

        // 4b. Landscape Key Aspect Ratio Slider (1.8 to 4.5, default 3.00)
        val sbAspectRatioLandscape = findViewById<SeekBar>(R.id.sbAspectRatioLandscape)
        val etAspectRatioValueLandscape = findViewById<EditText>(R.id.etAspectRatioValueLandscape)
        val currentRatioLandscape = prefs.getFloat("pref_keyboard_aspect_ratio_landscape", 3.00f).coerceIn(1.8f, 4.5f)

        val initialRatioProgLandscape = ((currentRatioLandscape - 1.8f) * 11.11f).toInt().coerceIn(0, 30)
        sbAspectRatioLandscape?.max = 30
        sbAspectRatioLandscape?.progress = initialRatioProgLandscape
        etAspectRatioValueLandscape?.setText(String.format(java.util.Locale.US, "%.2f", currentRatioLandscape))

        sbAspectRatioLandscape?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val ratioVal = 1.8f + (progress / 11.11f)
                if (fromUser) {
                    isUpdatingAspectRatioFromText = true
                    etAspectRatioValueLandscape?.setText(String.format(java.util.Locale.US, "%.2f", ratioVal))
                    isUpdatingAspectRatioFromText = false
                }
                prefs.edit().putFloat("pref_keyboard_aspect_ratio_landscape", ratioVal).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etAspectRatioValueLandscape?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingAspectRatioFromText) return
                val inputStr = s?.toString() ?: ""
                val inputVal = inputStr.toFloatOrNull()
                if (inputVal != null) {
                    val clamped = inputVal.coerceIn(1.8f, 4.5f)
                    val prog = ((clamped - 1.8f) * 11.11f).toInt().coerceIn(0, 30)
                    sbAspectRatioLandscape?.progress = prog
                    prefs.edit().putFloat("pref_keyboard_aspect_ratio_landscape", clamped).apply()
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

        // 6. Default Layout for Unseen Apps Spinner
        val spDefaultUnseenLayout = findViewById<Spinner>(R.id.spDefaultUnseenLayout)
        val unseenLayoutOptions = listOf(
            getString(R.string.setting_layout_main),
            getString(R.string.setting_layout_mobile),
            getString(R.string.setting_layout_mobile_number),
            getString(R.string.setting_layout_mobile_symbol),
            getString(R.string.setting_layout_function)
        )
        val unseenLayoutAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, unseenLayoutOptions)
        spDefaultUnseenLayout.adapter = unseenLayoutAdapter

        val currentDefaultUnseen = prefs.getString("pref_default_unseen_layout", "mobile") ?: "mobile"
        val initialUnseenPos = when (currentDefaultUnseen) {
            "main" -> 0
            "mobile" -> 1
            "mobile_number" -> 2
            "mobile_symbol" -> 3
            "function" -> 4
            else -> 1
        }
        spDefaultUnseenLayout.setSelection(initialUnseenPos)

        spDefaultUnseenLayout.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val layoutKey = when (position) {
                    0 -> "main"
                    1 -> "mobile"
                    2 -> "position2"
                    3 -> "position3"
                    4 -> "position4"
                    else -> "mobile"
                }
                val actualLayout = when (layoutKey) {
                    "main" -> "main"
                    "mobile" -> "mobile"
                    "position2" -> "mobile_number"
                    "position3" -> "mobile_symbol"
                    "position4" -> "function"
                    else -> "mobile"
                }
                prefs.edit().putString("pref_default_unseen_layout", actualLayout).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val cbMinimalVoiceFeedback = findViewById<android.widget.CheckBox>(R.id.cbMinimalVoiceFeedback)
        cbMinimalVoiceFeedback.isChecked = prefs.getBoolean("pref_minimal_voice_feedback", true)
        cbMinimalVoiceFeedback.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_minimal_voice_feedback", isChecked).apply()
        }

        val cbEnableSpacebarTrackpad = findViewById<android.widget.CheckBox>(R.id.cbEnableSpacebarTrackpad)
        cbEnableSpacebarTrackpad.isChecked = prefs.getBoolean("pref_enable_spacebar_trackpad", true)
        cbEnableSpacebarTrackpad.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_enable_spacebar_trackpad", isChecked).apply()
        }

        val cbEnableArrowTrackpad = findViewById<android.widget.CheckBox>(R.id.cbEnableArrowTrackpad)
        cbEnableArrowTrackpad.isChecked = prefs.getBoolean("pref_enable_arrow_trackpad", false)
        cbEnableArrowTrackpad.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_enable_arrow_trackpad", isChecked).apply()
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

        val btnLaunchThemesFolder = findViewById<android.widget.Button>(R.id.btnLaunchThemesFolder)

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
                prefs.edit().putString("pref_custom_theme_json", newJsonStr).putInt("pref_theme_preset_idx", 7).apply()
                if (spThemePreset.selectedItemPosition != 7) {
                    spThemePreset.setSelection(7)
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
                        prefs.edit().putString("pref_custom_theme_json", jsonStr).putInt("pref_theme_preset_idx", 7).apply()
                        spThemePreset.setSelection(7)
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

        fun getUserThemesDir(): java.io.File {
            val dir = java.io.File(getExternalFilesDir(null), "themes")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

        btnLaunchThemesFolder.setOnClickListener {
            val dir = getUserThemesDir()
            var launched = false
            try {
                val folderUri = android.provider.DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents",
                    "primary:Android/data/${packageName}/files/themes"
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(folderUri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                launched = true
            } catch (_: Exception) {}

            if (!launched) {
                try {
                    val appFilesUri = android.provider.DocumentsContract.buildDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:Android/data/${packageName}/files"
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(appFilesUri, "vnd.android.document/directory")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    launched = true
                } catch (_: Exception) {}
            }

            if (!launched) {
                Toast.makeText(this, "Themes stored at: ${dir.absolutePath}", Toast.LENGTH_LONG).show()
            }
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

        fun getUserLayoutsDir(): java.io.File {
            val dir = java.io.File(getExternalFilesDir(null), "layouts")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

        fun syncDefaultLayoutsToFolder() {
            com.programmerkeyboard.util.IconRenderer.getUserIconsDir(this)
            com.programmerkeyboard.engine.LayoutParser.syncAndUpgradeDefaultLayouts(this)
        }

        syncDefaultLayoutsToFolder()

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
        var hasUnsavedChanges = false

        fun updateSaveButtonState() {
            val isDirty = undoStack.isNotEmpty() || hasUnsavedChanges
            if (isDirty) {
                btnEditorSave.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F59E0B"))
                btnEditorSave.setTextColor(android.graphics.Color.parseColor("#000000"))
                btnEditorSave.text = "💾 Save"
            } else {
                btnEditorSave.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E293B"))
                btnEditorSave.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                btnEditorSave.text = "💾 Save"
            }
        }

        fun updateUndoRedoButtons() {
            btnEditorUndo.isEnabled = undoStack.isNotEmpty()
            btnEditorUndo.alpha = if (undoStack.isNotEmpty()) 1.0f else 0.4f
            btnEditorRedo.isEnabled = redoStack.isNotEmpty()
            btnEditorRedo.alpha = if (redoStack.isNotEmpty()) 1.0f else 0.4f
            updateSaveButtonState()
        }

        val spEditorLayoutSelector = findViewById<Spinner>(R.id.spEditorLayoutSelector)
        
        val defaultAssetFiles = listOf("main.json", "mobile.json", "mobile_number.json", "mobile_symbol.json", "function.json", "phone.json")
        val generatedAssetFiles = listOf("emoji.json", "emoji_animals.json", "emoji_body.json", "emoji_flags.json", "emoji_food.json", "emoji_objects.json", "emoji_sports.json", "emoji_symbols.json", "emoji_travel.json")

        data class LayoutSelectorEntry(
            val baseDisplayName: String,
            val targetId: String,
            val assetFileName: String?,
            var version: String = "1.0",
            var isEdited: Boolean = false,
            val categoryType: Int = 1,
            val isActionItem: Boolean = false
        ) {
            fun getFullFormattedTitle(): String {
                if (isActionItem) return baseDisplayName
                val categoryIcon = when (categoryType) {
                    1 -> "⭐ "
                    2 -> "👤 "
                    3 -> "⚡ "
                    else -> ""
                }
                val statusTag = if (isEdited) "[Edited]" else "[Default]"
                return "$categoryIcon$baseDisplayName (v$version) $statusTag"
            }
        }

        fun getDisplayNameForAsset(fileName: String): String {
            return when (fileName) {
                "main.json" -> "⌨️ Main / Terminal Layout"
                "mobile.json" -> "📱 Mobile Layout"
                "mobile_number.json" -> "🔢 Mobile Numbers"
                "mobile_symbol.json" -> "🔣 Mobile Symbols"
                "function.json" -> "⚡ Function / Fn Layer"
                "phone.json" -> "📞 Phone Dialpad"
                "emoji.json" -> "😃 Emojis"
                "emoji_animals.json" -> "🐾 Emoji Animals"
                "emoji_body.json" -> "🙋 Emoji Body & People"
                "emoji_flags.json" -> "🚩 Emoji Flags"
                "emoji_food.json" -> "🍔 Emoji Food"
                "emoji_objects.json" -> "💡 Emoji Objects"
                "emoji_sports.json" -> "⚽ Emoji Sports"
                "emoji_symbols.json" -> "🔣 Emoji Symbols"
                "emoji_travel.json" -> "✈️ Emoji Travel"
                else -> "📄 ${fileName.removeSuffix(".json").replace('_', ' ').capitalize()}"
            }
        }

        val btnResetLayout = findViewById<Button>(R.id.btnResetLayout)

        fun updateResetButtonState(entry: LayoutSelectorEntry?) {
            val isUserCreated = entry?.categoryType == 2
            if (isUserCreated) {
                btnResetLayout.isEnabled = true
                btnResetLayout.alpha = 1.0f
                btnResetLayout.text = "🗑️ Delete Custom Layout"
                btnResetLayout.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#991B1B"))
                btnResetLayout.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            } else {
                btnResetLayout.isEnabled = true
                btnResetLayout.alpha = 1.0f
                btnResetLayout.text = "🔄 Reset to Default Layout"
                btnResetLayout.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E293B"))
                btnResetLayout.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            }
        }

        fun loadLayoutEntries(): MutableList<LayoutSelectorEntry> {
            com.programmerkeyboard.engine.LayoutParser.syncAndUpgradeDefaultLayouts(this@SettingsActivity)
            val entries = mutableListOf<LayoutSelectorEntry>()
            val layoutsDir = getUserLayoutsDir()

            // 1. Default Layouts
            for (file in defaultAssetFiles) {
                val targetId = file.removeSuffix(".json")
                val customFile = java.io.File(layoutsDir, file)
                val customJsonPref = prefs.getString("pref_custom_layout_json_$targetId", null)
                    ?: if (targetId == "main") prefs.getString("pref_custom_layout_json", null) else null

                val defaultAssetJson = try { assets.open("layouts/$file").bufferedReader().use { it.readText() }.trim() } catch (_: Exception) { "" }
                val currentFileJson = if (customFile.exists()) customFile.readText().trim() else ""
                val isEdited = prefs.getBoolean("pref_layout_is_edited_$targetId", false) || (!customJsonPref.isNullOrEmpty() && currentFileJson != defaultAssetJson)

                val loaded = try {
                    if (currentFileJson.isNotEmpty()) {
                        com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(currentFileJson)
                    } else if (!customJsonPref.isNullOrEmpty()) {
                        com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(customJsonPref)
                    } else {
                        com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, file)
                    }
                } catch (_: Exception) {
                    com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, file)
                }
                val baseName = getDisplayNameForAsset(file)
                entries.add(LayoutSelectorEntry(baseName, targetId, file, loaded.version, isEdited, categoryType = 1))
            }

            // 2. User-Created / Custom Layouts
            val userFiles = try {
                layoutsDir.listFiles { _, name ->
                    name.endsWith(".json") && name !in defaultAssetFiles && name !in generatedAssetFiles
                }?.sortedBy { it.name } ?: emptyList<java.io.File>()
            } catch (_: Exception) { emptyList<java.io.File>() }

            for (userFile in userFiles) {
                val file = userFile.name
                val targetId = file.removeSuffix(".json")
                val customJsonPref = prefs.getString("pref_custom_layout_json_$targetId", null)
                val currentFileJson = try { userFile.readText().trim() } catch (_: Exception) { "" }
                val isEdited = prefs.getBoolean("pref_layout_is_edited_$targetId", true)

                val loaded = try {
                    if (currentFileJson.isNotEmpty()) {
                        com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(currentFileJson)
                    } else if (!customJsonPref.isNullOrEmpty()) {
                        com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(customJsonPref)
                    } else {
                        com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, file)
                    }
                } catch (_: Exception) {
                    com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, file)
                }
                val baseName = if (loaded.name.isNotEmpty()) loaded.name else file.removeSuffix(".json").replace('_', ' ')
                entries.add(LayoutSelectorEntry(baseName, targetId, file, loaded.version, isEdited = isEdited, categoryType = 2))
            }

            // 3. Generated Layouts
            for (file in generatedAssetFiles) {
                val targetId = file.removeSuffix(".json")
                val customFile = java.io.File(layoutsDir, file)
                val customJsonPref = prefs.getString("pref_custom_layout_json_$targetId", null)
                val defaultAssetJson = try { assets.open("layouts/$file").bufferedReader().use { it.readText() }.trim() } catch (_: Exception) { "" }
                val currentFileJson = if (customFile.exists()) customFile.readText().trim() else ""
                val isEdited = prefs.getBoolean("pref_layout_is_edited_$targetId", false) || (!customJsonPref.isNullOrEmpty() && currentFileJson != defaultAssetJson)

                val loaded = try {
                    if (currentFileJson.isNotEmpty()) {
                        com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(currentFileJson)
                    } else if (!customJsonPref.isNullOrEmpty()) {
                        com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(customJsonPref)
                    } else {
                        com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, file)
                    }
                } catch (_: Exception) {
                    com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, file)
                }
                val baseName = getDisplayNameForAsset(file)
                entries.add(LayoutSelectorEntry(baseName, targetId, file, loaded.version, isEdited, categoryType = 3))
            }

            // 4. Create New Empty Layout Option
            entries.add(LayoutSelectorEntry("➕ Create New Empty Layout...", "create_new_action", null, categoryType = 4, isActionItem = true))

            return entries
        }

        val layoutEntries = loadLayoutEntries()

        fun updateLayoutSpinner() {
            val titles = layoutEntries.map { it.getFullFormattedTitle() }
            val layoutAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, titles)
            spEditorLayoutSelector.adapter = layoutAdapter
        }

        fun showCreateEmptyLayoutDialog() {
            val input = EditText(this).apply {
                hint = "Layout Name (e.g. Custom Coding Keyboard)"
                setSingleLine()
            }
            val container = android.widget.FrameLayout(this).apply {
                val p = (16 * resources.displayMetrics.density).toInt()
                setPadding(p, p / 2, p, p / 2)
                addView(input)
            }

            AlertDialog.Builder(this)
                .setTitle("Create New Custom Layout")
                .setMessage("Enter a name for your new custom keyboard layout:")
                .setView(container)
                .setPositiveButton("Create") { _, _ ->
                    val layoutName = input.text.toString().trim().ifEmpty { "Custom Layout" }
                    val rawSlug = layoutName.lowercase().replace(Regex("[^a-z0-9_]"), "_").trim('_')
                    val baseTargetId = if (rawSlug.isEmpty()) "custom_layout" else rawSlug

                    var targetId = baseTargetId
                    var targetFile = java.io.File(getUserLayoutsDir(), "${targetId}.json")
                    var count = 2
                    while (targetFile.exists()) {
                        targetId = "${baseTargetId}_$count"
                        targetFile = java.io.File(getUserLayoutsDir(), "${targetId}.json")
                        count++
                    }

                    val newLayout = com.programmerkeyboard.model.LayoutDefinition(
                        id = targetId,
                        name = layoutName,
                        version = "0.1.28",
                        author = "User Custom",
                        description = "Custom layout created by user.",
                        metadata = com.programmerkeyboard.model.LayoutMetadata(
                            horizontalSpacing = com.programmerkeyboard.model.DimensionValue.Absolute(4),
                            verticalSpacing = com.programmerkeyboard.model.DimensionValue.Absolute(4),
                            defaultScreenMode = "FULL_WIDTH_DOCKED",
                            defaultHeightPercentage = 30,
                            showKeyPreview = true
                        ),
                        rows = listOf(
                            com.programmerkeyboard.model.KeyRow(
                                id = 1,
                                keys = listOf(
                                    com.programmerkeyboard.model.KeyDefinition(
                                        primaryLabel = "Key 1",
                                        widthWeight = com.programmerkeyboard.model.DimensionValue.Ratio(1.0f),
                                        styleName = "alphaKey",
                                        onPressAction = com.programmerkeyboard.model.KeyAction.SendText("Key 1")
                                    )
                                )
                            ),
                            com.programmerkeyboard.model.KeyRow(
                                id = 2,
                                keys = listOf(
                                    com.programmerkeyboard.model.KeyDefinition(
                                        primaryLabel = "⌨ Main",
                                        widthWeight = com.programmerkeyboard.model.DimensionValue.Ratio(1.5f),
                                        styleName = "modifierKey",
                                        onPressAction = com.programmerkeyboard.model.KeyAction.SwitchLayout("main")
                                    ),
                                    com.programmerkeyboard.model.KeyDefinition(
                                        primaryLabel = "␣",
                                        widthWeight = com.programmerkeyboard.model.DimensionValue.Ratio(3.0f),
                                        styleName = "alphaKey",
                                        onPressAction = com.programmerkeyboard.model.KeyAction.SendText(" ")
                                    ),
                                    com.programmerkeyboard.model.KeyDefinition(
                                        primaryLabel = "⌫",
                                        widthWeight = com.programmerkeyboard.model.DimensionValue.Ratio(1.5f),
                                        styleName = "actionKey",
                                        onPressAction = com.programmerkeyboard.model.KeyAction.SendCode(android.view.KeyEvent.KEYCODE_DEL)
                                    )
                                )
                            )
                        )
                    )

                    try {
                        val jsonStr = serializeLayoutToJson(newLayout)
                        targetFile.writeText(jsonStr)

                        prefs.edit()
                            .putString("pref_custom_layout_json_$targetId", jsonStr)
                            .putBoolean("pref_layout_is_edited_$targetId", true)
                            .putString("pref_keyboard_layout_target", targetId)
                            .apply()

                        val newEntries = loadLayoutEntries()
                        layoutEntries.clear()
                        layoutEntries.addAll(newEntries)
                        updateLayoutSpinner()

                        val newPos = layoutEntries.indexOfFirst { it.targetId == targetId }
                        if (newPos >= 0) {
                            spEditorLayoutSelector.setSelection(newPos)
                        }

                        editingLayout = newLayout
                        editorKeyboardView.setLayout(newLayout)
                        undoStack.clear()
                        redoStack.clear()
                        hasUnsavedChanges = true
                        updateUndoRedoButtons()

                        Toast.makeText(this, "Created new custom layout '$layoutName'!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to create layout: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        updateLayoutSpinner()

        val activeTarget = prefs.getString("pref_keyboard_layout_target", "main")
        var initialPosition = layoutEntries.indexOfFirst { it.targetId == activeTarget }
        if (initialPosition < 0) initialPosition = 0
        spEditorLayoutSelector.setSelection(initialPosition)
        if (initialPosition in layoutEntries.indices) {
            updateResetButtonState(layoutEntries[initialPosition])
        }

        var lastSelectedPos = initialPosition
        spEditorLayoutSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position !in layoutEntries.indices) return
                val entry = layoutEntries[position]
                if (entry.isActionItem) {
                    spEditorLayoutSelector.setSelection(lastSelectedPos)
                    showCreateEmptyLayoutDialog()
                    return
                }
                lastSelectedPos = position
                com.programmerkeyboard.engine.LayoutParser.syncAndUpgradeDefaultLayouts(this@SettingsActivity)
                val targetId = entry.targetId
                val targetFile = entry.assetFileName

                if (targetId != "custom") {
                    prefs.edit().putString("pref_keyboard_layout_target", targetId).apply()
                }

                val customFile = java.io.File(getUserLayoutsDir(), targetFile ?: "${targetId}.json")
                val customJsonPref = if (targetId != "custom") {
                    prefs.getString("pref_custom_layout_json_$targetId", null)
                        ?: if (targetId == "main") prefs.getString("pref_custom_layout_json", null) else null
                } else {
                    prefs.getString("pref_custom_layout_json", null)
                }

                val rawLayout = try {
                    if (customFile.exists()) {
                        com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(customFile.readText())
                    } else if (!customJsonPref.isNullOrEmpty()) {
                        com.programmerkeyboard.engine.LayoutParser.parseJsonLayoutDescriptor(customJsonPref)
                    } else {
                        com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, targetFile ?: "${targetId}.json")
                    }
                } catch (_: Exception) {
                    com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this@SettingsActivity, targetFile ?: "${targetId}.json")
                }
                editingLayout = com.programmerkeyboard.engine.LayoutParser.applyThemeOverrides(this@SettingsActivity, rawLayout)

                undoStack.clear()
                redoStack.clear()
                hasUnsavedChanges = false
                updateUndoRedoButtons()
                updateResetButtonState(entry)
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
                hasUnsavedChanges = true
                updateUndoRedoButtons()
            }
        }

        val btnEditRowProperties = findViewById<Button>(R.id.btnEditRowProperties)
        btnEditRowProperties.setOnClickListener {
            showRowEditorDialog(initialRowIdx = 0, pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                editingLayout = updatedLayout
                editorKeyboardView.setLayout(updatedLayout)
                hasUnsavedChanges = true
                updateSaveButtonState()
            })
        }

        editorKeyboardView.onRowTapForEditingListener = { rowIdx, _ ->
            showRowEditorDialog(initialRowIdx = rowIdx, pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                editingLayout = updatedLayout
                editorKeyboardView.setLayout(updatedLayout)
                hasUnsavedChanges = true
                updateSaveButtonState()
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
                    val dir = getUserLayoutsDir()
                    val targetFile = java.io.File(dir, "${targetId}.json")
                    targetFile.writeText(jsonStr)

                    val editor = prefs.edit()
                        .putString("pref_custom_layout_json_$targetId", jsonStr)
                        .putString("pref_keyboard_layout_target", targetId)
                        .putBoolean("pref_layout_is_edited_$targetId", true)
                    if (targetId == "main") {
                        editor.putString("pref_custom_layout_json", jsonStr)
                    } else {
                        editor.remove("pref_custom_layout_json")
                    }
                    editor.apply()
                    hasUnsavedChanges = false
                    undoStack.clear()
                    redoStack.clear()
                    updateUndoRedoButtons()
                    val pos = spEditorLayoutSelector.selectedItemPosition
                    if (pos in layoutEntries.indices) {
                        layoutEntries[pos].isEdited = true
                        updateLayoutSpinner()
                        spEditorLayoutSelector.setSelection(pos)
                        updateResetButtonState(layoutEntries[pos])
                    }
                    Toast.makeText(this, "Layout configuration for '${layout.name}' saved to ${targetFile.name} & active!", Toast.LENGTH_SHORT).show()
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

        val btnEditSpacing = findViewById<Button>(R.id.btnEditSpacing)
        btnEditSpacing.setOnClickListener {
            showSpacingEditorDialog(pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                editingLayout = updatedLayout
                hasUnsavedChanges = true
                updateUndoRedoButtons()
                editorKeyboardView.setLayout(updatedLayout)
            })
        }

        editorKeyboardView.onSpacingTapForEditingListener = {
            showSpacingEditorDialog(pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                editingLayout = updatedLayout
                hasUnsavedChanges = true
                updateUndoRedoButtons()
                editorKeyboardView.setLayout(updatedLayout)
            })
        }

        editorKeyboardView.onKeyTapForEditingListener = { rIdx, kIdx, key ->
            if (key.isSpacer) {
                showPhantomSpacerEditorDialog(rIdx, kIdx, key, pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                    editingLayout = updatedLayout
                    hasUnsavedChanges = true
                    updateUndoRedoButtons()
                    editorKeyboardView.setLayout(updatedLayout)
                })
            } else {
                showKeyEditorDialog(rIdx, kIdx, key, pushUndoState = { pushUndoState() }, onUpdate = { updatedLayout ->
                    editingLayout = updatedLayout
                    hasUnsavedChanges = true
                    updateUndoRedoButtons()
                    editorKeyboardView.setLayout(updatedLayout)
                })
            }
        }

        browseKeyImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val savedPath = com.programmerkeyboard.util.IconRenderer.saveUserIcon(this, it)
                onKeyIconPickedListener?.invoke(savedPath ?: it.toString())
            }
        }

        val btnLaunchFilesApp = findViewById<Button>(R.id.btnLaunchFilesApp)
        btnLaunchFilesApp.setOnClickListener {
            syncDefaultLayoutsToFolder()
            val dir = getUserLayoutsDir()

            var launched = false

            // 1. Try launching directly into Android/data/com.programmerkeyboard/files/layouts folder using SAF Document URI
            try {
                val layoutsFolderUri = android.provider.DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents",
                    "primary:Android/data/${packageName}/files/layouts"
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(layoutsFolderUri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                launched = true
            } catch (_: Exception) {}

            if (!launched) {
                // 2. Try launching into Android/data/com.programmerkeyboard/files
                try {
                    val appFilesUri = android.provider.DocumentsContract.buildDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:Android/data/${packageName}/files"
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(appFilesUri, "vnd.android.document/directory")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    launched = true
                } catch (_: Exception) {}
            }

            if (!launched) {
                // 3. Fallback: Launch default system Files app package
                try {
                    val pm = packageManager
                    val filesIntent = pm.getLaunchIntentForPackage("com.google.android.documentsui")
                        ?: pm.getLaunchIntentForPackage("com.android.documentsui")
                    if (filesIntent != null) {
                        filesIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(filesIntent)
                        launched = true
                    }
                } catch (_: Exception) {}
            }

            if (!launched) {
                Toast.makeText(this, "Layouts stored at: ${dir.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }

        btnResetLayout.setOnClickListener {
            val pos = spEditorLayoutSelector.selectedItemPosition
            if (pos !in layoutEntries.indices) return@setOnClickListener
            val currentEntry = layoutEntries[pos]

            if (currentEntry.categoryType == 2) {
                val targetId = currentEntry.targetId
                val layoutName = currentEntry.baseDisplayName
                AlertDialog.Builder(this)
                    .setTitle("Delete Custom Layout")
                    .setMessage("Are you sure you want to delete custom layout '$layoutName'? This action cannot be undone.")
                    .setPositiveButton("Delete Layout") { _, _ ->
                        try {
                            val targetFile = java.io.File(getUserLayoutsDir(), currentEntry.assetFileName ?: "${targetId}.json")
                            if (targetFile.exists()) targetFile.delete()

                            prefs.edit()
                                .remove("pref_custom_layout_json_$targetId")
                                .remove("pref_layout_is_edited_$targetId")
                                .putString("pref_keyboard_layout_target", "main")
                                .apply()

                            val newEntries = loadLayoutEntries()
                            layoutEntries.clear()
                            layoutEntries.addAll(newEntries)
                            updateLayoutSpinner()

                            val mainPos = layoutEntries.indexOfFirst { it.targetId == "main" }.coerceAtLeast(0)
                            spEditorLayoutSelector.setSelection(mainPos)

                            Toast.makeText(this, "Custom layout '$layoutName' deleted!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "Failed to delete layout: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                val activeTarget = currentEntry.targetId
                val targetFile = currentEntry.assetFileName ?: "${activeTarget}.json"
                AlertDialog.Builder(this)
                    .setTitle("Reset Keyboard Layout")
                    .setMessage("Reset layout '$activeTarget' to static factory default?")
                    .setPositiveButton("Reset Layout") { _, _ ->
                        pushUndoState()
                        prefs.edit()
                            .remove("pref_custom_layout_json_$activeTarget")
                            .putBoolean("pref_layout_is_edited_$activeTarget", false)
                            .apply()
                        try {
                            val dir = getUserLayoutsDir()
                            val customFile = java.io.File(dir, targetFile)
                            assets.open("layouts/$targetFile").use { input ->
                                customFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        } catch (_: Exception) {}

                        editingLayout = com.programmerkeyboard.engine.LayoutParser.loadLayoutFromAsset(this, targetFile)
                        editorKeyboardView.setLayout(editingLayout!!)
                        hasUnsavedChanges = true
                        updateUndoRedoButtons()
                        val newPos = spEditorLayoutSelector.selectedItemPosition
                        if (newPos in layoutEntries.indices) {
                            layoutEntries[newPos].isEdited = false
                            updateLayoutSpinner()
                            spEditorLayoutSelector.setSelection(newPos)
                            updateResetButtonState(layoutEntries[newPos])
                        }
                        Toast.makeText(this, "Layout '$activeTarget' reset to factory default!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
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
            is KeyAction.Copy, is KeyAction.Cut, is KeyAction.Paste, is KeyAction.PasteEcho, is KeyAction.SelectAll -> 8
            is KeyAction.LaunchApp -> 9
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
            is KeyAction.PasteEcho -> "PASTE_ECHO"
            is KeyAction.SelectAll -> "SELECT_ALL"
            is KeyAction.LaunchApp -> action.packageName
            else -> ""
        }
    }

    private fun parseKeyActionFromInputs(typeIdx: Int, paramStr: String, defaultText: String): KeyAction {
        val trimmed = paramStr.trim()
        return when (typeIdx) {
            0 -> KeyAction.None
            1 -> {
                val text = if (paramStr.isNotEmpty()) paramStr else defaultText
                KeyAction.SendText(text)
            }
            2 -> KeyAction.SendCode(trimmed.toIntOrNull() ?: 66)
            3 -> KeyAction.AutoRepeat(trimmed.toIntOrNull() ?: 67)
            4 -> KeyAction.ToggleModifier(trimmed.uppercase().ifEmpty { "SHIFT" })
            5 -> KeyAction.SwitchLayout(trimmed.ifEmpty { "main" })
            6 -> KeyAction.ShowWidget(trimmed.ifEmpty { "VOICE_INPUT" })
            7 -> KeyAction.SetScreenMode(trimmed.uppercase().ifEmpty { "SPLIT" })
            8 -> when (trimmed.uppercase()) {
                "COPY" -> KeyAction.Copy
                "CUT" -> KeyAction.Cut
                "PASTE" -> KeyAction.Paste
                "PASTE_ECHO", "ECHO_CLIPBOARD" -> KeyAction.PasteEcho
                else -> KeyAction.SelectAll
            }
            9 -> KeyAction.LaunchApp(trimmed)
            else -> KeyAction.SendText(if (paramStr.isNotEmpty()) paramStr else defaultText)
        }
    }
    private data class InstalledAppInfo(
        val label: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable?
    )

    private fun getInstalledLaunchableApps(): List<InstalledAppInfo> {
        val pm = packageManager
        val appMap = mutableMapOf<String, InstalledAppInfo>()

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launcherInfos = try {
            pm.queryIntentActivities(mainIntent, 0)
        } catch (_: Exception) { emptyList() }

        for (info in launcherInfos) {
            val label = info.loadLabel(pm).toString()
            val pkg = info.activityInfo.packageName
            val icon = try { info.loadIcon(pm) } catch (_: Exception) { null }
            appMap[pkg] = InstalledAppInfo(label, pkg, icon)
        }

        val installedApps = try {
            pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        } catch (_: Exception) { emptyList() }

        for (app in installedApps) {
            val pkg = app.packageName
            if (!appMap.containsKey(pkg)) {
                val launchIntent = try { pm.getLaunchIntentForPackage(pkg) } catch (_: Exception) { null }
                if (launchIntent != null) {
                    val label = try { app.loadLabel(pm).toString() } catch (_: Exception) { pkg }
                    val icon = try { app.loadIcon(pm) } catch (_: Exception) { null }
                    appMap[pkg] = InstalledAppInfo(label, pkg, icon)
                }
            }
        }

        return appMap.values.sortedBy { it.label.lowercase() }
    }

    private fun showAppPickerDialog(onSelected: (packageName: String, label: String) -> Unit) {
        val allApps = getInstalledLaunchableApps()
        var filteredApps = allApps.toList()

        val searchInput = EditText(this).apply {
            hint = "🔍 Search installed apps..."
            setSingleLine()
            setBackgroundColor(android.graphics.Color.parseColor("#1E293B"))
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#94A3B8"))
            val p = (12 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }

        val listView = android.widget.ListView(this).apply {
            divider = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#334155"))
            dividerHeight = 1
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            val searchParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * resources.displayMetrics.density).toInt()
            }
            addView(searchInput, searchParams)
            val listParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                (350 * resources.displayMetrics.density).toInt()
            )
            addView(listView, listParams)
        }

        lateinit var dialog: AlertDialog

        fun createAdapter(items: List<InstalledAppInfo>): ArrayAdapter<InstalledAppInfo> {
            return object : ArrayAdapter<InstalledAppInfo>(this@SettingsActivity, 0, items) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val view = convertView ?: layoutInflater.inflate(android.R.layout.activity_list_item, parent, false)
                    val item = getItem(position) ?: return view
                    val iconView = view.findViewById<android.widget.ImageView>(android.R.id.icon)
                    val textView = view.findViewById<TextView>(android.R.id.text1)

                    iconView?.setImageDrawable(item.icon)
                    textView?.text = "${item.label}\n${item.packageName}"
                    textView?.setTextColor(android.graphics.Color.WHITE)
                    textView?.textSize = 14f
                    return view
                }
            }
        }

        listView.adapter = createAdapter(filteredApps)

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase()?.trim() ?: ""
                filteredApps = if (query.isEmpty()) {
                    allApps
                } else {
                    allApps.filter { it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
                }
                listView.adapter = createAdapter(filteredApps)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position in filteredApps.indices) {
                val selected = filteredApps[position]
                onSelected(selected.packageName, selected.label)
                dialog.dismiss()
            }
        }

        dialog = AlertDialog.Builder(this)
            .setTitle("Select App to Launch")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
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

        val spActionParamSelect = view.findViewById<Spinner>(R.id.spEditKeyActionParamSelect)
        val spLongPressParamSelect = view.findViewById<Spinner>(R.id.spEditKeyLongPressParamSelect)
        val spSwipeUpParamSelect = view.findViewById<Spinner>(R.id.spEditKeySwipeUpParamSelect)
        val spSwipeDownParamSelect = view.findViewById<Spinner>(R.id.spEditKeySwipeDownParamSelect)

        val btnDelete = view.findViewById<Button>(R.id.btnDeleteKey)
        val cbIsSpacer = view.findViewById<CheckBox>(R.id.cbEditKeyIsSpacer)

        etPrimary.setText(key.primaryLabel)
        etSecondary.setText(key.secondaryLabel ?: "")
        etTopLeft.setText(key.topLeftLabel ?: "")
        etTopRight.setText(key.topRightLabel ?: "")

        val currentWeight = (key.widthWeight as? DimensionValue.Ratio)?.value ?: 1.0f
        etWeight.setText("$currentWeight")
        cbIsSpacer.isChecked = key.isSpacer

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
            "Clipboard (COPY/CUT/PASTE/SELECT_ALL)",
            "Launch App"
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

        fun setupActionParamSelector(
            spType: Spinner,
            spParamSelect: Spinner,
            etParam: EditText,
            initialAction: KeyAction
        ) {
            val keycodeOptions = listOf(
                Pair("Enter (66)", "66"),
                Pair("Delete / Backspace (67)", "67"),
                Pair("Space (62)", "62"),
                Pair("Tab (61)", "61"),
                Pair("Escape (111)", "111"),
                Pair("Arrow Left (21)", "21"),
                Pair("Arrow Right (22)", "22"),
                Pair("Arrow Up (19)", "19"),
                Pair("Arrow Down (20)", "20"),
                Pair("Home (122)", "122"),
                Pair("End (123)", "123"),
                Pair("Page Up (92)", "92"),
                Pair("Page Down (93)", "93"),
                Pair("F1 (131)", "131"),
                Pair("F2 (132)", "132"),
                Pair("F3 (133)", "133"),
                Pair("F4 (134)", "134"),
                Pair("F5 (135)", "135"),
                Pair("F6 (136)", "136"),
                Pair("F7 (137)", "137"),
                Pair("F8 (138)", "138"),
                Pair("F9 (139)", "139"),
                Pair("F10 (140)", "140"),
                Pair("F11 (141)", "141"),
                Pair("F12 (142)", "142"),
                Pair("Custom Keycode...", "custom")
            )

            val modifierOptions = listOf(
                Pair("Shift Modifier", "SHIFT"),
                Pair("Control (Ctrl) Modifier", "CTRL"),
                Pair("Alt Modifier", "ALT"),
                Pair("Meta / Super / Windows Modifier", "META"),
                Pair("Fn (Function) Layer Modifier", "FN"),
                Pair("Sym (Symbols) Layer Modifier", "SYM"),
                Pair("Caps Lock Toggle", "CAPS_LOCK"),
                Pair("Num Lock Toggle", "NUM_LOCK"),
                Pair("Custom Modifier...", "custom")
            )

            val layoutTargets = try {
                assets.list("layouts")?.filter { it.endsWith(".json") }?.map { it.removeSuffix(".json") } ?: listOf("main", "mobile", "mobile_number", "mobile_symbol", "function", "phone", "emoji")
            } catch (_: Exception) {
                listOf("main", "mobile", "mobile_number", "mobile_symbol", "function", "phone", "emoji")
            }
            val layoutOptions = layoutTargets.map { Pair("Layout Target: $it", it) } + Pair("Custom Layout Target...", "custom")

            val widgetOptions = listOf(
                Pair("Voice Input (Continuous)", "VOICE_INPUT"),
                Pair("Joystick Navigation", "JOYSTICK"),
                Pair("Clipboard History Overlay", "CLIPBOARD_OVERLAY"),
                Pair("Emoji Picker Overlay", "EMOJI_PICKER"),
                Pair("Key Preview Overlay", "KEY_PREVIEW"),
                Pair("Custom Widget...", "custom")
            )

            val screenModeOptions = listOf(
                Pair("Full Width Docked", "FULL_WIDTH_DOCKED"),
                Pair("Left Docked", "LEFT_DOCKED"),
                Pair("Right Docked", "RIGHT_DOCKED"),
                Pair("Split Screen Keyboard", "SPLIT")
            )

            val clipboardOptions = listOf(
                Pair("Copy to Clipboard", "COPY"),
                Pair("Cut Selected Text", "CUT"),
                Pair("Paste from Clipboard", "PASTE"),
                Pair("Paste Echo", "PASTE_ECHO"),
                Pair("Select All Text", "SELECT_ALL")
            )

            val launchableApps = try {
                getInstalledLaunchableApps().map { Pair(it.label, it.packageName) }
            } catch (_: Exception) { emptyList() }

            val launchAppOptions = launchableApps.map { Pair("🚀 Launch ${it.first} (${it.second})", it.second) } + listOf(
                Pair("🔍 Search All Installed Apps...", "search_apps"),
                Pair("Custom Package Name...", "custom")
            )

            fun updateParamUi(typePosition: Int, paramVal: String) {
                when (typePosition) {
                    0 -> {
                        spParamSelect.visibility = View.GONE
                        etParam.visibility = View.GONE
                    }
                    1 -> {
                        spParamSelect.visibility = View.GONE
                        etParam.visibility = View.VISIBLE
                        etParam.hint = "Text output (e.g. A, hello, space)"
                    }
                    2, 3 -> {
                        spParamSelect.visibility = View.VISIBLE
                        val options = keycodeOptions.map { it.first }
                        spParamSelect.adapter = ArrayAdapter<String>(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, options)
                        val matchIdx = keycodeOptions.indexOfFirst { it.second == paramVal }
                        if (matchIdx >= 0) {
                            spParamSelect.setSelection(matchIdx)
                            etParam.visibility = View.GONE
                        } else {
                            spParamSelect.setSelection(keycodeOptions.lastIndex)
                            etParam.visibility = View.VISIBLE
                            etParam.hint = "Numeric Keycode (e.g. 66)"
                        }
                    }
                    4 -> {
                        spParamSelect.visibility = View.VISIBLE
                        val options = modifierOptions.map { it.first }
                        spParamSelect.adapter = ArrayAdapter<String>(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, options)
                        val matchIdx = modifierOptions.indexOfFirst { it.second.equals(paramVal, ignoreCase = true) }
                        if (matchIdx >= 0) {
                            spParamSelect.setSelection(matchIdx)
                            etParam.visibility = View.GONE
                        } else {
                            spParamSelect.setSelection(modifierOptions.lastIndex)
                            etParam.visibility = View.VISIBLE
                            etParam.hint = "Modifier Name (e.g. SHIFT, CTRL)"
                        }
                    }
                    5 -> {
                        spParamSelect.visibility = View.VISIBLE
                        val options = layoutOptions.map { it.first }
                        spParamSelect.adapter = ArrayAdapter<String>(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, options)
                        val matchIdx = layoutOptions.indexOfFirst { it.second.equals(paramVal, ignoreCase = true) }
                        if (matchIdx >= 0) {
                            spParamSelect.setSelection(matchIdx)
                            etParam.visibility = View.GONE
                        } else {
                            spParamSelect.setSelection(layoutOptions.lastIndex)
                            etParam.visibility = View.VISIBLE
                            etParam.hint = "Layout Target (e.g. main, mobile)"
                        }
                    }
                    6 -> {
                        spParamSelect.visibility = View.VISIBLE
                        val options = widgetOptions.map { it.first }
                        spParamSelect.adapter = ArrayAdapter<String>(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, options)
                        val matchIdx = widgetOptions.indexOfFirst { it.second.equals(paramVal, ignoreCase = true) }
                        if (matchIdx >= 0) {
                            spParamSelect.setSelection(matchIdx)
                            etParam.visibility = View.GONE
                        } else {
                            spParamSelect.setSelection(widgetOptions.lastIndex)
                            etParam.visibility = View.VISIBLE
                            etParam.hint = "Widget Name"
                        }
                    }
                    7 -> {
                        spParamSelect.visibility = View.VISIBLE
                        val options = screenModeOptions.map { it.first }
                        spParamSelect.adapter = ArrayAdapter<String>(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, options)
                        val matchIdx = screenModeOptions.indexOfFirst { it.second.equals(paramVal, ignoreCase = true) }.coerceAtLeast(0)
                        spParamSelect.setSelection(matchIdx)
                        etParam.visibility = View.GONE
                        etParam.setText(screenModeOptions[matchIdx].second)
                    }
                    8 -> {
                        spParamSelect.visibility = View.VISIBLE
                        val options = clipboardOptions.map { it.first }
                        spParamSelect.adapter = ArrayAdapter<String>(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, options)
                        val matchIdx = clipboardOptions.indexOfFirst { it.second.equals(paramVal, ignoreCase = true) }.coerceAtLeast(0)
                        spParamSelect.setSelection(matchIdx)
                        etParam.visibility = View.GONE
                        etParam.setText(clipboardOptions[matchIdx].second)
                    }
                    9 -> {
                        spParamSelect.visibility = View.VISIBLE
                        val options = launchAppOptions.map { it.first }
                        spParamSelect.adapter = ArrayAdapter<String>(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, options)
                        val matchIdx = launchAppOptions.indexOfFirst { it.second.equals(paramVal, ignoreCase = true) }
                        if (matchIdx >= 0 && launchAppOptions[matchIdx].second != "search_apps") {
                            spParamSelect.setSelection(matchIdx)
                            etParam.visibility = View.GONE
                        } else {
                            val searchIdx = launchAppOptions.indexOfFirst { it.second == "search_apps" }.coerceAtLeast(0)
                            spParamSelect.setSelection(searchIdx)
                            etParam.visibility = View.VISIBLE
                            etParam.hint = "Package Name (e.g. com.termux)"
                        }
                    }
                }
            }

            val currentParamStr = getActionParamString(initialAction)
            updateParamUi(spType.selectedItemPosition, currentParamStr)

            spType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateParamUi(position, etParam.text.toString())
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            spParamSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (spType.selectedItemPosition) {
                        2, 3 -> {
                            val selected = keycodeOptions.getOrNull(position)
                            if (selected != null && selected.second != "custom") {
                                etParam.setText(selected.second)
                                etParam.visibility = View.GONE
                            } else {
                                etParam.visibility = View.VISIBLE
                            }
                        }
                        4 -> {
                            val selected = modifierOptions.getOrNull(position)
                            if (selected != null && selected.second != "custom") {
                                etParam.setText(selected.second)
                                etParam.visibility = View.GONE
                            } else {
                                etParam.visibility = View.VISIBLE
                            }
                        }
                        5 -> {
                            val selected = layoutOptions.getOrNull(position)
                            if (selected != null && selected.second != "custom") {
                                etParam.setText(selected.second)
                                etParam.visibility = View.GONE
                            } else {
                                etParam.visibility = View.VISIBLE
                            }
                        }
                        6 -> {
                            val selected = widgetOptions.getOrNull(position)
                            if (selected != null && selected.second != "custom") {
                                etParam.setText(selected.second)
                                etParam.visibility = View.GONE
                            } else {
                                etParam.visibility = View.VISIBLE
                            }
                        }
                        7 -> {
                            val selected = screenModeOptions.getOrNull(position)
                            if (selected != null) {
                                etParam.setText(selected.second)
                            }
                        }
                        8 -> {
                            val selected = clipboardOptions.getOrNull(position)
                            if (selected != null) {
                                etParam.setText(selected.second)
                            }
                        }
                        9 -> {
                            val selected = launchAppOptions.getOrNull(position)
                            if (selected != null) {
                                if (selected.second == "search_apps") {
                                    showAppPickerDialog { pkg, label ->
                                        etParam.setText(pkg)
                                        if (etPrimary.text.isNullOrEmpty()) {
                                            etPrimary.setText(label)
                                        }
                                        updateParamUi(9, pkg)
                                    }
                                } else if (selected.second != "custom") {
                                    etParam.setText(selected.second)
                                    etParam.visibility = View.GONE
                                    if (etPrimary.text.isNullOrEmpty()) {
                                        val appLabel = selected.first.removePrefix("🚀 Launch ").substringBefore(" (")
                                        etPrimary.setText(appLabel)
                                    }
                                } else {
                                    etParam.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        setupActionParamSelector(spActionType, spActionParamSelect, etActionParam, key.onPressAction)
        setupActionParamSelector(spLongPressType, spLongPressParamSelect, etLongPressParam, key.onLongPressAction)
        setupActionParamSelector(spSwipeUpType, spSwipeUpParamSelect, etSwipeUpParam, key.onSwipeUpAction)
        setupActionParamSelector(spSwipeDownType, spSwipeDownParamSelect, etSwipeDownParam, key.onSwipeDownAction)

        val spIcon = view.findViewById<Spinner>(R.id.spEditKeyIcon)

        var currentIconName: String? = key.iconName
        var currentIconOptions = listOf<Pair<String, String>>()
        var isInitializingSpinner = true

        fun populateIconSpinner(selectedPath: String?) {
            isInitializingSpinner = true
            val options = mutableListOf<Pair<String, String>>()

            // 1. Static Predefined Vector SVG Icons (Only .svg assets)
            options.add(Pair("None (Text Label Only)", ""))
            options.add(Pair("Microphone (mic.svg)", "mic.svg"))
            options.add(Pair("Speech TTS (tts.svg)", "tts.svg"))
            options.add(Pair("Paperclip (paperclip.svg)", "paperclip.svg"))
            options.add(Pair("Clipboard (clipboard.svg)", "clipboard.svg"))
            options.add(Pair("Copy (copy.svg)", "copy.svg"))
            options.add(Pair("Cut (cut.svg)", "cut.svg"))
            options.add(Pair("Paste (paste.svg)", "paste.svg"))
            options.add(Pair("Select All (select_all.svg)", "select_all.svg"))
            options.add(Pair("Keyboard (keyboard)", "keyboard"))

            try {
                val imageAssetFiles = assets.list("images") ?: emptyArray()
                val knownNames = options.map { it.second.lowercase() }
                for (file in imageAssetFiles) {
                    if (file.endsWith(".svg")) {
                        if (!knownNames.contains(file.lowercase())) {
                            val nameWithoutExt = file.substringBeforeLast('.').replace('_', ' ')
                            options.add(Pair("$nameWithoutExt ($file)", file))
                        }
                    }
                }
            } catch (_: Exception) {}

            // 2. User Icons Directory (files/icons/)
            val userIcons = com.programmerkeyboard.util.IconRenderer.getUserCustomIcons(this)
            options.addAll(userIcons)

            // 3. Action Option to Import
            options.add(Pair("Import New User Icon...", "custom"))

            currentIconOptions = options.toList()
            val customIdx = currentIconOptions.lastIndex

            val iconAdapter = com.programmerkeyboard.util.IconSpinnerAdapter(this, currentIconOptions)
            spIcon.adapter = iconAdapter

            val selectIdx = if (selectedPath.isNullOrEmpty()) 0
            else {
                val cleanSelected = selectedPath.trim().lowercase()
                val noExtSelected = cleanSelected.substringBeforeLast('.')
                val idx = currentIconOptions.indexOfFirst { option ->
                    val optPath = option.second.lowercase()
                    val optNoExt = optPath.substringBeforeLast('.')
                    optPath == cleanSelected ||
                    optNoExt == cleanSelected ||
                    optPath == noExtSelected ||
                    optNoExt == noExtSelected ||
                    (cleanSelected.isNotEmpty() && optPath.endsWith("/$cleanSelected")) ||
                    (optPath.isNotEmpty() && cleanSelected.endsWith("/$optPath"))
                }
                if (idx >= 0) idx else 0
            }
            spIcon.setSelection(selectIdx)
        }

        populateIconSpinner(currentIconName)

        onKeyIconPickedListener = { savedPath ->
            currentIconName = savedPath
            populateIconSpinner(currentIconName)
        }

        spIcon.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitializingSpinner) {
                    isInitializingSpinner = false
                    return
                }
                if (position in currentIconOptions.indices) {
                    val sel = currentIconOptions[position]
                    if (sel.second == "custom") {
                        browseKeyImageLauncher.launch("image/*")
                    } else {
                        currentIconName = sel.second
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        fun probeAndScrollToWidget(targetView: android.view.View) {
            val scrollView = view as? android.widget.ScrollView ?: return
            scrollView.postDelayed({
                val rect = android.graphics.Rect()
                targetView.getDrawingRect(rect)
                scrollView.offsetDescendantRectToMyCoords(targetView, rect)
                scrollView.smoothScrollTo(0, maxOf(0, rect.top - 40))
            }, 100)
        }

        val allEditTexts = listOf(etPrimary, etSecondary, etTopLeft, etTopRight, etWeight, etActionParam, etLongPressParam, etSwipeUpParam, etSwipeDownParam)
        allEditTexts.forEach { et ->
            et.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    probeAndScrollToWidget(v)
                }
            }
        }

        val allSpinners = listOf(spIcon, spCategory, spActionType, spActionParamSelect, spLongPressType, spLongPressParamSelect, spSwipeUpType, spSwipeUpParamSelect, spSwipeDownType, spSwipeDownParamSelect)
        allSpinners.forEach { sp ->
            sp.setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    probeAndScrollToWidget(v)
                }
                false
            }
        }

        var dialogRef: AlertDialog? = null

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
            dialogRef?.dismiss()
        }

        val createdDialog = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Save Key Properties") { _, _ ->
                pushUndoState()
                val newPrimary = etPrimary.text.toString().ifEmpty { "Key" }
                val newSecondary = etSecondary.text.toString().ifEmpty { null }
                val newTopLeft = etTopLeft.text.toString().ifEmpty { null }
                val newTopRight = etTopRight.text.toString().ifEmpty { null }
                val newCat = availableStyles[spCategory.selectedItemPosition.coerceIn(0, availableStyles.size - 1)]
                val newWeightVal = etWeight.text.toString().toFloatOrNull() ?: 1.0f
                val newIconName = currentIconName?.ifEmpty { null }
                val newIsSpacer = cbIsSpacer.isChecked

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
                                isSpacer = newIsSpacer,
                                iconName = newIconName,
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
            .create()

        dialogRef = createdDialog

        createdDialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        createdDialog.window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener {
            val r = android.graphics.Rect()
            createdDialog.window?.decorView?.getWindowVisibleDisplayFrame(r)
            val screenHeight = createdDialog.window?.decorView?.rootView?.height ?: 0
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) {
                val availableHeight = r.height() - 40
                if (availableHeight in 200..screenHeight) {
                    createdDialog.window?.setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        availableHeight
                    )
                }
            }
        }

        createdDialog.show()
    }

    private fun showRowEditorDialog(initialRowIdx: Int = 0, pushUndoState: () -> Unit, onUpdate: (LayoutDefinition) -> Unit) {
        val layout = editingLayout ?: return
        val view = layoutInflater.inflate(R.layout.dialog_edit_row, null)
        val spRowSelect = view.findViewById<Spinner>(R.id.spEditRowSelect)
        val cbHidden = view.findViewById<CheckBox>(R.id.cbEditRowHidden)
        val etSplitIndex = view.findViewById<EditText>(R.id.etEditRowSplitIndex)
        val cbSplitKey = view.findViewById<CheckBox>(R.id.cbEditRowSplitKey)
        val btnAddKeyToRow = view.findViewById<Button>(R.id.btnAddKeyToRow)
        val btnAddSpacerToRow = view.findViewById<Button>(R.id.btnAddSpacerToRow)
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

        btnAddSpacerToRow.setOnClickListener {
            val selectedIdx = spRowSelect.selectedItemPosition
            if (selectedIdx in layout.rows.indices) {
                pushUndoState()
                val newRows = layout.rows.toMutableList()
                val targetRow = newRows[selectedIdx]
                val updatedKeys = targetRow.keys.toMutableList()
                updatedKeys.add(
                    KeyDefinition(
                        primaryLabel = "",
                        isSpacer = true,
                        widthWeight = DimensionValue.Ratio(0.5f)
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
            .create()

        dialogRef?.show()
    }

    private fun showPhantomSpacerEditorDialog(
        rowIdx: Int,
        keyIdx: Int,
        key: KeyDefinition,
        pushUndoState: () -> Unit,
        onUpdate: (LayoutDefinition) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_phantom_spacer, null)
        val etWidth = view.findViewById<EditText>(R.id.etPhantomWidthWeight)
        val btnConvert = view.findViewById<Button>(R.id.btnConvertToRegularKey)
        val btnDelete = view.findViewById<Button>(R.id.btnDeletePhantomSpacer)

        val currentWeight = (key.widthWeight as? DimensionValue.Ratio)?.value ?: 0.5f
        etWidth.setText("$currentWeight")

        var dialogRef: AlertDialog? = null

        btnConvert.setOnClickListener {
            pushUndoState()
            editingLayout?.let { layout ->
                if (rowIdx in layout.rows.indices) {
                    val newRows = layout.rows.toMutableList()
                    val targetKeys = newRows[rowIdx].keys.toMutableList()
                    if (keyIdx in targetKeys.indices) {
                        targetKeys[keyIdx] = targetKeys[keyIdx].copy(
                            primaryLabel = "Key",
                            isSpacer = false,
                            styleName = "alphaKey",
                            onPressAction = KeyAction.SendText("Key")
                        )
                        newRows[rowIdx] = newRows[rowIdx].copy(keys = targetKeys)
                        onUpdate(layout.copy(rows = newRows))
                    }
                }
            }
            dialogRef?.dismiss()
        }

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
            dialogRef?.dismiss()
        }

        val createdDialog = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Save Width") { _, _ ->
                pushUndoState()
                val newWeight = etWidth.text.toString().toFloatOrNull() ?: currentWeight
                editingLayout?.let { layout ->
                    if (rowIdx in layout.rows.indices) {
                        val newRows = layout.rows.toMutableList()
                        val targetKeys = newRows[rowIdx].keys.toMutableList()
                        if (keyIdx in targetKeys.indices) {
                            targetKeys[keyIdx] = targetKeys[keyIdx].copy(
                                widthWeight = DimensionValue.Ratio(newWeight)
                            )
                            newRows[rowIdx] = newRows[rowIdx].copy(keys = targetKeys)
                            onUpdate(layout.copy(rows = newRows))
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialogRef = createdDialog
        createdDialog.show()
    }

    private fun showSpacingEditorDialog(pushUndoState: () -> Unit, onUpdate: (LayoutDefinition) -> Unit) {
        val layout = editingLayout ?: return
        val view = layoutInflater.inflate(R.layout.dialog_edit_spacing, null)

        val sbH = view.findViewById<SeekBar>(R.id.sbEditHorizontalSpacing)
        val etH = view.findViewById<EditText>(R.id.etEditHorizontalSpacing)
        val sbV = view.findViewById<SeekBar>(R.id.sbEditVerticalSpacing)
        val etV = view.findViewById<EditText>(R.id.etEditVerticalSpacing)
        val sbHeight = view.findViewById<SeekBar>(R.id.sbEditHeightPercentage)
        val etHeight = view.findViewById<EditText>(R.id.etEditHeightPercentage)

        val currentH = when (val h = layout.metadata.horizontalSpacing) {
            is DimensionValue.Absolute -> h.value
            is DimensionValue.Ratio -> h.value.toInt()
            else -> 4
        }
        val currentV = when (val v = layout.metadata.verticalSpacing) {
            is DimensionValue.Absolute -> v.value
            is DimensionValue.Ratio -> v.value.toInt()
            else -> 4
        }
        val currentHeight = layout.metadata.defaultHeightPercentage ?: 30

        sbH.progress = currentH.coerceIn(0, 24)
        etH.setText("$currentH")

        sbV.progress = currentV.coerceIn(0, 24)
        etV.setText("$currentV")

        sbHeight.progress = (currentHeight - 15).coerceIn(0, 35)
        etHeight.setText("$currentHeight")

        sbH.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) etH.setText("$progress")
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbV.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) etV.setText("$progress")
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) etHeight.setText("${progress + 15}")
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val createdDialog = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Apply Spacing Changes") { _, _ ->
                pushUndoState()
                val newH = etH.text.toString().toIntOrNull() ?: currentH
                val newV = etV.text.toString().toIntOrNull() ?: currentV
                val newHeight = etHeight.text.toString().toIntOrNull() ?: currentHeight

                val updatedMetadata = layout.metadata.copy(
                    horizontalSpacing = DimensionValue.Absolute(newH),
                    verticalSpacing = DimensionValue.Absolute(newV),
                    defaultHeightPercentage = newHeight
                )
                onUpdate(layout.copy(metadata = updatedMetadata))
            }
            .setNegativeButton("Cancel", null)
            .create()

        createdDialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        createdDialog.show()
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
            is KeyAction.PasteEcho -> obj.addProperty("type", "PASTE_ECHO")
            is KeyAction.SelectAll -> obj.addProperty("type", "SELECT_ALL")
            is KeyAction.SwitchIme -> obj.addProperty("type", "SWITCH_IME")
            is KeyAction.LaunchApp -> {
                obj.addProperty("type", "LAUNCH_APP")
                obj.addProperty("packageName", action.packageName)
            }
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
        key.iconName?.let { obj.addProperty("icon", it) }
        key.backgroundImage?.let { obj.addProperty("backgroundImage", it) }
        
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
        root.addProperty("version", layout.version)

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
