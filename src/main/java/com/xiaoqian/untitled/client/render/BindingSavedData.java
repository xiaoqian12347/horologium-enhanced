package com.xiaoqian.untitled.client.render;

import com.xiaoqian.untitled.Untitled;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.io.*;
import java.util.*;

public class BindingSavedData {

    private static File getFile(World world) {
        File worldDir = world.getSaveHandler().getWorldDirectory();
        return new File(worldDir, "data/horologium_positioning_bindings.dat");
    }

    public static void save(World world) {
        try {
            Map<BlockPos, List<BlockPos>> bindings = BinderBindingHandler.getServerBindings();
            NBTTagCompound root = new NBTTagCompound();
            NBTTagList list = new NBTTagList();
            for (Map.Entry<BlockPos, List<BlockPos>> entry : bindings.entrySet()) {
                NBTTagCompound entryTag = new NBTTagCompound();
                entryTag.setTag("node", NBTUtil.createPosTag(entry.getKey()));
                NBTTagList machinesTag = new NBTTagList();
                for (BlockPos m : entry.getValue()) {
                    machinesTag.appendTag(NBTUtil.createPosTag(m));
                }
                entryTag.setTag("machines", machinesTag);
                list.appendTag(entryTag);
            }
            root.setTag("bindings", list);

            File file = getFile(world);
            file.getParentFile().mkdirs();
            CompressedStreamTools.writeCompressed(root, new FileOutputStream(file));
            Untitled.logger.info("[Binder-SaveData] Saved {} binding entries to {}", bindings.size(), file.getAbsolutePath());
        } catch (Exception e) {
            Untitled.logger.error("[Binder-SaveData] Save failed: {}", e.toString());
        }
    }

    public static void load(World world) {
        try {
            File file = getFile(world);
            if (!file.exists()) {
                Untitled.logger.info("[Binder-SaveData] No save file found");
                return;
            }
            NBTTagCompound root = CompressedStreamTools.readCompressed(new FileInputStream(file));
            Map<BlockPos, List<BlockPos>> bindings = new HashMap<>();
            if (root.hasKey("bindings", Constants.NBT.TAG_LIST)) {
                NBTTagList list = root.getTagList("bindings", Constants.NBT.TAG_COMPOUND);
                Untitled.logger.info("[Binder-SaveData] Loading {} binding entries", list.tagCount());
                for (int i = 0; i < list.tagCount(); i++) {
                    NBTTagCompound entry = list.getCompoundTagAt(i);
                    BlockPos node = NBTUtil.getPosFromTag(entry.getCompoundTag("node"));
                    NBTTagList machinesTag = entry.getTagList("machines", Constants.NBT.TAG_COMPOUND);
                    List<BlockPos> machines = new ArrayList<>();
                    for (int j = 0; j < machinesTag.tagCount(); j++) {
                        machines.add(NBTUtil.getPosFromTag(machinesTag.getCompoundTagAt(j)));
                    }
                    bindings.put(node, machines);
                    Untitled.logger.info("[Binder-SaveData]   Node {} -> {} machines", node, machines.size());
                }
            }
            BinderBindingHandler.loadServerBindings(bindings);
        } catch (Exception e) {
            Untitled.logger.error("[Binder-SaveData] Load failed: {}", e.toString());
        }
    }
}
