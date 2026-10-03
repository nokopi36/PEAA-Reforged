package com.nokopi.peaareforged.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.nokopi.peaareforged.PEAACore;

/** Registers PEAA's network payloads. Replaces the 1.7.10 {@code PacketHandlerPEAA}. */
@EventBusSubscriber(modid = PEAACore.MODID)
public class PEAAPayloads {

	@SubscribeEvent
	static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToServer(SetFlyingPayload.TYPE, SetFlyingPayload.STREAM_CODEC, SetFlyingPayload::handle);
	}

	private PEAAPayloads() {
	}
}
