package tech.sethi.pebbles.cobbleditemdrop

import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.util.concurrent.TimeUnit

object ElementalTypeMultipliers {

    // Initialize a map with all the type names as keys and 1.0 as the default value
    private val dropMultipliers = mutableMapOf<String, Double>().apply {
        val typeNames = listOf(
            "normal", "fire", "water", "grass", "electric", "ice", "fighting",
            "poison", "ground", "flying", "psychic", "bug", "rock", "ghost",
            "dragon", "dark", "steel", "fairy"
        )

        typeNames.forEach { typeName ->
            this[typeName] = 1.0
        }
    }

    private val boostEndTimes = mutableMapOf<String, Long>()

    fun getMultiplier(typeName: String): Double = dropMultipliers[typeName] ?: 1.0

    fun setMultiplier(typeName: String, multiplier: Double, duration: Int) {
        dropMultipliers[typeName] = multiplier
        val endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(duration.toLong())
        boostEndTimes[typeName] = endTime
    }

    fun setAllMultipliers(multiplier: Double, duration: Int) {
        dropMultipliers.keys.forEach { typeName ->
            setMultiplier(typeName, multiplier, duration)
        }
    }

    fun resetMultiplier(typeName: String) {
        dropMultipliers[typeName] = 1.0
        boostEndTimes.remove(typeName)
    }

    fun getRemainingTime(typeName: String): Long {
        val currentTime = System.currentTimeMillis()
        return (boostEndTimes[typeName] ?: currentTime) - currentTime
    }

    fun resetAllMultipliers() {
        dropMultipliers.keys.forEach { typeName ->
            resetMultiplier(typeName)
        }
    }

    fun getCurrentBoosts(ctx: CommandContext<ServerCommandSource>) {
        ctx.source.player?.sendMessage(PM.returnStyledText("<yellow>Current Boosts:</yellow>"), false)
        CobbledItemDrop.LOGGER.info("Current Boosts:")

        dropMultipliers.forEach { (typeName, multiplier) ->
            if (multiplier > 1.0) {
                val remainingMillis = getRemainingTime(typeName)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
                ctx.source.player?.sendMessage(
                    PM.returnStyledText("<green>$typeName: $multiplier (${minutes}m ${seconds}s remaining)"),
                    false
                )
                CobbledItemDrop.LOGGER.info("$typeName: $multiplier (${minutes}m ${seconds}s remaining)")
            } else {
                ctx.source.player?.sendMessage(Text.literal("$typeName: $multiplier"), false)
                CobbledItemDrop.LOGGER.info("$typeName: $multiplier")
            }
        }
    }

    fun getAllTypeNames(): Set<String> = dropMultipliers.keys
}