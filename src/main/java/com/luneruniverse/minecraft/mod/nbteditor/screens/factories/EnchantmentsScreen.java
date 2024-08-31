package com.luneruniverse.minecraft.mod.nbteditor.screens.factories;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistry;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.LocalEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigCategory;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigItem;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigList;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigPanel;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigPath;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueDropdown;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueNumber;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.Enchants;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;

public class EnchantmentsScreen extends LocalEditorScreen<LocalItem> {
	
	@SuppressWarnings("unchecked")
	private static ConfigValueDropdown<String> getConfigEnchantment(ConfigCategory enchant) {
		return ((ConfigItem<ConfigValueDropdown<String>>) enchant.getConfigurable("enchantment")).getValue();
	}
	@SuppressWarnings("unchecked")
	private static ConfigValueNumber<Integer> getConfigLevel(ConfigCategory enchant) {
		return ((ConfigItem<ConfigValueNumber<Integer>>) enchant.getConfigurable("level")).getValue();
	}
	
	
	private final ConfigList config;
	private ConfigPanel panel;
	
	public EnchantmentsScreen(ItemReference ref) {
		super(TextInst.of("Enchantments"), ref);
		
		MVRegistry<Enchantment> registry = MVRegistry.getEnchantmentRegistry();
		Map<String, Enchantment> allEnchantments = registry.getEntrySet().stream()
				.map(enchant -> Map.entry(enchant.getKey().toString(), enchant.getValue()))
				.sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
		
		ItemStack inputItem = ref.getItem();
		ConfigCategory entry = new ConfigCategory();
		List<String> orderedEnchants = allEnchantments.entrySet().stream()
				.map(enchant -> Map.entry(enchant.getKey(), enchant.getValue().isAcceptableItem(inputItem)))
				.sorted((a, b) -> {
					if (a.getValue()) {
						if (!b.getValue())
							return -1;
					} else if (b.getValue())
						return 1;
					return a.getKey().compareToIgnoreCase(b.getKey());
				})
				.map(Map.Entry::getKey).toList();
		String firstEnchant = orderedEnchants.get(0);
		entry.setConfigurable("enchantment", new ConfigItem<>(TextInst.translatable("nbteditor.enchantments.enchantment"),
				ConfigValueDropdown.forList(firstEnchant, firstEnchant, orderedEnchants,
				allEnchantments.entrySet().stream().filter(enchant -> enchant.getValue().isAcceptableItem(inputItem)).map(Map.Entry::getKey).toList())));
		entry.setConfigurable("level", new ConfigItem<>(TextInst.translatable("nbteditor.enchantments.level"),
				ConfigValueNumber.forInt(1, 1, 1,
						Version.<Integer>newSwitch()
								.range("1.17.1", null, 255)
								.range(null, "1.17", 32767)
								.get())));
		config = new ConfigList(TextInst.translatable("nbteditor.enchantments"), false, entry);
		
		ItemTagReferences.ENCHANTMENTS.get(localNBT.getEditableItem()).getEnchants().forEach(enchant -> {
			ConfigCategory enchantConfig = entry.clone(true);
			getConfigEnchantment(enchantConfig).setValue(registry.getId(enchant.enchant()).toString());
			getConfigLevel(enchantConfig).setValue(enchant.level());
			config.addConfigurable(enchantConfig);
		});
		
		config.addValueListener(source -> {
			List<Enchants.EnchantWithLevel> newEnchants = new ArrayList<>();
			for (ConfigPath path : config.getConfigurables().values()) {
				ConfigCategory enchant = (ConfigCategory) path;
				newEnchants.add(new Enchants.EnchantWithLevel(
						allEnchantments.get(getConfigEnchantment(enchant).getValidValue()),
						getConfigLevel(enchant).getValidValue()));
			}
			ItemTagReferences.ENCHANTMENTS.set(localNBT.getEditableItem(), new Enchants(newEnchants));
			checkSave();
		});
	}
	
	@Override
	protected void initEditor() {
		ConfigPanel newPanel = addDrawableChild(new ConfigPanel(16, 64, width - 32, height - 80, config));
		if (panel != null)
			newPanel.setScroll(panel.getScroll());
		panel = newPanel;
	}
	
	@Override
	protected void renderEditor(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		renderTip(matrices, "nbteditor.enchantments.tip");
	}
	
}
