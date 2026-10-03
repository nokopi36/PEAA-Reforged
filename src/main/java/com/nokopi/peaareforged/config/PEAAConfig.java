package com.nokopi.peaareforged.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * PEAA's configuration (SPEC 3.9).
 *
 * <p>The type is COMMON because the only entry so far decides whether a recipe loads, which is
 * evaluated while the datapack is being read. ProjectE puts its equivalent
 * ({@code fullKleinStars}) in its common config for the same reason.
 *
 */
public class PEAAConfig {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.BooleanValue ENABLE_ARCHANGEL_SMITE_RECIPE = BUILDER
			.comment("Adds a second recipe for ProjectE's Archangel's Smite that uses Red Matter instead of Dark Matter.",
					"The original recipe is left alone; this one is added alongside it.",
					"Changing this takes effect on the next datapack reload (/reload or a restart).")
			.define("recipes.enableArchangelSmiteRecipe", false);

	public static final ModConfigSpec.BooleanValue ENABLE_HIGH_SPEED_MOVEMENT_WHEN_LANDING = BUILDER
			.comment("Whether Gem Boots keep boosting your airborne speed while you carry a Ring of the Space.",
					"With this off the boots stop boosting whenever the ring is on your hotbar, which is what the",
					"original mod did. With it on they keep boosting unless the ring is actually in flight mode.")
			.define("spaceRing.enableHighSpeedMovementAbilityWhenLanding", false);

	public static final ModConfigSpec SPEC = BUILDER.build();

	private PEAAConfig() {
	}
}
