package tech.sethi.pebbles.cobbleditemdrop

import com.cobblemon.mod.common.api.pokeball.PokeBalls.gson
import java.io.File

object ConfigLoader {

    val configFile = File("config/pebbles-cobbleditemdrop/config.json")


    var config: Config

    init {
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            config = Config(
                mutableListOf(
                    RewardTier(
                        0,
                        100,
                        0,
                        0,
                        mutableListOf(
                            "give {player_name} minecraft:stone 1")
                    )
                )
            )
            configFile.writeText(gson.toJson(config))
        } else {
            val configJson = configFile.readText()
            config = gson.fromJson(configJson, Config::class.java)
        }
    }

    fun reloadConfig() {
        val configJson = configFile.readText()
        config = gson.fromJson(configJson, Config::class.java)
    }


    data class Config(
        val rewardTiers: MutableList<RewardTier>,
        val victoryMessage: String = "You have defeated {pokemon} for {amount} pebbles!"
    )

    data class RewardTier(
        var minLevel: Int,
        var maxLevel: Int,
        var rewardMin: Int,
        var rewardMax: Int,
        var rewardCommands: MutableList<String>
    )
}