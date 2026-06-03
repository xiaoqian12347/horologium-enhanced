package com.xiaoqian.untitled;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.Type;

@Config(modid = Untitled.MODID, type = Type.INSTANCE, category = "display")
public class AddonConfig {

    @Comment("是否显示祭坛上方的星辉值")
    @LangKey("horologium_positioning.config.show_altar")
    public static boolean showAltar = true;

    @Comment("是否显示仪式基座的缓冲量和共鸣星座")
    @LangKey("horologium_positioning.config.show_pedestal")
    public static boolean showPedestal = true;

    @Comment("是否显示树之信标的充能值")
    @LangKey("horologium_positioning.config.show_tree_beacon")
    public static boolean showTreeBeacon = true;

    @Comment("是否显示万象泉的星辉值")
    @LangKey("horologium_positioning.config.show_bore")
    public static boolean showBore = true;

    @Comment("是否显示共鸣祭坛上当前共鸣的星座")
    @LangKey("horologium_positioning.config.show_attunement_altar")
    public static boolean showAttunementAltar = true;
}