package com.luneruniverse.minecraft.mod.nbteditor.packets;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistryKeys;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVPacket;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SummonEntityC2SPacket implements MVPacket {
	
	public static final Identifier ID = IdentifierInst.of("nbteditor", "summon_entity");
	
	private final int requestId;
	private final RegistryKey<World> world;
	private final Vec3d pos;
	private final Identifier id;
	private final NbtCompound nbt;
	
	public SummonEntityC2SPacket(int requestId, RegistryKey<World> world, Vec3d pos, Identifier id, NbtCompound nbt) {
		this.requestId = requestId;
		this.world = world;
		this.pos = pos;
		this.id = id;
		this.nbt = nbt;
	}
	public SummonEntityC2SPacket(PacketByteBuf payload) {
		this.requestId = payload.readVarInt();
		this.world = payload.readRegistryKey(MVRegistryKeys.WORLD);
		this.pos = payload.readVec3d();
		this.id = payload.readIdentifier();
		this.nbt = payload.readNbt();
	}
	
	public int getRequestId() {
		return requestId;
	}
	public RegistryKey<World> getWorld() {
		return world;
	}
	public Vec3d getPos() {
		return pos;
	}
	public Identifier getId() {
		return id;
	}
	public NbtCompound getNbt() {
		return nbt;
	}
	
	@Override
	public void write(PacketByteBuf payload) {
		payload.writeVarInt(requestId);
		payload.writeRegistryKey(world);
		payload.writeVec3d(pos);
		payload.writeIdentifier(id);
		payload.writeNbtCompound(nbt);
	}
	
	@Override
	public Identifier getPacketId() {
		return ID;
	}
	
}
