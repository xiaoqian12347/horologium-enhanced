package com.xiaoqian.untitled.proxy;

import com.xiaoqian.untitled.client.render.BindingSavedData;
import com.xiaoqian.untitled.client.render.BinderBindingHandler;
import com.xiaoqian.untitled.client.render.StarlightNetHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        StarlightNetHandler.init();
        MinecraftForge.EVENT_BUS.register(BinderBindingHandler.class);
        MinecraftForge.EVENT_BUS.register(StarlightNetHandler.class);
    }
}
