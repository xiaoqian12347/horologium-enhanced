package com.xiaoqian.untitled.proxy;

import com.xiaoqian.untitled.common.binding.BinderBindingHandler;
import com.xiaoqian.untitled.common.binding.RitualNodeInteractionHandler;
import com.xiaoqian.untitled.network.StarlightNetHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        StarlightNetHandler.init();
        MinecraftForge.EVENT_BUS.register(BinderBindingHandler.class);
        MinecraftForge.EVENT_BUS.register(RitualNodeInteractionHandler.class);
        MinecraftForge.EVENT_BUS.register(StarlightNetHandler.class);
    }

    public void handleStarlightSync(StarlightNetHandler.SyncMsg msg) {
    }

    public void handleBindingSync(StarlightNetHandler.BindSyncMsg msg) {
    }
}
