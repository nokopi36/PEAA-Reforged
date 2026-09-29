package peaa.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import peaa.gameObjs.registries.PEAABlocks;

/** Every PEAA block simply drops itself. */
public class PEAALootTableProvider extends LootTableProvider {

	public PEAALootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Set.of(), List.of(new SubProviderEntry(PEAABlockLoot::new, LootContextParamSets.BLOCK)), registries);
	}

	private static class PEAABlockLoot extends BlockLootSubProvider {

		PEAABlockLoot(HolderLookup.Provider lookupProvider) {
			super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
		}

		@Override
		protected Iterable<Block> getKnownBlocks() {
			return PEAABlocks.BLOCKS.getEntries().stream().map(e -> (Block) e.value()).toList();
		}

		@Override
		protected void generate() {
			// An AEGU broken while running still drops the stopped item, matching
			// peaa.gameObjs.blocks.AEGU:126-137. With D-004 that falls out of dropSelf for free.
			getKnownBlocks().forEach(this::dropSelf);
		}
	}
}
