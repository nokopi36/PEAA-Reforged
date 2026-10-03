package com.nokopi.peaareforged.gametest;

import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.block_entities.CollectorPEAABlockEntity;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlocks;

/**
 * Checks the generation numbers from SPEC 3.1.1 and the AEGU condition from SPEC 3.1.2.
 *
 * <p>At the maximum sun level of 16 the formula {@code genRate * (sunLevel / 320)} yields
 * {@code genRate / 20} EMC per tick, so MK4 (320 EMC/s) gains 16 per tick and MK5 (1280 EMC/s)
 * gains 64 per tick.
 */
@GameTestHolder(PEAACore.MODID)
@PrefixGameTestTemplate(false)
public class CollectorGameTests {

	private static final String TEMPLATE = "empty";
	private static final BlockPos COLLECTOR_POS = new BlockPos(1, 1, 1);

	@GameTest(template = TEMPLATE)
	public static void mk4GeneratesWithAeguAbove(GameTestHelper helper) {
		helper.setBlock(COLLECTOR_POS, PEAABlocks.COLLECTOR_MK4.get());
		helper.setBlock(COLLECTOR_POS.above(), PEAABlocks.AEGU_MK1.get());
		assertGeneratesPerTick(helper, 16);
	}

	@GameTest(template = TEMPLATE)
	public static void mk5GeneratesWithAeguAbove(GameTestHelper helper) {
		helper.setBlock(COLLECTOR_POS, PEAABlocks.COLLECTOR_MK5.get());
		helper.setBlock(COLLECTOR_POS.above(), PEAABlocks.AEGU_MK1.get());
		assertGeneratesPerTick(helper, 64);
	}

	/** A stopped AEGU still powers the collector -- decision 16, matching the original. */
	@GameTest(template = TEMPLATE)
	public static void stoppedAeguStillCounts(GameTestHelper helper) {
		helper.setBlock(COLLECTOR_POS, PEAABlocks.COLLECTOR_MK4.get());
		helper.setBlock(COLLECTOR_POS.above(), PEAABlocks.AEGU_MK3.get().defaultBlockState());
		assertGeneratesPerTick(helper, 16);
	}

	@GameTest(template = TEMPLATE)
	public static void noAeguMeansNoGeneration(GameTestHelper helper) {
		helper.setBlock(COLLECTOR_POS, PEAABlocks.COLLECTOR_MK4.get());
		CollectorPEAABlockEntity collector = collector(helper);
		helper.succeedWhen(() -> {
			helper.assertValueEqual(collector.getSunLevel(), 0, "sun level without an AEGU above");
			helper.assertValueEqual(collector.getStoredEmc(), 0L, "stored EMC without an AEGU above");
		});
	}

	/**
	 * An idle collector hands EMC onwards rather than hoarding it, so it must refuse EMC pushed in
	 * from outside (ProjectE's collectors behave the same way).
	 */
	@GameTest(template = TEMPLATE)
	public static void idleCollectorRefusesExternalEmc(GameTestHelper helper) {
		helper.setBlock(COLLECTOR_POS, PEAABlocks.COLLECTOR_MK5.get());
		CollectorPEAABlockEntity collector = collector(helper);
		collector.insertEmc(1_000L, IEmcStorage.EmcAction.EXECUTE);
		helper.assertValueEqual(collector.getStoredEmc(), 0L, "stored EMC after an external push while idle");
		helper.succeed();
	}

	/**
	 * The buffer must stop at the tier's capacity (SPEC 3.1.1: 60,000 for both tiers).
	 *
	 * <p>MK5 generates 64 EMC/tick, so it needs about 938 ticks to fill; 1000 ticks leaves margin.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 1_500)
	public static void storageIsCappedAtTierCapacity(GameTestHelper helper) {
		helper.setBlock(COLLECTOR_POS, PEAABlocks.COLLECTOR_MK5.get());
		helper.setBlock(COLLECTOR_POS.above(), PEAABlocks.AEGU_MK1.get());
		CollectorPEAABlockEntity collector = collector(helper);
		helper.assertValueEqual(collector.getMaximumEmc(), 60_000L, "maximum EMC");
		helper.runAfterDelay(1_000, () -> {
			helper.assertValueEqual(collector.getStoredEmc(), 60_000L, "stored EMC once full");
			helper.succeed();
		});
	}

	/**
	 * Lets the collector run for a while and checks the average gain per tick. A window is sampled
	 * rather than a single tick because generation accumulates fractionally before being banked.
	 */
	private static void assertGeneratesPerTick(GameTestHelper helper, long expectedPerTick) {
		CollectorPEAABlockEntity collector = collector(helper);
		helper.assertValueEqual(collector.getSunLevel(), 16, "sun level with an AEGU above");
		int ticks = 20;
		helper.runAfterDelay(ticks, () -> {
			long stored = collector.getStoredEmc();
			long expected = expectedPerTick * ticks;
			// The collector ticks once per game tick; allow one tick of slack either way for the
			// exact point in the tick order at which the test observes the value.
			if (Math.abs(stored - expected) > expectedPerTick) {
				helper.fail("expected about " + expected + " EMC after " + ticks + " ticks but found " + stored);
			}
			helper.succeed();
		});
	}

	private static CollectorPEAABlockEntity collector(GameTestHelper helper) {
		return helper.getBlockEntity(COLLECTOR_POS) instanceof CollectorPEAABlockEntity collector ? collector
				: throwMissing(helper);
	}

	private static CollectorPEAABlockEntity throwMissing(GameTestHelper helper) {
		helper.fail("no collector block entity at " + COLLECTOR_POS);
		throw new IllegalStateException("unreachable");
	}
}
