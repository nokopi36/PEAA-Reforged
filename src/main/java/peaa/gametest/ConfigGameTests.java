package peaa.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import peaa.PEAACore;
import peaa.config.PEAAConfig;
import peaa.gameObjs.customRecipes.ArchangelSmiteRecipeCondition;

/** Checks the config-gated recipe wiring from SPEC 3.9 / 4.7. */
@GameTestHolder(PEAACore.MODID)
@PrefixGameTestTemplate(false)
public class ConfigGameTests {

	private static final String TEMPLATE = "empty";
	private static final ResourceLocation RECIPE = PEAACore.rl("archangel_smite_red_matter");

	/**
	 * The condition has to report whatever the config says, since that is all that gates the recipe.
	 *
	 * <p>Both settings are exercised rather than just the current one: comparing the condition against
	 * the config as it happens to be would pass even if the condition ignored the config entirely.
	 * The original value is put back afterwards so the test leaves no trace.
	 */
	@GameTest(template = TEMPLATE)
	public static void conditionFollowsTheConfig(GameTestHelper helper) {
		boolean original = PEAAConfig.ENABLE_ARCHANGEL_SMITE_RECIPE.get();
		try {
			PEAAConfig.ENABLE_ARCHANGEL_SMITE_RECIPE.set(true);
			if (!ArchangelSmiteRecipeCondition.INSTANCE.test(null)) {
				helper.fail("the condition said no while the config was enabled");
				return;
			}
			PEAAConfig.ENABLE_ARCHANGEL_SMITE_RECIPE.set(false);
			if (ArchangelSmiteRecipeCondition.INSTANCE.test(null)) {
				helper.fail("the condition said yes while the config was disabled");
				return;
			}
		} finally {
			PEAAConfig.ENABLE_ARCHANGEL_SMITE_RECIPE.set(original);
		}
		helper.succeed();
	}

	/**
	 * With the config at its default of false the recipe must not be loaded, which is what proves the
	 * condition is actually attached to the recipe file rather than merely registered.
	 */
	@GameTest(template = TEMPLATE)
	public static void recipeIsAbsentWhileDisabled(GameTestHelper helper) {
		boolean present = helper.getLevel().getRecipeManager().byKey(RECIPE).isPresent();
		boolean expected = PEAAConfig.ENABLE_ARCHANGEL_SMITE_RECIPE.get();
		if (present != expected) {
			helper.fail("recipe " + RECIPE + " present=" + present + " but the config says " + expected);
		}
		helper.succeed();
	}
}
