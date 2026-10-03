package com.nokopi.peaareforged.gametest;

import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.items.RingOfTheSpace;
import com.nokopi.peaareforged.gameObjs.registries.PEAAItems;

/**
 * Checks the server-side half of the Ring of the Space: the EMC numbers from SPEC 3.5.2 and the
 * teleport target rule from SPEC 3.5.4.
 *
 * <p>The flight movement itself lives on the client ({@code com.nokopi.peaareforged.client.RingFlightController}) and
 * cannot run here, so it is on the manual checklist instead.
 */
@GameTestHolder(PEAACore.MODID)
@PrefixGameTestTemplate(false)
public class RingGameTests {

	private static final String TEMPLATE = "empty";
	private static final long START_EMC = 100_000L;

	/**
	 * A ring has to survive being written to disk at every combination of its state.
	 *
	 * <p>Regression test. ProjectE's persistent component codecs reject 0 (their
	 * {@code registerNonNegativeInt} actually uses {@code POSITIVE_INT}), so any component whose zero
	 * value is not declared as the item's prototype crashes the game on save. The ring first shipped
	 * without {@code projecte:charge} declared, which made it unsaveable once the charge was taken
	 * back down to 0.
	 */
	@GameTest(template = TEMPLATE)
	public static void ringSurvivesSaveAtEveryState(GameTestHelper helper) {
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		for (int charge = 0; charge <= RingOfTheSpace.NUM_CHARGES; charge++) {
			for (boolean flying : new boolean[]{false, true}) {
				for (long emc : new long[]{0L, 93L}) {
					ItemStack ring = new ItemStack(PEAAItems.RING_OF_THE_SPACE.get());
					ring.set(moze_intel.projecte.api.PEDataComponents.CHARGE.get(), charge);
					ring.set(PEDataComponentTypes.STORED_EMC.get(), emc);
					RingOfTheSpace.setFlying(ring, flying);

					String state = "charge=" + charge + " flying=" + flying + " emc=" + emc;
					Tag saved;
					try {
						saved = ring.save(registries);
					} catch (RuntimeException e) {
						helper.fail("saving a ring at " + state + " threw " + e);
						return;
					}
					ItemStack loaded = ItemStack.parse(registries, saved).orElse(ItemStack.EMPTY);
					if (loaded.isEmpty()) {
						helper.fail("a ring saved at " + state + " could not be read back");
						return;
					}
					if (loaded.getItem() instanceof RingOfTheSpace item && item.getCharge(loaded) != charge) {
						helper.fail("charge did not survive a save at " + state);
						return;
					}
					if (RingOfTheSpace.isFlying(loaded) != flying) {
						helper.fail("flight mode did not survive a save at " + state);
						return;
					}
					if (RingOfTheSpace.getStoredEmc(loaded) != emc) {
						helper.fail("stored EMC did not survive a save at " + state);
						return;
					}
				}
			}
		}
		helper.succeed();
	}

	/** SPEC 3.5.2: charge 0 drains 0.16 EMC per tick, doubling per level. */
	@GameTest(template = TEMPLATE)
	public static void flightDrainsEmcAtCharge0(GameTestHelper helper) {
		assertFlightDrain(helper, 0, 0.16F);
	}

	@GameTest(template = TEMPLATE)
	public static void flightDrainsEmcAtCharge3(GameTestHelper helper) {
		assertFlightDrain(helper, 3, 1.28F);
	}

	/** A ring that is not flying must not cost anything. */
	@GameTest(template = TEMPLATE)
	public static void idleRingCostsNothing(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack ring = chargedRing(0);
		player.getInventory().setItem(0, ring);
		tickRing(player, ring, 20);
		helper.assertValueEqual(RingOfTheSpace.getStoredEmc(ring), START_EMC, "stored EMC of an idle ring");
		helper.succeed();
	}

	/** SPEC 3.5.3: the ring only works from the hotbar, and switches itself off elsewhere. */
	@GameTest(template = TEMPLATE)
	public static void ringOutsideTheHotbarTurnsItselfOff(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack ring = chargedRing(0);
		RingOfTheSpace.setFlying(ring, true);
		int mainInventorySlot = 20;
		player.getInventory().setItem(mainInventorySlot, ring);
		ring.getItem().inventoryTick(ring, player.level(), player, mainInventorySlot, false);
		if (RingOfTheSpace.isFlying(ring)) {
			helper.fail("a ring off the hotbar stayed in flight mode");
		}
		helper.assertValueEqual(RingOfTheSpace.getStoredEmc(ring), START_EMC, "stored EMC off the hotbar");
		helper.succeed();
	}

	/** SPEC 3.5.5: a dropped ring stops flying. */
	@GameTest(template = TEMPLATE)
	public static void droppedRingStopsFlying(GameTestHelper helper) {
		ItemStack ring = chargedRing(0);
		RingOfTheSpace.setFlying(ring, true);
		var entity = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), 0, 0, 0, ring);
		ring.getItem().onEntityItemUpdate(ring, entity);
		if (RingOfTheSpace.isFlying(ring)) {
			helper.fail("a dropped ring stayed in flight mode");
		}
		helper.succeed();
	}

	/**
	 * SPEC 3.5.4: the target sits on the face of the block being looked at. Looking straight down at
	 * a block puts the player on top of it.
	 */
	@GameTest(template = TEMPLATE)
	public static void teleportTargetsTheBlockFace(GameTestHelper helper) {
		BlockPos stone = new BlockPos(1, 1, 1);
		helper.setBlock(stone, Blocks.STONE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 above = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
		player.setPos(above.x, above.y, above.z);
		player.setXRot(90.0F);
		player.setYRot(0.0F);

		Vec3 target = RingOfTheSpace.findTeleportTarget(player);
		if (target == null) {
			helper.fail("looking straight down at a block found no teleport target");
			return;
		}
		BlockPos absolute = helper.absolutePos(stone);
		Vec3 expected = new Vec3(absolute.getX() + 0.5 + Direction.UP.getStepX(),
				absolute.getY() + Direction.UP.getStepY(),
				absolute.getZ() + 0.5 + Direction.UP.getStepZ());
		if (target.distanceTo(expected) > 1.0E-6) {
			helper.fail("teleport target was " + target + " but should have been " + expected);
		}
		helper.succeed();
	}

	/**
	 * SPEC 3.5.4: hitting the underside of a block drops the player a further block, so they end up
	 * standing under it rather than inside it. This is the one special case in the target formula.
	 *
	 * <p>There is no test for "looking at nothing": a game test structure is boxed in by barriers, so
	 * every direction eventually hits something.
	 */
	@GameTest(template = TEMPLATE)
	public static void teleportUnderACeilingDropsAnExtraBlock(GameTestHelper helper) {
		BlockPos ceiling = new BlockPos(1, 3, 1);
		helper.setBlock(ceiling, Blocks.STONE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 below = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.setPos(below.x, below.y, below.z);
		player.setXRot(-90.0F);
		player.setYRot(0.0F);

		Vec3 target = RingOfTheSpace.findTeleportTarget(player);
		if (target == null) {
			helper.fail("looking straight up at a block found no teleport target");
			return;
		}
		BlockPos absolute = helper.absolutePos(ceiling);
		// face = DOWN, so y = blockY - 1, then one more for the player's own height.
		Vec3 expected = new Vec3(absolute.getX() + 0.5, absolute.getY() - 2.0, absolute.getZ() + 0.5);
		if (target.distanceTo(expected) > 1.0E-6) {
			helper.fail("teleport target was " + target + " but should have been " + expected);
		}
		helper.succeed();
	}

	private static void assertFlightDrain(GameTestHelper helper, int charge, float perTick) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack ring = chargedRing(charge);
		RingOfTheSpace.setFlying(ring, true);
		player.getInventory().setItem(0, ring);

		int ticks = 100;
		tickRing(player, ring, ticks);

		long spent = START_EMC - RingOfTheSpace.getStoredEmc(ring);
		// Fractions below 1 EMC are banked in projecte:unprocessed_emc, so the whole-EMC total can
		// trail the exact figure by at most one.
		long expected = (long) (perTick * ticks);
		if (Math.abs(spent - expected) > 1) {
			helper.fail("charge " + charge + " should have spent about " + expected + " EMC over " + ticks
						+ " ticks but spent " + spent);
		}
		helper.succeed();
	}

	private static void tickRing(Player player, ItemStack ring, int ticks) {
		for (int i = 0; i < ticks; i++) {
			ring.getItem().inventoryTick(ring, player.level(), player, 0, false);
		}
	}

	private static ItemStack chargedRing(int charge) {
		ItemStack ring = new ItemStack(PEAAItems.RING_OF_THE_SPACE.get());
		ring.set(PEDataComponentTypes.STORED_EMC.get(), START_EMC);
		ring.set(moze_intel.projecte.api.PEDataComponents.CHARGE.get(), charge);
		return ring;
	}
}
