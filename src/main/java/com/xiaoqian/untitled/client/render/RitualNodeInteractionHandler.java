package com.xiaoqian.untitled.client.render;

import com.xiaoqian.untitled.items.ItemStarlightBinder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Handles right-click interactions on ritual pedestals and anchors.
 * Separated from BinderBindingHandler for clarity.
 */
public class RitualNodeInteractionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) return;

        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItem(event.getHand());

        TileEntity te = event.getWorld().getTileEntity(event.getPos());
        if (te == null) return;

        String className = te.getClass().getName();
        boolean isPedestal = className.contains("TileRitualPedestal");
        boolean isAnchor = className.contains("TileRitualLink");
        if (!isPedestal && !isAnchor) return;

        BlockPos pos = event.getPos();
        World world = event.getWorld();

        // Empty hand + non-sneaking + pedestal: show linked anchor coords
        if (held.isEmpty() && isPedestal && !player.isSneaking()) {
            BlockPos anchorPos = resolveAnchorPos(te);

            if (anchorPos != null) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "[基座] " + TextFormatting.AQUA
                    + "仪式锚坐标: "
                    + TextFormatting.WHITE + "X:" + anchorPos.getX()
                    + " Y:" + anchorPos.getY()
                    + " Z:" + anchorPos.getZ()));
            } else {
                player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "[基座] " + TextFormatting.YELLOW
                    + "未链接仪式锚"));
            }
            event.setCanceled(true);
            return;
        }

        // Wand logic below
        if (held.isEmpty() || !(held.getItem() instanceof ItemStarlightBinder)) return;

        boolean cancel = false;
        if (isPedestal) {
            cancel = handlePedestalWand(player, world, pos, te);
        } else {
            cancel = handleAnchorWand(player, te);
        }
        if (cancel) {
            event.setCanceled(true);
        }
    }

    /**
     * Handle wand right-click on pedestal.
     * @return true to cancel the event (block further processing), false to let it pass through.
     */
    private static boolean handlePedestalWand(EntityPlayer player, World world, BlockPos pos, TileEntity te) {
        BlockPos anchorPos = resolveAnchorPos(te);

        if (!player.isSneaking()) {
            // Show binding info: use anchor pos only when anchor has bindings
            BlockPos lookupPos = pos;
            if (anchorPos != null) {
                List<BlockPos> anchorBindings = BinderBindingHandler.getServerBindings().get(anchorPos);
                if (anchorBindings != null && !anchorBindings.isEmpty()) {
                    lookupPos = anchorPos;
                }
            }
            List<BlockPos> bound = BinderBindingHandler.getServerBindings().get(lookupPos);
            int count = (bound != null) ? bound.size() : 0;
            int max = BinderBindingHandler.getMaxBindingCount();
            int range = BinderBindingHandler.getEffectiveRangeForNode(world, pos);

            if (count > 0) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "[Binder] " + TextFormatting.GREEN + "已连接: "
                    + TextFormatting.AQUA + count + "/" + max
                    + TextFormatting.GREEN + " | 范围: " + TextFormatting.AQUA + range + " 格"));
            } else {
                player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "[Binder] " + TextFormatting.YELLOW + "暂无连接"
                    + TextFormatting.GRAY + " | 上限: " + max + " | 范围: " + range + " 格"));
            }
            return true; // Cancel: info already shown, prevent onItemUse duplicate
        } else {
            // Sneaking + bind mode: block if anchor already has bindings
            ItemStack held = player.getHeldItemMainhand();
            int mode = ItemStarlightBinder.getMode(held);

            if (mode == ItemStarlightBinder.MODE_BIND && anchorPos != null) {
                List<BlockPos> anchorBindings = BinderBindingHandler.getServerBindings().get(anchorPos);
                if (anchorBindings != null && !anchorBindings.isEmpty()) {
                    player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "[Binder] " + TextFormatting.RED
                        + "对应仪式锚已有绑定，无法在此绑定"));
                    return true; // Cancel: block the bind
                }
            }
            // Unbind mode or no mutual exclusion: let AS handle crystal removal, let onItemUse handle unbind
            return false;
        }
    }

    /**
     * Handle wand right-click on anchor.
     * @return true to cancel the event (block further processing), false to let it pass through.
     */
    private static boolean handleAnchorWand(EntityPlayer player, TileEntity te) {
        // Non-sneaking: don't handle here, let onItemUse show info
        if (!player.isSneaking()) return false;

        // Sneaking + bind mode: block if pedestal already has bindings
        ItemStack held = player.getHeldItemMainhand();
        int mode = ItemStarlightBinder.getMode(held);

        if (mode == ItemStarlightBinder.MODE_BIND) {
            TileEntity pedestalTE = BinderBindingHandler.resolveToPedestal(te);
            if (pedestalTE != null) {
                BlockPos pedestalPos = pedestalTE.getPos();
                List<BlockPos> pedestalBindings = BinderBindingHandler.getServerBindings().get(pedestalPos);
                if (pedestalBindings != null && !pedestalBindings.isEmpty()) {
                    player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "[Binder] " + TextFormatting.RED
                        + "对应仪式基座已有绑定，无法在此绑定"));
                    return true; // Cancel: block the bind
                }
            }
        }
        // Unbind mode or no mutual exclusion: let onItemUse handle it
        return false;
    }

    // ========== Utility: resolve pedestal -> linked anchor position ==========

    /**
     * Resolves a ritual pedestal's linked anchor position via reflection.
     * Returns null if the pedestal has no linked anchor.
     */
    public static BlockPos resolveAnchorPos(TileEntity te) {
        try {
            Method getCache = findMethod(te.getClass(), "getUpdateCache");
            if (getCache != null) {
                Object receiver = getCache.invoke(te);
                if (receiver != null) {
                    Field linkField = findField(receiver.getClass(), "ritualLinkTo");
                    if (linkField != null) {
                        return (BlockPos) linkField.get(receiver);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Method findMethod(Class<?> c, String name) {
        while (c != null) {
            try { Method m = c.getDeclaredMethod(name); m.setAccessible(true); return m; }
            catch (NoSuchMethodException e) { c = c.getSuperclass(); }
        }
        return null;
    }

    private static Field findField(Class<?> c, String name) {
        while (c != null) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        return null;
    }
}
