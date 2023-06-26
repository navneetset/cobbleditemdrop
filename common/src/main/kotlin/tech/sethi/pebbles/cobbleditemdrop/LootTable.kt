package tech.sethi.pebbles.cobbleditemdrop

import net.minecraft.util.Identifier
import kotlin.random.Random

data class LootTable(val items: List<Identifier>) {
    fun rollForLoot(): Identifier = items[Random.nextInt(items.size)]
}
