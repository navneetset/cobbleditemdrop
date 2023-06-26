package tech.sethi.pebbles.cobbleditemdrop

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.text.Text

object PM {

    fun parseMessageWithStyles(text: String, placeholder: String): Component {
        var mm = MiniMessage.miniMessage();
        return mm.deserialize(text.replace("{placeholder}", placeholder)).decoration(TextDecoration.ITALIC, false)
    }

    fun returnStyledText(text: String): Text {
        val component = parseMessageWithStyles(text, "placeholder")
        val gson = GsonComponentSerializer.gson()
        val json = gson.serialize(component)
        return Text.Serializer.fromJson(json) as Text
    }

    fun returnStyledJson(text: String): String {
        val component = parseMessageWithStyles(text, "placeholder")
        val gson = GsonComponentSerializer.gson()
        val json = gson.serialize(component)
        return json
    }

    fun setLore(itemStack: ItemStack, lore: List<String>) {
        val itemNbt = itemStack.getOrCreateSubNbt("display")
        val loreNbt = NbtList()

        for (line in lore) {
            loreNbt.add(NbtString.of(returnStyledJson(line)))
        }

        itemNbt.put("Lore", loreNbt)
    }
}