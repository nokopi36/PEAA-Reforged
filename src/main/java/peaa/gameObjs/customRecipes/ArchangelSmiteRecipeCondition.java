package peaa.gameObjs.customRecipes;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.NotNull;
import peaa.config.PEAAConfig;
import peaa.gameObjs.registries.PEAARecipeConditions;

/**
 * Load condition gating the Red Matter variant of Archangel's Smite (SPEC 4.7).
 *
 * <p>The original decided this in code, skipping {@code GameRegistry.addRecipe} when the config was
 * off (peaa.gameObjs.ObjHandlerPEAA:73-77). Recipes are datapack content on 1.21.1, so the decision
 * moves into the recipe file as a condition. Modelled on ProjectE's {@code FullKleinStarsCondition}.
 */
public class ArchangelSmiteRecipeCondition implements ICondition {

	public static final ArchangelSmiteRecipeCondition INSTANCE = new ArchangelSmiteRecipeCondition();

	private ArchangelSmiteRecipeCondition() {
	}

	@Override
	public boolean test(@NotNull IContext context) {
		return PEAAConfig.ENABLE_ARCHANGEL_SMITE_RECIPE.get();
	}

	@NotNull
	@Override
	public MapCodec<? extends ICondition> codec() {
		return PEAARecipeConditions.ARCHANGEL_SMITE_RECIPE.get();
	}
}
