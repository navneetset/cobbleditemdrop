package tech.sethi.pebbles.cobbleditemdrop

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object PebblesDropCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("pebblesdrop")
                .requires { it.hasPermissionLevel(2) }
                .then(
                    CommandManager.literal("boost")
                        .then(
                            CommandManager.argument("type", StringArgumentType.word())
                                .suggests { _, builder ->
                                    ElementalTypeMultipliers.getAllTypeNames().forEach { typeName ->
                                        builder.suggest(typeName)
                                    }
                                    builder.buildFuture()
                                }
                                .then(
                                    CommandManager.argument("multiplier", DoubleArgumentType.doubleArg())
                                        .then(
                                            CommandManager.argument("duration", IntegerArgumentType.integer())
                                                .executes { context ->
                                                    val typeName = StringArgumentType.getString(context, "type")
                                                    val multiplier = DoubleArgumentType.getDouble(context, "multiplier")
                                                    val duration = IntegerArgumentType.getInteger(context, "duration")
                                                    executeBoost(context.source, typeName, multiplier, duration)
                                                }
                                        )
                                )
                        )
                )
                .then(
                    CommandManager.literal("boostall")
                        .then(
                            CommandManager.argument("multiplier", DoubleArgumentType.doubleArg())
                                .then(
                                    CommandManager.argument("duration", IntegerArgumentType.integer())
                                        .executes { context ->
                                            val multiplier = DoubleArgumentType.getDouble(context, "multiplier")
                                            val duration = IntegerArgumentType.getInteger(context, "duration")
                                            executeBoostAll(context.source, multiplier, duration)
                                        }
                                )
                        )
                )
                .then(
                    CommandManager.literal("reload")
                        .executes {
                            executeReload(it.source)
                        }
                )
                .then(
                    CommandManager.literal("checkboosts")
                        .executes {
                            ElementalTypeMultipliers.getCurrentBoosts(it)
                            1
                        }
                )
                .then(
                    CommandManager.literal("reset")
                        .then(
                            CommandManager.argument("type", StringArgumentType.word())
                                .suggests { _, builder ->
                                    ElementalTypeMultipliers.getAllTypeNames().forEach { typeName ->
                                        builder.suggest(typeName)
                                    }
                                    builder.buildFuture()
                                }
                                .executes { context ->
                                    val typeName = StringArgumentType.getString(context, "type")
                                    resetTypeMultiplier(typeName)
                                    context.source.sendFeedback(Text.literal("Reset multiplier for $typeName."), true)
                                    1
                                }
                        )
                )
                .then(
                    CommandManager.literal("resetall")
                        .executes { context ->
                            resetAllTypeMultipliers()
                            context.source.sendFeedback(Text.literal("Reset all multipliers."), true)
                            1
                        }
                )

        )
    }

    private fun executeBoost(source: ServerCommandSource, typeName: String, multiplier: Double, duration: Int): Int {
        // Verify the type name is valid
        if (!ElementalTypeMultipliers.getAllTypeNames().contains(typeName)) {
            source.sendError(Text.literal("Invalid type name: $typeName"))
            return 0
        }

        // Apply the drop rate multiplier
        ElementalTypeMultipliers.setMultiplier(typeName, multiplier, duration)

        // Set up a task to reset the drop rate multiplier after the specified duration
        val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        executor.schedule(
            {
                ElementalTypeMultipliers.resetMultiplier(typeName)
            },
            duration.toLong(),
            TimeUnit.MINUTES
        )

        // Send a confirmation message
        source.sendFeedback(Text.literal("Boosted drop rate for $typeName by $multiplier for $duration minutes."), true)
        return 1
    }

    private fun executeBoostAll(source: ServerCommandSource, multiplier: Double, duration: Int): Int {
        // Apply the drop rate multiplier
        ElementalTypeMultipliers.setAllMultipliers(multiplier, duration)

        // Set up a task to reset the drop rate multiplier after the specified duration
        val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        executor.schedule(
            {
                ElementalTypeMultipliers.resetAllMultipliers()
            },
            duration.toLong(),
            TimeUnit.MINUTES
        )

        // Send a confirmation message
        source.sendFeedback(Text.literal("Boosted drop rate for all types by $multiplier for $duration minutes."), true)
        return 1
    }

    private fun resetTypeMultiplier(typeName: String) {
        if (!ElementalTypeMultipliers.getAllTypeNames().contains(typeName)) {
            return
        }
        ElementalTypeMultipliers.resetMultiplier(typeName)
    }

    private fun resetAllTypeMultipliers() {
        ElementalTypeMultipliers.resetAllMultipliers()
    }

    private fun executeReload(source: ServerCommandSource): Int {
        // Reload the loot tables from the config file
        LootTableManager.load()
        LootTableManager.loadMessage()

        // Send a confirmation message
        source.sendFeedback(Text.literal("Reloaded loot tables from config."), true)
        return 1
    }

}