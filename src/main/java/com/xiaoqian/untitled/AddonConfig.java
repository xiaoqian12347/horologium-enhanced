package com.xiaoqian.untitled;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.Type;

@Config(modid = Untitled.MODID, type = Type.INSTANCE, category = "display")
public class AddonConfig {

    @Comment("Show starlight values above altars.")
    @LangKey("horologium_positioning.config.show_altar")
    public static boolean showAltar = true;

    @Comment("Show ritual pedestal buffer and constellation information.")
    @LangKey("horologium_positioning.config.show_pedestal")
    public static boolean showPedestal = true;

    @Comment("Show tree beacon charge values.")
    @LangKey("horologium_positioning.config.show_tree_beacon")
    public static boolean showTreeBeacon = true;

    @Comment("Show starlight values for the celestial bore.")
    @LangKey("horologium_positioning.config.show_bore")
    public static boolean showBore = true;

    @Comment("Show the current constellation on attunement altars.")
    @LangKey("horologium_positioning.config.show_attunement_altar")
    public static boolean showAttunementAltar = true;
}
