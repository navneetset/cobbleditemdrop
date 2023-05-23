package tech.sethi.pebbles.cobbleditemdrop.fabric;

import net.fabricmc.api.ModInitializer;

public class CobbledItemDropFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        tech.sethi.pebbles.cobbleditemdrop.CobbledItemDrop.init();
    }
}