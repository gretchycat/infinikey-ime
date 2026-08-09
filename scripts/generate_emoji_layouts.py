#!/usr/bin/env python3
import json
import os
import urllib.request

EMOJI_DATA_URL = "https://raw.githubusercontent.com/amio/emoji.json/master/emoji.json"

CATEGORY_MAPPING = {
    "Smileys & Emotion": {
        "id": "emoji",
        "name": "Emoji - Smileys",
        "icon": "😀"
    },
    "People & Body": {
        "id": "emoji_body",
        "name": "Emoji - Hands & Body",
        "icon": "👋"
    },
    "Animals & Nature": {
        "id": "emoji_animals",
        "name": "Emoji - Animals & Nature",
        "icon": "🐶"
    },
    "Food & Drink": {
        "id": "emoji_food",
        "name": "Emoji - Food & Drink",
        "icon": "🍕"
    },
    "Activities": {
        "id": "emoji_sports",
        "name": "Emoji - Sports & Activities",
        "icon": "⚽"
    },
    "Travel & Places": {
        "id": "emoji_travel",
        "name": "Emoji - Travel & Places",
        "icon": "🚀"
    },
    "Objects": {
        "id": "emoji_objects",
        "name": "Emoji - Objects",
        "icon": "💡"
    },
    "Symbols": {
        "id": "emoji_symbols",
        "name": "Emoji - Symbols & Flags",
        "icon": "🚩"
    }
}

SKIN_TONE_MODIFIERS = [chr(0x1F3FB), chr(0x1F3FC), chr(0x1F3FD), chr(0x1F3FE), chr(0x1F3FF)]

def get_base_and_modifier(emoji_char):
    found_modifier = None
    for mod in SKIN_TONE_MODIFIERS:
        if mod in emoji_char:
            found_modifier = mod
            break
    if not found_modifier:
        return emoji_char, None
    
    # Remove modifier to get base character
    base = emoji_char.replace(found_modifier, "")
    # Clean trailing zero-width joiners or variation selectors if left hanging
    base = base.replace("\u200d\u200d", "\u200d").strip("\u200d\ufe0f")
    return base, emoji_char

def main():
    output_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "layouts")
    os.makedirs(output_dir, exist_ok=True)

    print("Fetching fresh emoji dataset from amio/emoji.json...")
    try:
        with urllib.request.urlopen(EMOJI_DATA_URL, timeout=10) as response:
            emoji_list = json.loads(response.read().decode("utf-8"))
    except Exception as e:
        print(f"Warning: Failed to fetch fresh emojis online: {e}")
        # Verify if local layouts already exist. If so, skip generation to prevent build failure.
        all_exist = all(os.path.exists(os.path.join(output_dir, f"{info['id']}.json")) for info in CATEGORY_MAPPING.values())
        if all_exist:
            print("Local emoji layouts already exist. Skipping layout generation.")
            return
        else:
            print("Failed to load emojis and local layouts do not exist. Attempting fallback generation.")
            emoji_list = []

    # Group emojis by category (map to dict: base -> list of variants)
    grouped_emojis = {cat: {} for cat in CATEGORY_MAPPING.keys()}
    for item in emoji_list:
        category_name = item.get("category", "")
        matched_cat = None
        for key in CATEGORY_MAPPING.keys():
            if category_name.startswith(key):
                matched_cat = key
                break
        
        # Also group "Flags" under "Symbols"
        if not matched_cat and category_name.startswith("Flags"):
            matched_cat = "Symbols"

        if matched_cat:
            char = item.get("char")
            if char:
                base, variant = get_base_and_modifier(char)
                if base not in grouped_emojis[matched_cat]:
                    grouped_emojis[matched_cat][base] = []
                if variant and variant not in grouped_emojis[matched_cat][base]:
                    grouped_emojis[matched_cat][base].append(variant)

    # For each category, generate layout file
    for cat_name, cat_info in CATEGORY_MAPPING.items():
        layout_id = cat_info["id"]
        filename = f"{layout_id}.json"
        filepath = os.path.join(output_dir, filename)

        # Convert grouping dict to list of keys
        layout_keys = []
        for base, variants in grouped_emojis[cat_name].items():
            if variants:
                layout_keys.append({
                    "label": base,
                    "alternates": variants,
                    "style": "alphaKey",
                    "onPress": { "type": "SEND_TEXT", "text": base }
                })
            else:
                layout_keys.append(base)

        if not layout_keys:
            # Fallback values in case of empty fetch
            layout_keys = [cat_info["icon"]] * 24

        rows = []
        row_id_counter = 1

        # 1. Pinned Top Category Bar
        cat_keys = []
        cat_keys.append({
            "label": "🕒",
            "style": "actionKey" if layout_id == "emoji_recents" else "functionKey",
            "onPress": { "type": "SWITCH_LAYOUT", "target": "emoji_recents" }
        })
        for other_cat_name, other_cat_info in CATEGORY_MAPPING.items():
            is_active = (other_cat_info["id"] == layout_id)
            cat_keys.append({
                "label": other_cat_info["icon"],
                "style": "actionKey" if is_active else "functionKey",
                "onPress": { "type": "SWITCH_LAYOUT", "target": other_cat_info["id"] }
            })
        rows.append({
            "id": row_id_counter,
            "keys": cat_keys
        })
        row_id_counter += 1

        # 2. Scrollable Middle Rows of Emojis (chunked into 8 keys per row)
        chunk_size = 8
        for i in range(0, len(layout_keys), chunk_size):
            chunk = layout_keys[i:i + chunk_size]
            rows.append({
                "id": row_id_counter,
                "keys": chunk
            })
            row_id_counter += 1

        # 3. Pinned Bottom Navigation Row
        rows.append({
            "id": row_id_counter,
            "keys": [
                { "label": "⌨ ABC", "style": "modifierKey", "weight": 1.5, "onPress": { "type": "SWITCH_LAYOUT", "target": "[last]" } },
                { "label": "␣", "style": "alphaKey", "flexible": True, "onPress": { "type": "SEND_TEXT", "text": " " } },
                { "label": "⌫", "style": "actionKey", "weight": 1.5, "onPress": { "type": "SEND_CODE", "code": 67 }, "onLongPress": { "type": "AUTO_REPEAT", "code": 67, "intervalMs": 50 } }
            ]
        })

        layout_definition = {
            "id": layout_id,
            "name": cat_info["name"],
            "version": "1.0",
            "author": "Dynamic Emoji Generator",
            "description": f"Generated from amio/emoji.json for category {cat_name}.",
            "metadata": {
                "horizontalSpacing": 4,
                "verticalSpacing": 4,
                "defaultScreenMode": "FULL_WIDTH_DOCKED",
                "defaultHeightPercentage": 30,
                "showKeyPreview": False,
                "scrollDirection": "VERTICAL",
                "maxVisibleRows": 4
            },
            "styles": {
                "alphaKey": {
                    "cornerRadius": 8,
                    "fontSize": 26,
                    "showPreview": False
                },
                "functionKey": {
                    "cornerRadius": 8,
                    "fontSize": 22,
                    "showPreview": False
                },
                "modifierKey": {
                    "cornerRadius": 8,
                    "showPreview": False
                },
                "actionKey": {
                    "cornerRadius": 8,
                    "showPreview": False
                }
            },
            "rows": rows
        }

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(layout_definition, f, ensure_ascii=False, indent=2)

    print("Successfully generated all emoji layouts!")

if __name__ == "__main__":
    main()
