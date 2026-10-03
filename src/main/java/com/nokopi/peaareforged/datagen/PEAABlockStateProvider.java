package com.nokopi.peaareforged.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.EnumAEGUTier;
import com.nokopi.peaareforged.gameObjs.blocks.AEGUBlock;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlocks;

/** Blockstates and block/item models for PEAA's blocks. */
public class PEAABlockStateProvider extends BlockStateProvider {

	public PEAABlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, PEAACore.MODID, existingFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		// Collectors: distinct front/side/top, rotated by the FACING property inherited from BlockDirection.
		collector("collector_mk4");
		collector("collector_mk5");

		aegu(EnumAEGUTier.MK1);
		aegu(EnumAEGUTier.MK2);
		aegu(EnumAEGUTier.MK3);
	}

	private void collector(String name) {
		var block = name.equals("collector_mk4") ? PEAABlocks.COLLECTOR_MK4.get() : PEAABlocks.COLLECTOR_MK5.get();
		ModelFile model = models().orientable(name,
				modLoc("block/" + name + "_side"),
				modLoc("block/collector_front"),
				modLoc("block/" + name + "_top"));
		horizontalBlock(block, model);
		simpleBlockItem(block, model);
	}

	/** One block, two models, selected by the GENERATING blockstate (deviation D-004). */
	private void aegu(EnumAEGUTier tier) {
		String name = tier.getSerializedName();
		var block = switch (tier) {
			case MK1 -> PEAABlocks.AEGU_MK1.get();
			case MK2 -> PEAABlocks.AEGU_MK2.get();
			case MK3 -> PEAABlocks.AEGU_MK3.get();
		};
		ModelFile off = models().cubeAll(name, modLoc("block/" + name));
		ModelFile on = models().cubeAll(name + "_on", modLoc("block/" + name + "_on"));
		getVariantBuilder(block).forAllStates(state ->
				ConfiguredModel.builder().modelFile(state.getValue(AEGUBlock.GENERATING) ? on : off).build());
		simpleBlockItem(block, off);
	}
}
