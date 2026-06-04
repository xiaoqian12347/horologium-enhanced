package com.xiaoqian.untitled.client.render;

import com.xiaoqian.untitled.Untitled;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class BinderBindingHandler {

    private static final Map<BlockPos, List<BlockPos>> BINDINGS = new ConcurrentHashMap<>();
    private static int tickCount = 0;



    private static final Map<BlockPos, List<BlockPos>> CLIENT_BINDINGS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, net.minecraft.nbt.NBTTagCompound> FROZEN_NBT = new ConcurrentHashMap<>();
    private static final java.util.Set<BlockPos> ALCARA_BINDINGS = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<BlockPos, BlockPos> PEDestalCache = new ConcurrentHashMap<>();
    private static final Set<BlockPos> CLIENT_HIGHLIGHTS = ConcurrentHashMap.newKeySet();

/** Bind machines to node. Returns list of machines that are out of range. */
    public static List<BlockPos> bindMachinesToNode(World world, BlockPos ritualNode, List<BlockPos> machines) {
        List<BlockPos> outOfRange = new ArrayList<>();
        // Resolve to pedestal and read its actual CE range
        TileEntity nodeTE = world.getTileEntity(ritualNode);
        TileEntity pedestal = (nodeTE != null) ? resolveToPedestal(nodeTE) : null;
        int range = (pedestal != null) ? getPedestalRange(pedestal) : getHorologiumRange();
        // Center is always the ritual node (anchor/link), not the pedestal
        BlockPos center = ritualNode;
        Untitled.logger.info("[Binder] Binding range={}, center={}", range, center);

        int maxCount = getMaxBindingCount();
        List<BlockPos> existing = BINDINGS.getOrDefault(ritualNode, new CopyOnWriteArrayList<>());
        for (BlockPos m : machines) {
            if (existing.contains(m)) continue;
            // If machine is already bound to another node, remove from old binding (����)
            removeMachineFromOtherNodes(m, ritualNode);
            if (existing.size() >= maxCount) {
                outOfRange.add(m);
                Untitled.logger.info("[Binder] Machine {} rejected (max {} reached)", m, maxCount);
                continue;
            }
            double dist = Math.sqrt(center.distanceSq(m));
            if (dist > range + 0.5) {
                outOfRange.add(m);
                Untitled.logger.info("[Binder] Machine {} is out of range ({} > {})", m, String.format("%.1f", dist), range);
                continue;
            }
            existing.add(m);
        }

        if (!existing.isEmpty()) {
            BINDINGS.put(ritualNode, existing);
            saveBindings(world);
            Untitled.logger.info("[Binder] Bound {} machines to node at {}", existing.size(), ritualNode);
        } else {
            BINDINGS.remove(ritualNode);
        }
        return outOfRange;
    }


    /** Get Horologium search range from AS config (static fallback) */
    public static int getHorologiumRange() {
        try {
            Class<?> ceClass = Class.forName("hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectHorologium");
            java.lang.reflect.Field rangeField = ceClass.getDeclaredField("searchRange");
            rangeField.setAccessible(true);
            return rangeField.getInt(null);
        } catch (Exception e) {
            return 16; // default
        }
    }

    /** Get max binding count from AS config (horologiumCount) */
    public static int getMaxBindingCount() {
        try {
            Class<?> ceClass = Class.forName("hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectHorologium");
            java.lang.reflect.Field countField = ceClass.getDeclaredField("maxCount");
            countField.setAccessible(true);
            return countField.getInt(null);
        } catch (Exception e) {
            return 30; // default
        }
    }

    /** Get the effective range for a given ritual node (resolves to pedestal first) */
    public static int getEffectiveRangeForNode(World world, BlockPos ritualNode) {
        TileEntity te = world.getTileEntity(ritualNode);
        if (te == null) { Untitled.logger.warn("[Binder] getEffectiveRangeForNode: TE null at {}", ritualNode); return getHorologiumRange(); }
        TileEntity pedestal = resolveToPedestal(te);
        int range = (pedestal != null) ? getPedestalRange(pedestal) : getHorologiumRange();
        return range;
    }

    /** Read the actual effective range from a pedestal's CE + crystal properties + minor constellation */
    private static int getPedestalRange(TileEntity pedestal) {
        try {
            Method getCache = findMethod(pedestal.getClass(), "getUpdateCache");
            if (getCache == null) return getHorologiumRange();
            Object receiver = getCache.invoke(pedestal);
            if (receiver == null) return getHorologiumRange();

            // Get CE
            Field ceField = findField(receiver.getClass(), "ce");
            if (ceField == null) return getHorologiumRange();
            ceField.setAccessible(true);
            Object ce = ceField.get(receiver);
            if (ce == null) return getHorologiumRange();

            // Get crystal properties from receiver
            Field propsField = findField(receiver.getClass(), "properties");
            int collective = 100;
            if (propsField != null) {
                propsField.setAccessible(true);
                Object crystalProps = propsField.get(receiver);
                if (crystalProps != null) {
                    Method getCollective = findMethod(crystalProps.getClass(), "getCollectiveCapability");
                    if (getCollective != null) {
                        collective = (int) getCollective.invoke(crystalProps);
                    }
                }
            }

            // Get minor constellation (trait) from receiver
            Field traitField = findField(receiver.getClass(), "trait");
            Object minorConstellation = null;
            if (traitField != null) {
                traitField.setAccessible(true);
                minorConstellation = traitField.get(receiver);
            }

            // Call provideProperties(collective) to get base properties
            Method provideProps = null;
            try {
                provideProps = ce.getClass().getMethod("provideProperties", int.class);
            } catch (NoSuchMethodException e) {
                provideProps = findMethod(ce.getClass(), "provideProperties");
            }
            if (provideProps != null) {
                provideProps.setAccessible(true);
                Object effectProps = provideProps.invoke(ce, collective);
                if (effectProps != null) {
                    // Apply minor constellation modifier (ulteria: size*=0.2, gelu: size*=3.5, etc.)
                    if (minorConstellation != null) {
                        // modify(IMinorConstellation) requires explicit parameter type
                        Method modify = null;
                        for (Class<?> iface : minorConstellation.getClass().getInterfaces()) {
                            try {
                                modify = effectProps.getClass().getMethod("modify", iface);
                                break;
                            } catch (NoSuchMethodException ignored) {}
                        }
                        if (modify == null) {
                            modify = findMethod(effectProps.getClass(), "modify");
                        }
                        if (modify != null) {
                            modify.setAccessible(true);
                            effectProps = modify.invoke(effectProps, minorConstellation);

                        }
                    }
                    Method getSize = findMethod(effectProps.getClass(), "getSize");
                    if (getSize != null) {
                        getSize.setAccessible(true);
                        double size = (double) getSize.invoke(effectProps);
                        int range = (int) Math.ceil(size);

                        return range;
                    }
                }
            }

            // Fallback: read static searchRange
            Field rangeField = findField(ce.getClass(), "searchRange");
            if (rangeField != null) {
                rangeField.setAccessible(true);
                return rangeField.getInt(ce);
            }
        } catch (Exception e) {
            Untitled.logger.warn("[Binder] getPedestalRange failed: {}", e.toString());
        }
        return getHorologiumRange();
    }

    public static boolean unbindNode(BlockPos n) {
        // Clear frozen state for machines bound to this node
        List<BlockPos> machines = BINDINGS.get(n);
        if (machines != null) {
            net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
            for (BlockPos m : machines) {
                FROZEN_NBT.remove(m);
                ALCARA_BINDINGS.remove(m);
                // Restore to tickableTileEntities if frozen
                if (server != null) {
                    for (net.minecraft.world.World w : server.worlds) {
                        net.minecraft.tileentity.TileEntity mte = w.getTileEntity(m);
                        if (mte != null && mte instanceof net.minecraft.util.ITickable && !w.tickableTileEntities.contains(mte)) {
                            w.tickableTileEntities.add(mte);
                        }
                    }
                }
            }
        }
        boolean removed = BINDINGS.remove(n) != null;
if (removed) {
            // Clear CE cache for this node so AS stops accelerating
            clearCECacheForNode(n);
            // Save to world data
            try {
                net.minecraft.server.MinecraftServer server =
                        net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
                if (server != null && server.worlds.length > 0) {
                    saveBindings(server.worlds[0]);
                }
            } catch (Exception ignored) {}
        }
        return removed;
    }

    /** Clear the CE elements cache for a specific ritual node */
    private static void clearCECacheForNode(BlockPos ritualPos) {
        try {
            net.minecraft.server.MinecraftServer server =
                    net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null) return;
            for (net.minecraft.world.World world : server.worlds) {
                TileEntity te = world.getTileEntity(ritualPos);
                if (te == null) continue;
                TileEntity pedestal = resolveToPedestal(te);
                if (pedestal == null) break;
                clearCECache(pedestal);
break;
            }
        } catch (Exception e) {
            Untitled.logger.error("[Binder] clearCECacheForNode error: {}", e.toString());
        }
    }

/** Clear the CE elements cache and null the CE so AS recreates a fresh one */
    private static void clearCECache(TileEntity pedestal) {
        try {
            Method getCache = findMethod(pedestal.getClass(), "getUpdateCache");
            if (getCache == null) return;
            Object receiver = getCache.invoke(pedestal);
            if (receiver == null) return;

            // Clear the CE elements cache
            Field ceField = findField(receiver.getClass(), "ce");
            if (ceField != null) {
                ceField.setAccessible(true);
                Object ce = ceField.get(receiver);
                if (ce != null) {
                    Field elementsField = findField(ce.getClass(), "elements");
                    if (elementsField != null) {
                        elementsField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> elements = (java.util.List<Object>) elementsField.get(ce);
                        elements.clear();
                    }
                }
                // Re-enable CE in case it was disabled by alcara
                Field enabledField = findField(ce.getClass(), "enabled");
                if (enabledField != null) { enabledField.setAccessible(true); enabledField.setBoolean(ce, true); }
                // Set ce to null so AS recreates a fresh CE with original verifier
                ceField.set(receiver, null);

            }
        } catch (Exception ignored) {}
    }

    /** Remove a machine from any other node it is currently bound to (overwrite behavior) */
    private static void removeMachineFromOtherNodes(BlockPos machine, BlockPos keepNode) {
        Iterator<Map.Entry<BlockPos, List<BlockPos>>> it = BINDINGS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, List<BlockPos>> entry = it.next();
            if (entry.getKey().equals(keepNode)) continue;
            List<BlockPos> machines = entry.getValue();
            if (machines.remove(machine)) {
                Untitled.logger.info("[Binder] Overwrite: removed {} from old node {}", machine, entry.getKey());
                onMachineRemoved(machine);
                if (machines.isEmpty()) {
                    clearCECacheForNode(entry.getKey());
                    it.remove();
                    Untitled.logger.info("[Binder] Old node {} fully unbound (no machines left)", entry.getKey());
                }
                break;
            }
        }
    }
    /** Clean up frozen state when a single machine is removed from binding */
    public static void onMachineRemoved(BlockPos machinePos) {
        FROZEN_NBT.remove(machinePos);
        ALCARA_BINDINGS.remove(machinePos);
        net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            for (net.minecraft.world.World w : server.worlds) {
                net.minecraft.tileentity.TileEntity mte = w.getTileEntity(machinePos);
                if (mte != null && mte instanceof net.minecraft.util.ITickable && !w.tickableTileEntities.contains(mte)) {
                    w.tickableTileEntities.add(mte);
                }
            }
        }
    }

    public static Map<BlockPos, List<BlockPos>> getServerBindings() { return BINDINGS; }

    /** Trigger save from external code (e.g. handleUnbind) */
    public static void triggerSave(World world) { saveBindings(world); }

    private static void saveBindings(net.minecraft.world.World world) {
        try {
            if (world != null) {
                BindingSavedData.save(world);
            }
        } catch (Exception e) {
            Untitled.logger.error("[Binder] Save failed: {}", e.toString());
        }
    }

    /** Load bindings from saved data */
    public static void loadServerBindings(Map<BlockPos, List<BlockPos>> data) {
        BINDINGS.clear();
        for (Map.Entry<BlockPos, List<BlockPos>> e : data.entrySet()) { BINDINGS.put(e.getKey(), new CopyOnWriteArrayList<>(e.getValue())); }
    }
    public static Map<BlockPos, List<BlockPos>> getClientBindings() { return CLIENT_BINDINGS; }
    public static void updateClientBindings(Map<BlockPos, List<BlockPos>> d) {
        CLIENT_BINDINGS.clear(); CLIENT_BINDINGS.putAll(d);
    }
    public static Set<BlockPos> getClientHighlights() { return CLIENT_HIGHLIGHTS; }
    public static void updateClientHighlights(List<BlockPos> m) {
        CLIENT_HIGHLIGHTS.clear(); CLIENT_HIGHLIGHTS.addAll(m);
    }

    // Clear bindings when a block is broken
    @SubscribeEvent
    public static void onBlockBreak(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) return;
        BlockPos pos = event.getPos();
        boolean changed = false;

        // If a ritual node is broken, remove its binding
        if (BINDINGS.containsKey(pos)) {
            clearCECacheForNode(pos);
            BINDINGS.remove(pos);
            changed = true;
            Untitled.logger.info("[Binder] Node {} broken, binding removed", pos);
        }

        // If a bound machine is broken, remove it from the binding
        if (!changed) {
            for (Map.Entry<BlockPos, List<BlockPos>> entry : BINDINGS.entrySet()) {
                List<BlockPos> machines = entry.getValue();
                if (machines.remove(pos)) {
                    changed = true;
                    Untitled.logger.info("[Binder] Machine {} broken, removed from node {}", pos, entry.getKey());
                    if (machines.isEmpty()) {
                        clearCECacheForNode(entry.getKey());
                        BINDINGS.remove(entry.getKey());
                    }
                    break;
                }
            }
        }

        if (changed) {
            saveBindings(event.getWorld());
        }
    }


        @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Inject at START: before world tick so AS uses our positions
        // Inject at END: after world tick to repopulate for next tick
        if (event.phase != TickEvent.Phase.START && event.phase != TickEvent.Phase.END) return;

        net.minecraft.server.MinecraftServer server =
                net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        // Reset state if server was restarted
        if (!server.isServerRunning()) {
            if (tickCount > 0) {
                tickCount = 0;
                PEDestalCache.clear();
                BINDINGS.clear();
                ALCARA_BINDINGS.clear();
                FROZEN_NBT.clear();
                Untitled.logger.info("[Binder] Server not running, state reset");
            }
            return;
        }

        tickCount++;

        // Load bindings from world data on first tick
        if (tickCount == 1) {
            try {
                net.minecraft.server.MinecraftServer server2 =
                        net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
                if (server2 != null && server2.worlds.length > 0) {
                    BindingSavedData.load(server2.worlds[0]);
                    Untitled.logger.info("[Binder] Loaded bindings from world data, {} entries", BINDINGS.size());
                }
            } catch (Exception e) {
                Untitled.logger.error("[Binder] Failed to load bindings: {}", e.toString());
            }
        }

        if (BINDINGS.isEmpty()) return;

        for (Map.Entry<BlockPos, List<BlockPos>> entry : BINDINGS.entrySet()) {
            BlockPos ritualPos = entry.getKey();
            List<BlockPos> machines = entry.getValue();
            if (machines.isEmpty()) continue;

            for (World world : server.worlds) {
                TileEntity te = world.getTileEntity(ritualPos);
                if (te == null) continue;
TileEntity pedestal = resolveToPedestal(te);
                    if (pedestal == null) {
    
                        break;
                    }
                    if (tickCount % 100 == 0) {
                        if (tickCount % 200 == 0) Untitled.logger.info("[Binder] Resolved {} -> pedestal {}", ritualPos, pedestal.getPos());
                    }
                    // Range validation every 20 ticks: disconnect out-of-range machines immediately
                    if (tickCount % 20 == 0) {
                        int range = getPedestalRange(pedestal);
                        boolean changed = false;
                        List<BlockPos> toRemove = new ArrayList<>();
                        for (BlockPos m : machines) {
                            double dist = Math.sqrt(ritualPos.distanceSq(m));
                            if (dist > range + 0.5) {
                                toRemove.add(m);
                                changed = true;
                                Untitled.logger.info("[Binder] {} disconnected (dist={} > range={})", m, String.format("%.1f", dist), range);
                            }
                        }
                        machines.removeAll(toRemove);
                        if (changed) {
                            if (machines.isEmpty()) {
                                clearCECache(pedestal);
                                BINDINGS.remove(ritualPos);
                                Untitled.logger.info("[Binder] Node {} fully unbound", ritualPos);
                            }
                            saveBindings(world);
                            if (BINDINGS.containsKey(ritualPos)) continue; // re-inject remaining
                            break;
                        }
                    }
                    // Alcara freeze: START=remove from tick list, END=add back
                    if (event.phase == TickEvent.Phase.START) {
                        for (BlockPos m : machines) {
                            if (!ALCARA_BINDINGS.contains(m)) continue;
                            TileEntity mte = world.getTileEntity(m);
                            if (mte == null) continue;
                            if (mte instanceof net.minecraft.util.ITickable) {
                                world.tickableTileEntities.remove(mte);
                                FROZEN_NBT.put(m, new net.minecraft.nbt.NBTTagCompound()); // placeholder
                            }
                        }
                    } else { // END
                        for (BlockPos m : machines) {
                            if (!ALCARA_BINDINGS.contains(m)) continue;
                            if (!FROZEN_NBT.containsKey(m)) continue;
                            TileEntity mte = world.getTileEntity(m);
                            if (mte == null) continue;
                            if (!world.tickableTileEntities.contains(mte)) {
                                world.tickableTileEntities.add(mte);
                            }
                        }
                    }

                    injectPositionsIntoCE(pedestal, machines);
                break;
            }
        }

        // Safety restore: END phase always unfreezes any machines in FROZEN_NBT,
        // even if their binding was removed during this tick's START phase
        if (event.phase == TickEvent.Phase.END && !FROZEN_NBT.isEmpty()) {
            for (java.util.Map.Entry<BlockPos, net.minecraft.nbt.NBTTagCompound> frozen
                    : new ArrayList<>(FROZEN_NBT.entrySet())) {
                BlockPos m = frozen.getKey();
                for (World w : server.worlds) {
                    TileEntity mte = w.getTileEntity(m);
                    if (mte != null && mte instanceof net.minecraft.util.ITickable
                            && !w.tickableTileEntities.contains(mte)) {
                        w.tickableTileEntities.add(mte);
                    }
                }
            }
            FROZEN_NBT.clear();
            // Clean stale ALCARA_BINDINGS: remove entries no longer in any binding
            java.util.Set<BlockPos> allBound = new java.util.HashSet<>();
            for (List<BlockPos> ml : BINDINGS.values()) allBound.addAll(ml);
            ALCARA_BINDINGS.retainAll(allBound);
        }
    }

    /**
     * Inject bound machine positions into AS's CEffectHorologium.
     * Normal path: fill elements cache + replace verifier.
     * Alcara path: directly accelerate bound machines with effectAmplifier multiplier.
     */
    private static void injectPositionsIntoCE(TileEntity pedestal, List<BlockPos> boundMachines) {
        try {
            Method getCache = findMethod(pedestal.getClass(), "getUpdateCache");
            if (getCache == null) return;
            Object receiver = getCache.invoke(pedestal);
            if (receiver == null) return;

            Field ceField = findField(receiver.getClass(), "ce");
            if (ceField == null) return;
            ceField.setAccessible(true);
            Object ce = ceField.get(receiver);
            if (ce == null) {
                ALCARA_BINDINGS.removeAll(boundMachines);
                return;
            }

            if (!ce.getClass().getName().contains("Horologium")) {
                ALCARA_BINDINGS.removeAll(boundMachines);
                return;
            }

            // Check if alcara (corrupted) path
            boolean alcara = false;
            int amplifier = 1;
            try {
                Field propsField = findField(receiver.getClass(), "properties");
                int collective = 100;
                if (propsField != null) {
                    propsField.setAccessible(true);
                    Object cp = propsField.get(receiver);
                    if (cp != null) {
                        Method gc = findMethod(cp.getClass(), "getCollectiveCapability");
                        if (gc != null) collective = (int) gc.invoke(cp);
                    }
                }
                Field traitField = findField(receiver.getClass(), "trait");
                Object trait = null;
                if (traitField != null) { traitField.setAccessible(true); trait = traitField.get(receiver); }
                if (trait != null) {
                    Method provideProps = ce.getClass().getMethod("provideProperties", int.class);
                    provideProps.setAccessible(true);
                    Object ep = provideProps.invoke(ce, collective);
                    if (ep != null) {
                        Method modify = null;
                        for (Class<?> iface : trait.getClass().getInterfaces()) {
                            try { modify = ep.getClass().getMethod("modify", iface); break; } catch (NoSuchMethodException ignored) {}
                        }
                        if (modify != null) { modify.setAccessible(true); ep = modify.invoke(ep, trait); }
                        Method isCorr = findMethod(ep.getClass(), "isCorrupted");
                        if (isCorr != null && (boolean) isCorr.invoke(ep)) {
                            alcara = true;
                            Method getAmp = findMethod(ep.getClass(), "getEffectAmplifier");
                            if (getAmp != null) amplifier = Math.max(1, (int) ((double) getAmp.invoke(ep)));
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Always clean stale ALCARA_BINDINGS: remove entries not in ANY binding
            java.util.Set<BlockPos> allBound = new java.util.HashSet<>();
            for (List<BlockPos> ml : BINDINGS.values()) allBound.addAll(ml);
            ALCARA_BINDINGS.retainAll(allBound);

            if (alcara) {
                // Disable AS native Horologium effect to prevent TimeStopZone at pedestal
                Field enabledField = findField(ce.getClass(), "enabled");
                if (enabledField != null) { enabledField.setAccessible(true); enabledField.setBoolean(ce, false); }
                // Mark as alcara binding; freeze logic handled in tick handler
                ALCARA_BINDINGS.addAll(boundMachines);
                if (tickCount % 200 == 0) Untitled.logger.info("[Binder] Alcara active, {} frozen machines", ALCARA_BINDINGS.size());
                return;
            }

            // Normal path: inject into elements cache
            Field elementsField = findField(ce.getClass(), "elements");
            if (elementsField == null) return;
            elementsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> elements = (List<Object>) elementsField.get(ce);

            Field maxCountField = findField(ce.getClass(), "maxCount");
            int maxCount = 30;
            if (maxCountField != null) { maxCountField.setAccessible(true); maxCount = maxCountField.getInt(ce); }

            elements.clear();
            Class<?> entryClass = Class.forName("hellfirepvp.astralsorcery.common.constellation.effect.GenListEntries$SimpleBlockPosEntry");
            Constructor<?> entryCtor = entryClass.getConstructor(BlockPos.class);

            int copiesPerMachine = Math.max(1, maxCount / boundMachines.size());
            for (BlockPos pos : boundMachines) {
                for (int i = 0; i < copiesPerMachine; i++) elements.add(entryCtor.newInstance(pos));
            }
            while (elements.size() < maxCount && !boundMachines.isEmpty()) elements.add(entryCtor.newInstance(boundMachines.get(0)));

            Field verifierField = findField(ce.getClass(), "verifier");
            if (verifierField != null) {
                verifierField.setAccessible(true);
                Object currentVerifier = verifierField.get(ce);
                if (currentVerifier == null || !Proxy.isProxyClass(currentVerifier.getClass())) {
                    Class<?> verifierIface = Class.forName("hellfirepvp.astralsorcery.common.constellation.effect.CEffectPositionListGen$Verifier");
                    Object rejectAllVerifier = Proxy.newProxyInstance(verifierIface.getClassLoader(), new Class<?>[]{verifierIface}, (proxy, method, args) -> false);
                    verifierField.set(ce, rejectAllVerifier);
                }
            }

            if (tickCount % 200 == 0) {
                if (tickCount % 200 == 0) Untitled.logger.info("[Binder] Injected {} positions for {} machines", elements.size(), boundMachines.size());
            }

        } catch (Exception e) {
            if (tickCount % 200 == 0) Untitled.logger.error("[Binder] injectPositionsIntoCE error: {}", e.toString());
        }
    }

    static TileEntity resolveToPedestal(TileEntity te) {
        if (te == null) return null;
        String cn = te.getClass().getName();
        if (cn.contains("TileRitualPedestal")) return te;

        BlockPos tePos = te.getPos();

        // Check cache first
        BlockPos cachedPedestalPos = PEDestalCache.get(tePos);
        if (cachedPedestalPos != null) {
            TileEntity cached = te.getWorld().getTileEntity(cachedPedestalPos);
            if (cached != null && cached.getClass().getName().contains("TileRitualPedestal")) {
                return cached;
            }
            PEDestalCache.remove(tePos); // stale entry
        }
        BlockPos linkedPos = null;
        try {
            Method g = findMethod(te.getClass(), "getLinkedTo");
            if (g != null) linkedPos = (BlockPos) g.invoke(te);
        } catch (Exception ignored) {}



        int pedestalCount = 0;
        for (Object obj : te.getWorld().loadedTileEntityList) {
            TileEntity candidate = (TileEntity) obj;
            if (!candidate.getClass().getName().contains("TileRitualPedestal")) continue;
            pedestalCount++;
            try {
                Method getCache = findMethod(candidate.getClass(), "getUpdateCache");
                if (getCache == null) continue;
                Object receiver = getCache.invoke(candidate);
                if (receiver == null) continue;

                // Dump offsetMirrors
                Field mirrorsField = findField(receiver.getClass(), "offsetMirrors");
                if (mirrorsField != null) {
                    mirrorsField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.Map<BlockPos, Boolean> mirrors = (java.util.Map<BlockPos, Boolean>) mirrorsField.get(receiver);
                    if (mirrors != null) {
                        if (mirrors.containsKey(tePos)) {
                            PEDestalCache.put(tePos, candidate.getPos());
                            return candidate;
                        }
                        if (linkedPos != null && mirrors.containsKey(linkedPos)) {
                            PEDestalCache.put(tePos, candidate.getPos());
                            return candidate;
                        }
                    }
                }

                // Also check ritualLinkTo field
                Field linkToField = findField(receiver.getClass(), "ritualLinkTo");
                if (linkToField != null) {
                    linkToField.setAccessible(true);
                    BlockPos ritualLinkTo = (BlockPos) linkToField.get(receiver);
                    if (ritualLinkTo != null) {
                        if (ritualLinkTo.equals(tePos) || ritualLinkTo.equals(linkedPos)) {
                            PEDestalCache.put(tePos, candidate.getPos());
                            return candidate;
                        }
                    }
                }
            } catch (Exception e) {
                Untitled.logger.error("[Binder] resolveToPedestal error for {}: {}", candidate.getPos(), e.toString());
            }
        }


        return null;
    }
    private static Method findMethod(Class<?> c, String n) {
        while (c != null) { try { Method m = c.getDeclaredMethod(n); m.setAccessible(true); return m; } catch (NoSuchMethodException e) { c = c.getSuperclass(); } }
        return null;
    }

    private static Field findField(Class<?> c, String n) {
        while (c != null) { try { Field f = c.getDeclaredField(n); f.setAccessible(true); return f; } catch (NoSuchFieldException e) { c = c.getSuperclass(); } }
        return null;
    }
}
