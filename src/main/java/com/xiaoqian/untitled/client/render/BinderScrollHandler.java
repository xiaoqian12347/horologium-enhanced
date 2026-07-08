package com.xiaoqian.untitled.client.render;

import com.xiaoqian.untitled.items.ItemStarlightBinder;
import com.xiaoqian.untitled.network.StarlightNetHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BinderScrollHandler {

    @SubscribeEvent
    public static void onMouseScroll(MouseEvent event) {
        int dwheel = event.getDwheel();
        if (dwheel == 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        if (!mc.player.isSneaking()) return;

        ItemStack stack = mc.player.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemStarlightBinder)) return;

        event.setCanceled(true);

        int currentMode = ItemStarlightBinder.getMode(stack);
        int newMode;
        if (dwheel > 0) {
            newMode = (currentMode + 1) % ItemStarlightBinder.MODE_COUNT;
        } else {
            newMode = (currentMode - 1 + ItemStarlightBinder.MODE_COUNT) % ItemStarlightBinder.MODE_COUNT;
        }

        ItemStarlightBinder.setMode(stack, newMode);

        StarlightNetHandler.CHANNEL.sendToServer(new StarlightNetHandler.ModeSyncMsg(newMode));

        String modeName;
        if (newMode == ItemStarlightBinder.MODE_BIND) {
            modeName = "\u00a7a" + I18n.format("tooltip.horologium_positioning.binder.mode_bind");
        } else {
            modeName = "\u00a7c" + I18n.format("tooltip.horologium_positioning.binder.mode_unbind");
        }
        mc.player.sendMessage(new TextComponentString(
                I18n.format("message.horologium_positioning.binder.mode_changed", modeName)));
    }
}
