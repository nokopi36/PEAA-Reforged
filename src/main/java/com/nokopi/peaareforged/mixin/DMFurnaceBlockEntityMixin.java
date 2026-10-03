package com.nokopi.peaareforged.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.nokopi.peaareforged.util.InventoryPush;

/**
 * The two things PEAA changed about the matter furnaces: ores always double (SPEC 3.4.4) and the
 * output leaves in every direction but up (SPEC 3.4.6).
 *
 * <p>The original got here sideways: ASM re-parented {@code DMFurnaceTile} onto PEAA's red matter
 * furnace tile, whose {@code smeltItem} doubled any ore unconditionally
 * (com.nokopi.peaareforged.gameObjs.tiles.RMFurnaceTilePEAA:472-475). On 1.21.1 the inheritance runs the other way --
 * {@code RMFurnaceBlockEntity extends DMFurnaceBlockEntity} -- and the rate is a single overridable
 * method, so only that needs changing.
 *
 * <p>Red matter furnaces are unaffected: they override {@code getOreDoubleChance} with 1F already
 * (moze_intel.projecte.gameObjs.block_entities.RMFurnaceBlockEntity:26-28), so this implementation is
 * never reached for them.
 *
 * <p>What is not reproduced is the original's 1.6 EMC per tick in place of 2: EMC is a {@code long}
 * on 1.21.1, and the constant is inlined into the tick method anyway. See docs/DEVIATIONS.md D-021.
 */
@Mixin(DMFurnaceBlockEntity.class)
public class DMFurnaceBlockEntityMixin {

	@ModifyReturnValue(method = "getOreDoubleChance()F", at = @At("RETURN"))
	private float peaa$alwaysDoubleOres(float original) {
		return 1.0F;
	}

	/**
	 * SPEC 3.4.6: the output goes out in all five directions that are not up.
	 *
	 * <p>ProjectE only pushes straight down, through a private {@code pushToInventories} whose target
	 * is a {@code BlockCapabilityCache} fixed on the block below (DMFurnaceBlockEntity:155, 319-332).
	 * That method is private and returns early in three places, so this hooks the tail of
	 * {@code tickServer} instead: it is public, static, has no early return of its own, and calls
	 * {@code pushToInventories} unconditionally just before the end. Landing after it keeps downwards
	 * first, the order the original's direction loop had.
	 *
	 * <p>Red matter furnaces inherit {@code tickServer} unchanged, so they are covered too -- which is
	 * where the original ended up as well, by re-parenting the dark matter tile onto PEAA's red matter
	 * one.
	 */
	@Inject(method = "tickServer", at = @At("TAIL"))
	private static void peaa$pushOutputSideways(Level level, BlockPos pos, BlockState state, DMFurnaceBlockEntity furnace,
			CallbackInfo ci) {
		InventoryPush.pushFurnaceOutputSideways(level, pos, furnace);
	}
}
