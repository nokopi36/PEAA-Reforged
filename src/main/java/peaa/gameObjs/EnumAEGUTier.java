package peaa.gameObjs;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The three AEGU (Alchemical Energy Generating Unit) tiers.
 *
 * <p>Generation rates are from SPEC 3.2.2 (originally the {@code generateEmcOfAEGU} table in
 * peaa.gameObjs.tiles.CondenserMK2TilePEAA:28). They are EMC per second, and are only ever produced
 * once an AEGU group is wired up to an Energy Condenser MK2 -- that linkage is not implemented yet.
 */
public enum EnumAEGUTier implements StringRepresentable {
	MK1("aegu_mk1", 40),
	MK2("aegu_mk2", 1_000),
	MK3("aegu_mk3", 20_000);

	private final String name;
	private final long genRate;

	EnumAEGUTier(String name, long genRate) {
		this.name = name;
		this.genRate = genRate;
	}

	@NotNull
	@Override
	public String getSerializedName() {
		return name;
	}

	/** EMC per second this AEGU contributes to a linked Energy Condenser MK2. */
	public long getGenRate() {
		return genRate;
	}

	@Override
	public String toString() {
		return getSerializedName();
	}
}
