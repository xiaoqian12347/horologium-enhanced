package com.xiaoqian.untitled.init;

import com.xiaoqian.untitled.Untitled;
import com.xiaoqian.untitled.items.ItemStarlightBinder;
import hellfirepvp.astralsorcery.common.registry.RegistryItems;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Untitled.MODID)
public class ModItems {

    public static Item starlightBinder = new ItemStarlightBinder("starlight_binder", RegistryItems.creativeTabAstralSorcery);

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(starlightBinder);
        Untitled.logger.info("Registered items");
    }
}
