package com.xiaoqian.untitled.common.binding;

import com.xiaoqian.untitled.items.ItemStarlightBinder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 仪式节点交互。
 */
public class RitualNodeInteractionHandler {

    private static TextComponentTranslation binderTr(String key, Object... args) {
        return new TextComponentTranslation("message.horologium_positioning.binder." + key, args);
    }

    private static TextComponentTranslation pedestalTr(String key, Object... args) {
        return new TextComponentTranslation("message.horologium_positioning.pedestal." + key, args);
    }

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

        // 显示锚点坐标
        if (held.isEmpty() && isPedestal && !player.isSneaking()) {
            BlockPos anchorPos = resolveAnchorPos(te);

            if (anchorPos != null) {
                player.sendMessage(pedestalTr("anchor_pos",
                        anchorPos.getX(), anchorPos.getY(), anchorPos.getZ()));
            } else {
                player.sendMessage(pedestalTr("no_anchor"));
            }
            event.setCanceled(true);
            return;
        }

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

    /** 处理基座右键。 */
    private static boolean handlePedestalWand(EntityPlayer player, World world, BlockPos pos, TileEntity te) {
        BlockPos anchorPos = resolveAnchorPos(te);

        if (!player.isSneaking()) {
            // 锚点优先
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
                player.sendMessage(binderTr("connected", count, max, range));
            } else {
                player.sendMessage(binderTr("no_connection", max, range));
            }
            return true; // 避免重复提示
        } else {
            // 锚点互斥
            ItemStack held = player.getHeldItemMainhand();
            int mode = ItemStarlightBinder.getMode(held);

            if (mode == ItemStarlightBinder.MODE_BIND && anchorPos != null) {
                List<BlockPos> anchorBindings = BinderBindingHandler.getServerBindings().get(anchorPos);
                if (anchorBindings != null && !anchorBindings.isEmpty()) {
                    player.sendMessage(binderTr("anchor_already_bound"));
                    return true; // 阻止绑定
                }
            }
            // 交给 AS/onItemUse
            return false;
        }
    }

    /** 处理锚点右键。 */
    private static boolean handleAnchorWand(EntityPlayer player, TileEntity te) {
        if (!player.isSneaking()) return false;

        // 基座互斥
        ItemStack held = player.getHeldItemMainhand();
        int mode = ItemStarlightBinder.getMode(held);

        if (mode == ItemStarlightBinder.MODE_BIND) {
            TileEntity pedestalTE = BinderBindingHandler.resolveToPedestal(te);
            if (pedestalTE != null) {
                BlockPos pedestalPos = pedestalTE.getPos();
                List<BlockPos> pedestalBindings = BinderBindingHandler.getServerBindings().get(pedestalPos);
                if (pedestalBindings != null && !pedestalBindings.isEmpty()) {
                    player.sendMessage(binderTr("pedestal_already_bound"));
                    return true; // 阻止绑定
                }
            }
        }
        return false;
    }

    // 基座转锚点

    /** 读取链接锚点。 */
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
