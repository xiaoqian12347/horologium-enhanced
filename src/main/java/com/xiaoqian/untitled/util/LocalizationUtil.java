package com.xiaoqian.untitled.util;

import net.minecraft.client.resources.I18n;

/**
 * 本地化工具类
 */
public class LocalizationUtil {
    
    /**
     * 获取本地化文本
     */
    public static String translate(String key, Object... args) {
        return I18n.format(key, args);
    }
    
    // ==================== Binder Messages ====================
    
    public static String getBinderCleared() {
        return translate("message.binder.cleared");
    }
    
    public static String getBinderUnboundNode() {
        return translate("message.binder.unbound_node");
    }
    
    public static String getBinderNoMachineSelected() {
        return translate("message.binder.no_machine_selected");
    }
    
    public static String getBinderAlreadyInList() {
        return translate("message.binder.already_in_list");
    }
    
    public static String getBinderSelected(int count, int x, int y, int z) {
        return translate("message.binder.selected", count, x, y, z);
    }
    
    public static String getBinderBoundMachines(int count) {
        return translate("message.binder.bound_machines", count);
    }
    
    public static String getBinderOutOfRange(int count, int maxRange) {
        return translate("message.binder.out_of_range", count, maxRange);
    }
    
    public static String getBinderNoConnection(int max, int range) {
        return translate("message.binder.no_connection", max, range);
    }
    
    public static String getBinderConnected(int count, int max, int range) {
        return translate("message.binder.connected", count, max, range);
    }
    
    public static String getBinderMachineRemoved() {
        return translate("message.binder.machine_removed");
    }
    
    public static String getBinderNodeUnbound() {
        return translate("message.binder.node_unbound");
    }
    
    public static String getBinderNotBound() {
        return translate("message.binder.not_bound");
    }
    
    public static String getBinderAnchorHasBinding() {
        return translate("message.binder.anchor_has_binding");
    }
    
    public static String getBinderPedestalHasBinding() {
        return translate("message.binder.pedestal_has_binding");
    }
    
    public static String getBinderBindMode() {
        return translate("message.binder.bind_mode");
    }
    
    public static String getBinderUnbindMode() {
        return translate("message.binder.unbind_mode");
    }
    
    public static String getBinderScrollHint() {
        return translate("message.binder.scroll_hint");
    }
    
    public static String getBinderRightClickHint() {
        return translate("message.binder.right_click_hint");
    }
    
    // ==================== Anchor Messages ====================
    
    public static String getAnchorLinked(int x, int y, int z) {
        return translate("message.anchor.linked", x, y, z);
    }
    
    public static String getAnchorNotLinked() {
        return translate("message.anchor.not_linked");
    }
    
    // ==================== Renderer Labels ====================
    
    public static String getAltarLabel(int ordinal) {
        switch (ordinal) {
            case 0: return translate("label.altar.discovery");
            case 1: return translate("label.altar.collector");
            case 2: return translate("label.altar.constellation");
            case 3: return translate("label.altar.astral");
            default: return translate("label.altar.generic");
        }
    }
    
    public static String getPedestalLabel() {
        return translate("label.pedestal");
    }
    
    public static String getTreeBeaconLabel() {
        return translate("label.tree_beacon");
    }
    
    public static String getBoreLabel() {
        return translate("label.bore");
    }
    
    public static String getAttunementAltarLabel() {
        return translate("label.attunement_altar");
    }
    
    // ==================== Info Formats ====================
    
    public static String getAltarStoredInfo(int stored, int max) {
        return translate("info.altar.stored", stored, max);
    }
    
    public static String getAltarWithConstellationInfo(int stored, int max, String constellation) {
        return translate("info.altar.with_constellation", stored, max, constellation);
    }
    
    public static String getPedestalBufferChanneledInfo(double buffer, int channeled) {
        return translate("info.pedestal.buffer_chanled", String.format("%.1f", buffer), channeled);
    }
    
    public static String getPedestalBufferInfo(double buffer) {
        return translate("info.pedestal.buffer", String.format("%.1f", buffer));
    }
    
    public static String getPedestalMultiblockOk() {
        return translate("info.pedestal.multiblock_ok");
    }
    
    public static String getPedestalMultiblockMissing() {
        return translate("info.pedestal.multiblock_missing");
    }
    
    public static String getPedestalWithConstellationInfo(String info, String constellation, String trait) {
        return translate("info.pedestal.with_constellation", info, constellation, trait);
    }
    
    public static String getTreeBeaconChargeInfo(double charge) {
        return translate("info.tree_beacon.charge", String.format("%.1f", charge));
    }
    
    public static String getBoreMbInfo(int mb) {
        return translate("info.bore.mb", mb);
    }
    
    public static String getAttunementConstellationInfo(String constellation) {
        return translate("info.attunement.constellation", constellation);
    }
    
    public static String getAttunementNoneInfo() {
        return translate("info.attunement.none");
    }
}
