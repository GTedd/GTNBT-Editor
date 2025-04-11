package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.fancytext.FancyText;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalBlock;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalEntity;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.EditableText;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.util.FancyConfirmScreen;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.text.StringVisitable.StyledVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class TextUtil {
	
	public static List<Text> getLongTranslatableTextLines(String key) {
		List<Text> lines = new ArrayList<>();
		for (int i = 1; i <= 50; i++) {
			Text line = TextInst.translatable(key + "_" + i);
			String str = line.getString();
			if (str.equals(key + "_" + i))
				break;
			
			if (str.startsWith("[LINK] ")) {
				String url = str.substring("[LINK] ".length());
				line = TextInst.literal(url).styled(style -> style.withClickEvent(new ClickEvent(Action.OPEN_URL, url))
						.withUnderline(true).withItalic(true).withColor(Formatting.GOLD));
			}
			if (str.startsWith("[FORMAT] ")) {
				String toFormat = str.substring("[FORMAT] ".length());
				line = FancyText.parse(toFormat);
			}
			lines.add(line);
		}
		return lines;
	}
	public static Text getLongTranslatableText(String key) {
		List<Text> lines = getLongTranslatableTextLines(key);
		if (lines.isEmpty())
			return TextInst.of(key);
		EditableText output = TextInst.copy(lines.get(0));
		for (int i = 1; i < lines.size(); i++)
			output.append("\n").append(lines.get(i));
		return output;
	}
	
	public static Text parseTranslatableFormatted(String key, Object... args) {
		return FancyText.parse(TextInst.translatable(key, args).getString());
	}
	
	public static Text substring(Text text, int start, int end) {
		EditableText output = TextInst.literal("");
		text.visit(new StyledVisitor<Boolean>() {
			private int i;
			@Override
			public Optional<Boolean> accept(Style style, String str) {
				if (i + str.length() <= start) {
					i += str.length();
					return Optional.empty();
				}
				if (i >= start) {
					if (end >= 0 && i + str.length() > end)
						return accept(style, str.substring(0, end - i));
					output.append(TextInst.literal(str).fillStyle(style));
					i += str.length();
					if (end >= 0 && i == end)
						return Optional.of(true);
					return Optional.empty();
				} else {
					str = str.substring(start - i);
					i = start;
					accept(style, str);
					return Optional.empty();
				}
			}
		}, Style.EMPTY);
		return output;
	}
	public static Text substring(Text text, int start) {
		return substring(text, start, -1);
	}
	
	public static Text deleteCharAt(Text text, int index) {
		EditableText output = TextInst.literal("");
		AtomicInteger pos = new AtomicInteger(0);
		text.visit((style, str) -> {
			int strLen = str.length();
			if (pos.getPlain() <= index && index < pos.getPlain() + strLen)
				str = new StringBuilder(str).deleteCharAt(index - pos.getPlain()).toString();
			if (!str.isEmpty())
				output.append(TextInst.literal(str).setStyle(style));
			pos.setPlain(pos.getPlain() + strLen);
			return Optional.empty();
		}, Style.EMPTY);
		return output;
	}
	
	public static Text joinLines(List<Text> lines) {
		EditableText output = TextInst.literal("");
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0)
				output.append("\n");
			output.append(lines.get(i));
		}
		return output;
	}
	public static List<Text> splitText(Text text) {
		List<Text> output = new ArrayList<>();
		int i;
		while ((i = text.getString().indexOf('\n')) != -1) {
			output.add(substring(text, 0, i));
			text = substring(text, i + 1);
		}
		output.add(text);
		return output;
	}
	
	public static Text stripInvalidChars(Text text, boolean allowLineBreaks) {
		EditableText output = TextInst.literal("");
		text.visit((style, str) -> {
			output.append(TextInst.literal(MVMisc.stripInvalidChars(str, allowLineBreaks)).setStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return output;
	}
	
	public static Text attachFileTextOptions(EditableText link, File file) {
		return link.append(" ").append(TextInst.translatable("nbteditor.file_options.show").styled(style ->
				style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
						file.getAbsoluteFile().getParentFile().getAbsolutePath()))))
				.append(" ").append(TextInst.translatable("nbteditor.file_options.delete").styled(style ->
				MixinLink.withRunClickEvent(style, () -> MainUtil.client.setScreen(
						new FancyConfirmScreen(confirmed -> {
							if (confirmed) {
								if (file.exists()) {
									try {
										Files.deleteIfExists(file.toPath());
										MainUtil.client.player.sendMessage(TextInst.translatable("nbteditor.file_options.delete.success", "§6" + file.getName()), false);
									} catch (IOException e) {
										NBTEditor.LOGGER.error("Error deleting file", e);
										MainUtil.client.player.sendMessage(TextInst.translatable("nbteditor.file_options.delete.error", "§6" + file.getName()), false);
									}
								} else
									MainUtil.client.player.sendMessage(TextInst.translatable("nbteditor.file_options.delete.missing", "§6" + file.getName()), false);
							}
							MainUtil.client.setScreen(null);
						}, TextInst.translatable("nbteditor.file_options.delete.title", file.getName()),
								TextInst.translatable("nbteditor.file_options.delete.desc", file.getName()))))));
	}
	
	public static boolean isTextFormatted(Text text, boolean allowNonNull, String baseColor) {
		return isTextFormatted(TextInst.toJsonTree(text), allowNonNull, baseColor);
	}
	private static boolean isTextFormatted(JsonElement dataElement, boolean allowNonNull, String baseColor) {
		if (!(dataElement instanceof JsonObject data))
			return false;
		
		if (data.has("extra")) {
			for (JsonElement part : data.get("extra").getAsJsonArray()) {
				if (isTextFormatted(part, allowNonNull, baseColor))
					return true;
			}
		}
		
		if (!allowNonNull)
			return data.keySet().stream().anyMatch(key -> !key.equals("text") && !key.equals("extra"));
		
		if (data.has("bold") && data.get("bold").getAsBoolean())
			return true;
		if (data.has("italic") && data.get("italic").getAsBoolean())
			return true;
		if (data.has("underlined") && data.get("underlined").getAsBoolean())
			return true;
		if (data.has("strikethrough") && data.get("strikethrough").getAsBoolean())
			return true;
		if (data.has("obfuscated") && data.get("obfuscated").getAsBoolean())
			return true;
		if (data.has("color") && (baseColor == null || !data.get("color").getAsString().equals(baseColor)))
			return true;
		if (data.has("insertion") && data.get("insertion").getAsBoolean())
			return true;
		if (data.has("clickEvent"))
			return true;
		if (data.has("hoverEvent"))
			return true;
		if (data.has("font") && !data.get("font").getAsString().equals(Style.DEFAULT_FONT_ID.toString()))
			return true;
		
		return false;
	}
	
	public static boolean styleEqualsExact(Style a, Style b) {
		return Objects.equals(a.getColor(), b.getColor()) &&
				a.bold == b.bold &&
				a.italic == b.italic &&
				a.underlined == b.underlined &&
				a.strikethrough == b.strikethrough &&
				a.obfuscated == b.obfuscated &&
				Objects.equals(a.getClickEvent(), b.getClickEvent()) &&
				Objects.equals(a.getHoverEvent(), b.getHoverEvent()) &&
				Objects.equals(a.getInsertion(), b.getInsertion()) &&
				Objects.equals(a.getFont(), b.getFont());
	}
	
	public static boolean hasFormatting(Style style, Formatting formatting) {
		return styleEqualsExact(style, style.withFormatting(formatting));
	}
	
	public static Style removeFormatting(Style style, Formatting formatting, boolean force) {
		if (formatting == Formatting.RESET)
			return style;
		if (formatting.isColor())
			return style.withColor(force ? Formatting.WHITE : null);
		Boolean newValue = (force ? false : null);
		return switch (formatting) {
			case BOLD -> style.withBold(newValue);
			case ITALIC -> style.withItalic(newValue);
			case UNDERLINE -> style.withUnderline(newValue);
			case STRIKETHROUGH -> style.withStrikethrough(newValue);
			case OBFUSCATED -> style.withObfuscated(newValue);
			default -> throw new IllegalArgumentException("Unknown formatting: " + formatting);
		};
	}
	
	public static Style forceReset(Style base) {
		return Style.EMPTY
				.withColor(base.getColor() == null ||
						base.getColor().equals(TextColor.fromFormatting(Formatting.WHITE)) ? null : Formatting.WHITE)
				.withBold(base.bold == null || !base.bold ? null : false)
				.withItalic(base.italic == null || !base.italic ? null : false)
				.withUnderline(base.underlined == null || !base.underlined ? null : false)
				.withStrikethrough(base.strikethrough == null || !base.strikethrough ? null : false)
				.withObfuscated(base.obfuscated == null || !base.obfuscated ? null : false);
	}
	
	public static int lastIndexOf(Text text, int ch) {
		AtomicInteger output = new AtomicInteger(-1);
		AtomicInteger pos = new AtomicInteger(0);
		text.visit(str -> {
			int i = str.lastIndexOf(ch);
			if (i != -1)
				output.setPlain(pos.getPlain() + i);
			pos.setPlain(pos.getPlain() + str.length());
			return Optional.empty();
		});
		return output.getPlain();
	}
	
	public static Text fromJsonSafely(String json) {
		try {
			Text output = TextInst.fromJson(json);
			if (output != null)
				return output;
		} catch (JsonParseException e) {}
		return TextInst.of(json);
	}
	
	public static final Style RESET_STYLE = Style.EMPTY.withColor(Formatting.WHITE)
			.withBold(false).withItalic(false).withUnderline(false).withStrikethrough(false).withObfuscated(false);
	
	public static Style getBaseNameStyle(LocalNBT localNBT, boolean itemName) {
		Style baseNameStyle = Style.EMPTY;
		if (localNBT instanceof LocalItem item) {
			if (!itemName)
				baseNameStyle = baseNameStyle.withFormatting(Formatting.ITALIC);
			baseNameStyle = baseNameStyle.withFormatting(item.getEditableItem().getRarity().formatting);
		} else if (localNBT instanceof LocalBlock)
			;
		else if (localNBT instanceof LocalEntity)
			baseNameStyle = baseNameStyle.withFormatting(Formatting.WHITE);
		else
			throw new IllegalStateException("Cannot get base name style for " + localNBT.getClass().getName());
		
		return baseNameStyle;
	}
	
	public static final Style BASE_LORE_STYLE = Style.EMPTY.withFormatting(Formatting.ITALIC, Formatting.DARK_PURPLE);
	
	public static Style simplifyStyle(Style style, Style base) {
		return Style.EMPTY
				.withColor(Objects.equals(style.getColor(), base.getColor()) ? null : style.getColor())
				.withBold(Objects.equals(style.bold, base.bold) ? null : style.bold)
				.withItalic(Objects.equals(style.italic, base.italic) ? null : style.italic)
				.withUnderline(Objects.equals(style.underlined, base.underlined) ? null : style.underlined)
				.withStrikethrough(Objects.equals(style.strikethrough, base.strikethrough) ? null : style.strikethrough)
				.withObfuscated(Objects.equals(style.obfuscated, base.obfuscated) ? null : style.obfuscated)
				.withClickEvent(Objects.equals(style.getClickEvent(), base.getClickEvent()) ? null : style.getClickEvent())
				.withHoverEvent(Objects.equals(style.getHoverEvent(), base.getHoverEvent()) ? null : style.getHoverEvent());
	}
	
}
