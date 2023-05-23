package tech.sethi.pebbles.cobbleditemdrop

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.EntityEvent
import dev.architectury.event.events.common.LifecycleEvent
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.apache.logging.log4j.LogManager

object CobbledItemDrop {
    const val MOD_ID = "cobbleditemdrop"
    val LOGGER = LogManager.getLogger()

    val rewardTiers = listOf(
        RewardTier(1, 25, 3, 5),
        RewardTier(26, 50, 5, 7),
        RewardTier(51, 75, 7, 9),
        RewardTier(76, 100, 9, 12)
    )

    private fun getRewardForKill(): Int {
        return (1..3).random()
    }

    private fun getRewardForBattleVictory(pokemonLevel: Int): Int {
        val rewardTier = getRewardTierForLevel(pokemonLevel)
        return (rewardTier.battleVictoryRewardMin..rewardTier.battleVictoryRewardMax).random()
    }

    private fun getRewardTierForLevel(level: Int): RewardTier {
        return rewardTiers.find { level in it.minLevel..it.maxLevel } ?: rewardTiers.last()
    }

    @JvmStatic
    fun init() {
        LOGGER.info("Pebble's Cobbled Item Drop Initialized!")

        LifecycleEvent.SERVER_STARTED.register { server ->
            EntityEvent.LIVING_DEATH.register(EntityEvent.LivingDeath { entity: Entity, source: DamageSource ->
                if (source.attacker is PlayerEntity && "cobblemon.pokemon" in entity.type.toString().lowercase()) {
                    val player = source.attacker as PlayerEntity
                    player.sendMessage(
                        Text.literal(
                            "You shamelessly slaughtered a wild " + entity.name.string + "."
                        ).formatted(Formatting.GREEN), false
                    )

                    val pebbles = getRewardForKill()
                    val pebbleItem = Items.STONE_BUTTON
                    val pebbleItemStack = ItemStack(pebbleItem, 1)
                    val nbt = pebbleItemStack.orCreateNbt
                    pebbleItemStack.setCustomName(Text.literal("$pebbles Pebble").formatted(Formatting.GOLD))

                    nbt.putInt("Pebbles", pebbles)

                    player.inventory.offerOrDrop(pebbleItemStack)

                    player.sendMessage(
                        Text.literal(
                            "You looted $pebbles pebbles from its corpse."
                        ).formatted(Formatting.GOLD), false
                    )
                }

                EventResult.pass() // Continue with the normal death process
            })

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
                    val reward = getRewardForBattleVictory(defeatedPokemon.level)

                    val pebbleItem = Items.STONE_BUTTON
                    val pebbleItemStack = ItemStack(pebbleItem, 1)
                    val nbt = pebbleItemStack.orCreateNbt
                    pebbleItemStack.setCustomName(Text.literal("$reward Pebbles").formatted(Formatting.GOLD))

                    nbt.putInt("Pebbles", reward)

                    player?.inventory?.offerOrDrop(pebbleItemStack)

                    player?.sendMessage(
                        Text.literal("You defeated wild ${defeatedPokemon.species.name}.")
                            .formatted(Formatting.GREEN), false
                    )

                    player?.sendMessage(
                        Text.literal("You looted $reward pebbles from its corpse.")
                            .formatted(Formatting.GOLD), false
                    )
                }
                EventResult.pass()
            }
        }
    }

    data class RewardTier(
        val minLevel: Int,
        val maxLevel: Int,
        val battleVictoryRewardMin: Int,
        val battleVictoryRewardMax: Int
    )
}