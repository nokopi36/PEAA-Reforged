package com.nokopi.peaareforged.gametest;

import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.block_entities.RMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.config.PEAAConfig;
import com.nokopi.peaareforged.gameObjs.items.RingOfTheSpace;
import com.nokopi.peaareforged.gameObjs.registries.PEAAItems;
import com.nokopi.peaareforged.util.InventoryPush;

/**
 * Proves the mixins are actually applied, rather than merely compiling.
 *
 * <p>A mixin that fails to apply is silent at build time, so these read the patched behaviour back
 * out of a real block entity.
 */
@GameTestHolder(PEAACore.MODID)
@PrefixGameTestTemplate(false)
public class MixinGameTests {

	private static final String TEMPLATE = "empty";
	private static final BlockPos FURNACE_POS = new BlockPos(1, 1, 1);
	/** The push tests need all six neighbours inside the 5x5x5 structure. */
	private static final BlockPos CENTRE = new BlockPos(2, 2, 2);
	/** Long enough for the furnace to tick, short enough to stay under the default timeout. */
	private static final int SETTLE_TICKS = 5;

	/** SPEC 3.4.4: the Dark Matter Furnace has to double ores every time, not half the time. */
	@GameTest(template = TEMPLATE)
	public static void darkMatterFurnaceAlwaysDoublesOres(GameTestHelper helper) {
		helper.setBlock(FURNACE_POS, PEBlocks.DARK_MATTER_FURNACE.getBlock());
		if (!(helper.getBlockEntity(FURNACE_POS) instanceof DMFurnaceBlockEntity furnace)) {
			helper.fail("no dark matter furnace block entity", FURNACE_POS);
			return;
		}
		float chance = oreDoubleChance(furnace);
		if (chance != 1.0F) {
			helper.fail("dark matter furnace ore doubling chance was " + chance + ", expected 1.0 "
						+ "(the GemFeet/DMFurnace mixin most likely did not apply)");
		}
		helper.succeed();
	}

	/** Red matter furnaces already doubled every time; the mixin must not have changed that. */
	@GameTest(template = TEMPLATE)
	public static void redMatterFurnaceStillDoublesOres(GameTestHelper helper) {
		helper.setBlock(FURNACE_POS, PEBlocks.RED_MATTER_FURNACE.getBlock());
		if (!(helper.getBlockEntity(FURNACE_POS) instanceof RMFurnaceBlockEntity furnace)) {
			helper.fail("no red matter furnace block entity", FURNACE_POS);
			return;
		}
		helper.assertValueEqual(oreDoubleChance(furnace), 1.0F, "red matter furnace ore doubling chance");
		helper.succeed();
	}

	/**
	 * {@code getOreDoubleChance} is protected, so it is read reflectively rather than by widening
	 * ProjectE's access for the sake of a test.
	 */
	private static float oreDoubleChance(DMFurnaceBlockEntity furnace) {
		try {
			var method = DMFurnaceBlockEntity.class.getDeclaredMethod("getOreDoubleChance");
			method.setAccessible(true);
			return (float) method.invoke(furnace);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("could not read getOreDoubleChance", e);
		}
	}

	/**
	 * SPEC 3.6.1, every combination. The boost stops while a ring is carried; turning the config on
	 * narrows that to only while the ring is flying.
	 */
	@GameTest(template = TEMPLATE)
	public static void gemBootsSuppressionFollowsTheRule(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		boolean original = PEAAConfig.ENABLE_HIGH_SPEED_MOVEMENT_WHEN_LANDING.get();
		try {
			PEAAConfig.ENABLE_HIGH_SPEED_MOVEMENT_WHEN_LANDING.set(false);
			check(helper, player, false, false, false, "no ring, config off");
			check(helper, player, true, false, true, "idle ring, config off");
			check(helper, player, true, true, true, "flying ring, config off");

			PEAAConfig.ENABLE_HIGH_SPEED_MOVEMENT_WHEN_LANDING.set(true);
			check(helper, player, false, false, false, "no ring, config on");
			check(helper, player, true, false, false, "idle ring, config on");
			check(helper, player, true, true, true, "flying ring, config on");
		} finally {
			PEAAConfig.ENABLE_HIGH_SPEED_MOVEMENT_WHEN_LANDING.set(original);
		}
		helper.succeed();
	}

	private static void check(GameTestHelper helper, Player player, boolean hasRing, boolean flying, boolean expected, String what) {
		player.getInventory().setItem(0, ItemStack.EMPTY);
		if (hasRing) {
			ItemStack ring = new ItemStack(PEAAItems.RING_OF_THE_SPACE.get());
			RingOfTheSpace.setFlying(ring, flying);
			player.getInventory().setItem(0, ring);
		}
		boolean actual = RingOfTheSpace.suppressesGemBootsBoost(player);
		if (actual != expected) {
			helper.fail("with " + what + " the boost suppression was " + actual + ", expected " + expected);
		}
	}

	/**
	 * Pins down what each furnace actually does per input type (SPEC 3.4.4).
	 *
	 * <p>The original doubled anything whose ore dictionary name began with "ore", and on 1.7.10 that
	 * meant ore blocks -- raw materials did not exist yet. ProjectE rates raw materials at two thirds
	 * of the ore rate on 1.21.1, which is left alone, so smelting raw iron doubles only part of the
	 * time on both furnaces. That is expected, not a mixin that failed to apply, and this test says so
	 * out loud because the difference is easy to mistake for a bug.
	 */
	@GameTest(template = TEMPLATE)
	public static void doublingRatesPerInputType(GameTestHelper helper) {
		assertRates(helper, PEBlocks.DARK_MATTER_FURNACE.getBlock(), "dark matter furnace");
		assertRates(helper, PEBlocks.RED_MATTER_FURNACE.getBlock(), "red matter furnace");
		helper.succeed();
	}

	private static void assertRates(GameTestHelper helper, net.minecraft.world.level.block.Block furnaceBlock, String what) {
		helper.setBlock(FURNACE_POS, furnaceBlock);
		if (!(helper.getBlockEntity(FURNACE_POS) instanceof DMFurnaceBlockEntity furnace)) {
			helper.fail("no block entity for " + what, FURNACE_POS);
			return;
		}
		// Ore blocks: the case the original actually specified, and the one the mixin exists for.
		assertChance(helper, furnace, new ItemStack(Items.IRON_ORE), 1.0F, what + " smelting an ore block");
		// Raw materials: ProjectE's two-thirds rate, deliberately left as it is.
		assertChance(helper, furnace, new ItemStack(Items.RAW_IRON), 2.0F / 3.0F, what + " smelting a raw material");
		// Anything that is not an ore never doubles.
		assertChance(helper, furnace, new ItemStack(Items.SAND), 0.0F, what + " smelting a non-ore");
	}

	private static void assertChance(GameTestHelper helper, DMFurnaceBlockEntity furnace, ItemStack input, float expected, String what) {
		float actual = doubleChance(furnace, input);
		if (Math.abs(actual - expected) > 1.0E-5) {
			helper.fail(what + ": doubling chance was " + actual + ", expected " + expected);
		}
	}

	private static float doubleChance(DMFurnaceBlockEntity furnace, ItemStack input) {
		try {
			var method = DMFurnaceBlockEntity.class.getDeclaredMethod("getDoubleChance", ItemStack.class);
			method.setAccessible(true);
			return (float) method.invoke(furnace, input);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("could not read getDoubleChance", e);
		}
	}

	// --- output pushed out sideways (SPEC 3.4.6) -------------------------------------------------

	/** A chest beside the furnace has to be filled, which vanilla ProjectE never does. */
	@GameTest(template = TEMPLATE)
	public static void furnacePushesOutputToTheSide(GameTestHelper helper) {
		assertPushesToNorthChest(helper, PEBlocks.DARK_MATTER_FURNACE.getBlock());
	}

	/** Red matter furnaces inherit {@code tickServer}, so they have to behave identically. */
	@GameTest(template = TEMPLATE)
	public static void redMatterFurnacePushesOutputToTheSide(GameTestHelper helper) {
		assertPushesToNorthChest(helper, PEBlocks.RED_MATTER_FURNACE.getBlock());
	}

	private static void assertPushesToNorthChest(GameTestHelper helper, Block furnaceBlock) {
		BlockPos chestPos = CENTRE.north();
		helper.setBlock(chestPos, Blocks.CHEST);
		DMFurnaceBlockEntity furnace = placeFurnace(helper, furnaceBlock);
		furnace.getOutput().insertItem(0, new ItemStack(Items.DIAMOND), false);
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			helper.assertContainerContains(chestPos, Items.DIAMOND);
			helper.succeed();
		});
	}

	/** "Every direction but up" is the whole point, so the one excluded direction gets its own test. */
	@GameTest(template = TEMPLATE)
	public static void furnaceNeverPushesUpwards(GameTestHelper helper) {
		BlockPos chestPos = CENTRE.above();
		helper.setBlock(chestPos, Blocks.CHEST);
		DMFurnaceBlockEntity furnace = placeFurnace(helper, PEBlocks.DARK_MATTER_FURNACE.getBlock());
		furnace.getOutput().insertItem(0, new ItemStack(Items.DIAMOND), false);
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			helper.assertContainerEmpty(chestPos);
			assertOutputStillHoldsOneItem(helper, furnace, "with only a chest above");
			helper.succeed();
		});
	}

	/**
	 * SPEC 3.4.6: other matter furnaces are refused, or two side by side would hand their output back
	 * and forth for ever.
	 *
	 * <p>Coal, not a diamond: a furnace exposes fuel-in plus output-out to its sides
	 * (DMFurnaceBlockEntity:63-71, 137-146), so a diamond would be turned down anyway and the test
	 * could not tell the exclusion from that refusal. Coal is accepted, so removing the exclusion
	 * really does move it.
	 */
	@GameTest(template = TEMPLATE)
	public static void furnaceRefusesOtherMatterFurnaces(GameTestHelper helper) {
		helper.setBlock(CENTRE.north(), PEBlocks.DARK_MATTER_FURNACE.getBlock());
		helper.setBlock(CENTRE.south(), PEBlocks.RED_MATTER_FURNACE.getBlock());
		helper.setBlock(CENTRE.east(), PEBlocks.DARK_MATTER_FURNACE.getBlock());
		helper.setBlock(CENTRE.west(), PEBlocks.RED_MATTER_FURNACE.getBlock());
		DMFurnaceBlockEntity furnace = placeFurnace(helper, PEBlocks.DARK_MATTER_FURNACE.getBlock());
		furnace.getOutput().insertItem(0, new ItemStack(Items.COAL), false);
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			assertOutputStillHoldsOneItem(helper, furnace, "surrounded by matter furnaces");
			helper.succeed();
		});
	}

	/**
	 * The other half of the exclusion, checked directly rather than in the world.
	 *
	 * <p>A relay only takes items that carry EMC (SlotPredicates:24-27), and this server has no EMC
	 * map at all: ProjectE builds it in {@code OnDatapackSyncEvent}, which needs a player, and a game
	 * test server has none. So an in-world relay would refuse the item whether or not PEAA excluded
	 * it, and the test would pass for the wrong reason. Asserting on the rule itself does not pretend
	 * otherwise.
	 */
	@GameTest(template = TEMPLATE)
	public static void relaysAreRefusedTargets(GameTestHelper helper) {
		assertRefused(helper, PEBlocks.RELAY.getBlock(), true, "an mk1 relay");
		assertRefused(helper, PEBlocks.RELAY_MK2.getBlock(), true, "an mk2 relay");
		assertRefused(helper, PEBlocks.RELAY_MK3.getBlock(), true, "an mk3 relay");
		assertRefused(helper, PEBlocks.DARK_MATTER_FURNACE.getBlock(), true, "a dark matter furnace");
		assertRefused(helper, PEBlocks.RED_MATTER_FURNACE.getBlock(), true, "a red matter furnace");
		// Negative control: an ordinary inventory has to stay a valid target.
		assertRefused(helper, Blocks.CHEST, false, "a chest");
		helper.succeed();
	}

	private static void assertRefused(GameTestHelper helper, Block block, boolean expected, String what) {
		BlockPos pos = CENTRE.north();
		helper.setBlock(pos, block);
		boolean actual = InventoryPush.isRefusedTarget(helper.getLevel(), helper.absolutePos(pos));
		if (actual != expected) {
			helper.fail(what + " was " + (actual ? "refused" : "accepted") + " as a push target, expected the opposite");
		}
	}

	private static DMFurnaceBlockEntity placeFurnace(GameTestHelper helper, Block furnaceBlock) {
		helper.setBlock(CENTRE, furnaceBlock);
		if (helper.getBlockEntity(CENTRE) instanceof DMFurnaceBlockEntity furnace) {
			return furnace;
		}
		helper.fail("no matter furnace block entity", CENTRE);
		throw new IllegalStateException("unreachable: fail() throws");
	}

	private static void assertOutputStillHoldsOneItem(GameTestHelper helper, DMFurnaceBlockEntity furnace, String what) {
		int found = 0;
		for (int slot = 0, slots = furnace.getOutput().getSlots(); slot < slots; slot++) {
			found += furnace.getOutput().getStackInSlot(slot).getCount();
		}
		if (found != 1) {
			helper.fail("the furnace output held " + found + " items " + what + ", expected the one item to stay put");
		}
	}
}
