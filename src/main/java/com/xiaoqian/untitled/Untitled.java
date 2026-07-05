package com.xiaoqian.untitled;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = Untitled.MODID, name = Untitled.NAME, version = Untitled.VERSION)
public class Untitled {
    public static final String MODID = "horologium_positioning";
    public static final String NAME = "时钟 定位 | Horologium Positioning";
    public static final String VERSION = "1.03";

    public static Logger logger;

    @SidedProxy(
            clientSide = "com.xiaoqian.untitled.proxy.ClientProxy",
            serverSide = "com.xiaoqian.untitled.proxy.CommonProxy"
    )
    public static com.xiaoqian.untitled.proxy.CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Astral Sorcery Addon pre-initializing...");
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Astral Sorcery Addon initialized.");
        proxy.init(event);
    }
}
