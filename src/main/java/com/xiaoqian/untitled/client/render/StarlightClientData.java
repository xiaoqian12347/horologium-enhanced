package com.xiaoqian.untitled.client.render;

import com.xiaoqian.untitled.common.binding.BinderBindingHandler;
import com.xiaoqian.untitled.network.StarlightNetHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public final class StarlightClientData {

    private static final Map<BlockPos, CachedData> CACHE = new ConcurrentHashMap<>();

    private StarlightClientData() {
    }

    public static void handleSync(StarlightNetHandler.SyncMsg msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            CachedData d = new CachedData();
            d.type = msg.type;
            d.val1 = msg.val1;
            d.val2 = msg.val2;
            d.val3 = msg.val3;
            d.flags = msg.flags;
            d.tick = 0;
            CACHE.put(new BlockPos(msg.x, msg.y, msg.z), d);
        });
    }

    public static void handleBindingSync(StarlightNetHandler.BindSyncMsg msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Map<BlockPos, List<BlockPos>> data = new HashMap<>();
            for (int i = 0; i < msg.count; i++) {
                BlockPos node = new BlockPos(msg.nodeX[i], msg.nodeY[i], msg.nodeZ[i]);
                List<BlockPos> machines = new ArrayList<>();
                for (int j = 0; j < msg.machX[i].length; j++) {
                    machines.add(new BlockPos(
                            msg.machX[i][j], msg.machY[i][j], msg.machZ[i][j]));
                }
                data.put(node, machines);
            }
            BinderBindingHandler.updateClientBindings(data);
        });
    }

    public static void tickCache() {
        CACHE.values().removeIf(d -> ++d.tick > 60);
    }

    public static CachedData get(BlockPos pos) {
        return CACHE.get(pos);
    }

    public static class CachedData {
        public byte type;
        public double val1;
        public int val2, val3;
        public byte flags;
        public int tick;
    }
}
