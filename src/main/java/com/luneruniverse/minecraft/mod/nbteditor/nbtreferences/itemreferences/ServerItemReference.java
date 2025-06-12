package com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVClientNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.packets.SetCursorC2SPacket;
import com.luneruniverse.minecraft.mod.nbteditor.packets.SetSlotC2SPacket;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class ServerItemReference extends HandledScreenItemReference {
	
	private final int slot;
	private final HandledScreen<?> screen;
	private ItemStack item;
	
	public ServerItemReference(int slot, HandledScreen<?> screen) {
		super(HandledScreenItemReferenceParent.forRoot(screen));
		this.slot = slot;
		this.screen = screen;
		
		if (slot == -1) {
			item = screen.getScreenHandler().getCursorStack();
			return;
		}
		
		Slot slotObj = screen.getScreenHandler().getSlot(slot);
		if (slotObj.inventory == MainUtil.client.player.getInventory())
			throw new IllegalArgumentException("The slot cannot be in the player's inventory!");
		this.item = slotObj.getStack();
	}
	
	public int getSlot() {
		return slot;
	}
	
	@Override
	public boolean exists() {
		return !MixinLink.CLOSED_SERVER_HANDLED_SCREENS.containsKey(screen);
	}
	
	@Override
	public ItemStack getItem() {
		return item;
	}
	
	@Override
	public void saveItem(ItemStack toSave, Runnable onFinished) {
		item = toSave;
		if (slot == -1)
			screen.getScreenHandler().setCursorStack(toSave);
		else
			screen.getScreenHandler().getSlot(slot).setStackNoCallbacks(toSave);
		if (screen instanceof InventoryScreen || NBTEditorClient.SERVER_CONN.isContainerScreen()) {
			if (slot == -1)
				MVClientNetworking.send(new SetCursorC2SPacket(toSave));
			else
				MVClientNetworking.send(new SetSlotC2SPacket(slot, toSave));
			onFinished.run();
		}
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
	public HandledScreenItemReference setParent(HandledScreenItemReferenceParent parent) {
		throw new UnsupportedOperationException("ServerItemReferences cannot have custom parents");
	}
	@Override
	public HandledScreenItemReferenceParent getDefaultParent() {
		throw new UnsupportedOperationException("ServerItemReferences cannot have default parents");
	}
	
}
