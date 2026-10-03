package com.nokopi.peaareforged.gameObjs.registries;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nokopi.peaareforged.PEAACore;

/**
 * Data components PEAA stores on its own items.
 *
 * <p>Replaces the 1.7.10 practice of writing straight into an item's NBT. Of the five tags the Ring
 * of the Space used (SPEC 3.5.9) only the flight toggle needs storing; the rest are recomputed every
 * tick. EMC itself lives in ProjectE's {@code projecte:stored_emc}, and the charge level in its
 * {@code projecte:charge}.
 */
public class PEAADataComponents {

	public static final DeferredRegister.DataComponents DATA_COMPONENTS =
			DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PEAACore.MODID);

	/** Whether the Ring of the Space is currently in flight mode. */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FLYING =
			DATA_COMPONENTS.registerComponentType("flying", builder -> builder
					.persistent(Codec.BOOL)
					.networkSynchronized(ByteBufCodecs.BOOL));

	private PEAADataComponents() {
	}
}
