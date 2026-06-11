package com.xiaoqian.untitled.items;

import com.xiaoqian.untitled.Untitled;
import com.xiaoqian.untitled.client.render.BinderBindingHandler;
import com.xiaoqian.untitled.util.LocalizationUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ItemStarlightBinder extends ModItemBase {

    public static final int MODE_BIND = 0;
    public static final int MODE_UNBIND = 1;
    public static final int MODE_COUNT = 2;

    public ItemStarlightBinder(String name, CreativeTabs tab) {
        super(name, tab);
        setMaxStackSize(1);
    }

    public static int getMode(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return MODE_BIND;
        return stack.getTagCompound().getInteger("mode");
    }

    public static void setMode(ItemStack stack, int mode) {
        if (stack.isEmpty()) return;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }
        nbt.setInteger("mode", mode);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking() && hand == EnumHand.MAIN_HAND) {
            if (!world.isRemote) {
                clearTargets(stack);
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + LocalizationUtil.getBinderCleared()));
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                       EnumHand hand, net.minecraft.util.EnumFacing facing,
                                       float hitX, float hitY, float hitZ) {
        if (world.isRemote) return EnumActionResult.SUCCESS;

        ItemStack stack = player.getHeldItem(hand);
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return EnumActionResult.PASS;

        // Normal right-click (no shift) on ritual node �� show binding info
        if (!player.isSneaking()) {
            if (isRitualNode(te)) {
                List<BlockPos> bound = BinderBindingHandler.getServerBindings().get(pos);
                int count = (bound != null) ? bound.size() : 0;
                int max = BinderBindingHandler.getMaxBindingCount();
                int range = BinderBindingHandler.getEffectiveRangeForNode(world, pos);
                if (count > 0) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + LocalizationUtil.getBinderConnected(count, max, range)));
                } else {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + LocalizationUtil.getBinderNoConnection(max, range)));
                }
                return EnumActionResult.SUCCESS;
            }
            return EnumActionResult.PASS;
        }

        // Shift + right-click �� bind/unbind
        int mode = getMode(stack);

        if (mode == MODE_UNBIND) {
            return handleUnbind(player, world, pos, te, stack);
        }

        return handleBind(player, world, pos, te, stack);
    }

    private EnumActionResult handleBind(EntityPlayer player, World world, BlockPos pos, TileEntity te, ItemStack stack) {
        if (isRitualNode(te)) {
            List<BlockPos> targets = getTargets(stack);
            if (targets.isEmpty()) {
                if (BinderBindingHandler.getServerBindings().containsKey(pos)) {
                    BinderBindingHandler.unbindNode(pos);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + LocalizationUtil.getBinderUnboundNode()));
                } else {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + LocalizationUtil.getBinderNoMachineSelected()));
                }
                return EnumActionResult.SUCCESS;
            }
List<BlockPos> outOfRange = BinderBindingHandler.bindMachinesToNode(world, pos, targets);
            int boundCount = targets.size() - outOfRange.size();
            if (boundCount > 0) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + LocalizationUtil.getBinderBoundMachines(boundCount)));
            }
            if (!outOfRange.isEmpty()) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + LocalizationUtil.getBinderOutOfRange(outOfRange.size(),
                                BinderBindingHandler.getEffectiveRangeForNode(world, pos))));
            }
            clearTargets(stack);
            return EnumActionResult.SUCCESS;
        }

        if (isMachine(te)) {
            List<BlockPos> targets = getTargets(stack);
            for (BlockPos t : targets) {
                if (t.equals(pos)) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + LocalizationUtil.getBinderAlreadyInList()));
                    return EnumActionResult.SUCCESS;
                }
            }
            addTarget(stack, pos);
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + LocalizationUtil.getBinderSelected(targets.size(),
                            pos.getX(), pos.getY(), pos.getZ())));
            return EnumActionResult.SUCCESS;
        }

        return EnumActionResult.PASS;
    }

    private EnumActionResult handleUnbind(EntityPlayer player, World world, BlockPos pos, TileEntity te, ItemStack stack) {
        // Unbind by clicking ritual node
        if (isRitualNode(te)) {
            boolean removed = BinderBindingHandler.unbindNode(pos);
            if (removed) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + LocalizationUtil.getBinderUnboundNode()));
            } else {
                // Try resolving ritual link to pedestal
                String cn = te.getClass().getName();
                if (cn.contains("TileRitualLink")) {
                    try {
                        java.lang.reflect.Method g = te.getClass().getMethod("getLinkedTo");
                        BlockPos linked = (BlockPos) g.invoke(te);
                        if (linked != null) {
                            removed = BinderBindingHandler.unbindNode(linked);
                        }
                    } catch (Exception ignored) {}
                }
                if (removed) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + LocalizationUtil.getBinderUnboundNode()));
                } else {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + LocalizationUtil.getBinderNotBound()));
                }
            }
            return EnumActionResult.SUCCESS;
        }

        // Unbind by clicking a bound machine
        if (isMachine(te)) {
            Map<BlockPos, List<BlockPos>> bindings = BinderBindingHandler.getServerBindings();
            for (Map.Entry<BlockPos, List<BlockPos>> entry : bindings.entrySet()) {
                List<BlockPos> machines = entry.getValue();
                if (machines.contains(pos)) {
                    machines.remove(pos);
                    BinderBindingHandler.onMachineRemoved(pos);
                    BinderBindingHandler.triggerSave(world);
                    Untitled.logger.info("[Binder] Removed machine {} from node {}, remaining: {}",
                            pos, entry.getKey(), machines.size());
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + LocalizationUtil.getBinderMachineRemoved()));
                    if (machines.isEmpty()) {
                        BinderBindingHandler.unbindNode(entry.getKey());
                        Untitled.logger.info("[Binder] Node {} unbound (no machines left)", entry.getKey());
                        player.sendMessage(new TextComponentString(
                                TextFormatting.GRAY + LocalizationUtil.getBinderNodeUnbound()));
                    }
                    return EnumActionResult.SUCCESS;
                }
            }
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + LocalizationUtil.getBinderNotBound()));
            return EnumActionResult.SUCCESS;
        }

        return EnumActionResult.PASS;
    }

    private boolean isRitualNode(TileEntity te) {
        String className = te.getClass().getName();
        return className.contains("TileRitualPedestal") || className.contains("TileRitualLink");
    }

    private boolean isMachine(TileEntity te) {
        return te.hasCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
                || te instanceof net.minecraft.tileentity.TileEntityFurnace
                || te instanceof net.minecraft.tileentity.TileEntityHopper
                || te instanceof net.minecraft.tileentity.TileEntityBrewingStand
                || te instanceof net.minecraft.tileentity.TileEntityDispenser;
    }

    public static List<BlockPos> getTargets(ItemStack stack) {
        List<BlockPos> list = new ArrayList<>();
        if (stack.isEmpty() || !stack.hasTagCompound()) return list;
        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey("targets", Constants.NBT.TAG_LIST)) return list;
        NBTTagList tagList = nbt.getTagList("targets", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            list.add(NBTUtil.getPosFromTag(tagList.getCompoundTagAt(i)));
        }
        return list;
    }

    public static void addTarget(ItemStack stack, BlockPos pos) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }
        NBTTagList tl = nbt.hasKey("targets", Constants.NBT.TAG_LIST)
                ? nbt.getTagList("targets", Constants.NBT.TAG_COMPOUND) : new NBTTagList();
        tl.appendTag(NBTUtil.createPosTag(pos));
        nbt.setTag("targets", tl);
    }

    public static void clearTargets(ItemStack stack) {
        if (stack.hasTagCompound()) stack.getTagCompound().removeTag("targets");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int mode = getMode(stack);
        if (mode == MODE_BIND) {
            tooltip.add(TextFormatting.GREEN + LocalizationUtil.getBinderBindMode());
        } else {
            tooltip.add(TextFormatting.RED + LocalizationUtil.getBinderUnbindMode());
        }
        tooltip.add(TextFormatting.GRAY + LocalizationUtil.getBinderScrollHint());
        tooltip.add(TextFormatting.GRAY + LocalizationUtil.getBinderRightClickHint());
    }
}
