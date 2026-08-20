package com.infinikey_ime.view

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
    private val initialEchoMode: Boolean = true,
    private val onItemPicked: (selectedText: String, isEchoPaste: Boolean) -> Unit,
    private val onPasteMethodChanged: ((isEchoMode: Boolean) -> Unit)? = null,
    private val onDeleteItem: (Int, String) -> Unit,
    private val onClearHistory: () -> Unit,
    var onDismissListener: (() -> Unit)? = null
) {
    private var popupWindow: PopupWindow? = null
    private var isEchoMode: Boolean = initialEchoMode
    private var attachedAnchorView: View? = null
    private var attachListener: View.OnAttachStateChangeListener? = null

    fun isShowing(): Boolean = popupWindow?.isShowing == true

    fun show(anchorView: View) {
        val oldPw = popupWindow
        popupWindow = null
        if (oldPw != null) {
            try {
                oldPw.setOnDismissListener(null)
                oldPw.dismiss()
            } catch (_: Exception) {}
        }

        if (!anchorView.isAttachedToWindow || anchorView.windowToken == null) {
            return
        }

        attachedAnchorView?.let { oldView ->
            attachListener?.let { l ->
                try { oldView.removeOnAttachStateChangeListener(l) } catch (_: Exception) {}
            }
        }
        attachedAnchorView = anchorView
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                dismiss()
            }
        }
        attachListener = listener
        try {
            anchorView.addOnAttachStateChangeListener(listener)
        } catch (_: Exception) {}

        val location = IntArray(2)
        try {
            anchorView.getLocationOnScreen(location)
        } catch (_: Exception) {
            location[0] = 0
            location[1] = 0
        }
        val x = location[0]
        val y = location[1]
        val width = anchorView.width.coerceAtLeast(200)
        val height = anchorView.height.coerceAtLeast(200)

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
            text = "📋 History (${historyItems.size})"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnToggleMethod = Button(context).apply {
            textSize = 11f
            setPadding(10, 0, 10, 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 6
            }
            fun updateStyle() {
                if (isEchoMode) {
                    text = "Echo"
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#7C3AED"))
                } else {
                    text = "Normal"
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#0284C7"))
                }
            }
            updateStyle()
            setOnClickListener {
                isEchoMode = !isEchoMode
                updateStyle()
                onPasteMethodChanged?.invoke(isEchoMode)
            }
        }

        val btnClear = Button(context).apply {
            text = "🗑 Clear"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#991B1B"))
            setPadding(10, 0, 10, 0)
            setOnClickListener {
                onClearHistory()
                dismiss()
            }
        }

        val btnClose = Button(context).apply {
            text = "✕"
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#334155"))
            setPadding(10, 0, 10, 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 6
            }
            setOnClickListener { dismiss() }
        }

        headerRow.addView(titleView)
        headerRow.addView(btnToggleMethod)
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
                    val itemText = try {
                        if (position in 0 until historyItems.size) historyItems[position] else (getItem(position) ?: "")
                    } catch (_: Exception) { "" }
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
                            if (position in 0 until historyItems.size) {
                                val text = historyItems[position]
                                historyItems.removeAt(position)
                                onDeleteItem(position, text)
                                if (historyItems.isEmpty()) {
                                    dismiss()
                                } else {
                                    adapter.notifyDataSetChanged()
                                    titleView.text = "📋 History (${historyItems.size})"
                                }
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
                        try {
                            com.infinikey_ime.util.FontFallbackManager.applyToTextView(this, maxSnippet)
                        } catch (_: Exception) {}
                        setPadding(0, 4, 0, 0)
                    }

                    container.addView(badgeRow)
                    container.addView(snippetView)

                    container.setOnClickListener {
                        if (position in 0 until historyItems.size) {
                            val selectedText = historyItems[position]
                            onItemPicked(selectedText, isEchoMode)
                            dismiss()
                        }
                    }

                    container.setOnLongClickListener {
                        if (position in 0 until historyItems.size) {
                            val text = historyItems[position]
                            historyItems.removeAt(position)
                            onDeleteItem(position, text)
                            if (historyItems.isEmpty()) {
                                dismiss()
                            } else {
                                adapter.notifyDataSetChanged()
                                titleView.text = "📋 History (${historyItems.size})"
                            }
                        }
                        true
                    }

                    return container
                }
            }

            listView.adapter = adapter
            rootLayout.addView(listView)
        }

        try {
            val pw = PopupWindow(rootLayout, width, height, false).apply {
                isClippingEnabled = true
                isTouchable = true
                isOutsideTouchable = true
                setOnDismissListener {
                    val dListener = onDismissListener
                    onDismissListener = null
                    dListener?.invoke()
                }
                showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)
            }
            popupWindow = pw
        } catch (e: Exception) {
            e.printStackTrace()
            popupWindow = null
        }
    }

    fun dismiss() {
        val pw = popupWindow
        popupWindow = null
        try {
            pw?.setOnDismissListener(null)
            pw?.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        attachedAnchorView?.let { v ->
            attachListener?.let { l ->
                try { v.removeOnAttachStateChangeListener(l) } catch (_: Exception) {}
            }
        }
        attachedAnchorView = null
        attachListener = null

        val listener = onDismissListener
        onDismissListener = null
        listener?.invoke()
    }
}
