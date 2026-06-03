package com.xiaoqian.untitled.client.render;

import com.xiaoqian.untitled.items.ItemStarlightBinder;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
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

        // Only switch mode when holding Shift + scroll
        if (!mc.player.isSneaking()) return;

        ItemStack stack = mc.player.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemStarlightBinder)) return;

        // Cancel the event to prevent hotbar slot change
        event.setCanceled(true);

        int currentMode = ItemStarlightBinder.getMode(stack);
        int newMode;
        if (dwheel > 0) {
            newMode = (currentMode + 1) % ItemStarlightBinder.MODE_COUNT;
        } else {
            newMode = (currentMode - 1 + ItemStarlightBinder.MODE_COUNT) % ItemStarlightBinder.MODE_COUNT;
        }

        // Update client-side NBT
        ItemStarlightBinder.setMode(stack, newMode);

        // Send to server
        StarlightNetHandler.CHANNEL.sendToServer(new StarlightNetHandler.ModeSyncMsg(newMode));

        // Show mode change message
        String modeName;
        if (newMode == ItemStarlightBinder.MODE_BIND) {
            modeName = "\u00a7a绑定模式";
        } else {
            modeName = "\u00a7c解绑模式";
        }
        mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                "\u00a77[Binder] " + modeName));
    }
}
