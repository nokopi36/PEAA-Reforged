package com.nokopi.peaareforged.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import com.nokopi.peaareforged.PEAACore;

/**
 * Item models for PEAA's non-block items. Block items are handled by
 * {@link PEAABlockStateProvider}.
 */
public class PEAAItemModelProvider extends ItemModelProvider {

	public PEAAItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, PEAACore.MODID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		// Two models, picked by the `peaa:flying` item property registered in PEAAClient, so the ring
		// visibly lights up while flight is on (SPEC 3.5.4's on/off icons).
		ItemModelBuilder flying = singleTexture("ring_of_the_space_on", mcLoc("item/generated"),
				"layer0", modLoc("item/ring_of_the_space_on"));
		singleTexture("ring_of_the_space", mcLoc("item/generated"), "layer0", modLoc("item/ring_of_the_space"))
				.override()
				.predicate(PEAACore.rl("flying"), 1)
				.model(flying)
				.end();
	}
}
