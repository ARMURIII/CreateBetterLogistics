package com.yision.bettersaw;

import com.yision.bettersaw.content.DeployingUnpackingHandler;
import com.yision.bettersaw.content.ProcessingBufferItemHandler;
import com.yision.bettersaw.content.SawBufferManager;
import com.yision.bettersaw.content.SawingUnpackingHandler;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(CreateBetterSaw.MOD_ID)
public final class CreateBetterSaw {
    public static final String MOD_ID = "bettersaw";

    public CreateBetterSaw(IEventBus modEventBus) {
        BetterSawRegistries.register(modEventBus);
        modEventBus.addListener(ProcessingBufferItemHandler::registerCapabilities);
        SawBufferManager.register();
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SawingUnpackingHandler::register);
        event.enqueueWork(DeployingUnpackingHandler::register);
    }
}
