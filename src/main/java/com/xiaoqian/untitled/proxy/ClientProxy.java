package com.xiaoqian.untitled.proxy;

import com.xiaoqian.untitled.client.render.AltarStarlightRenderer;
import com.xiaoqian.untitled.client.render.BinderHighlightRenderer;
import com.xiaoqian.untitled.client.render.BinderScrollHandler;
import com.xiaoqian.untitled.client.render.RitualNodeInteractionHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(BinderScrollHandler.class);
        MinecraftForge.EVENT_BUS.register(RitualNodeInteractionHandler.class);
        MinecraftForge.EVENT_BUS.register(BinderHighlightRenderer.class);
        MinecraftForge.EVENT_BUS.register(AltarStarlightRenderer.class);
    }
}
