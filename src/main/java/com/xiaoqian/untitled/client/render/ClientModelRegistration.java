package com.xiaoqian.untitled.client.render;

import com.xiaoqian.untitled.init.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientModelRegistration {

    private ClientModelRegistration() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerModel(ModItems.starlightBinder);
    }

    private static void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(
                item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }
}
