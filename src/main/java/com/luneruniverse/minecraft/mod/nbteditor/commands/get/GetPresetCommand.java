package com.luneruniverse.minecraft.mod.nbteditor.commands.get;

import static com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager.literal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.commands.ClientCommand;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.datafixer.TypeReferences;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class GetPresetCommand extends ClientCommand {
	
	private static final Map<String, Supplier<ItemStack>> presetItems = new HashMap<>();
	public static void registerPresetItem(String name, Supplier<ItemStack> item) {
		presetItems.put(name, item);
	}
	public static Supplier<ItemStack> registerPresetItem(String name) {
		Supplier<ItemStack> output = () -> Optional.ofNullable(getItem(name)).orElseGet(() -> new ItemStack(Items.BARRIER)
				.manager$setCustomName(TextInst.translatable("nbteditor.get.preset_item.missing")));
		presetItems.put(name, output);
		return output;
	}
	private static ItemStack getItem(String name) {
		try {
			return NBTManagers.ITEM.deserialize(MainUtil.updateDynamic(TypeReferences.ITEM_STACK, MainUtil.readNBT(
					MVMisc.getResource(IdentifierInst.of("nbteditor", "presetitems/" + name + ".nbt")).orElseThrow())));
		} catch (Exception e) {
			NBTEditor.LOGGER.error("Error while loading preset item '" + name + "'", e);
			return null;
		}
	}
	public static final Supplier<ItemStack> COLOR_CODES = registerPresetItem("colorcodes");
	
	
	@Override
	public String getName() {
		return "preset";
	}
	
	@Override
	public String getExtremeAlias() {
		return null;
	}
	
	@Override
	public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
		presetItems.forEach((name, item) -> {
			builder.then(literal(name).executes(context -> {
				MainUtil.getWithMessage(item.get().copy());
				return Command.SINGLE_SUCCESS;
			}));
		});
	}
	
}
