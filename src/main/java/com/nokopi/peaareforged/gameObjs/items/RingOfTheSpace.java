package com.nokopi.peaareforged.gameObjs.items;

import java.util.List;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.PEKeybind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.nokopi.peaareforged.config.PEAAConfig;
import com.nokopi.peaareforged.gameObjs.registries.PEAADataComponents;

/**
 * Ring of the Space (SPEC 3.5).
 *
 * <p>Two abilities, both paid for in EMC: a creative-like flight mode toggled by double-tapping
 * jump, and a right-click teleport to whatever block the player is looking at.
 *
 * <p>The flight itself is driven client side by {@code com.nokopi.peaareforged.client.RingFlightController}; this class
 * owns the state, the EMC accounting and the teleport.
 */
public class RingOfTheSpace extends ItemPE implements IItemCharge {

	/** Charge levels 0..3, matching the original's {@code super("RingFlightTeleport", (byte) 3)}. */
	public static final int NUM_CHARGES = 3;

	/** SPEC 3.5.2: base flight speed, doubling with each charge level. */
	public static final float BASE_SPEED = 0.022F;
	private static final float[] SPEEDS = {BASE_SPEED, BASE_SPEED * 2, BASE_SPEED * 4, BASE_SPEED * 8};

	/** SPEC 3.5.2: EMC drained per tick while flying, doubling with each charge level. */
	private static final float BASE_FLIGHT_EMC = 0.16F;
	private static final float[] FLIGHT_EMC = {BASE_FLIGHT_EMC, BASE_FLIGHT_EMC * 2, BASE_FLIGHT_EMC * 4, BASE_FLIGHT_EMC * 8};

	/** SPEC 3.5.2: EMC that must be available for the ring to work at all. */
	public static final long FLIGHT_REQUIREMENT = 256;
	/** SPEC 3.5.2: cost of one teleport. */
	public static final long TELEPORT_COST = 3072;
	/** SPEC 3.5.2: how far the teleport reaches. */
	public static final double TELEPORT_RANGE = 30.0;

	/**
	 * Every one of ProjectE's components has to be declared with its zero value here, and that is not
	 * optional: their persistent codecs are all {@code POSITIVE_*} and reject 0 outright, despite the
	 * {@code registerNonNegative*} names (DataComponentTypeDeferredRegister.java:42-55). Declaring the
	 * zero as the item's prototype keeps it out of the serialized patch, so it never reaches the codec.
	 * Miss one and the item becomes unsaveable the moment it reaches that value. ProjectE's own
	 * chargeable items do the same; TimeWatch.java:57-59 is the matching example.
	 */
	public RingOfTheSpace(Properties props) {
		super(props.stacksTo(1)
				.component(PEDataComponentTypes.CHARGE, 0)
				.component(PEDataComponentTypes.STORED_EMC, 0L)
				// ItemPE#removeEmc banks fractional EMC in this component, so it has to exist.
				.component(PEDataComponentTypes.UNPROCESSED_EMC, 0.0)
				.component(PEAADataComponents.FLYING, false));
	}

	@Override
	public int getNumCharges(@NotNull ItemStack stack) {
		return NUM_CHARGES;
	}

	public static float getSpeed(int charge) {
		return SPEEDS[Math.clamp(charge, 0, NUM_CHARGES)];
	}

	public static float getFlightEmcCost(int charge) {
		return FLIGHT_EMC[Math.clamp(charge, 0, NUM_CHARGES)];
	}

	// --- state ----------------------------------------------------------------------------------

	public static boolean isFlying(ItemStack stack) {
		return stack.getOrDefault(PEAADataComponents.FLYING, false);
	}

	public static void setFlying(ItemStack stack, boolean flying) {
		stack.set(PEAADataComponents.FLYING, flying);
	}

	/** EMC banked on the ring itself, without touching the player's fuel. */
	public static long getStoredEmc(ItemStack stack) {
		return stack.getOrDefault(PEDataComponentTypes.STORED_EMC, 0L);
	}

	/**
	 * Whether the ring can currently power flight: it has to be on the hotbar and have EMC, drawing
	 * on the player's fuel if its own buffer has run dry (SPEC 3.5.3).
	 */
	public static boolean canFly(Player player, ItemStack stack, int slot) {
		return isOnHotbar(slot) && hasEmc(player, stack, FLIGHT_REQUIREMENT, true);
	}

	/**
	 * The original only worked from hotbar slots 0-8 (SPEC 3.5.3: {@code invSlot > 8} disabled it).
	 * The off-hand is deliberately not included, to stay with the spec.
	 */
	public static boolean isOnHotbar(int slot) {
		return slot >= 0 && slot < Inventory.getSelectionSize();
	}

	/**
	 * The ring on the player's hotbar, or an empty stack. A ring already in flight wins, matching how
	 * the flight controller picks one.
	 */
	public static ItemStack findOnHotbar(Player player) {
		ItemStack found = ItemStack.EMPTY;
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!(stack.getItem() instanceof RingOfTheSpace)) {
				continue;
			}
			if (isFlying(stack)) {
				return stack;
			}
			if (found.isEmpty()) {
				found = stack;
			}
		}
		return found;
	}

	/**
	 * Whether the Gem Boots should stop boosting airborne movement (SPEC 3.6.1).
	 *
	 * <p>Carrying a ring normally switches the boost off entirely. Turning on
	 * {@code spaceRing.enableHighSpeedMovementAbilityWhenLanding} narrows that to only while the ring
	 * is actually flying, so the boots still help when the ring is idle.
	 *
	 * <p>Lives here rather than in the mixin so the rule can be tested directly and the mixin stays a
	 * one-line hook.
	 */
	public static boolean suppressesGemBootsBoost(Player player) {
		ItemStack ring = findOnHotbar(player);
		if (ring.isEmpty()) {
			return false;
		}
		return !PEAAConfig.ENABLE_HIGH_SPEED_MOVEMENT_WHEN_LANDING.get() || isFlying(ring);
	}

	// --- ticking (SPEC 3.5.3) -------------------------------------------------------------------

	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean isHeld) {
		super.inventoryTick(stack, level, entity, slot, isHeld);
		if (level.isClientSide || !(entity instanceof Player player)) {
			return;
		}
		if (!canFly(player, stack, slot)) {
			setFlying(stack, false);
			return;
		}
		if (isFlying(stack)) {
			removeEmc(stack, getFlightEmcCost(getCharge(stack)));
		}
	}

	/** A ring lying on the ground is not flying anyone anywhere (SPEC 3.5.5). */
	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		if (!entity.level().isClientSide && isFlying(stack)) {
			setFlying(stack, false);
		}
		return false;
	}

	// --- tooltip --------------------------------------------------------------------------------

	/**
	 * Shows which of the four speed settings the ring is on, and what that costs.
	 *
	 * <p>Not in the original, which showed nothing: without this the charge level is invisible, and
	 * since it also decides whether the ring keeps its momentum (SPEC 3.5.6) that is confusing.
	 * The stored EMC is left alone -- ProjectE's own tooltip handler already prints it.
	 */
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip,
			@NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, tooltip, flags);
		int charge = getCharge(stack);
		tooltip.add(Component.translatable("peaa_reforged.tooltip.ring_speed", charge, NUM_CHARGES)
				.withStyle(ChatFormatting.DARK_PURPLE));
		tooltip.add(Component.translatable("peaa_reforged.tooltip.ring_flight_cost", getFlightEmcCost(charge))
				.withStyle(ChatFormatting.DARK_PURPLE));
		if (FMLEnvironment.dist.isClient()) {
			// ClientKeyHelper reaches into client-only classes, so only touch it on that side.
			tooltip.add(Component.translatable("peaa_reforged.tooltip.ring_charge_prompt",
					ClientKeyHelper.getKeyName(PEKeybind.CHARGE)).withStyle(ChatFormatting.GRAY));
		}
	}

	// --- teleport (SPEC 3.5.4) ------------------------------------------------------------------

	@NotNull
	@Override
	public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide) {
			return InteractionResultHolder.success(stack);
		}
		Vec3 target = findTeleportTarget(player);
		if (target == null || !consumeFuel(player, stack, TELEPORT_COST, true)) {
			return InteractionResultHolder.fail(stack);
		}
		player.teleportTo(target.x, target.y, target.z);
		player.resetFallDistance();
		return InteractionResultHolder.success(stack);
	}

	/**
	 * Where a teleport would put the player: on the face of whatever block they are looking at,
	 * within {@link #TELEPORT_RANGE} blocks. Null when they are looking at nothing.
	 */
	@Nullable
	public static Vec3 findTeleportTarget(Player player) {
		HitResult hit = player.pick(TELEPORT_RANGE, 1.0F, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		Direction face = ((BlockHitResult) hit).getDirection();
		double x = pos.getX() + 0.5 + face.getStepX();
		double y = pos.getY() + face.getStepY();
		double z = pos.getZ() + 0.5 + face.getStepZ();
		if (face == Direction.DOWN) {
			// Standing under a ceiling needs one more block of clearance for the player's own height.
			y--;
		}
		return new Vec3(x, y, z);
	}
}
