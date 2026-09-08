package com.infinikey_ime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.infinikey_ime.util.LauncherPreferencesManager

class AppPickerActivity : Activity() {

    companion object {
        const val EXTRA_SLOT_ID = "extra_slot_id"
    }

    private var slotId: String = ""
    private var allApps: List<AppEntry> = emptyList()
    private var filteredApps: MutableList<AppEntry> = mutableListOf()
    private lateinit var adapter: AppAdapter

    data class AppEntry(
        val label: String,
        val packageName: String,
        val resolveInfo: ResolveInfo
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        slotId = intent.getStringExtra(EXTRA_SLOT_ID) ?: ""

        val tvPickerTitle = findViewById<TextView>(R.id.tvPickerTitle)
        if (slotId.isNotBlank()) {
            tvPickerTitle.text = "🚀 Select App ($slotId)"
        }

        val btnClearSlot = findViewById<Button>(R.id.btnClearSlot)
        btnClearSlot.setOnClickListener {
            if (slotId.isNotBlank()) {
                LauncherPreferencesManager.clearAppForSlot(this, slotId)
                Toast.makeText(this, "Unassigned key slot $slotId", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

        val btnClosePicker = findViewById<Button>(R.id.btnClosePicker)
        btnClosePicker.setOnClickListener {
            finish()
        }

        loadApps()

        val etSearchApp = findViewById<EditText>(R.id.etSearchApp)
        etSearchApp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val lvAppList = findViewById<ListView>(R.id.lvAppList)
        adapter = AppAdapter(this, filteredApps)
        lvAppList.adapter = adapter

        lvAppList.setOnItemClickListener { _, _, position, _ ->
            val entry = filteredApps.getOrNull(position) ?: return@setOnItemClickListener
            if (slotId.isNotBlank()) {
                LauncherPreferencesManager.saveAppForSlot(this, slotId, entry.packageName)
                Toast.makeText(this, "Assigned '${entry.label}' to $slotId", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun loadApps() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pm = packageManager
        val resolveList = pm.queryIntentActivities(mainIntent, 0)

        allApps = resolveList.map { res ->
            val label = res.loadLabel(pm)?.toString() ?: res.activityInfo.packageName
            AppEntry(label, res.activityInfo.packageName, res)
        }.sortedBy { it.label.lowercase() }

        filteredApps.clear()
        filteredApps.addAll(allApps)
    }

    private fun filterApps(query: String) {
        val q = query.trim().lowercase()
        filteredApps.clear()
        if (q.isEmpty()) {
            filteredApps.addAll(allApps)
        } else {
            allApps.filterTo(filteredApps) {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
        adapter.notifyDataSetChanged()
    }

    private class AppAdapter(
        context: Context,
        private val items: List<AppEntry>
    ) : ArrayAdapter<AppEntry>(context, 0, items) {

        private val pm = context.packageManager

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_app_picker, parent, false)
            val entry = getItem(position) ?: return view

            val ivAppIcon = view.findViewById<ImageView>(R.id.ivAppIcon)
            val tvAppName = view.findViewById<TextView>(R.id.tvAppName)
            val tvAppPackage = view.findViewById<TextView>(R.id.tvAppPackage)

            tvAppName.text = entry.label
            tvAppPackage.text = entry.packageName

            val icon = entry.resolveInfo.loadIcon(pm)
            ivAppIcon.setImageDrawable(icon)

            return view
        }
    }
}
