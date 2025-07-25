package com.luneruniverse.minecraft.mod.nbteditor.fancytext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.EditableText;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.util.StyleUtil;
import com.mojang.brigadier.StringReader;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class FancyText {
	
	public static Text parse(String str, Style base) {
		List<FancyTextToken> tokens = FancyTextToken.parse(new StringReader(str));
		List<FancyTextNode> nodes = FancyTextNode.parse(tokens);
		return gen(nodes, base.withParent(StyleUtil.RESET_STYLE));
	}
	private static EditableText gen(List<FancyTextNode> nodes, Style base) {
		int numberOfTextNodes = nodes.stream().mapToInt(FancyTextNode::getNumberOfTextNodes).sum();
		if (numberOfTextNodes == 0)
			return TextInst.literal("");
		
		EditableText output = TextInst.literal("");
		Style style = base;
		for (FancyTextNode node : nodes) {
			if (node instanceof FancyTextTextNode text)
				output.append(TextInst.literal(text.text()).setStyle(StyleUtil.minus(style, base)));
			else if (node instanceof FancyTextStyleOptionNode event) {
				if (numberOfTextNodes != 1 || event.getNumberOfTextNodes() == 1) {
					Style eventStyle = event.modifyStyle(style);
					output.append(gen(event.contents(), eventStyle).styled(
							genStyle -> StyleUtil.minus(genStyle.withParent(eventStyle), base)));
				}
			} else
				style = node.modifyStyle(style);
		}
		if (numberOfTextNodes == 1)
			return (EditableText) output.getSiblings().get(0);
		return output;
	}
	
	public static Map.Entry<String, Boolean> stringify(Text text, Style base) {
		base = base.withParent(StyleUtil.RESET_STYLE);
		StringBuilder output = new StringBuilder();
		
		AtomicReference<Style> style = new AtomicReference<>(base);
		AtomicReference<Style> eventContentsStyle = new AtomicReference<>(base);
		AtomicReference<ClickEvent> clickEvent = new AtomicReference<>(null);
		AtomicReference<HoverEvent> hoverEvent = new AtomicReference<>(null);
		AtomicReference<String> insertion = new AtomicReference<>(null);
		AtomicReference<Identifier> font = new AtomicReference<>(null);
		AtomicBoolean errors = new AtomicBoolean(false);
		text.visit((partStyle, partText) -> {
			if (!Objects.equals(partStyle.getClickEvent(), clickEvent.getPlain()) ||
					!Objects.equals(partStyle.getHoverEvent(), hoverEvent.getPlain()) ||
					!Objects.equals(partStyle.getInsertion(), insertion.getPlain()) ||
					!Objects.equals(partStyle.font, font.getPlain())) {
				if (clickEvent.getPlain() != null)
					output.append(')');
				if (hoverEvent.getPlain() != null)
					output.append(')');
				if (insertion.getPlain() != null)
					output.append(')');
				if (font.getPlain() != null)
					output.append(')');
				eventContentsStyle.setPlain(style.getPlain());
				clickEvent.setPlain(partStyle.getClickEvent());
				hoverEvent.setPlain(partStyle.getHoverEvent());
				insertion.setPlain(partStyle.getInsertion());
				font.setPlain(partStyle.font);
				if (partStyle.getClickEvent() != null) {
					output.append('[');
					output.append(MVMisc.getClickEventActionName(partStyle.getClickEvent().getAction()));
					output.append("]{");
					output.append(partStyle.getClickEvent().getValue().replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}"));
					output.append("}(");
				}
				if (partStyle.getHoverEvent() != null) {
					output.append('[');
					output.append(MVMisc.getHoverEventActionName(partStyle.getHoverEvent().getAction()));
					output.append(']');
					if (partStyle.getHoverEvent().getAction() == HoverEvent.Action.SHOW_TEXT) {
						Map.Entry<String, Boolean> showTextContents =
								stringify(partStyle.getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT));
						if (showTextContents.getValue())
							errors.setPlain(true);
						output.append('{');
						output.append(showTextContents.getKey());
						output.append('}');
					} else if (partStyle.getHoverEvent().getAction() == HoverEvent.Action.SHOW_ITEM) {
						errors.setPlain(true);
					} else if (partStyle.getHoverEvent().getAction() == HoverEvent.Action.SHOW_ENTITY) {
						output.append('{');
						output.append(partStyle.getHoverEvent().getValue(HoverEvent.Action.SHOW_ENTITY).uuid.toString());
						output.append('}');
						errors.setPlain(true);
					}
					output.append("(");
				}
				if (partStyle.getInsertion() != null) {
					output.append("[insertion]{");
					output.append(partStyle.getInsertion().replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}"));
					output.append("}(");
				}
				if (partStyle.font != null) {
					output.append("[font]{");
					output.append(partStyle.font.toString());
					output.append("}(");
				}
			}
			
			AtomicReference<Style> currentStyle =
					(clickEvent.getPlain() != null || hoverEvent.getPlain() != null ? eventContentsStyle : style);
			Style changes = StyleUtil.minus(partStyle, currentStyle.getPlain());
			currentStyle.setPlain(partStyle);
			
			if (changes.bold != null && !changes.bold ||
					changes.italic != null && !changes.italic ||
					changes.underlined != null && !changes.underlined ||
					changes.strikethrough != null && !changes.strikethrough ||
					changes.obfuscated != null && !changes.obfuscated) {
				output.append("&r");
				changes = StyleUtil.minus(partStyle, StyleUtil.RESET_STYLE);
			}
			
			if (changes.getColor() != null) {
				Formatting formatting = Formatting.byName(changes.getColor().getName());
				if (formatting == null)
					output.append("&" + changes.getColor().getHexCode());
				else
					output.append("&" + formatting.getCode());
			}
			if (StyleUtil.SHADOW_COLOR_EXISTS && changes.getShadowColor() != null) {
				if (changes.getShadowColor() >>> 24 == 0xFF)
					output.append(String.format("&_#%06X;", changes.getShadowColor() & 0xFFFFFF));
				else
					output.append(String.format("&_#%08X;", changes.getShadowColor()));
			}
			if (changes.isBold())
				output.append("&l");
			if (changes.isItalic())
				output.append("&o");
			if (changes.isUnderlined())
				output.append("&n");
			if (changes.isStrikethrough())
				output.append("&m");
			if (changes.isObfuscated())
				output.append("&k");
			
			output.append(partText.replace("\\", "\\\\")
					.replace("&", "\\&").replace("§", "\\§")
					.replace("(", "\\(").replace(")", "\\)")
					.replace("[", "\\[").replace("]", "\\]")
					.replace("{", "\\{").replace("}", "\\}"));
			
			return Optional.empty();
		}, base);
		
		if (clickEvent.getPlain() != null)
			output.append(')');
		if (hoverEvent.getPlain() != null)
			output.append(')');
		if (insertion.getPlain() != null)
			output.append(')');
		if (font.getPlain() != null)
			output.append(')');
		
		return Map.entry(output.toString(), errors.getPlain());
	}
	
	public static Text parse(String str) {
		return parse(str, Style.EMPTY);
	}
	public static Map.Entry<String, Boolean> stringify(Text text) {
		return stringify(text, Style.EMPTY);
	}
	
}
