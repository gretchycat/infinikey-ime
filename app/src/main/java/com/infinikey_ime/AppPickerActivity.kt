package com.infinikey_ime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
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
    private var displayItems: MutableList<ListItem> = mutableListOf()
    private lateinit var adapter: CategorizedAppAdapter

    data class AppEntry(
        val label: String,
        val packageName: String,
        val category: String,
        val appInfo: ApplicationInfo
    )

    sealed class ListItem {
        data class Header(val title: String, val count: Int) : ListItem()
        data class App(val entry: AppEntry) : ListItem()
    }

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
        adapter = CategorizedAppAdapter(this, displayItems)
        lvAppList.adapter = adapter

        lvAppList.setOnItemClickListener { _, _, position, _ ->
            val item = displayItems.getOrNull(position) ?: return@setOnItemClickListener
            if (item is ListItem.App) {
                val entry = item.entry
                if (slotId.isNotBlank()) {
                    LauncherPreferencesManager.saveAppForSlot(this, slotId, entry.packageName)
                    Toast.makeText(this, "Assigned '${entry.label}' to $slotId", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }
    }

    private fun loadApps() {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        } else {
            pm.queryIntentActivities(mainIntent, 0)
        }

        val appMap = mutableMapOf<String, AppEntry>()

        for (res in resolveList) {
            val pkg = res.activityInfo.packageName
            if (appMap.containsKey(pkg)) continue
            val label = res.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
            val appInfo = res.activityInfo.applicationInfo
            val category = getCategoryForApp(appInfo, label, pkg)
            appMap[pkg] = AppEntry(label, pkg, category, appInfo)
        }

        // Fallback: Query all installed applications with launch intents
        val installedApps = pm.getInstalledApplications(0)
        for (appInfo in installedApps) {
            val pkg = appInfo.packageName
            if (appMap.containsKey(pkg)) continue
            if (pm.getLaunchIntentForPackage(pkg) == null) continue
            val label = pm.getApplicationLabel(appInfo).toString().takeIf { it.isNotBlank() } ?: pkg
            val category = getCategoryForApp(appInfo, label, pkg)
            appMap[pkg] = AppEntry(label, pkg, category, appInfo)
        }

        allApps = appMap.values.sortedBy { it.label.lowercase() }
        buildDisplayList("")
    }

    private fun filterApps(query: String) {
        buildDisplayList(query)
        adapter.notifyDataSetChanged()
    }

    private fun buildDisplayList(query: String) {
        displayItems.clear()
        val q = query.trim().lowercase()

        val matching = if (q.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }

        val categoryOrder = listOf(
            "🌐 Internet & Browsers",
            "💬 Social & Communication",
            "🎵 Audio & Music",
            "🎥 Video & Movies",
            "📷 Photos & Graphics",
            "🛠️ Productivity & Utilities",
            "🎮 Games",
            "⚙️ System & Settings",
            "📱 Other Applications"
        )

        val grouped = matching.groupBy { it.category }

        for (cat in categoryOrder) {
            val appsInCat = grouped[cat] ?: continue
            if (appsInCat.isNotEmpty()) {
                displayItems.add(ListItem.Header(cat, appsInCat.size))
                for (app in appsInCat.sortedBy { it.label.lowercase() }) {
                    displayItems.add(ListItem.App(app))
                }
            }
        }

        // Catch any categories not in fixed order list
        for ((cat, appsInCat) in grouped) {
            if (cat !in categoryOrder && appsInCat.isNotEmpty()) {
                displayItems.add(ListItem.Header(cat, appsInCat.size))
                for (app in appsInCat.sortedBy { it.label.lowercase() }) {
                    displayItems.add(ListItem.App(app))
                }
            }
        }
    }

    private fun getCategoryForApp(appInfo: ApplicationInfo, label: String, pkg: String): String {
        val pkgLower = pkg.lowercase()
        val labelLower = label.lowercase()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return "🎮 Games"
                ApplicationInfo.CATEGORY_AUDIO -> return "🎵 Audio & Music"
                ApplicationInfo.CATEGORY_VIDEO -> return "🎥 Video & Movies"
                ApplicationInfo.CATEGORY_IMAGE -> return "📷 Photos & Graphics"
                ApplicationInfo.CATEGORY_SOCIAL -> return "💬 Social & Communication"
                ApplicationInfo.CATEGORY_NEWS -> return "💬 Social & Communication"
                ApplicationInfo.CATEGORY_MAPS -> return "🛠️ Productivity & Utilities"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return "🛠️ Productivity & Utilities"
            }
        }

        if (pkgLower.contains("browser") || pkgLower.contains("chrome") || pkgLower.contains("firefox") ||
            pkgLower.contains("opera") || pkgLower.contains("edge") || pkgLower.contains("web") ||
            labelLower.contains("browser") || labelLower.contains("chrome")) {
            return "🌐 Internet & Browsers"
        }

        if (pkgLower.contains("messaging") || pkgLower.contains("chat") || pkgLower.contains("whatsapp") ||
            pkgLower.contains("telegram") || pkgLower.contains("signal") || pkgLower.contains("discord") ||
            pkgLower.contains("slack") || pkgLower.contains("messenger") || pkgLower.contains("email") ||
            pkgLower.contains("gmail") || pkgLower.contains("mail") || pkgLower.contains("dialer") ||
            pkgLower.contains("phone") || pkgLower.contains("contacts") || labelLower.contains("phone") ||
            labelLower.contains("contacts") || labelLower.contains("message") || labelLower.contains("mail")) {
            return "💬 Social & Communication"
        }

        if (pkgLower.contains("youtube") || pkgLower.contains("music") || pkgLower.contains("spotify") ||
            pkgLower.contains("audio") || pkgLower.contains("sound") || pkgLower.contains("player") ||
            pkgLower.contains("vlc") || pkgLower.contains("podcast") || labelLower.contains("music") ||
            labelLower.contains("audio") || labelLower.contains("player")) {
            return "🎵 Audio & Music"
        }

        if (pkgLower.contains("video") || pkgLower.contains("movie") || pkgLower.contains("netflix") ||
            pkgLower.contains("twitch") || labelLower.contains("video") || labelLower.contains("movie")) {
            return "🎥 Video & Movies"
        }

        if (pkgLower.contains("camera") || pkgLower.contains("gallery") || pkgLower.contains("photo") ||
            labelLower.contains("camera") || labelLower.contains("gallery") || labelLower.contains("photo")) {
            return "📷 Photos & Graphics"
        }

        if (pkgLower.contains("calculator") || pkgLower.contains("calendar") || pkgLower.contains("clock") ||
            pkgLower.contains("notes") || pkgLower.contains("documents") || pkgLower.contains("office") ||
            pkgLower.contains("drive") || pkgLower.contains("files") || pkgLower.contains("terminal") ||
            pkgLower.contains("termux") || pkgLower.contains("editor") || pkgLower.contains("code") ||
            pkgLower.contains("keep") || pkgLower.contains("pdf") || labelLower.contains("calculator") ||
            labelLower.contains("calendar") || labelLower.contains("clock") || labelLower.contains("file")) {
            return "🛠️ Productivity & Utilities"
        }

        if (pkgLower.contains("game") || labelLower.contains("game")) {
            return "🎮 Games"
        }

        if (pkgLower.contains("setting") || pkgLower.contains("system") || pkgLower.contains("infinikey") ||
            pkgLower.contains("vending") || labelLower.contains("setting") || labelLower.contains("system")) {
            return "⚙️ System & Settings"
        }

        return "📱 Other Applications"
    }

    private class CategorizedAppAdapter(
        context: Context,
        private val items: List<ListItem>
    ) : ArrayAdapter<ListItem>(context, 0, items) {

        private val pm = context.packageManager

        override fun getViewTypeCount(): Int = 2

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is ListItem.Header -> 0
                is ListItem.App -> 1
                null -> 1
            }
        }

        override fun isEnabled(position: Int): Boolean {
            return getItem(position) is ListItem.App
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position) ?: return View(context)

            return when (item) {
                is ListItem.Header -> {
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_app_category_header, parent, false)
                    val tvCategoryTitle = view.findViewById<TextView>(R.id.tvCategoryTitle)
                    tvCategoryTitle.text = "${item.title} (${item.count})"
                    view
                }
                is ListItem.App -> {
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_app_picker, parent, false)
                    val entry = item.entry

                    val ivAppIcon = view.findViewById<ImageView>(R.id.ivAppIcon)
                    val tvAppName = view.findViewById<TextView>(R.id.tvAppName)
                    val tvAppPackage = view.findViewById<TextView>(R.id.tvAppPackage)

                    tvAppName.text = entry.label
                    tvAppPackage.text = entry.packageName

                    try {
                        val icon: Drawable = entry.appInfo.loadIcon(pm)
                        ivAppIcon.setImageDrawable(icon)
                    } catch (_: Exception) {
                        ivAppIcon.setImageDrawable(null)
                    }

                    view
                }
            }
        }
    }
}
