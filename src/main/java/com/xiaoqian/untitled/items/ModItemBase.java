package com.xiaoqian.untitled.items;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ModItemBase extends Item {

    public ModItemBase(String name, CreativeTabs tab) {
        setRegistryName(name);
        setUnlocalizedName(name);
        setCreativeTab(tab);
    }
}
