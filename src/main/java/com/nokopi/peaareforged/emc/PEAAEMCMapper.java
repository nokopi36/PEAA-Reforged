package com.nokopi.peaareforged.emc;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlocks;

/**
 * Supplies the one EMC value ProjectE cannot work out on its own.
 *
 * <p>ProjectE derives EMC from recipes, keying each ingredient on its item <em>and</em> its data
 * components (BaseRecipeTypeMapper.java:203 into NSSItem.java:34-39). The Ultimate AEGU asks for a
 * <em>charged</em> Klein Star Omega, so its ingredient is
 * {@code projecte:klein_star_omega{stored_emc: 51200000}} -- an identity nothing else produces, which
 * leaves the equation unsolvable and the Ultimate AEGU without a value. Every other PEAA addition is
 * derived normally and is deliberately left alone here.
 *
 * <p>Off by default, keeping the original's behaviour of shipping no EMC values at all (SPEC 4.8).
 * ProjectE builds the toggle automatically at
 * {@code config/ProjectE/mapping.toml -> mappers.peaa.enabled} (MappingConfig.java:52-64).
 *
 * <p>This only ever calls {@code addConversion}, which feeds the solver rather than pinning a number.
 * {@code /projecte setemc} uses {@code setValueBefore}, which overrides anything derived
 * (MappingCollector.java:86-96), so a value set by hand always wins.
 */
@EMCMapper
public class PEAAEMCMapper implements IEMCMapper<NormalizedSimpleStack, Long> {

	/** Matches the recipe in SPEC 4.5: eight Advanced AEGUs around one Klein Star Omega. */
	private static final int ADVANCED_AEGU_COUNT = 8;

	@Override
	public void addMappings(IMappingCollector<NormalizedSimpleStack, Long> mapper, ReloadableServerResources serverResources,
			RegistryAccess registryAccess, ResourceManager resourceManager) {
		List<NormalizedSimpleStack> ingredients = new ArrayList<>(ADVANCED_AEGU_COUNT + 1);
		for (int i = 0; i < ADVANCED_AEGU_COUNT; i++) {
			ingredients.add(NSSItem.createItem(PEAABlocks.AEGU_MK2.get()));
		}
		// The uncharged star: the 51,200,000 EMC the recipe actually demands cannot be expressed here,
		// because a conversion is written in items and EMC is not an item. The resulting value is that
		// much lower than what crafting one costs, which errs towards being worth too little rather
		// than too much. Use /projecte setemc to pin an exact figure.
		ingredients.add(NSSItem.createItem(PEItems.KLEIN_STAR_OMEGA.get()));
		mapper.addConversion(1, NSSItem.createItem(PEAABlocks.AEGU_MK3.get()), ingredients);
	}

	@Override
	public String getName() {
		return "PEAAEMCMapper";
	}

	@Override
	public String getConfigPath() {
		return PEAACore.MODID;
	}

	@Override
	public String getTranslationKey() {
		return "peaa_reforged.config.emc_mapper";
	}

	@Override
	public String getDescription() {
		return "Gives the Ultimate AEGU an EMC value, which ProjectE cannot derive because its recipe "
			   + "requires a fully charged Klein Star Omega. Off by default, matching the original mod, "
			   + "which shipped no EMC values. The value ignores the star's stored EMC, so it is lower "
			   + "than the true crafting cost; use /projecte setemc to override it.";
	}

	@Override
	public boolean isAvailable() {
		// Default off: SPEC 4.8 -- the original registered no EMC values.
		return false;
	}
}
