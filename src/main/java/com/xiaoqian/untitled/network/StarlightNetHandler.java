package com.xiaoqian.untitled.network;

import com.xiaoqian.untitled.Untitled;
import com.xiaoqian.untitled.common.binding.BinderBindingHandler;
import hellfirepvp.astralsorcery.common.tile.TileAltar;
import hellfirepvp.astralsorcery.common.tile.TileAttunementRelay;
import hellfirepvp.astralsorcery.common.tile.TileBore;
import hellfirepvp.astralsorcery.common.tile.TileRitualPedestal;
import hellfirepvp.astralsorcery.common.tile.TileTreeBeacon;
import hellfirepvp.astralsorcery.common.tile.TileWell;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StarlightNetHandler {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel("horologium_pos_sl");

    private static int disc = 0;
    private static int serverTick = 0;

    public static void init() {
        CHANNEL.registerMessage(SyncHandler.class, SyncMsg.class, disc++, Side.CLIENT);
        CHANNEL.registerMessage(BindSyncHandler.class, BindSyncMsg.class, disc++, Side.CLIENT);
        CHANNEL.registerMessage(ModeSyncHandler.class, ModeSyncMsg.class, disc++, Side.SERVER);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++serverTick % 20 != 0) return;

        net.minecraft.server.MinecraftServer server =
                FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        for (net.minecraft.world.World world : server.worlds) {
            if (!(world instanceof WorldServer)) continue;
            WorldServer ws = (WorldServer) world;
            int dim = ws.provider.getDimension();

            for (TileEntity te : ws.loadedTileEntityList) {
                try {
                    SyncMsg msg = buildSyncMsg(te, dim);
                    if (msg != null) {
                        double px = te.getPos().getX() + 0.5;
                        double py = te.getPos().getY() + 0.5;
                        double pz = te.getPos().getZ() + 0.5;
                        CHANNEL.sendToAllAround(msg,
                                new NetworkRegistry.TargetPoint(dim, px, py, pz, 48.0));
                    }
                } catch (Exception ignored) {}
            }
        }

        syncBindings(server);
    }

    private static void syncBindings(net.minecraft.server.MinecraftServer server) {
        Map<BlockPos, List<BlockPos>> bindings = BinderBindingHandler.getServerBindings();

        for (net.minecraft.world.World world : server.worlds) {
            if (!(world instanceof WorldServer)) continue;
            BindSyncMsg msg = new BindSyncMsg(bindings);
            for (net.minecraft.entity.player.EntityPlayer p :
                    ((WorldServer) world).playerEntities) {
                if (p instanceof net.minecraft.entity.player.EntityPlayerMP) {
                    CHANNEL.sendTo(msg, (net.minecraft.entity.player.EntityPlayerMP) p);
                }
            }
        }
    }

    private static SyncMsg buildSyncMsg(TileEntity te, int dim) {
        BlockPos pos = te.getPos();
        if (te instanceof TileAltar) {
            TileAltar altar = (TileAltar) te;
            return new SyncMsg(dim, pos, (byte) 0,
                    altar.getStarlightStored(), altar.getMaxStarlightStorage(),
                    altar.getAltarLevel().ordinal(), (byte) 0);
        }
        if (te instanceof TileRitualPedestal) {
            TileRitualPedestal pedestal = (TileRitualPedestal) te;
            Object receiver = null;
            try { receiver = pedestal.getUpdateCache(); } catch (Exception ignored) {}
            double buffer = 0;
            int channeled = 0;
            if (receiver != null) {
                try {
                    java.lang.reflect.Field f = receiver.getClass()
                            .getDeclaredField("collectionChannelBuffer");
                    f.setAccessible(true);
                    buffer = f.getDouble(receiver);
                } catch (Exception ignored) {}
                try {
                    java.lang.reflect.Field f = receiver.getClass()
                            .getDeclaredField("channeled");
                    f.setAccessible(true);
                    channeled = f.getInt(receiver);
                } catch (Exception ignored) {}
            }
            boolean working = false;
            try { working = pedestal.isWorking(); } catch (Exception ignored) {}
            boolean hasCrystal = false;
            try {
                net.minecraft.item.ItemStack c = pedestal.getCurrentPedestalCrystal();
                hasCrystal = c != null && !c.isEmpty();
            } catch (Exception ignored) {}
            byte flags = (byte) ((working ? 1 : 0) | (hasCrystal ? 2 : 0));
            return new SyncMsg(dim, pos, (byte) 1, buffer, channeled, 0, flags);
        }
        if (te instanceof TileWell) {
            TileWell well = (TileWell) te;
            double buffer = 0;
            try {
                java.lang.reflect.Field f = TileWell.class
                        .getDeclaredField("starlightBuffer");
                f.setAccessible(true);
                buffer = f.getDouble(well);
            } catch (Exception ignored) {}
            return new SyncMsg(dim, pos, (byte) 2,
                    buffer, (int) (well.getPercFilled() * 1000), 0, (byte) 0);
        }
        if (te instanceof TileTreeBeacon) {
            double charge = 0;
            try {
                java.lang.reflect.Field f = TileTreeBeacon.class
                        .getDeclaredField("starlightCharge");
                f.setAccessible(true);
                charge = f.getDouble(te);
            } catch (Exception ignored) {}
            return new SyncMsg(dim, pos, (byte) 3, charge, 0, 0, (byte) 0);
        }
        if (te instanceof TileBore) {
            int mb = 0;
            try {
                java.lang.reflect.Field f = TileBore.class
                        .getDeclaredField("mbStarlight");
                f.setAccessible(true);
                mb = f.getInt(te);
            } catch (Exception ignored) {}
            return new SyncMsg(dim, pos, (byte) 4, mb, 0, 0, (byte) 0);
        }
        if (te instanceof TileAttunementRelay) {
            float mult = 0;
            try {
                java.lang.reflect.Field f = TileAttunementRelay.class
                        .getDeclaredField("collectionMultiplier");
                f.setAccessible(true);
                mult = f.getFloat(te);
            } catch (Exception ignored) {}
            boolean hasMB = false;
            try {
                java.lang.reflect.Field f = TileAttunementRelay.class
                        .getDeclaredField("hasMultiblock");
                f.setAccessible(true);
                hasMB = f.getBoolean(te);
            } catch (Exception ignored) {}
            return new SyncMsg(dim, pos, (byte) 5, mult, 0, 0,
                    (byte) (hasMB ? 1 : 0));
        }
        return null;
    }

    public static class SyncMsg implements IMessage {
        public int dim, x, y, z;
        public byte type;
        public double val1;
        public int val2, val3;
        public byte flags;

        public SyncMsg() {}

        SyncMsg(int dim, BlockPos pos, byte type,
                double val1, int val2, int val3, byte flags) {
            this.dim = dim;
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.type = type;
            this.val1 = val1;
            this.val2 = val2;
            this.val3 = val3;
            this.flags = flags;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            dim = buf.readInt();
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
            type = buf.readByte();
            val1 = buf.readDouble();
            val2 = buf.readInt();
            val3 = buf.readInt();
            flags = buf.readByte();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(dim);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeByte(type);
            buf.writeDouble(val1);
            buf.writeInt(val2);
            buf.writeInt(val3);
            buf.writeByte(flags);
        }
    }

    public static class SyncHandler implements IMessageHandler<SyncMsg, IMessage> {
        @Override
        public IMessage onMessage(SyncMsg msg, MessageContext ctx) {
            Untitled.proxy.handleStarlightSync(msg);
            return null;
        }
    }

    public static class BindSyncMsg implements IMessage {
        public int[] nodeX, nodeY, nodeZ;
        public int[][] machX, machY, machZ;
        public int count;

        public BindSyncMsg() {}

        BindSyncMsg(Map<BlockPos, List<BlockPos>> bindings) {
            List<BlockPos> keys = new ArrayList<>(bindings.keySet());
            this.count = keys.size();
            nodeX = new int[count];
            nodeY = new int[count];
            nodeZ = new int[count];
            machX = new int[count][];
            machY = new int[count][];
            machZ = new int[count][];
            for (int i = 0; i < count; i++) {
                BlockPos k = keys.get(i);
                nodeX[i] = k.getX();
                nodeY[i] = k.getY();
                nodeZ[i] = k.getZ();
                List<BlockPos> machines = bindings.get(k);
                int mCount = machines.size();
                machX[i] = new int[mCount];
                machY[i] = new int[mCount];
                machZ[i] = new int[mCount];
                for (int j = 0; j < mCount; j++) {
                    BlockPos m = machines.get(j);
                    machX[i][j] = m.getX();
                    machY[i][j] = m.getY();
                    machZ[i][j] = m.getZ();
                }
            }
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            count = buf.readInt();
            nodeX = new int[count];
            nodeY = new int[count];
            nodeZ = new int[count];
            machX = new int[count][];
            machY = new int[count][];
            machZ = new int[count][];
            for (int i = 0; i < count; i++) {
                nodeX[i] = buf.readInt();
                nodeY[i] = buf.readInt();
                nodeZ[i] = buf.readInt();
                int mc = buf.readInt();
                machX[i] = new int[mc];
                machY[i] = new int[mc];
                machZ[i] = new int[mc];
                for (int j = 0; j < mc; j++) {
                    machX[i][j] = buf.readInt();
                    machY[i][j] = buf.readInt();
                    machZ[i][j] = buf.readInt();
                }
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(count);
            for (int i = 0; i < count; i++) {
                buf.writeInt(nodeX[i]);
                buf.writeInt(nodeY[i]);
                buf.writeInt(nodeZ[i]);
                int mc = machX[i].length;
                buf.writeInt(mc);
                for (int j = 0; j < mc; j++) {
                    buf.writeInt(machX[i][j]);
                    buf.writeInt(machY[i][j]);
                    buf.writeInt(machZ[i][j]);
                }
            }
        }
    }

    public static class BindSyncHandler implements IMessageHandler<BindSyncMsg, IMessage> {
        @Override
        public IMessage onMessage(BindSyncMsg msg, MessageContext ctx) {
            Untitled.proxy.handleBindingSync(msg);
            return null;
        }
    }

    public static class ModeSyncMsg implements IMessage {
        int mode;

        public ModeSyncMsg() {}

        public ModeSyncMsg(int mode) {
            this.mode = mode;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            mode = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(mode);
        }
    }

    public static class ModeSyncHandler implements IMessageHandler<ModeSyncMsg, IMessage> {
        @Override
        public IMessage onMessage(ModeSyncMsg msg, MessageContext ctx) {
            net.minecraft.entity.player.EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                net.minecraft.item.ItemStack stack = player.getHeldItemMainhand();
                if (!stack.isEmpty() && stack.getItem() instanceof com.xiaoqian.untitled.items.ItemStarlightBinder) {
                    com.xiaoqian.untitled.items.ItemStarlightBinder.setMode(stack, msg.mode);
                }
            });
            return null;
        }
    }
}
