package com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class ArmorItemReference extends HandledScreenItemReference {
	
	private final EquipmentSlot slot;
	
	public ArmorItemReference(EquipmentSlot slot) {
		if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
			throw new IllegalArgumentException("Invalid armor slot");
		
		this.slot = slot;
	}
	public ArmorItemReference(int slot) {
		this(switch (slot) {
				case 0 -> EquipmentSlot.FEET;
				case 1 -> EquipmentSlot.LEGS;
				case 2 -> EquipmentSlot.CHEST;
				case 3 -> EquipmentSlot.HEAD;
				case 5 -> EquipmentSlot.HEAD;
				case 6 -> EquipmentSlot.CHEST;
				case 7 -> EquipmentSlot.LEGS;
				case 8 -> EquipmentSlot.FEET;
				default -> throw new IllegalArgumentException("Invalid armor slot");
			});
	}
	
	public EquipmentSlot getSlot() {
		return slot;
	}
	
	@Override
	public boolean exists() {
		return true;
	}
	
	@Override
	public ItemStack getItem() {
		return MainUtil.client.player.getEquippedStack(slot);
	}
	
	@Override
	public void saveItem(ItemStack toSave, Runnable onFinished) {
		MainUtil.saveItem(slot, toSave);
		onFinished.run();
	}
	
	@Override
	public boolean isLocked() {
		return false;
	}
	
	@Override
	public boolean isLockable() {
		return false;
	}
	
	@Override
	public int getBlockedInvSlot() {
		return -1;
	}
	
	@Override
	public int getBlockedHotbarSlot() {
		return -1;
	}
	
	@Override
	public Runnable getDefaultParent() {
		return NBTEditorClient.CURSOR_MANAGER::showRoot;
	}
	
}
