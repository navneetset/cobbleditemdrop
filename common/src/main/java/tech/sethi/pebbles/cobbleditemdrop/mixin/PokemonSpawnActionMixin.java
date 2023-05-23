package tech.sethi.pebbles.cobbleditemdrop.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction;
import kotlin.random.Random;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokemonSpawnAction.class, remap = false)
public abstract class PokemonSpawnActionMixin {
    @Shadow
    private PokemonProperties props;

    @Inject(method = "createEntity", at = @At("HEAD"), cancellable = true)
    private void modifyShinyRate(CallbackInfoReturnable<Entity> cir) {
        float shinyRate = Cobblemon.config.getShinyRate();
        if (shinyRate >= 1 && Random.Default.nextFloat() < 1 / shinyRate) {
            props.setShiny(true);
        }
    }
}
