package com.xiaoqian.untitled.client.render;

import hellfirepvp.astralsorcery.common.tile.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import java.util.List;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT)
public class AltarStarlightRenderer {

    private static final double RENDER_DISTANCE = 24.0;
    private static final List<TileEntity> CACHED_AS_ENTITIES = new java.util.ArrayList<>();
    private static int rebuildCounter = 0;

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;
        if (player == null || world == null) return;

        // Expire stale cache entries
        StarlightNetHandler.tickCache();

        float pt = event.getPartialTicks();
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;

        // Rebuild AS entity cache every 40 ticks (~2s)
        if (++rebuildCounter >= 40) {
            rebuildCounter = 0;
            CACHED_AS_ENTITIES.clear();
            for (TileEntity te : world.loadedTileEntityList) {
                if (te instanceof TileAltar || te instanceof TileRitualPedestal ||
                    te instanceof TileTreeBeacon || te instanceof TileBore ||
                    te instanceof TileAttunementAltar) {
                    CACHED_AS_ENTITIES.add(te);
                }
            }
        }

        for (TileEntity te : CACHED_AS_ENTITIES) {
            try {
                if (te.isInvalid()) continue;
                if (te instanceof TileAltar) {
                    renderAltar((TileAltar) te, px, py, pz, mc);
                } else if (te instanceof TileRitualPedestal) {
                    renderPedestal((TileRitualPedestal) te, px, py, pz, mc);
                } else if (te instanceof TileTreeBeacon) {
                    renderTreeBeacon((TileTreeBeacon) te, px, py, pz, mc);
                } else if (te instanceof TileBore) {
                    renderBore((TileBore) te, px, py, pz, mc);
                } else if (te instanceof TileAttunementAltar) {
                    renderAttunementAltar((TileAttunementAltar) te, px, py, pz, mc);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void renderAltar(TileAltar altar, double px, double py, double pz, Minecraft mc) {
        BlockPos pos = altar.getPos();
        if (pos.distanceSq(px, py, pz) > RENDER_DISTANCE * RENDER_DISTANCE) return;

        // Get focused constellation
        String focusedName = "";
        try {
            hellfirepvp.astralsorcery.common.constellation.IConstellation fc = altar.getFocusedConstellation();
            if (fc != null) {
                focusedName = net.minecraft.client.resources.I18n.format(fc.getUnlocalizedName());
            }
        } catch (Exception ignored) {}

        String info;
        int color;

        StarlightNetHandler.CachedData cd = StarlightNetHandler.get(pos);
        if (cd != null) {
            int stored = (int) cd.val1;
            int max = cd.val2;
            int levelOrd = cd.val3;
            color = altarColor(levelOrd);
            info = stored + " / " + max;
        } else {
            int stored = altar.getStarlightStored();
            int max = altar.getMaxStarlightStorage();
            color = altarColor(altar.getAltarLevel().ordinal());
            info = stored + " / " + max;
        }

        if (!focusedName.isEmpty()) {
            info = info + " §b" + focusedName;
        }

        int levelOrd = (cd != null) ? cd.val3 : altar.getAltarLevel().ordinal();
        renderFloatingText(altarLabel(levelOrd), info, color, pos, px, py, pz, mc);
    }

    private static void renderPedestal(TileRitualPedestal pedestal, double px, double py, double pz, Minecraft mc) {
        BlockPos pos = pedestal.getPos();
        if (pos.distanceSq(px, py, pz) > RENDER_DISTANCE * RENDER_DISTANCE) return;

        String info;
        int color;

        // Get constellation and trait names from tuned crystal
        String constellationName = "";
        String traitName = "";
        try {
            net.minecraft.item.ItemStack crystal = pedestal.getCurrentPedestalCrystal();
            if (crystal != null && !crystal.isEmpty() && crystal.getItem() instanceof hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase) {
                hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase tuned =
                        (hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase) crystal.getItem();
                hellfirepvp.astralsorcery.common.constellation.IConstellation c = tuned.getFocusConstellation(crystal);
                if (c != null) {
                    constellationName = net.minecraft.client.resources.I18n.format(c.getUnlocalizedName());
                }
                hellfirepvp.astralsorcery.common.constellation.IMinorConstellation trait = hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase.getTrait(crystal);
                if (trait != null) {
                    traitName = net.minecraft.client.resources.I18n.format(trait.getUnlocalizedName());
                }
            }
        } catch (Exception ignored) {}

        StarlightNetHandler.CachedData cd = StarlightNetHandler.get(pos);
        if (cd != null) {
            double buffer = cd.val1;
            int channeled = cd.val2;
            boolean working = (cd.flags & 1) != 0;
            boolean hasCrystal = (cd.flags & 2) != 0;

            if (hasCrystal || working) {
                info = String.format("%.1f [%d]", buffer, channeled);
                color = working ? 0xFFAA00 : 0xAA80FF;
            } else {
                info = String.format("%.1f", buffer);
                color = 0x5599FF;
            }
        } else {
            boolean working = false;
            try { working = pedestal.isWorking(); } catch (Exception ignored) {}
            boolean hasMB = false;
            try { hasMB = pedestal.hasMultiblock(); } catch (Exception ignored) {}
            info = hasMB ? "[MB OK]" : "[--]";
            color = 0x888888;
        }

        // Append constellation + trait if present
        if (!constellationName.isEmpty()) {
            info = info + " §b" + constellationName;
            if (!traitName.isEmpty()) {
                info = info + " §7| §d" + traitName;
            }
        }

        renderFloatingText("仪式基座", info, color, pos, px, py, pz, mc);
    }

    private static void renderTreeBeacon(TileTreeBeacon beacon, double px, double py, double pz, Minecraft mc) {
        BlockPos pos = beacon.getPos();
        if (pos.distanceSq(px, py, pz) > RENDER_DISTANCE * RENDER_DISTANCE) return;

        StarlightNetHandler.CachedData cd = StarlightNetHandler.get(pos);
        if (cd != null) {
            renderFloatingText("树木信标", String.format("%.1f", cd.val1),
                    0x55FF80, pos, px, py, pz, mc);
        } else {
            double charge = 0;
            try {
                java.lang.reflect.Field f = TileTreeBeacon.class.getDeclaredField("starlightCharge");
                f.setAccessible(true);
                charge = f.getDouble(beacon);
            } catch (Exception ignored) {}
            renderFloatingText("树木信标", String.format("%.1f", charge),
                    0x55FF80, pos, px, py, pz, mc);
        }
    }

    private static void renderBore(TileBore bore, double px, double py, double pz, Minecraft mc) {
        BlockPos pos = bore.getPos();
        if (pos.distanceSq(px, py, pz) > RENDER_DISTANCE * RENDER_DISTANCE) return;

        StarlightNetHandler.CachedData cd = StarlightNetHandler.get(pos);
        if (cd != null) {
            renderFloatingText("万象泉", (int) cd.val1 + " mB",
                    0xFF8844, pos, px, py, pz, mc);
        } else {
            int mb = 0;
            try {
                java.lang.reflect.Field f = TileBore.class.getDeclaredField("mbStarlight");
                f.setAccessible(true);
                mb = f.getInt(bore);
            } catch (Exception ignored) {}
            renderFloatingText("万象泉", mb + " mB", 0xFF8844, pos, px, py, pz, mc);
        }
    }

    private static java.lang.reflect.Field fHighlight;
    private static java.lang.reflect.Field fActiveFound;
    static {
        try {
            fHighlight = TileAttunementAltar.class.getDeclaredField("highlight");
            fHighlight.setAccessible(true);
            fActiveFound = TileAttunementAltar.class.getDeclaredField("activeFound");
            fActiveFound.setAccessible(true);
        } catch (Exception e) {
            fHighlight = null;
            fActiveFound = null;
        }
    }

    private static void renderAttunementAltar(TileAttunementAltar altar, double px, double py, double pz, Minecraft mc) {
        BlockPos pos = altar.getPos();
        if (pos.distanceSq(px, py, pz) > RENDER_DISTANCE * RENDER_DISTANCE) return;

        String constellationName = "";
        try {
            // Try highlight first, then activeFound
            hellfirepvp.astralsorcery.common.constellation.IConstellation c = null;
            if (fHighlight != null) c = (hellfirepvp.astralsorcery.common.constellation.IConstellation) fHighlight.get(altar);
            if (c == null && fActiveFound != null) c = (hellfirepvp.astralsorcery.common.constellation.IConstellation) fActiveFound.get(altar);
            if (c != null) {
                constellationName = net.minecraft.client.resources.I18n.format(c.getUnlocalizedName());
            }
        } catch (Exception ignored) {}

        String info;
        if (!constellationName.isEmpty()) {
            info = "§b" + constellationName;
        } else {
            info = "§7-";
        }

        renderFloatingText("共鸣祭坛", info, 0xFFCC00, pos, px, py, pz, mc);
    }

    // ========== Render ==========
    private static void renderFloatingText(String label, String text, int color,
                                            BlockPos pos, double px, double py, double pz, Minecraft mc) {
        double x = pos.getX() + 0.5 - px;
        double y = pos.getY() + 1.5 - py;
        double z = pos.getZ() + 0.5 - pz;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-mc.player.rotationYaw, 0, 1, 0);
        GlStateManager.rotate(mc.player.rotationPitch, 1, 0, 0);

        float scale = 0.025f;
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        int labelWidth = mc.fontRenderer.getStringWidth(label);
        int textWidth = mc.fontRenderer.getStringWidth(text);

        mc.fontRenderer.drawString(label, -labelWidth / 2, -18, color, true);
        mc.fontRenderer.drawString(text, -textWidth / 2, -8, 0xFFFFFF, true);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static String altarLabel(int ordinal) {
        switch (ordinal) {
            case 0: return "星辉合成台";
            case 1: return "星辉祭坛";
            case 2: return "天辉祭坛";
            case 3: return "五彩祭坛";
            default: return "祭坛";
        }
    }

    private static int altarColor(int ordinal) {
        switch (ordinal) {
            case 0: return 0x55FFFF;
            case 1: return 0x55FF55;
            case 2: return 0xFFFF55;
            case 3: return 0xFF55FF;
            default: return 0xFFFFFF;
        }
    }
}
