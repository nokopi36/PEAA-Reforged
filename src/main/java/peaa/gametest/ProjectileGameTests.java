package peaa.gametest;

import moze_intel.projecte.gameObjs.entity.EntityLavaProjectile;
import moze_intel.projecte.gameObjs.entity.EntityWaterProjectile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import peaa.PEAACore;

/** Checks the water orb / lava orb annihilation from SPEC 3.7.2. */
@GameTestHolder(PEAACore.MODID)
@PrefixGameTestTemplate(false)
public class ProjectileGameTests {

	private static final String TEMPLATE = "empty";

	/** Orbs that meet must take each other out. */
	@GameTest(template = TEMPLATE)
	public static void touchingOrbsCancelOut(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 pos = helper.absoluteVec(new Vec3(2.5, 2.5, 2.5));

		EntityWaterProjectile water = new EntityWaterProjectile(player, helper.getLevel());
		water.setPos(pos.x, pos.y, pos.z);
		helper.getLevel().addFreshEntity(water);

		EntityLavaProjectile lava = new EntityLavaProjectile(player, helper.getLevel());
		lava.setPos(pos.x, pos.y, pos.z);
		helper.getLevel().addFreshEntity(lava);

		helper.succeedWhen(() -> {
			if (water.isAlive()) {
				helper.fail("the water orb survived contact with a lava orb");
			}
			if (lava.isAlive()) {
				helper.fail("the lava orb survived contact with a water orb");
			}
		});
	}

	/** A water orb on its own must keep flying; the handler has to look for an actual counterpart. */
	@GameTest(template = TEMPLATE)
	public static void loneWaterOrbSurvives(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 pos = helper.absoluteVec(new Vec3(2.5, 2.5, 2.5));

		EntityWaterProjectile water = new EntityWaterProjectile(player, helper.getLevel());
		water.setPos(pos.x, pos.y, pos.z);
		water.setDeltaMovement(Vec3.ZERO);
		helper.getLevel().addFreshEntity(water);

		helper.runAfterDelay(10, () -> {
			if (!water.isAlive()) {
				helper.fail("a water orb with no lava orb nearby was destroyed");
			}
			helper.succeed();
		});
	}
}
