package tech.sethi.pebbles.cobbleditemdrop

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.LifecycleEvent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtHelper
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager

object CobbledItemDrop {
    const val MOD_ID = "cobbleditemdrop"
    val LOGGER = LogManager.getLogger()

    var lootMessage = "You looted {amount} {item} from the defeated {pokemon}."

    @JvmStatic
    fun init() {
        LOGGER.info("Pebble's Cobbled Item Drop Initialized!")

        CommandRegistrationEvent.EVENT.register { dispatcher, dedicated, _ ->
            PebblesDropCommand.register(dispatcher)
        }

        LifecycleEvent.SERVER_STARTED.register { server ->
            CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
                val actors = event.battle.actors
                var defeatedActor: BattleActor? = null
                var playerActor: BattleActor? = null
                if (actors.all { !it.battle.isPvP }) {
                    for (actor in actors) {
                        val player = server.playerManager.getPlayer(actor.uuid)
                        if (player != null && actor.pokemonList.any { it.health > 0 }) {
                            // This actor is a player and won the battle
                            playerActor = actor
                        } else if (player == null) {
                            // This actor might be a wild Pokémon
                            defeatedActor = actor
                        }
                    }
                }
                if (defeatedActor != null && playerActor != null && defeatedActor.pokemonList.all { it.health <= 0 }) {
                    // This is a wild Pokémon that was defeated by the player
                    val defeatedPokemon = defeatedActor.pokemonList.first().originalPokemon
                    val player = server.playerManager.getPlayer(playerActor.uuid)

                    val lootItem = LootTableManager.rollLootTable(
                        defeatedPokemon.primaryType.name,
                        defeatedPokemon.secondaryType?.name
                    )

                    if (lootItem != null) {
                        val item = Registry.ITEM.get(lootItem.identifier)
                        val itemStack = ItemStack(
                            item,
                            lootItem.amount,
                        )

                        val originalName = itemStack.item.name.string

                        if (lootItem.nbt != null && lootItem.nbt != "") {
                            val nbt = NbtHelper.fromNbtProviderString(lootItem.nbt)
                            itemStack.nbt = nbt
                        }

                        if (lootItem.name != null && lootItem.name != "") {
                            itemStack.setCustomName(PM.returnStyledText(lootItem.name))
                        }

                        player?.inventory?.offerOrDrop(itemStack)

                        if (lootItem.name != null && lootItem.name != "") {
                            val itemName = lootItem.name
                            val message = lootMessage
                                .replace("{amount}", lootItem.amount.toString())
                                .replace("{item}", itemName)
                                .replace("{pokemon}", defeatedPokemon.species.name)
                            player?.sendMessage(PM.returnStyledText(message), false)

                        } else {
                            val itemName = Identifier(itemStack.item.translationKey).toString()
                            val message = lootMessage
                                .replace("{amount}", lootItem.amount.toString())
                                .replace("{item}", originalName)
                                .replace("{pokemon}", defeatedPokemon.species.name)
                            player?.sendMessage(PM.returnStyledText(message), false)
                        }

                    } else {
                        player?.sendMessage(
                            Text.literal("The defeated ${defeatedPokemon.species.name} had no loot.")
                                .formatted(Formatting.RED), false
                        )
                    }
                }

                EventResult.pass()
            }
        }
    }
}