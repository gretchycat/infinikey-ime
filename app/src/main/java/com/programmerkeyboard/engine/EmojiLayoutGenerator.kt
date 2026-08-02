package com.programmerkeyboard.engine

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.view.KeyEvent
import com.programmerkeyboard.model.*

/**
 * Intelligent Layout Generator that queries Paint.hasGlyph() to dynamically generate
 * an Emoji Layout containing ONLY emojis natively supported and renderable by the device.
 */
object EmojiLayoutGenerator {

    val masterCategories = listOf(
        Pair("😀", listOf("😀","😃","😄","😁","😆","😅","😂","🤣","🥲","😊","😇","🙂","🙃","😉","😌","😍","🥰","😘","😋","😛","😜","🤪","🤨","🧐","🤓","😎","🤩","🥳","😏","😒","😞","😔","😟","😕","🙁","☹️","😣","😖","😫","😩","🥺","😢","😭","😤","😠","😡","🤬","🤯","😳","🥵","🥶","😱","😨","😰","😥","😓","🤗","🤔","🤭","🤫","🤥","😶","😐","😑","😬","🙄","😯","😦","😧","😮","😲","🥱","😴","🤤","😪","😵","🤐","🥴","🤢","🤮","🤧","😷","🤒","🤕")),
        Pair("👋", listOf("👍","👎","👌","🤌","🤏","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","🖕","👇","☝️","✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏","✍️","💅","🤳","💪","🦵","🦶","👂","👃","🧠","🫀","🫁","🦷","骨","👀","👁️","👅","👄")),
        Pair("🐶", listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🐔","🐧","🐦","🐤","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🐛","🦋","🐌","🐞","🐜","🦟","🦗","🕷️","🦂","🐢","🐍","🦎","🦖","🦕","🐙","🦑","🦐","🦞","🦀","🐡","🐠","🐟","🐬","🐳","🐋","🦈","🐊","🐅","🐆","🦓","🦍","🦧","🐘","🦛","🦏","🐪","🐫","🦒","🦘","🦬","🐃","🐂","🐄","🐎","🐖","🐐","🐑","🦙","🐕","🐩","🦮","🐕‍🦺","🐈","🐈‍⬛","🐓","🦃","🦚","🦜","🦩","🕊️","🐇","🦝","🦨","🦡","🦦","🦥","🦔")),
        Pair("🍕", listOf("🍏","🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥬","🥒","🌶️","🫑","🌽","🥕","🫒","🧄","🧅","🥔","🍠","🥐","🥯","🍞","🥖","🥨","🧀","🥚","🍳","バター","🥞","🧇","🥓","🥩","🍗","🍖","骨","🌭","🍔","🍟","🍕","🫓","🥪","🥙","🧆","🌮","🌯","🫔","🥗","🥘","🫕","🥫","🍝","🍜","🍲","🍛","🍣","🍱","🥟","🦪","🍤","🍙","🍚","🍘","🍥","🥠","🥮","🍢","🍡","🍧","🍨","🍦","🥧","🧁","🍰","🎂","🍮","🍭","🍬","🍫","🍿","🍩","🍪","🌰","🥜","🍯","🥛","☕","🫖","🍵","🍶","🍾","🍷","🍸","🍹","🍺","🍻","🥂","🥃","🥤","🧃","🧉","🧊")),
        Pair("⚽", listOf("⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🪀","🏓","🏸","🏒","🏑","🥍","🏏","🪃","🥅","⛳","kite","🏹","🎣","🏲","🥊","🥋","🎽","🛹","🛼","🛷","⛸️","🥌","🎿","⛷️","🏂","🪂","🏋️","🤼","🤸","⛹️","🤺","🤾","🏌️","🏇","🧘","🏄","🏊","🤽","🚣","🧗","🚵","🚴")),
        Pair("🚀", listOf("🚗","🚕","🚙","🚌","🏎️","🚓","🚑","🚒","🚐","🛻","🚚","🚛","🚜","🦯","🦽","🦼","🛴","🚲","🛵","🏍️","🛺","🚨","🚔","🚍","🚘","🚖","🚡","🚠","🚟","🚃","🚋","🚞","🚝","🚄","🚅","🚆","🚇","🗾","🚀","🛸","🚁","🛶","⛵","🚤","🛥️","🛳️","⚓")),
        Pair("💡", listOf("💻","🖥️","🖨️","🖱️","🪛","🔧","🔨","⚙️","⚖️","⛓️","⚡","🔋","🔌","📱","📞","📟","📠","📷","📹","🎥","📻","💡","🔦","🏮","📔","📕","📖","📗","📘","📙","📚","📓","📒","📝","✏️","✒️","🖊️","🖌️","🖍️","📌","📍","✂️","🔒","🔓","🔑","🗝️","❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","✨","⭐","🌟","💫","💥","🔥","🎉","🎊","💯","✅","❌")),
        Pair("🚩", listOf("🏁","🚩","🎌","🏴","🏳️","🏳️‍🌈","🏴‍☠️","🇺🇸","🇨🇦","🇬🇧","🇫🇷","🇩🇪","🇮🇹","🇪🇸","🇯🇵","🇰🇷","🇨🇳","🇮🇳","🇧🇷","🇲🇽","🇦🇺","🇷🇺"))
    )

    fun generateSupportedEmojiLayout(context: Context, categoryIndex: Int = 0): LayoutDefinition {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT
        }

        val (catIcon, emojis) = masterCategories.getOrElse(categoryIndex) { masterCategories[0] }
        val supportedEmojis = emojis.filter { emoji ->
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    paint.hasGlyph(emoji)
                } else true
            } catch (_: Exception) { true }
        }

        val rowsList = mutableListOf<KeyRow>()
        val chunkSize = 8
        var rowIdCounter = 1

        supportedEmojis.chunked(chunkSize).forEach { chunk ->
            val keysList = chunk.map { emoji ->
                KeyDefinition(
                    primaryLabel = emoji,
                    styleName = "alphaKey",
                    onPressAction = KeyAction.SendText(emoji)
                )
            }.toMutableList()
            rowsList.add(KeyRow(id = rowIdCounter++, keys = keysList))
        }

        // Category Switcher Bar
        val catRowKeys = mutableListOf<KeyDefinition>()
        masterCategories.forEachIndexed { idx, pair ->
            catRowKeys.add(KeyDefinition(
                primaryLabel = pair.first,
                styleName = if (idx == categoryIndex) "actionKey" else "functionKey",
                widthWeight = DimensionValue.Ratio(1.0f),
                onPressAction = KeyAction.SwitchLayout("emoji_auto_$idx")
            ))
        }
        rowsList.add(KeyRow(id = rowIdCounter++, keys = catRowKeys))

        // Bottom Navigation Row: ABC, Space, Backspace ⌫
        val bottomRowKeys = mutableListOf<KeyDefinition>()
        bottomRowKeys.add(KeyDefinition(
            primaryLabel = "ABC",
            styleName = "modifierKey",
            widthWeight = DimensionValue.Ratio(1.5f),
            onPressAction = KeyAction.SwitchLayout("previous")
        ))

        bottomRowKeys.add(KeyDefinition(
            primaryLabel = "␣",
            styleName = "alphaKey",
            isFlexible = true,
            onPressAction = KeyAction.SendText(" ")
        ))

        bottomRowKeys.add(KeyDefinition(
            primaryLabel = "⌫",
            styleName = "actionKey",
            widthWeight = DimensionValue.Ratio(1.5f),
            onPressAction = KeyAction.SendCode(KeyEvent.KEYCODE_DEL),
            onLongPressAction = KeyAction.AutoRepeat(KeyEvent.KEYCODE_DEL, 50)
        ))

        rowsList.add(KeyRow(id = rowIdCounter, keys = bottomRowKeys))

        return LayoutDefinition(
            id = "emoji_auto",
            name = "Device Emojis ($catIcon)",
            version = "1.0",
            author = "Gretchen Maculo",
            metadata = LayoutMetadata(
                scrollDirection = "VERTICAL",
                maxVisibleRows = 4
            ),
            rows = rowsList
        )
    }
}
