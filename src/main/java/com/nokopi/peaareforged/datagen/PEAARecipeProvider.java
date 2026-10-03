package com.nokopi.peaareforged.datagen;

import java.util.concurrent.CompletableFuture;
import moze_intel.projecte.gameObjs.items.KleinStar;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.jetbrains.annotations.NotNull;
import com.nokopi.peaareforged.gameObjs.customRecipes.ArchangelSmiteRecipeCondition;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlocks;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.registries.PEAAItems;

/** Crafting recipes from SPEC 4.1 to 4.5. */
public class PEAARecipeProvider extends RecipeProvider {

	public PEAARecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void buildRecipes(@NotNull RecipeOutput output) {
		// SPEC 4.1 -- ring of ProjectE MK3 collectors around a Red Matter.
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PEAABlocks.COLLECTOR_MK4.get())
				.pattern("CCC")
				.pattern("CMC")
				.pattern("CCC")
				.define('C', PEBlocks.COLLECTOR_MK3)
				.define('M', PEItems.RED_MATTER)
				.unlockedBy("has_collector_mk3", has(PEBlocks.COLLECTOR_MK3))
				.save(output);

		// SPEC 4.2 -- same shape, one tier up.
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PEAABlocks.COLLECTOR_MK5.get())
				.pattern("CCC")
				.pattern("CMC")
				.pattern("CCC")
				.define('C', PEAABlocks.COLLECTOR_MK4.get())
				.define('M', PEItems.RED_MATTER)
				.unlockedBy("has_collector_mk4", has(PEAABlocks.COLLECTOR_MK4.get()))
				.save(output);

		// SPEC 4.3
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PEAABlocks.AEGU_MK1.get())
				.pattern("CCC")
				.pattern("CPC")
				.pattern("CCC")
				.define('C', PEBlocks.COLLECTOR_MK3)
				.define('P', PEBlocks.DARK_MATTER_PEDESTAL)
				.unlockedBy("has_dm_pedestal", has(PEBlocks.DARK_MATTER_PEDESTAL))
				.save(output);

		// SPEC 4.4 -- Klein Star Sphere is meta 4 in the original (ObjHandlerPEAA:62).
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PEAABlocks.AEGU_MK2.get())
				.pattern("AAA")
				.pattern("ASA")
				.pattern("AAA")
				.define('A', PEAABlocks.AEGU_MK1.get())
				.define('S', PEItems.KLEIN_STAR_SPHERE)
				.unlockedBy("has_aegu_mk1", has(PEAABlocks.AEGU_MK1.get()))
				.save(output);

		// SPEC 4.5 -- the original used a hand-written IRecipe purely to require a *full* Klein Star
		// Omega in the middle (com.nokopi.peaareforged.gameObjs.customRecipes.RecipeAEGUMk3:46-52). A data component
		// ingredient expresses the same condition as an ordinary shaped recipe.
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PEAABlocks.AEGU_MK3.get())
				.pattern("AAA")
				.pattern("AKA")
				.pattern("AAA")
				.define('A', PEAABlocks.AEGU_MK2.get())
				.define('K', fullKleinStarOmega())
				.unlockedBy("has_aegu_mk2", has(PEAABlocks.AEGU_MK2.get()))
				.save(output);

		// SPEC 4.6 -- shapeless: Swiftwolf's Rending Gale, a Void Ring and seven Red Matter.
		ShapelessRecipeBuilder shapeless = ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, PEAAItems.RING_OF_THE_SPACE.get())
				.requires(PEItems.SWIFTWOLF_RENDING_GALE)
				.requires(PEItems.VOID_RING);
		shapeless.requires(PEItems.RED_MATTER, 7)
				.unlockedBy("has_swrg", has(PEItems.SWIFTWOLF_RENDING_GALE))
				.save(output);

		archangelSmiteRedMatter(output);
	}

	/**
	 * SPEC 4.7 -- an extra Archangel's Smite recipe that asks for Red Matter where ProjectE's asks for
	 * Dark Matter. ProjectE's own recipe stays; this one is added beside it, and only loads when the
	 * config switch is on, which is off by default just as in the original.
	 */
	private static void archangelSmiteRedMatter(@NotNull RecipeOutput output) {
		ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, PEItems.ARCHANGEL_SMITE)
				.pattern("AFA")
				.pattern("RIR")
				.pattern("AFA")
				.define('A', Items.BOW)
				.define('F', Items.FEATHER)
				.define('R', PEItems.RED_MATTER)
				.define('I', PEItems.IRON_BAND)
				.unlockedBy("has_red_matter", has(PEItems.RED_MATTER))
				.save(output.withConditions(ArchangelSmiteRecipeCondition.INSTANCE),
						PEAACore.rl("archangel_smite_red_matter"));
	}

	/**
	 * A Klein Star Omega charged to its maximum EMC.
	 *
	 * <p>Matching is partial (deviation D-006): only {@code projecte:stored_emc} has to line up, so a
	 * renamed or otherwise decorated star still works. Comparing the whole component map the way the
	 * 1.7.10 recipe compared whole NBT tags would also pin down unrelated defaults such as the item's
	 * rarity, which is brittle without expressing anything the recipe actually cares about.
	 */
	private static Ingredient fullKleinStarOmega() {
		KleinStar omega = PEItems.KLEIN_STAR_OMEGA.get();
		return DataComponentIngredient.of(false, PEDataComponentTypes.STORED_EMC.get(), omega.tier.maxEmc, omega);
	}
}
