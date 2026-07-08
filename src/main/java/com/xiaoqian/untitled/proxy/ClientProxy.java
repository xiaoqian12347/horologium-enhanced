package com.xiaoqian.untitled.proxy;

import com.xiaoqian.untitled.client.render.AltarStarlightRenderer;
import com.xiaoqian.untitled.client.render.BinderHighlightRenderer;
import com.xiaoqian.untitled.client.render.BinderScrollHandler;
import com.xiaoqian.untitled.client.render.ClientModelRegistration;
import com.xiaoqian.untitled.client.render.StarlightClientData;
import com.xiaoqian.untitled.network.StarlightNetHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(ClientModelRegistration.class);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(BinderScrollHandler.class);
        MinecraftForge.EVENT_BUS.register(BinderHighlightRenderer.class);
        MinecraftForge.EVENT_BUS.register(AltarStarlightRenderer.class);
    }

    @Override
    public void handleStarlightSync(StarlightNetHandler.SyncMsg msg) {
        StarlightClientData.handleSync(msg);
    }

    @Override
    public void handleBindingSync(StarlightNetHandler.BindSyncMsg msg) {
        StarlightClientData.handleBindingSync(msg);
    }
}
