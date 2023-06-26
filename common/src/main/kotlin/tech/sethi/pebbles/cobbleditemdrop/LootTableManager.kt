package tech.sethi.pebbles.cobbleditemdrop

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.util.Identifier
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*

object LootTableManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val defaultLootTables = mapOf(
        "normal" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:white_wool"), 1, 5),
                LootEntry(Identifier("minecraft:white_terracotta"), 2, 5)
            )
        ),
        "fire" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:red_wool"), 1, 5),
                LootEntry(Identifier("minecraft:red_terracotta"), 2, 5)
            )
        ),
        "water" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:light_blue_wool"), 1, 5),
                LootEntry(Identifier("minecraft:light_blue_terracotta"), 2, 5)
            )
        ),
        "grass" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:green_wool"), 1, 5),
                LootEntry(Identifier("minecraft:green_terracotta"), 2, 5)
            )
        ),
        "electric" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:yellow_wool"), 1, 5),
                LootEntry(Identifier("minecraft:yellow_terracotta"), 2, 5)
            )
        ),
        "ice" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:light_blue_wool"), 1, 5),
                LootEntry(Identifier("minecraft:light_blue_terracotta"), 2, 5)
            )
        ),
        "fighting" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:orange_wool"), 1, 5),
                LootEntry(Identifier("minecraft:orange_terracotta"), 2, 5)
            )
        ),
        "poison" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:purple_wool"), 1, 5),
                LootEntry(Identifier("minecraft:purple_terracotta"), 2, 5)
            )
        ),
        "ground" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:brown_wool"), 1, 5),
                LootEntry(Identifier("minecraft:brown_terracotta"), 2, 5)
            )
        ),
        "flying" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:light_gray_wool"), 1, 5),
                LootEntry(Identifier("minecraft:light_gray_terracotta"), 2, 5)
            )
        ),
        "psychic" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:magenta_wool"), 1, 5),
                LootEntry(Identifier("minecraft:magenta_terracotta"), 2, 5)
            )
        ),
        "bug" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:lime_wool"), 1, 5),
                LootEntry(Identifier("minecraft:lime_terracotta"), 2, 5)
            )
        ),
        "rock" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:gray_wool"), 1, 5),
                LootEntry(Identifier("minecraft:gray_terracotta"), 2, 5)
            )
        ),
        "ghost" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:black_wool"), 1, 5),
                LootEntry(Identifier("minecraft:black_terracotta"), 2, 5)
            )
        ),
        "dragon" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:cyan_wool"), 1, 5),
                LootEntry(Identifier("minecraft:cyan_terracotta"), 2, 5)
            )
        ),
        "dark" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:black_wool"), 1, 5),
                LootEntry(Identifier("minecraft:black_terracotta"), 2, 5)
            )
        ),
        "steel" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:light_gray_wool"), 1, 5),
                LootEntry(Identifier("minecraft:light_gray_terracotta"), 2, 5)
            )
        ),
        "fairy" to LootTable(
            listOf(
                LootEntry(Identifier("minecraft:pink_wool"), 1, 5),
                LootEntry(Identifier("minecraft:pink_terracotta"), 2, 5)
            )
        )
    )


    var lootTables: Map<String, LootTable> = defaultLootTables

    private val file = File("config/pebbles-cobbleddrop/loot_tables.json")
    private val messageFile = File("config/pebbles-cobbleddrop/message.json")

    init {
        if (file.exists()) {
            load()
        } else {
            save()
        }

        if (messageFile.exists()) {
            loadMessage()
        } else {
            saveMessage()
        }

    }

    fun load() {
        FileReader(file).use { reader ->
            val typeToken = object : TypeToken<Map<String, LootTable>>() {}.type
            val lootTablesFromJson: Map<String, LootTable> = gson.fromJson(reader, typeToken)
            lootTables = lootTablesFromJson
        }
    }

    fun save() {

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }

        FileWriter(file).use { writer ->
            gson.toJson(lootTables, writer)
        }
    }

    fun loadMessage() {
        FileReader(messageFile).use { reader ->
            val typeToken = object : TypeToken<String>() {}.type
            val messageFromJson: String = gson.fromJson(reader, typeToken)

            CobbledItemDrop.lootMessage = messageFromJson
        }
    }

    fun saveMessage() {

        if (!messageFile.exists()) {
            messageFile.parentFile.mkdirs()
            messageFile.createNewFile()
        }

        FileWriter(messageFile).use { writer ->
            gson.toJson(CobbledItemDrop.lootMessage, writer)
        }
    }

    fun rollLootTable(primaryType: String, secondaryType: String?): LootEntry? {
        val random = Random()

        // Choose the loot table for the primary or secondary type (50/50 chance if secondary type exists)
        val typeToUse = if (secondaryType != null && random.nextBoolean()) secondaryType else primaryType
        val lootTable = lootTables[typeToUse] ?: return null

        // Get the multiplier for the selected type from ElementalTypeMultipliers object
        val multiplier = ElementalTypeMultipliers.getMultiplier(typeToUse)

        // Calculate the total weight of all items in the loot table
        val totalWeight = lootTable.entries.sumOf { it.weight }

        // Select a random number between 0 (inclusive) and totalWeight (exclusive)
        val randomWeight = random.nextInt(totalWeight)

        // Find the item that corresponds to this random weight
        var cumulativeWeight = 0
        for (lootItem in lootTable.entries) {
            cumulativeWeight += lootItem.weight
            if (cumulativeWeight > randomWeight) {
                // Multiply the loot item amount by the multiplier
                val boostedAmount = (lootItem.amount * multiplier).toInt()
                // Return a new loot item with the boosted amount
                return LootEntry(lootItem.identifier, boostedAmount, lootItem.weight, lootItem.nbt, lootItem.name)
            }
        }

        // This should never happen if the loot table is constructed correctly
        return null
    }


    data class LootEntry(
        val identifier: Identifier,
        val amount: Int,
        val weight: Int,
        val nbt: String? = "",
        val name: String? = ""
    )

    data class LootTable(
        val entries: List<LootEntry>
    )

}
