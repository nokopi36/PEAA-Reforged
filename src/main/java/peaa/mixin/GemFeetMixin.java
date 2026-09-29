package peaa.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import moze_intel.projecte.gameObjs.items.armor.GemFeet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import peaa.gameObjs.items.RingOfTheSpace;

/**
 * Stops the Gem Boots from boosting airborne movement while a Ring of the Space is carried
 * (SPEC 3.6.1).
 *
 * <p>The original reached this by replacing ProjectE's Gem Boots with its own subclass through ASM
 * (peaa.gameObjs.items.armor.GemFeetPEAA:46-47). ProjectE's item cannot be swapped out on 1.21.1, and
 * its API exposes nothing for this, so a mixin is the remaining option.
 *
 * <p>The boost is the {@code if (!flying)} block of {@code GemFeet#inventoryTick}, which decides what
 * to do purely from {@code player.zza}:
 * <pre>
 * if (player.zza &lt; 0)      { ...slow down... }
 * else if (player.zza &gt; 0) { ...speed up...  }
 * </pre>
 * Reporting {@code zza} as 0 makes neither branch run. Nothing else in the method reads it, so the
 * fall damping and the jump assist are left exactly as they were -- rather than rewriting ProjectE's
 * logic, this only changes what that one condition sees.
 *
 * <p>The other half of the original's change, suppressing flight from the gem armour, has no target
 * here: on 1.21.1 only the SWRG and the Arcana Ring grant flight
 * (moze_intel.projecte.handlers.InternalAbilities:80-85), so gem armour never granted it to suppress.
 */
@Mixin(GemFeet.class)
public class GemFeetMixin {

	// The field is declared on LivingEntity, but javac writes the owner of the receiver's static type,
	// and ProjectE reads it through a Player local -- so the descriptor has to say Player.
	@ModifyExpressionValue(
			method = "inventoryTick",
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;zza:F", opcode = org.objectweb.asm.Opcodes.GETFIELD),
			require = 2
	)
	private float peaa$suppressBoostWhileCarryingRing(float forwardImpulse, ItemStack stack, Level level, Entity entity, int slot, boolean isHeld) {
		if (entity instanceof Player player && RingOfTheSpace.suppressesGemBootsBoost(player)) {
			return 0;
		}
		return forwardImpulse;
	}

}
