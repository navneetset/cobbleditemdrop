package tech.sethi.pebbles.cobbleditemdrop.forge;

import dev.architectury.platform.forge.EventBuses;
import tech.sethi.pebbles.cobbleditemdrop.CobbledItemDrop;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CobbledItemDrop.MOD_ID)
public class CobbledItemDropForge {
    public CobbledItemDropForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(CobbledItemDrop.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        CobbledItemDrop.init();
    }
}