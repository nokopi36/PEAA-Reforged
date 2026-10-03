package com.nokopi.peaareforged.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.items.RingOfTheSpace;

/**
 * Client to server: the player toggled flight on the Ring of the Space in the given hotbar slot.
 *
 * <p>Flight is driven client side, so the client is the one that knows when the toggle happened; the
 * server owns the item state. Replaces the 1.7.10 {@code IsFlyingModeSyncPKT}.
 *
 * <p>Deviation D-011: the original answered by broadcasting to <em>every</em> player
 * ({@code IsFlyingModeSyncPKTHandlerToServer.java:23}), which overwrote the ring in the same slot of
 * everyone else's hotbar. Here the server just updates the sender's own stack and lets the normal
 * inventory sync carry it back.
 */
public record SetFlyingPayload(int slot, boolean flying) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SetFlyingPayload> TYPE =
			new CustomPacketPayload.Type<>(PEAACore.rl("set_flying"));

	public static final StreamCodec<ByteBuf, SetFlyingPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SetFlyingPayload::slot,
			ByteBufCodecs.BOOL, SetFlyingPayload::flying,
			SetFlyingPayload::new);

	@NotNull
	@Override
	public CustomPacketPayload.Type<SetFlyingPayload> type() {
		return TYPE;
	}

	public void handle(IPayloadContext context) {
		Player player = context.player();
		if (!RingOfTheSpace.isOnHotbar(slot)) {
			return;
		}
		ItemStack stack = player.getInventory().getItem(slot);
		if (stack.getItem() instanceof RingOfTheSpace) {
			RingOfTheSpace.setFlying(stack, flying);
		}
	}
}
