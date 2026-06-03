package com.xiaoqian.untitled;

import com.xiaoqian.untitled.client.render.StarlightNetHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = Untitled.MODID, name = Untitled.NAME, version = Untitled.VERSION)
public class Untitled
{
    public static final String MODID = "horologium_positioning";
    public static final String NAME = "时钟定位 | Horologium Positioning";
    public static final String VERSION = "1.0";

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        logger.info("Astral Sorcery Addon pre-initializing...");
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        logger.info("Astral Sorcery Addon initialized.");
        StarlightNetHandler.init();
        MinecraftForge.EVENT_BUS.register(StarlightNetHandler.class);
        MinecraftForge.EVENT_BUS.register(com.xiaoqian.untitled.client.render.BinderBindingHandler.class);
        MinecraftForge.EVENT_BUS.register(com.xiaoqian.untitled.client.render.BinderScrollHandler.class);
        MinecraftForge.EVENT_BUS.register(com.xiaoqian.untitled.client.render.RitualNodeInteractionHandler.class);
    }
}