package com.nokopi.peaareforged.events;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.items.RingOfTheSpace;

/**
 * Cancels fall damage for anyone carrying a charged Ring of the Space (SPEC 3.5.7).
 *
 * <p>Per decision 9 this matches the original: simply having the ring on the hotbar with EMC in it is
 * enough, whether or not flight is switched on ({@code FlightEventHookPEAA.java:58-68}).
 */
@EventBusSubscriber(modid = PEAACore.MODID)
public class RingFallHandler {

	@SubscribeEvent
	static void onFall(LivingFallEvent event) {
		if (event.getEntity() instanceof Player player && hasChargedRing(player)) {
			event.setCanceled(true);
		}
	}

	private static boolean hasChargedRing(Player player) {
		for (int slot = 0; slot < net.minecraft.world.entity.player.Inventory.getSelectionSize(); slot++) {
			var stack = player.getInventory().getItem(slot);
			// Read the banked EMC rather than asking whether fuel could be consumed, so that landing
			// never quietly eats items out of the player's inventory.
			if (stack.getItem() instanceof RingOfTheSpace && RingOfTheSpace.getStoredEmc(stack) > 0) {
				return true;
			}
		}
		return false;
	}

	private RingFallHandler() {
	}
}
