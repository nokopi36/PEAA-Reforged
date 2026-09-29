package peaa.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import peaa.PEAACore;
import peaa.gameObjs.items.RingOfTheSpace;
import peaa.network.SetFlyingPayload;

/**
 * Client-side flight for the Ring of the Space (SPEC 3.5.6).
 *
 * <p>The original hooked {@code LivingUpdateEvent} and re-ran {@code updatePlayerMoveState()} by hand
 * to read the jump key twice in one tick. 1.21.1 has no equivalent, so the double tap is detected
 * from the rising edge of {@link net.minecraft.client.player.Input#jumping} across ticks instead --
 * same 7-tick window, same result.
 *
 * <p>Movement is applied to the local player's delta movement; the server picks it up through the
 * normal player movement packets. This deliberately does not use vanilla creative flight, matching
 * the original, which switched itself off whenever creative flight was available.
 */
@EventBusSubscriber(modid = PEAACore.MODID, value = Dist.CLIENT)
public class RingFlightController {

	/** SPEC 3.5.2: how long after the first jump press a second one still counts as a double tap. */
	private static final int TOGGLE_WINDOW_TICKS = 7;
	/** SPEC 3.5.6: vertical speed applied per tick while flying. */
	private static final double VERTICAL_SPEED = 0.4;
	/** SPEC 3.5.6: horizontal multipliers, the larger one while sneaking. */
	private static final float HORIZONTAL_MULTIPLIER = 10.0F;
	private static final float SNEAK_HORIZONTAL_MULTIPLIER = 35.0F;

	private static int toggleTimer;
	private static boolean wasJumping;

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			reset();
			return;
		}
		int slot = findRingSlot(player);
		if (slot < 0) {
			reset();
			return;
		}
		ItemStack ring = player.getInventory().getItem(slot);

		// Creative flight, from creative mode or another mod, takes priority; the original bowed out
		// in exactly the same situation (ClientProxy.java:40-45).
		if (player.getAbilities().flying || player.getAbilities().mayfly) {
			if (RingOfTheSpace.isFlying(ring)) {
				setFlying(player, slot, ring, false);
			}
			return;
		}

		boolean flying = RingOfTheSpace.isFlying(ring);
		if (consumeJumpDoubleTap(player)) {
			flying = !flying;
			setFlying(player, slot, ring, flying);
		}
		if (flying && player.onGround()) {
			// Landing cancels flight, as in the original.
			setFlying(player, slot, ring, false);
			return;
		}
		if (flying) {
			applyFlightMovement(player, ring);
		}
	}

	/** True on the second jump press inside the toggle window. */
	private static boolean consumeJumpDoubleTap(LocalPlayer player) {
		boolean jumping = player.input.jumping;
		boolean pressed = jumping && !wasJumping;
		wasJumping = jumping;
		boolean toggled = false;
		if (pressed) {
			if (toggleTimer == 0) {
				toggleTimer = TOGGLE_WINDOW_TICKS;
			} else {
				toggled = true;
				toggleTimer = 0;
			}
		}
		if (toggleTimer > 0) {
			toggleTimer--;
		}
		return toggled;
	}

	private static void applyFlightMovement(LocalPlayer player, ItemStack ring) {
		boolean sneaking = player.input.shiftKeyDown;
		boolean held = player.getMainHandItem() == ring || player.getOffhandItem() == ring;

		// Vertical: hold jump to climb, sneak to descend. Holding the ring disables descending,
		// because sneak is then the speed boost modifier (SPEC 3.5.6).
		double vertical = 0;
		if (sneaking && !held) {
			vertical -= VERTICAL_SPEED;
		}
		if (player.input.jumping) {
			vertical += VERTICAL_SPEED;
		}

		float forward = player.input.forwardImpulse;
		float strafe = player.input.leftImpulse;
		float speed = RingOfTheSpace.getSpeed(getCharge(ring));

		Vec3 movement = player.getDeltaMovement();
		if (forward != 0 || strafe != 0 || speed == RingOfTheSpace.BASE_SPEED) {
			// Kill inherited horizontal drift so the ring's own speed is what the player feels.
			player.setDeltaMovement(0, vertical, 0);
		} else {
			player.setDeltaMovement(movement.x, vertical, movement.z);
		}
		float multiplier = sneaking ? SNEAK_HORIZONTAL_MULTIPLIER : HORIZONTAL_MULTIPLIER;
		player.moveRelative(speed * multiplier, new Vec3(strafe, 0, forward));
	}

	private static int getCharge(ItemStack ring) {
		return ring.getItem() instanceof RingOfTheSpace item ? item.getCharge(ring) : 0;
	}

	private static void setFlying(LocalPlayer player, int slot, ItemStack ring, boolean flying) {
		if (RingOfTheSpace.isFlying(ring) == flying) {
			return;
		}
		// Set it locally for an immediate response, then let the server confirm.
		RingOfTheSpace.setFlying(ring, flying);
		PacketDistributor.sendToServer(new SetFlyingPayload(slot, flying));
	}

	/**
	 * Finds the ring to use, preferring one that is already flying.
	 *
	 * <p>Unlike the original ({@code FlightEventHookPEAA.java:126}) two rings in flight mode do not
	 * throw; the first one simply wins.
	 */
	private static int findRingSlot(@Nullable LocalPlayer player) {
		if (player == null) {
			return -1;
		}
		int found = -1;
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!(stack.getItem() instanceof RingOfTheSpace)) {
				continue;
			}
			if (RingOfTheSpace.isFlying(stack)) {
				return slot;
			}
			if (found < 0) {
				found = slot;
			}
		}
		return found;
	}

	private static void reset() {
		toggleTimer = 0;
		wasJumping = false;
	}

	private RingFlightController() {
	}
}
