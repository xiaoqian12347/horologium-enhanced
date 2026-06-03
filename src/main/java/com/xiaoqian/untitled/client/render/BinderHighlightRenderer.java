package com.xiaoqian.untitled.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT)
public class BinderHighlightRenderer {

    private static final String BINDER_CLASS = "starlight_binder";

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;
        if (player == null || world == null) return;

        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || !held.getUnlocalizedName().contains(BINDER_CLASS)) return;

        // Find which ritual node the player is looking at
        RayTraceResult ray = player.rayTrace(8.0, event.getPartialTicks());
        if (ray == null || ray.typeOfHit != RayTraceResult.Type.BLOCK) return;

        BlockPos lookPos = ray.getBlockPos();
        TileEntity lookTE = world.getTileEntity(lookPos);
        if (lookTE == null) return;

        String teName = lookTE.getClass().getName();
        boolean isRitualNode = teName.contains("TileRitualPedestal") || teName.contains("TileRitualLink");

        // Get highlights: either from the looked-at node, or show all from client cache
        Set<BlockPos> highlights = BinderBindingHandler.getClientHighlights();

        if (isRitualNode) {
            Map<BlockPos, List<BlockPos>> bindings = BinderBindingHandler.getClientBindings();
            boolean isPedestal = teName.contains("TileRitualPedestal");

            // Resolve lookup pos: pedestal -> anchor if anchor has bindings
            BlockPos lookupPos = lookPos;
            BlockPos anchorPos = isPedestal ? RitualNodeInteractionHandler.resolveAnchorPos(lookTE) : null;
            if (anchorPos != null) {
                List<BlockPos> anchorBindings = bindings.get(anchorPos);
                if (anchorBindings != null && !anchorBindings.isEmpty()) {
                    lookupPos = anchorPos;
                }
            }

            List<BlockPos> bound = bindings.get(lookupPos);
            if (bound != null && !bound.isEmpty()) {
                highlights = new java.util.HashSet<>(bound);
            } else {
                highlights = java.util.Collections.emptySet();
            }

            // Highlight the node itself
            float pt = event.getPartialTicks();
            double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
            double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
            double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;
            renderBlockOutline(lookPos, px, py, pz, 0.6f, 0.3f, 1.0f, 1.0f);
            // If pedestal resolved to anchor, also highlight the anchor
            if (!lookupPos.equals(lookPos)) {
                renderBlockOutline(lookupPos, px, py, pz, 0.6f, 0.3f, 1.0f, 1.0f);
            }
        }

        if (highlights == null || highlights.isEmpty()) return;

        float pt = event.getPartialTicks();
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;

        for (BlockPos target : highlights) {
            if (target.distanceSq(lookPos) > 10000) continue; // skip far away
            renderBlockOutline(target, px, py, pz, 1.0f, 0.85f, 0.0f, 0.9f);
        }
    }

    private static void renderBlockOutline(BlockPos pos, double px, double py, double pz,
                                            float r, float g, float b, float a) {
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(0.005);
        box = box.offset(-px, -py, -pz);

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.glLineWidth(3.0f);

        Tessellator tes = Tessellator.getInstance();
        BufferBuilder buf = tes.getBuffer();
        buf.begin(3, DefaultVertexFormats.POSITION_COLOR);

        // Draw 12 edges of the box
        drawEdge(buf, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
        drawEdge(buf, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        drawEdge(buf, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
        drawEdge(buf, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, a);
        drawEdge(buf, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        drawEdge(buf, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        drawEdge(buf, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        drawEdge(buf, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        drawEdge(buf, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        drawEdge(buf, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        drawEdge(buf, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        drawEdge(buf, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);

        tes.draw();

        GlStateManager.glLineWidth(1.0f);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawEdge(BufferBuilder buf,
                                  double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  float r, float g, float b, float a) {
        buf.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(x2, y2, z2).color(r, g, b, a).endVertex();
    }


}