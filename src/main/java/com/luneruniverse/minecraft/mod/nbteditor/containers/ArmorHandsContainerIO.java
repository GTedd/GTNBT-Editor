package com.luneruniverse.minecraft.mod.nbteditor.containers;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NBTManagers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

public class ArmorHandsContainerIO implements NonItemNBTContainerIO {
	
	@Override
	public int getMaxNBTSize(NbtCompound nbt, SourceContainerType source) {
		return 6;
	}
	
	@Override
	public ItemStack[] readNBT(NbtCompound container, SourceContainerType source) {
		ItemStack[] items = new ItemStack[6];
		
		NbtList armorItemsNbt = container.getList("ArmorItems", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < armorItemsNbt.size() && i < 4; i++)
			items[3 - i] = NBTManagers.ITEM.deserialize(armorItemsNbt.getCompound(i), true);
		
		NbtList handItemsNbt = container.getList("HandItems", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < handItemsNbt.size() && i < 2; i++)
			items[4 + i] = NBTManagers.ITEM.deserialize(handItemsNbt.getCompound(i), true);
		
		return items;
	}
	
	@Override
	public int writeNBT(NbtCompound container, ItemStack[] contents, SourceContainerType source) {
		ItemStack[] actualContents = new ItemStack[6];
		for (int i = 0; i < 6; i++) {
			ItemStack item = null;
			if (i < contents.length)
				item = contents[i];
			if (item == null)
				item = ItemStack.EMPTY;
			actualContents[i] = item;
		}
		
		NbtList armorItemsNbt = new NbtList();
		for (int i = 0; i < 4; i++)
			armorItemsNbt.add(actualContents[3 - i].manager$serialize(true));
		container.put("ArmorItems", armorItemsNbt);
		
		NbtList handItemsNbt = new NbtList();
		for (int i = 0; i < 2; i++)
			handItemsNbt.add(actualContents[4 + i].manager$serialize(true));
		container.put("HandItems", handItemsNbt);
		
		return 6;
	}
	
}
