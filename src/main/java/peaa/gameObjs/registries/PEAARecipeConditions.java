package peaa.gameObjs.registries;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import peaa.PEAACore;
import peaa.gameObjs.customRecipes.ArchangelSmiteRecipeCondition;

/** Recipe load conditions. Modelled on ProjectE's {@code PERecipeConditions}. */
public class PEAARecipeConditions {

	public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
			DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, PEAACore.MODID);

	public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ArchangelSmiteRecipeCondition>> ARCHANGEL_SMITE_RECIPE =
			CONDITION_CODECS.register("archangel_smite_recipe", () -> MapCodec.unit(ArchangelSmiteRecipeCondition.INSTANCE));

	private PEAARecipeConditions() {
	}
}
