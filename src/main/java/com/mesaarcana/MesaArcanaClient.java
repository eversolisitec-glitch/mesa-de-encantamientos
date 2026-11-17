package com.mesaarcana;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class MesaArcanaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MesaConfig.get();
		BlockEntityRenderers.register(BlockEntityType.ENCHANTING_TABLE, ArcaneTableRenderer::new);
	}
}
