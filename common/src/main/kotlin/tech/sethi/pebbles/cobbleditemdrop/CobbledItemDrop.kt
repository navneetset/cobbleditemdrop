package tech.sethi.pebbles.cobbleditemdrop

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.EntityEvent
import dev.architectury.event.events.common.LifecycleEvent
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.apache.logging.log4j.LogManager
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object CobbledItemDrop {
    const val MOD_ID = "cobbleditemdrop"
    val LOGGER = LogManager.getLogger()

    val rewardTiers = listOf(
        RewardTier(1, 25, 5, 8),
        RewardTier(26, 50, 8, 12),
        RewardTier(51, 75, 12, 18),
        RewardTier(76, 100, 18, 25)
    )

    private fun getRewardForKill(): Int {
        return ((1..3).random() * dropMultiplier).roundToInt()
    }

    private fun getRewardForBattleVictory(pokemonLevel: Int): Int {
        val rewardTier = getRewardTierForLevel(pokemonLevel)
        return ((rewardTier.battleVictoryRewardMin..rewardTier.battleVictoryRewardMax).random() * dropMultiplier).roundToInt()
    }


    private fun getRewardTierForLevel(level: Int): RewardTier {
        return rewardTiers.find { level in it.minLevel..it.maxLevel } ?: rewardTiers.last()
    }

    @JvmStatic
    fun init() {
        LOGGER.info("Pebble's Cobbled Item Drop Initialized!")

        CommandRegistrationEvent.EVENT.register { dispatcher, dedicated, _ ->
            registerCommands(dispatcher)
        }

        LifecycleEvent.SERVER_STARTED.register { server ->
            EntityEvent.LIVING_DEATH.register(EntityEvent.LivingDeath { entity: Entity, source: DamageSource ->
                if (source.attacker is PlayerEntity && "cobblemon.pokemon" in entity.type.toString().lowercase()) {
                    val player = source.attacker as PlayerEntity
                    player.sendMessage(
                        Text.literal(
                            "You shamelessly slaughtered a wild " + entity.name.string + "."
                        ).formatted(Formatting.GRAY), false
                    )

                    val pebbles = getRewardForKill()
                    val pebbleItem = Items.FEATHER
                    val pebbleItemStack = ItemStack(pebbleItem, 1)
                    val nbt = pebbleItemStack.orCreateNbt
                    pebbleItemStack.setCustomName(Text.literal("$pebbles Pebble").formatted(Formatting.GOLD))

                    nbt.putInt("Pebbles", pebbles)
                    nbt.putInt("CustomModelData", 8)

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
                if (event.battle.isPvW && !event.losers.first().pokemonList.first().originalPokemon.isPlayerOwned()) {
                    val level = event.losers.first().pokemonList.first().originalPokemon.level
                    val reward = getRewardForBattleVictory(level)

                    val pebbleItem = Items.FEATHER
                    val pebbleItemStack = ItemStack(pebbleItem, 1)
                    val nbt = pebbleItemStack.orCreateNbt
                    pebbleItemStack.setCustomName(Text.literal("$reward Pebble").formatted(Formatting.GOLD))

                    nbt.putInt("Pebbles", reward)
                    nbt.putInt("CustomModelData", 8)

                    event.winners.first().getPlayerUUIDs().forEach { uuid ->
                        val player = server.playerManager.getPlayer(uuid)
                        player?.inventory?.offerOrDrop(pebbleItemStack)

                        player?.sendMessage(
                            Text.literal("You looted $reward pebbles from its corpse.")
                                .formatted(Formatting.GOLD), false
                        )
                    }
                }

//                val actors = event.battle.actors
//                var defeatedActor: BattleActor? = null
//                var playerActor: BattleActor? = null
//                if (actors.all { !it.battle.isPvP }) {
//                    for (actor in actors) {
//                        val player = server.playerManager.getPlayer(actor.uuid)
//                        if (player != null && actor.pokemonList.any { it.health > 0 }) {
//                            // This actor is a player and won the battle
//                            playerActor = actor
//                        } else if (player == null) {
//                            // This actor might be a wild Pokémon
//                            defeatedActor = actor
//                        }
//                    }
//                }
//                if (defeatedActor != null && playerActor != null && defeatedActor.pokemonList.all { it.health <= 0 }) {
//                    // This is a wild Pokémon that was defeated by the player
//                    val defeatedPokemon = defeatedActor.pokemonList.first().originalPokemon
//                    val player = server.playerManager.getPlayer(playerActor.uuid)
//                    val reward = getRewardForBattleVictory(defeatedPokemon.level)
//
//                    val pebbleItem = Items.FEATHER
//                    val pebbleItemStack = ItemStack(pebbleItem, 1)
//                    val nbt = pebbleItemStack.orCreateNbt
//                    pebbleItemStack.setCustomName(Text.literal("$reward Pebbles").formatted(Formatting.GOLD))
//
//                    nbt.putInt("Pebbles", reward)
//                    nbt.putInt("CustomModelData", 8)
//
//                    player?.inventory?.offerOrDrop(pebbleItemStack)
//
//                    player?.sendMessage(
//                        Text.literal("You defeated wild ${defeatedPokemon.species.name}.")
//                            .formatted(Formatting.GRAY), false
//                    )
//
//                    player?.sendMessage(
//                        Text.literal("You looted $reward pebbles from its corpse.")
//                            .formatted(Formatting.GOLD), false
//                    )
//                }
                EventResult.pass()
            }
        }
    }

    var dropMultiplier = 1.0
    private var boostTask: ScheduledFuture<*>? = null
    private val executor = Executors.newSingleThreadScheduledExecutor()

    fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("pebblesdrop")
                .requires { it.hasPermissionLevel(2) }
                .then(
                    CommandManager.literal("boost")
                        .then(
                            CommandManager.argument("multiplier", DoubleArgumentType.doubleArg(0.0))
                                .then(
                                    CommandManager.argument("duration", IntegerArgumentType.integer(1))
                                        .executes { ctx: CommandContext<ServerCommandSource> ->
                                            setDropMultiplier(
                                                ctx,
                                                DoubleArgumentType.getDouble(ctx, "multiplier"),
                                                IntegerArgumentType.getInteger(ctx, "duration")
                                            )
                                        }
                                )
                        )
                )
                .then(
                    CommandManager.literal("reset")
                        .executes { ctx: CommandContext<ServerCommandSource> ->
                            resetMultiplier(ctx)
                        }
                )
                .then(
                    CommandManager.literal("check")
                        .executes { ctx: CommandContext<ServerCommandSource> ->
                            checkBoost(ctx)
                        }
                )
        )
    }

    private fun setDropMultiplier(
        ctx: CommandContext<ServerCommandSource>,
        multiplier: Double,
        duration: Int
    ): Int {
        dropMultiplier = multiplier
        boostTask?.cancel(false)  // Cancel the previous task if it exists
        LOGGER.info("Pebble drop multiplier set to $multiplier for $duration minutes.")

        // Schedule a task to reset the multiplier after the given duration
        boostTask = executor.schedule(
            {
                dropMultiplier = 1.0
                LOGGER.info("Pebble drop multiplier reset to 1.")
            },
            duration.toLong(),
            TimeUnit.MINUTES
        )

        ctx.source.sendFeedback(
            { Text.literal("Pebble drop multiplier set to $multiplier for $duration minutes.") },
            false
        )

        return 1  // Return 1 to indicate the command executed successfully
    }

    private fun resetMultiplier(ctx: CommandContext<ServerCommandSource>): Int {
        dropMultiplier = 1.0
        boostTask?.cancel(false)
        boostTask = null
        LOGGER.info("Pebble drop multiplier manually reset to 1.")
        ctx.source.sendFeedback({ Text.literal("Pebble drop multiplier manually reset to 1.") }, false)
        return 1
    }

    private fun checkBoost(ctx: CommandContext<ServerCommandSource>): Int {
        val remainingTime = boostTask?.getDelay(TimeUnit.SECONDS)?.let { TimeUnit.SECONDS.toMinutes(it + 30) }
        if (remainingTime != null && remainingTime > 0) {
            ctx.source.sendFeedback(
                { Text.literal("Active boost: x$dropMultiplier. Remaining time: $remainingTime minutes.") },
                false
            )
        } else {
            ctx.source.sendFeedback({ Text.literal("No active boost.") }, false)
        }
        return 1
    }


    data class RewardTier(
        val minLevel: Int,
        val maxLevel: Int,
        val battleVictoryRewardMin: Int,
        val battleVictoryRewardMax: Int
    )
}