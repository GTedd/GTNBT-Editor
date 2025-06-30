package com.luneruniverse.minecraft.mod.nbteditor.containers;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalBlock;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.TagNames;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class BlockEntityTagContainerIO extends ItemTagContainerIO implements BlockContainerIO {
	
	private final NonItemNBTContainerIO blockNbtIO;
	
	public BlockEntityTagContainerIO(NBTContainerIO itemNbtIO, NonItemNBTContainerIO blockNbtIO) {
		super(TagNames.BLOCK_ENTITY_TAG, itemNbtIO);
		this.blockNbtIO = blockNbtIO;
	}
	public BlockEntityTagContainerIO(NBTContainerIO nbtIO) {
		this(nbtIO, nbtIO);
	}
	
	@Override
	public int getMaxBlockSize(LocalBlock block) {
		return blockNbtIO.getMaxNBTSize(getNBT(block), SourceContainerType.BLOCK);
	}
	
	@Override
	public boolean isBlockReadable(LocalBlock block) {
		return blockNbtIO.isNBTReadable(getNBT(block), SourceContainerType.BLOCK);
	}
	
	@Override
	public ItemStack[] readBlock(LocalBlock container) {
		return blockNbtIO.readNBT(getNBT(container), SourceContainerType.BLOCK);
	}
	
	@Override
	public int writeBlock(LocalBlock container, ItemStack[] contents) {
		NbtCompound nbt = getNBT(container);
		int output = blockNbtIO.writeNBT(nbt, contents, SourceContainerType.BLOCK);
		container.setNBT(nbt);
		return output;
	}
	
	@Override
	public int getWrittenBlockSlotIndex(LocalBlock container, ItemStack[] contents, int slot) {
		return blockNbtIO.getWrittenNBTSlotIndex(getNBT(container), contents, slot, SourceContainerType.BLOCK);
	}
	
}
