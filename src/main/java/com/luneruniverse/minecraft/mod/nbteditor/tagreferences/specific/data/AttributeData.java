package com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data;

import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.AttributeData.AttributeModifierData.AttributeModifierId;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.AttributeData.AttributeModifierData.Operation;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.AttributeData.AttributeModifierData.Slot;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public record AttributeData(EntityAttribute attribute, double value, Optional<AttributeModifierData> modifierData) {
	
	public static record AttributeModifierData(Operation operation, Slot slot, AttributeModifierId id) {
		
		public enum Operation {
			ADD("nbteditor.attributes.operation.add"),
			ADD_MULTIPLIED_BASE("nbteditor.attributes.operation.add_multiplied_base"),
			ADD_MULTIPLIED_TOTAL("nbteditor.attributes.operation.add_multiplied_total");
			
			public static Operation fromMinecraft(net.minecraft.entity.attribute.EntityAttributeModifier.Operation operation) {
				return switch (operation) {
					case ADD_VALUE -> ADD;
					case ADD_MULTIPLIED_BASE -> ADD_MULTIPLIED_BASE;
					case ADD_MULTIPLIED_TOTAL -> ADD_MULTIPLIED_TOTAL;
				};
			}
			
			private final Text name;
			private Operation(String key) {
				this.name = TextInst.translatable(key);
			}
			public net.minecraft.entity.attribute.EntityAttributeModifier.Operation toMinecraft() {
				return switch (this) {
					case ADD -> net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE;
					case ADD_MULTIPLIED_BASE -> net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE;
					case ADD_MULTIPLIED_TOTAL -> net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
				};
			}
			@Override
			public String toString() {
				return name.getString();
			}
		}
		
		public enum Slot {
			ANY("nbteditor.attributes.slot.any", false),
			HAND("nbteditor.attributes.slot.hand", true),
			MAINHAND("nbteditor.attributes.slot.mainhand", false),
			OFFHAND("nbteditor.attributes.slot.offhand", false),
			ARMOR("nbteditor.attributes.slot.armor", true),
			HEAD("nbteditor.attributes.slot.head", false),
			CHEST("nbteditor.attributes.slot.chest", false),
			LEGS("nbteditor.attributes.slot.legs", false),
			FEET("nbteditor.attributes.slot.feet", false),
			BODY("nbteditor.attributes.slot.body", true);
			
			public static Slot fromMinecraft(Object slot) {
				return switch ((AttributeModifierSlot) slot) {
					case ANY -> ANY;
					case HAND -> HAND;
					case MAINHAND -> MAINHAND;
					case OFFHAND -> OFFHAND;
					case ARMOR -> ARMOR;
					case HEAD -> HEAD;
					case CHEST -> CHEST;
					case LEGS -> LEGS;
					case FEET -> FEET;
					case BODY -> BODY;
				};
			}
			public static List<Slot> getNotOnlyForComponents() {
				return Arrays.stream(values()).filter(slot -> !slot.isOnlyForComponents()).toList();
			}
			
			private final Text name;
			private final boolean onlyForComponents;
			private Slot(String key, boolean onlyForComponents) {
				this.name = TextInst.translatable(key);
				this.onlyForComponents = onlyForComponents;
			}
			public Object toMinecraft() {
				return switch (this) {
					case ANY -> AttributeModifierSlot.ANY;
					case HAND -> AttributeModifierSlot.HAND;
					case MAINHAND -> AttributeModifierSlot.MAINHAND;
					case OFFHAND -> AttributeModifierSlot.OFFHAND;
					case ARMOR -> AttributeModifierSlot.ARMOR;
					case HEAD -> AttributeModifierSlot.HEAD;
					case CHEST -> AttributeModifierSlot.CHEST;
					case LEGS -> AttributeModifierSlot.LEGS;
					case FEET -> AttributeModifierSlot.FEET;
					case BODY -> AttributeModifierSlot.BODY;
				};
			}
			public boolean isOnlyForComponents() {
				return onlyForComponents;
			}
			public boolean isInThisVersion() {
				return !onlyForComponents || NBTManagers.COMPONENTS_EXIST;
			}
			@Override
			public String toString() {
				return name.getString();
			}
		}
		
		public static class AttributeModifierId {
			
			public static final boolean ID_IS_IDENTIFIER = Version.<Boolean>newSwitch()
					.range("1.21.0", null, true)
					.range(null, "1.20.6", false)
					.get();
			
			public static AttributeModifierId randomUUID() {
				return new AttributeModifierId(UUID.randomUUID());
			}
			
			private static final Supplier<Reflection.MethodInvoker> EntityAttributeModifier_uuid =
					Reflection.getOptionalMethod(EntityAttributeModifier.class, "comp_2447", MethodType.methodType(UUID.class));
			public static AttributeModifierId fromMinecraft(EntityAttributeModifier modifier) {
				if (ID_IS_IDENTIFIER)
					return new AttributeModifierId(modifier.id());
				return new AttributeModifierId((UUID) EntityAttributeModifier_uuid.get().invoke(modifier));
			}
			
			private final Object id;
			
			public AttributeModifierId(UUID id) {
				this.id = id;
			}
			public AttributeModifierId(Identifier id) {
				if (!ID_IS_IDENTIFIER)
					throw new IllegalArgumentException("Attribute IDs are UUIDs in this version!");
				this.id = id;
			}
			
			public UUID getUUID() {
				return (UUID) id;
			}
			
			public Identifier getIdentifier() {
				if (id instanceof UUID uuid)
					return IdentifierInst.of("minecraft", uuid.toString());
				return (Identifier) id;
			}
			
			public EntityAttributeModifier toMinecraft(String name, double value, net.minecraft.entity.attribute.EntityAttributeModifier.Operation operation) {
				if (ID_IS_IDENTIFIER)
					return new EntityAttributeModifier(getIdentifier(), value, operation);
				
				return Reflection.newInstance(
						EntityAttributeModifier.class,
						new Class<?>[] {UUID.class, String.class, double.class, net.minecraft.entity.attribute.EntityAttributeModifier.Operation.class},
						getUUID(), name, value, operation);
			}
			
		}
		
		public static AttributeModifierData fromMinecraft(EntityAttributeModifier modifier, AttributeModifierSlot slot) {
			return new AttributeModifierData(
					Operation.fromMinecraft(modifier.operation()),
					Slot.fromMinecraft(slot),
					AttributeModifierId.fromMinecraft(modifier));
		}
		
		public EntityAttributeModifier toMinecraft(String name, double value) {
			return id.toMinecraft(name, value, operation.toMinecraft());
		}
		
	}
	
	public static AttributeData fromComponentEntry(AttributeModifiersComponent.Entry entry) {
		return new AttributeData(
				entry.attribute().value(),
				entry.modifier().value(),
				Optional.of(AttributeModifierData.fromMinecraft(entry.modifier(), entry.slot())));
	}
	
	public AttributeData(EntityAttribute attribute, double value) {
		this(attribute, value, Optional.empty());
	}
	public AttributeData(EntityAttribute attribute, double value, Operation operation, Slot slot, AttributeModifierId id) {
		this(attribute, value, Optional.of(new AttributeModifierData(operation, slot, id)));
	}
	
	public AttributeModifiersComponent.Entry toComponentEntry() {
		return new AttributeModifiersComponent.Entry(
				Registries.ATTRIBUTE.getEntry(attribute),
				modifierData.get().toMinecraft(Registries.ATTRIBUTE.getId(attribute).toString(), value),
				(AttributeModifierSlot) modifierData.get().slot().toMinecraft());
	}
	
}
