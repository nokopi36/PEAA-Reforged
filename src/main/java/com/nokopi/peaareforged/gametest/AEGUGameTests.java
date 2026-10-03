package com.nokopi.peaareforged.gametest;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.EnumAEGUTier;
import com.nokopi.peaareforged.gameObjs.block_entities.AEGUBlockEntity;
import com.nokopi.peaareforged.gameObjs.blocks.AEGUBlock;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlocks;

/**
 * Checks the AEGU group rules from SPEC 3.2 / 3.3.2.
 *
 * <p>The condenser in these tests has no target item set, so it refuses EMC. That is deliberate: it
 * lets the produced amount be read straight off each AEGU's buffer, which is exactly the number
 * SPEC 3.2.2 specifies, and proves nothing is silently dropped.
 *
 * <p>The final hand-off -- buffered EMC moving into the condenser once a target is set -- is not
 * covered here. ProjectE builds its EMC map from {@code OnDatapackSyncEvent}
 * (refs/projecte/.../PECore.java:264-274), which needs a player, and the game test server has none.
 * Every item therefore has an EMC value of 0, so a condenser can never be put into its
 * accepting state. That step is on the manual checklist in the Step 4 report instead.
 */
@GameTestHolder(PEAACore.MODID)
@PrefixGameTestTemplate(false)
public class AEGUGameTests {

	private static final String TEMPLATE = "empty";
	private static final BlockPos CONDENSER_POS = new BlockPos(2, 2, 2);

	/** 25 of the 26 surrounding positions is the threshold (decision 15). */
	@GameTest(template = TEMPLATE)
	public static void twentyFiveAeguStartTheGroup(GameTestHelper helper) {
		List<BlockPos> placed = surround(helper, EnumAEGUTier.MK1, 25);
		helper.succeedWhen(() -> {
			for (BlockPos pos : placed) {
				helper.assertBlockProperty(pos, AEGUBlock.GENERATING, true);
			}
		});
	}

	@GameTest(template = TEMPLATE)
	public static void twentyFourAeguDoNotStartTheGroup(GameTestHelper helper) {
		List<BlockPos> placed = surround(helper, EnumAEGUTier.MK1, 24);
		// Give the group a chance to (wrongly) start before declaring success.
		helper.runAfterDelay(30, () -> {
			for (BlockPos pos : placed) {
				helper.assertBlockProperty(pos, AEGUBlock.GENERATING, false);
				if (aegu(helper, pos).getBuffer() != 0) {
					helper.fail("an AEGU produced EMC with only 24 in the group", pos);
				}
			}
			helper.succeed();
		});
	}

	/** SPEC 3.2.2: MK1 produces 40 EMC/s, i.e. 2 EMC per tick per AEGU. */
	@GameTest(template = TEMPLATE)
	public static void mk1ProducesTwoEmcPerTick(GameTestHelper helper) {
		assertProducesPerTick(helper, EnumAEGUTier.MK1, 2);
	}

	/** SPEC 3.2.2: MK2 produces 1,000 EMC/s, i.e. 50 EMC per tick per AEGU. */
	@GameTest(template = TEMPLATE)
	public static void mk2ProducesFiftyEmcPerTick(GameTestHelper helper) {
		assertProducesPerTick(helper, EnumAEGUTier.MK2, 50);
	}

	/** SPEC 3.2.2: MK3 produces 20,000 EMC/s, i.e. 1,000 EMC per tick per AEGU. */
	@GameTest(template = TEMPLATE)
	public static void mk3ProducesThousandEmcPerTick(GameTestHelper helper) {
		assertProducesPerTick(helper, EnumAEGUTier.MK3, 1_000);
	}

	/** Without a condenser nearby an AEGU is inert, whatever else is around it. */
	@GameTest(template = TEMPLATE)
	public static void loneAeguDoesNothing(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, PEAABlocks.AEGU_MK3.get());
		helper.runAfterDelay(30, () -> {
			helper.assertBlockProperty(pos, AEGUBlock.GENERATING, false);
			if (aegu(helper, pos).getBuffer() != 0) {
				helper.fail("a lone AEGU produced EMC", pos);
			}
			helper.succeed();
		});
	}

	private static void assertProducesPerTick(GameTestHelper helper, EnumAEGUTier tier, long expectedPerTick) {
		List<BlockPos> placed = surround(helper, tier, 25);
		BlockPos sample = placed.getFirst();
		int ticks = 20;
		// The group needs a tick to form before production starts, so measure a window after it is running.
		helper.runAfterDelay(5, () -> {
			long before = aegu(helper, sample).getBuffer();
			helper.runAfterDelay(ticks, () -> {
				long produced = aegu(helper, sample).getBuffer() - before;
				long expected = expectedPerTick * ticks;
				if (produced != expected) {
					helper.fail("expected " + expected + " EMC over " + ticks + " ticks but produced " + produced, sample);
				}
				helper.succeed();
			});
		});
	}

	/**
	 * Places a condenser and the given number of AEGUs around it, and returns the AEGU positions.
	 */
	private static List<BlockPos> surround(GameTestHelper helper, EnumAEGUTier tier, int count) {
		helper.setBlock(CONDENSER_POS, PEBlocks.CONDENSER_MK2.getBlock());
		Block block = switch (tier) {
			case MK1 -> PEAABlocks.AEGU_MK1.get();
			case MK2 -> PEAABlocks.AEGU_MK2.get();
			case MK3 -> PEAABlocks.AEGU_MK3.get();
		};
		List<BlockPos> placed = new ArrayList<>();
		for (BlockPos candidate : BlockPos.betweenClosed(CONDENSER_POS.offset(-1, -1, -1), CONDENSER_POS.offset(1, 1, 1))) {
			if (candidate.equals(CONDENSER_POS) || placed.size() >= count) {
				continue;
			}
			BlockPos pos = candidate.immutable();
			helper.setBlock(pos, block);
			placed.add(pos);
		}
		helper.assertValueEqual(placed.size(), count, "number of AEGUs placed");
		return placed;
	}

	private static AEGUBlockEntity aegu(GameTestHelper helper, BlockPos pos) {
		if (helper.getBlockEntity(pos) instanceof AEGUBlockEntity aegu) {
			return aegu;
		}
		helper.fail("no AEGU block entity", pos);
		throw new IllegalStateException("unreachable");
	}
}
