package peaa.gameObjs;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The collector tiers PEAA adds on top of ProjectE's MK1-MK3.
 *
 * <p>Numbers come from SPEC 3.1.1 (originally peaa.utils.ConstantsPEAA:5-8). The generation rate is
 * EMC per second at full "sun level"; see {@link peaa.gameObjs.block_entities.CollectorPEAABlockEntity}
 * for how it is applied per tick.
 *
 * <p>This deliberately does not extend ProjectE's {@code EnumCollectorTier} -- enums cannot be
 * extended, and ProjectE does not declare it as a NeoForge extensible enum.
 */
public enum EnumCollectorTierPEAA implements StringRepresentable {
	MK4("collector_mk4", 320, 60_000),
	MK5("collector_mk5", 1_280, 60_000);

	private final String name;
	private final long genRate;
	private final long storage;

	EnumCollectorTierPEAA(String name, long genRate, long storage) {
		this.name = name;
		this.genRate = genRate;
		this.storage = storage;
	}

	@NotNull
	@Override
	public String getSerializedName() {
		return name;
	}

	/** EMC per second produced while running. */
	public long getGenRate() {
		return genRate;
	}

	/** Internal EMC buffer size. */
	public long getStorage() {
		return storage;
	}

	/**
	 * Number of fuel storage slots. Both tiers use 16, matching ProjectE's MK3
	 * (SPEC 3.1.5 -- peaa.gameObjs.container.CollectorMK4Container:30-32).
	 */
	public int getInvSize() {
		return 16;
	}

	@Override
	public String toString() {
		return getSerializedName();
	}
}
