package peaa.events;

import java.util.List;
import moze_intel.projecte.gameObjs.entity.EntityLavaProjectile;
import moze_intel.projecte.gameObjs.entity.EntityWaterProjectile;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import peaa.PEAACore;

/**
 * Makes a water orb and a lava orb destroy each other on contact (SPEC 3.7.2).
 *
 * <p>The original got this by swapping ProjectE's water projectile for its own subclass through ASM
 * (peaa.gameObjs.entity.EntityWaterProjectilePEAA:33-56). Replacing ProjectE's entity class is not an
 * option on 1.21.1, but the behaviour does not need it: watching the water orb tick from an event is
 * enough, and leaves ProjectE untouched.
 *
 * <p>ProjectE's own orbs do not interact on 1.21.1 -- the water orb turns nearby lava to obsidian and
 * the lava orb drains nearby water, but neither touches the other's projectile, so they simply pass
 * through one another without this.
 *
 * <p>The original scanned every loaded entity in the level each tick and compared truncated block
 * coordinates (an O(n) pass per orb). A bounding box query is used here instead, which reads the same
 * one block radius out of the chunk's entity sections.
 */
@EventBusSubscriber(modid = PEAACore.MODID)
public class ProjectileCollisionHandler {

	/** How far apart the two orbs may be and still cancel out, matching the original's +/-1 block sweep. */
	private static final double REACH = 1.0;

	@SubscribeEvent
	static void onEntityTick(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof EntityWaterProjectile water)) {
			return;
		}
		Level level = water.level();
		if (level.isClientSide || !water.isAlive()) {
			return;
		}
		List<EntityLavaProjectile> lavaOrbs = level.getEntitiesOfClass(EntityLavaProjectile.class, water.getBoundingBox().inflate(REACH),
				EntityLavaProjectile::isAlive);
		if (lavaOrbs.isEmpty()) {
			return;
		}
		// Only the first one is consumed: each orb cancels exactly one counterpart, as in the original.
		lavaOrbs.getFirst().discard();
		water.discard();
	}

	private ProjectileCollisionHandler() {
	}
}
