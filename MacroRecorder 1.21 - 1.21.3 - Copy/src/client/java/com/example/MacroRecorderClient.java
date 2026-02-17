package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class MacroRecorderClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MacroRecorderConfigManager.load();
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			MacroRecorderConfigManager.save();
		});
		MacroRecorder.init();
	}
}
