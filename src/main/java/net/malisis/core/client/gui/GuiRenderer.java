/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 PaleoCrafter, Ordinastie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.malisis.core.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.decoration.UITooltip;
import net.malisis.core.client.gui.element.GuiShape;
import net.malisis.core.client.gui.icon.GuiIcon;
import net.malisis.core.renderer.BaseRenderer;
import net.malisis.core.renderer.RenderParameters;
import net.malisis.core.renderer.element.Face;
import net.malisis.core.renderer.element.Shape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GuiRenderer extends BaseRenderer
{
	/**
	 * Font renderer used to draw strings.
	 */
	public static FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
	/**
	 * RenderItem used to draw itemStacks.
	 */
	public static RenderItem itemRenderer = new RenderItem();
	/**
	 * Font height.
	 */
	public static int FONT_HEIGHT = fontRenderer.FONT_HEIGHT;
	/**
	 * Current component being drawn.
	 */
	public UIComponent currentComponent;
	/**
	 * Width of the Minecraft window.
	 */
	private int displayWidth;
	/**
	 * Height of the Minecraft window.
	 */
	private int displayHeight;
	/**
	 * Multiplying factor between GUI size and pixel size.
	 */
	private int scaleFactor;
	/**
	 * Should the rendering be done according to scaleFactor
	 */
	private boolean ignoreScale = false;
	/**
	 * Scale to use when drawing fonts
	 */
	private float fontScale = 1F;
	/**
	 * Current X position of the mouse.
	 */
	public int mouseX;
	/**
	 * Current Y position of the mouse.
	 */
	public int mouseY;
	/**
	 * Default texture to use for current gui.
	 */
	private GuiTexture defaultGuiTexture;
	/**
	 * Determines whether the texture has been changed.
	 */
	private boolean defaultTexture = true;

	public GuiRenderer()
	{
		defaultGuiTexture = new GuiTexture(new ResourceLocation("malisiscore", "textures/gui/gui.png"), 300, 100);
		updateGuiScale();
	}

	/**
	 * @return the defaultGuiTexture
	 */
	public GuiTexture getGuiTexture()
	{
		return defaultGuiTexture;
	}

	/**
	 * Sets whether to ignore default Minecraft GUI scale factor.<br />
	 * If set to true, 1 pixel size will be equal to 1 pixel on screen.
	 *
	 * @param ignore
	 */
	public void setIgnoreScale(boolean ignore)
	{
		ignoreScale = ignore;
	}

	/**
	 * @return is the scale ignored
	 */
	public boolean isIgnoreScale()
	{
		return ignoreScale;
	}

	/**
	 * Sets a custom font scale factor.
	 *
	 * @param scale
	 */
	public void setFontScale(float scale)
	{
		fontScale = scale;
	}

	/**
	 * Sets the width, height and scale factor for this <code>GuiRenderer</code>.
	 *
	 * @return
	 */
	public int updateGuiScale()
	{
		Minecraft mc = Minecraft.getMinecraft();
		displayWidth = mc.displayWidth;
		displayHeight = mc.displayHeight;
		calcScaleFactor(mc.gameSettings.guiScale);
		return scaleFactor;
	}

	/**
	 * Sets the mouse position and the partial tick.
	 *
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 */
	public void set(int mouseX, int mouseY, float partialTicks)
	{
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.partialTick = partialTicks;
	}

	/**
	 * Draws the component to the screen.
	 *
	 * @param container
	 * @param mouseX
	 * @param mouseY
	 * @param partialTick
	 */
	public void drawScreen(UIContainer container, int mouseX, int mouseY, float partialTick)
	{
		set(mouseX, mouseY, partialTick);

		if (container != null)
		{
			setDefaultTexture();

			if (ignoreScale)
			{
				GL11.glPushMatrix();
				GL11.glScalef(1F / scaleFactor, 1F / scaleFactor, 1);
			}

			startDrawing();

			container.draw(this, mouseX, mouseY, partialTick);

			draw();

			if (ignoreScale)
				GL11.glPopMatrix();
		}
	}

	/**
	 * Draws a tooltip to the screen
	 *
	 * @param tooltip
	 */
	public void drawTooltip(UITooltip tooltip)
	{
		if (tooltip != null)
		{
			t.startDrawingQuads();
			tooltip.draw(this, mouseX, mouseY, partialTick);
			t.draw();
		}
	}

	/**
	 * Draws a shape to the GUI with the specified parameters.
	 *
	 * @param s
	 * @param params
	 * @param icons
	 */
	public void drawShape(GuiShape s, RenderParameters params)
	{
		if (s == null)
			return;

		shape = s;
		rp = params != null ? params : new RenderParameters();

		// move the shape at the right coord on screen
		shape.translate(currentComponent.screenX(), currentComponent.screenY(), currentComponent.getZIndex());
		shape.applyMatrix();

		applyTexture(shape, rp);

		for (Face face : s.getFaces())
			drawFace(face, face.getParameters());
	}

	@Override
	public void applyTexture(Shape shape, RenderParameters parameters)
	{
		if (parameters.icon.get() == null)
			return;

		Face[] faces = shape.getFaces();
		IIcon icon = parameters.icon.get();
		boolean isGuiIcon = icon instanceof GuiIcon;

		for (int i = 0; i < faces.length; i++)
			faces[i].setTexture(isGuiIcon ? ((GuiIcon) icon).getIcon(i) : icon, false, false, false);
	}

	/**
	 * Clips a string to fit in the specified width. The string is translated before clipping.
	 *
	 * @param text
	 * @param width
	 * @return
	 */
	public String clipString(String text, int width)
	{
		text = StatCollector.translateToLocal(text);
		StringBuilder ret = new StringBuilder();
		int strWidth = 0;
		int index = 0;
		while (index < text.length())
		{
			char c = text.charAt(index);
			strWidth += getCharWidth(c);
			if (strWidth < width)
				ret.append(c);
			else
				return ret.toString();
			index++;
		}

		return ret.toString();
	}

	/**
	 * Splits the string in multiple lines to fit in the specified maxWidth.
	 *
	 * @param text
	 * @param maxWidth
	 * @return list of lines that won't exceed maxWidth limit
	 */
	public static List<String> wrapText(String text, int maxWidth)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		StringBuilder word = new StringBuilder();

		int lineWidth = 0;
		int wordWidth = 0;
		int index = 0;
		while (index < text.length())
		{
			char c = text.charAt(index);
			int w = getCharWidth(c);
			lineWidth += w;
			wordWidth += w;
			word.append(c);
			if (c == ' ' || c == '-' || c == '.')
			{
				line.append(word);
				word.setLength(0);
				wordWidth = 0;
			}
			if (lineWidth >= maxWidth)
			{
				if (line.length() == 0)
				{
					line.append(word);
					word.setLength(0);
					wordWidth = 0;
				}
				lines.add(line.toString());
				line.setLength(0);
				lineWidth = wordWidth;
			}
			index++;
		}

		line.append(word);
		lines.add(line.toString());

		return lines;
	}

	/**
	 * Draws a white text on the GUI without shadow.
	 *
	 * @param text
	 */
	public void drawText(String text)
	{
		drawText(text, 0, 0, 0, 0xFFFFFF, false);
	}

	/**
	 * Draws a text on the GUI with specified color and shadow.
	 *
	 * @param text
	 * @param color
	 * @param shadow
	 */
	public void drawText(String text, int color, boolean shadow)
	{
		drawText(text, 0, 0, 0, color, shadow);
	}

	/**
	 * Draws a text on the GUI at the specified coordinates, relative to its parent container, with color and shadow.
	 *
	 * @param text
	 * @param x
	 * @param y
	 * @param color
	 * @param shadow
	 */
	public void drawText(String text, int x, int y, int color, boolean shadow)
	{
		drawText(text, x, y, 0, color, shadow);
	}

	/**
	 * Draws a text on the GUI at the specified coordinates, relative to its parent container, with zIndex, color and shadow.
	 *
	 * @param text
	 * @param x
	 * @param y
	 * @param zIndex
	 * @param color
	 * @param shadow
	 */
	public void drawText(String text, int x, int y, int zIndex, int color, boolean shadow)
	{
		drawString(text, currentComponent.screenX() + x, currentComponent.screenY() + y, currentComponent.getZIndex() + zIndex, color,
				shadow);
	}

	/**
	 * Draws a string at the specified coordinates, with color and shadow. The string gets translated. Uses FontRenderer.drawString().
	 *
	 * @param text
	 * @param x
	 * @param y
	 * @param z
	 * @param color
	 * @param shadow
	 */
	public void drawString(String text, int x, int y, int z, int color, boolean shadow)
	{
		if (fontRenderer == null)
			return;

		text = StatCollector.translateToLocal(text);

		GL11.glPushMatrix();
		//if (fontScale != 1)
		{
			GL11.glTranslatef(x * (1 - fontScale), y * (1 - fontScale), 0);
			GL11.glScalef(fontScale, fontScale, 1);
		}
		GL11.glTranslatef(0, 0, z);
		// GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		fontRenderer.drawString(text, x, y, color, shadow);
		GL11.glPopMatrix();
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		// GL11.glEnable(GL11.GL_DEPTH_TEST);
		setDefaultTexture();
	}

	/**
	 * Draws an itemStack to the GUI. Uses RenderItem.renderItemAndEffectIntoGUI() and RenderItem.renderItemOverlayIntoGUI();
	 *
	 * @param itemStack
	 * @param x
	 * @param y
	 */
	public void drawItemStack(ItemStack itemStack, int x, int y)
	{
		drawItemStack(itemStack, x, y, null);
	}

	/**
	 * Draws itemStack to the GUI. Uses RenderItem.renderItemAndEffectIntoGUI() and RenderItem.renderItemOverlayIntoGUI(); TODO: use
	 * currrentComponent position
	 *
	 * @param itemStack
	 * @param x
	 * @param y
	 * @param format
	 */
	public void drawItemStack(ItemStack itemStack, int x, int y, EnumChatFormatting format)
	{
		if (itemStack == null)
			return;

		FontRenderer fontRenderer = itemStack.getItem().getFontRenderer(itemStack);
		if (fontRenderer == null)
			fontRenderer = GuiRenderer.fontRenderer;

		String s = null;
		if (format != null)
			s = format + Integer.toString(itemStack.stackSize);

		t.draw();
		RenderHelper.enableGUIStandardItemLighting();
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);

		itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, Minecraft.getMinecraft().getTextureManager(), itemStack, x, y);
		itemRenderer.renderItemOverlayIntoGUI(fontRenderer, Minecraft.getMinecraft().getTextureManager(), itemStack, x, y, s);

		RenderHelper.disableStandardItemLighting();
		GL11.glColor4f(1, 1, 1, 1);
		//	GL11.glDisable(GL11.GL_ALPHA_TEST);

		setDefaultTexture();
		t.startDrawingQuads();
	}

	/**
	 * Starts clipping an area to prevent drawing outside of it.
	 *
	 * @param area
	 */
	public void startClipping(ClipArea area)
	{
		if (area.noClip || area.width() <= 0 || area.height() <= 0)
			return;

		GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);

		int f = ignoreScale ? 1 : scaleFactor;
		int x = area.x * f;
		int y = displayHeight - (area.y + area.height()) * f;
		int w = area.width() * f;
		int h = area.height() * f;;
		GL11.glScissor(x, y, w, h);
	}

	/**
	 * Ends the clipping.
	 *
	 * @param area
	 */
	public void endClipping(ClipArea area)
	{
		if (area.noClip || area.width() <= 0 || area.height() <= 0)
			return;

		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		GL11.glPopAttrib();
	}

	/**
	 * Calculate GUI scale factor.
	 *
	 * @param guiScale
	 */
	private void calcScaleFactor(int guiScale)
	{
		this.scaleFactor = 1;
		if (guiScale == 0)
			guiScale = 1000;

		while (this.scaleFactor < guiScale && this.displayWidth / (this.scaleFactor + 1) >= 320
				&& this.displayHeight / (this.scaleFactor + 1) >= 240)
			++this.scaleFactor;
	}

	/**
	 * Render the picked up itemStack at the cursor position.
	 *
	 * @param itemStack
	 */
	public void renderPickedItemStack(ItemStack itemStack)
	{
		if (itemStack == null)
			return;

		itemRenderer.zLevel = 100;
		t.startDrawingQuads();
		drawItemStack(itemStack, mouseX - 8, mouseY - 8, itemStack.stackSize == 0 ? EnumChatFormatting.YELLOW : null);
		t.draw();
		itemRenderer.zLevel = 0;
	}

	/**
	 * Gets rendering width of a string.
	 *
	 * @param str
	 * @return
	 */
	public static int getStringWidth(String str)
	{
		return getStringWidth(str, 1);
	}

	/**
	 * Gets rendering width of a string according to fontScale
	 *
	 * @param str
	 * @return
	 */
	public static int getStringWidth(String str, float fontScale)
	{
		str = StatCollector.translateToLocal(str);
		return Math.round(fontRenderer.getStringWidth(str) * fontScale);
	}

	/**
	 * Gets the rendering height of strings
	 *
	 * @return
	 */
	public static int getStringHeight()
	{
		return getStringHeight(1);
	}

	/**
	 * Gets the rendering height of strings according to fontscale
	 *
	 * @param fontScale
	 * @return
	 */
	public static int getStringHeight(float fontScale)
	{
		return Math.round(FONT_HEIGHT * fontScale);
	}

	/**
	 * Gets max rendering width of an array of string
	 *
	 * @param strings
	 * @return
	 */
	public static int getMaxStringWidth(List<String> strings)
	{
		int width = 0;
		for (String str : strings)
			width = Math.max(width, getStringWidth(str));
		return width;
	}

	/**
	 * Gets max rendering width of an array of string
	 *
	 * @param strings
	 * @return
	 */
	public static int getMaxStringWidth(String[] strings)
	{
		return getMaxStringWidth(Arrays.asList(strings));
	}

	/**
	 * Gets the rendering width of a char.
	 *
	 * @param c
	 * @return
	 */
	public static int getCharWidth(char c)
	{
		return fontRenderer.getCharWidth(c);
	}

	/**
	 * Bind a new texture for rendering.
	 *
	 * @param texture
	 */
	public void bindTexture(GuiTexture texture)
	{
		if (texture == null)
			return;

		defaultTexture = false;
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture.getResourceLocation());
	}

	/**
	 * Reset the texture to its default GuiRenderer.GUI_TEXTURE.
	 */
	public void setDefaultTexture()
	{
		bindTexture(defaultGuiTexture);
		defaultTexture = true;
	}

	@Override
	public void next()
	{
		super.next();
		if (!defaultTexture)
			setDefaultTexture();
	}
}