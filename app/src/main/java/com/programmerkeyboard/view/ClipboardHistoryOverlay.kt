package com.programmerkeyboard.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView

/**
 * Clipboard History Overlay Widget.
 * Renders directly over KeyboardView to let users browse, select, paste, or delete past clipboard items.
 */
class ClipboardHistoryOverlay(
    private val context: Context,
    private val historyItems: MutableList<String>,
    private val onItemPicked: (String) -> Unit,
    private val onDeleteItem: (Int, String) -> Unit,
    private val onClearHistory: () -> Unit
) {
    private var popupWindow: PopupWindow? = null

    fun show(anchorView: View) {
        dismiss()

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val width = anchorView.width
        val height = anchorView.height

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(16, 12, 16, 12)
        }

        // Header Row
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 8)
        }

        val titleView = TextView(context).apply {
            text = "📋 Clipboard History (${historyItems.size})"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnClear = Button(context).apply {
            text = "🗑 Clear All"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#991B1B"))
            setPadding(12, 0, 12, 0)
            setOnClickListener {
                onClearHistory()
                dismiss()
            }
        }

        val btnClose = Button(context).apply {
            text = "✕"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#334155"))
            setPadding(12, 0, 12, 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 8
            }
            setOnClickListener { dismiss() }
        }

        headerRow.addView(titleView)
        if (historyItems.isNotEmpty()) {
            headerRow.addView(btnClear)
        }
        headerRow.addView(btnClose)
        rootLayout.addView(headerRow)

        if (historyItems.isEmpty()) {
            val emptyView = TextView(context).apply {
                text = "No clipboard history saved yet.\nItems you copy anywhere on your device will appear here!"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            rootLayout.addView(emptyView)
        } else {
            val listView = ListView(context).apply {
                divider = null
                dividerHeight = 6
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }

            lateinit var adapter: ArrayAdapter<String>
            adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, historyItems) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemText = getItem(position) ?: ""
                    val container = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(Color.parseColor("#1E293B"))
                        setPadding(16, 12, 16, 12)
                    }

                    val badgeRow = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val indexBadge = TextView(context).apply {
                        text = "#${position + 1}"
                        setTextColor(Color.parseColor("#F59E0B"))
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    val lenBadge = TextView(context).apply {
                        text = "${itemText.length} chars"
                        setTextColor(Color.parseColor("#64748B"))
                        textSize = 11f
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginStart = 12
                        }
                    }

                    val btnDeleteSingle = TextView(context).apply {
                        text = "🗑"
                        textSize = 13f
                        setPadding(16, 4, 16, 4)
                        setTextColor(Color.parseColor("#EF4444"))
                        setOnClickListener {
                            if (position in historyItems.indices) {
                                val text = historyItems[position]
                                historyItems.removeAt(position)
                                onDeleteItem(position, text)
                                adapter.notifyDataSetChanged()
                                titleView.text = "📋 Clipboard History (${historyItems.size})"
                            }
                        }
                    }

                    badgeRow.addView(indexBadge)
                    badgeRow.addView(lenBadge)
                    badgeRow.addView(btnDeleteSingle)

                    val snippetView = TextView(context).apply {
                        val maxSnippet = if (itemText.length > 120) itemText.take(120) + "…" else itemText
                        text = maxSnippet
                        setTextColor(Color.parseColor("#F8FAFC"))
                        textSize = 13f
                        typeface = Typeface.MONOSPACE
                        setPadding(0, 4, 0, 0)
                    }

                    container.addView(badgeRow)
                    container.addView(snippetView)

                    container.setOnClickListener {
                        if (position in historyItems.indices) {
                            val selectedText = historyItems[position]
                            onItemPicked(selectedText)
                            dismiss()
                        }
                    }

                    container.setOnLongClickListener {
                        if (position in historyItems.indices) {
                            val text = historyItems[position]
                            historyItems.removeAt(position)
                            onDeleteItem(position, text)
                            adapter.notifyDataSetChanged()
                            titleView.text = "📋 Clipboard History (${historyItems.size})"
                        }
                        true
                    }

                    return container
                }
            }

            listView.adapter = adapter
            rootLayout.addView(listView)
        }

        popupWindow = PopupWindow(rootLayout, width, height, true).apply {
            isClippingEnabled = true
            showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }
}
