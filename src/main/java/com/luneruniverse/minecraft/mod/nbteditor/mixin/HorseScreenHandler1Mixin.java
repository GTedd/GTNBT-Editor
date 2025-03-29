package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMixinLink;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(targets = {"net.minecraft.screen.HorseScreenHandler$1"})
public class HorseScreenHandler1Mixin {
	@Inject(method = "<init>(Lnet/minecraft/screen/HorseScreenHandler;Lnet/minecraft/inventory/Inventory;IIILnet/minecraft/entity/passive/AbstractHorseEntity;)V", at = @At("RETURN"))
	private void init(HorseScreenHandler handler, Inventory inventory, int index, int x, int y, AbstractHorseEntity horse, CallbackInfo info) {
		PlayerEntity owner = ServerMixinLink.SCREEN_HANDLER_OWNER.get(Thread.currentThread());
		if (owner == null)
			return;
		ServerMixinLink.SLOT_OWNER.put((Slot) (Object) this, owner);
	}
	@Inject(method = "canInsert(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void canInsert(ItemStack item, CallbackInfoReturnable<Boolean> info) {
		PlayerEntity owner = ServerMixinLink.SLOT_OWNER.get((Slot) (Object) this);
		if (owner == null)
			return;
		if (!((Slot) (Object) this).isEnabled()) {
			info.setReturnValue(false);
			return;
		}
		if (owner instanceof ServerPlayerEntity) {
			if (MVMisc.hasPermissionLevel(owner, 2) && ServerMixinLink.NO_SLOT_RESTRICTIONS_PLAYERS.getOrDefault(owner, false))
				info.setReturnValue(true);
		} else {
			if (NBTEditorClient.SERVER_CONN.isEditingExpanded() && ConfigScreen.isNoSlotRestrictions())
				info.setReturnValue(true);
		}
	}
}
