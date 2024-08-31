package com.luneruniverse.minecraft.mod.nbteditor.packets;

import com.luneruniverse.minecraft.mod.nbteditor.misc.BlockStateProperties;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistryKeys;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVPacket;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SetBlockC2SPacket implements MVPacket {
	
	public static final Identifier ID = IdentifierInst.of("nbteditor", "set_block");
	
	private final RegistryKey<World> world;
	private final BlockPos pos;
	private final Identifier id;
	private final BlockStateProperties state;
	private final NbtCompound nbt;
	private final boolean recreate;
	private final boolean triggerUpdate;
	
	public SetBlockC2SPacket(RegistryKey<World> world, BlockPos pos, Identifier id,
			BlockStateProperties state, NbtCompound nbt, boolean recreate, boolean triggerUpdate) {
		this.world = world;
		this.pos = pos;
		this.id = id;
		this.state = state;
		this.nbt = nbt;
		this.recreate = recreate;
		this.triggerUpdate = triggerUpdate;
	}
	public SetBlockC2SPacket(PacketByteBuf payload) {
		this.world = payload.readRegistryKey(MVRegistryKeys.WORLD);
		this.pos = payload.readBlockPos();
		this.id = payload.readIdentifier();
		this.state = new BlockStateProperties(payload);
		this.nbt = payload.readNbt();
		this.recreate = payload.readBoolean();
		this.triggerUpdate = payload.readBoolean();
	}
	
	public RegistryKey<World> getWorld() {
		return world;
	}
	public BlockPos getPos() {
		return pos;
	}
	public Identifier getId() {
		return id;
	}
	public BlockStateProperties getState() {
		return state;
	}
	public NbtCompound getNbt() {
		return nbt;
	}
	public boolean isRecreate() {
		return recreate;
	}
	public boolean isTriggerUpdate() {
		return triggerUpdate;
	}
	
	@Override
	public void write(PacketByteBuf payload) {
		payload.writeRegistryKey(world);
		payload.writeBlockPos(pos);
		payload.writeIdentifier(id);
		state.writeToPayload(payload);
		payload.writeNbtCompound(nbt);
		payload.writeBoolean(recreate);
		payload.writeBoolean(triggerUpdate);
	}
	
	@Override
	public Identifier getPacketId() {
		return ID;
	}
	
}
