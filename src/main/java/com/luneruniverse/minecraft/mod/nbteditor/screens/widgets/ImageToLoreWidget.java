package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.EditableText;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.ScreenTexts;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlayScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlaySupportingScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ImageToLoreWidget extends GroupWidget implements InitializableOverlay<Screen> {
	
	public static List<Text> imageToLore(BufferedImage img, int width, int height) {
		img = MainUtil.scaleImage(img, width, height);
		List<Text> output = new ArrayList<>();
		for (int line = 0; line < height; line++) {
			EditableText lineText = TextInst.literal("").styled(style -> style.withItalic(false));
			for (int i = 0; i < width; i++) {
				final int color = img.getRGB(i, line) & 0xFFFFFF;
				lineText.append(TextInst.literal("█").styled(style -> style.withColor(color)));
			}
			output.add(lineText);
		}
		return output;
	}
	
	public static boolean openImportFiles(List<Path> paths, BiConsumer<File, List<Text>> loreConsumers, Runnable onDone) {
		Map<File, BufferedImage> imgs = new LinkedHashMap<>();
		for (Path path : paths) {
			File file = path.toFile();
			try {
				BufferedImage img = ImageIO.read(file);
				if (img != null)
					imgs.put(file, img);
			} catch (IOException e) {}
		}
		if (imgs.isEmpty())
			return false;
		
		OverlayScreen.setOverlayOrScreen(new ImageToLoreWidget(optional -> {
			OverlaySupportingScreen.setOverlayStatic(null);
			
			if (optional.isEmpty())
				return;
			ImageToLoreOptions options = optional.get();
			
			imgs.forEach((file, img) -> {
				int width = img.getWidth();
				int height = img.getHeight();
				if (options.width() != null && options.height() != null) {
					width = options.width();
					height = options.height();
				} else if (options.width() != null) {
					height = (int) ((double) options.width() / width * height);
					width = options.width();
				} else if (options.height() != null) {
					width = (int) ((double) options.height() / height * width);
					height = options.height();
				}
				
				loreConsumers.accept(file, imageToLore(img, width, height));
			});
			
			onDone.run();
		}), 500, true);
		
		return true;
	}
	
	public static record ImageToLoreOptions(Integer width, Integer height) {}
	
	private final Consumer<Optional<ImageToLoreOptions>> optionsConsumer;
	private final TextRenderer textRenderer;
	private int width;
	private int height;
	private NamedTextFieldWidget imgWidth;
	private NamedTextFieldWidget imgHeight;
	
	public ImageToLoreWidget(Consumer<Optional<ImageToLoreOptions>> optionsConsumer) {
		this.optionsConsumer = optionsConsumer;
		this.textRenderer = MainUtil.client.textRenderer;
	}
	
	@Override
	public void init(Screen parent, int width, int height) {
		clearWidgets();
		
		this.width = width;
		this.height = height;
		
		String prevImgWidth = (imgWidth == null ? null : imgWidth.getText());
		String prevImgHeight = (imgHeight == null ? null : imgHeight.getText());
		
		imgWidth = addWidget(new NamedTextFieldWidget(width / 2 - 102, height / 2 - 18, 100, 16)
				.name(TextInst.translatable("nbteditor.img_to_lore.width")));
		imgHeight = addWidget(new NamedTextFieldWidget(width / 2 + 2, height / 2 - 18, 100, 16)
				.name(TextInst.translatable("nbteditor.img_to_lore.height")));
		
		imgWidth.setTextPredicate(MainUtil.intPredicate(1, Integer.MAX_VALUE, true));
		imgHeight.setTextPredicate(MainUtil.intPredicate(1, Integer.MAX_VALUE, true));
		
		if (prevImgWidth != null)
			imgWidth.setText(prevImgWidth);
		if (prevImgHeight != null)
			imgHeight.setText(prevImgHeight);
		
		addWidget(MVMisc.newButton(width / 2 - 102, height / 2 + 2, 100, 20, ScreenTexts.DONE, btn -> {
			optionsConsumer.accept(Optional.of(new ImageToLoreOptions(
					MainUtil.parseOptionalInt(imgWidth.getText()), MainUtil.parseOptionalInt(imgHeight.getText()))));
		}));
		addWidget(MVMisc.newButton(width / 2 + 2, height / 2 + 2, 100, 20, ScreenTexts.CANCEL, btn -> {
			optionsConsumer.accept(Optional.empty());
		}));
	}
	
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		MainUtil.client.currentScreen.renderBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);
		MVDrawableHelper.drawCenteredTextWithShadow(matrices, textRenderer, TextInst.translatable("nbteditor.img_to_lore"),
				width / 2, height / 2 - textRenderer.fontHeight - 22, -1);
		MainUtil.renderLogo(matrices);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			OverlaySupportingScreen.setOverlayStatic(null);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			optionsConsumer.accept(Optional.of(new ImageToLoreOptions(
					MainUtil.parseOptionalInt(imgWidth.getText()), MainUtil.parseOptionalInt(imgHeight.getText()))));
			return true;
		}
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
}
