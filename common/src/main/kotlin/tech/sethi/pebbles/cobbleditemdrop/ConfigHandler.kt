package tech.sethi.pebbles.cobbleditemdrop

import java.io.File

object ConfigHandler {

    val configFile = File("config/pebbles-cobbleditemdrop/config.json")


    data class Config(
        val tierOne: CobbledItemDrop.RewardTier = CobbledItemDrop.RewardTier(1, 25, 5, 8),
        val tierTwo: CobbledItemDrop.RewardTier = CobbledItemDrop.RewardTier(26, 50, 8, 12),
        val tierThree: CobbledItemDrop.RewardTier = CobbledItemDrop.RewardTier(51, 75, 12, 18),
        val tierFour: CobbledItemDrop.RewardTier = CobbledItemDrop.RewardTier(76, 100, 18, 25)
    )
}