package lNalator.Cultivationcraft.Client.GUI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;

public class BetterFontRenderer {

    final static int heightSpacing = 1;
    protected static int currentHeight = 0;

    // Makes "\n" work, cuz apparently that's too complicated for base Minecraft!?
    public static void draw(Font font, GuiGraphics guiGraphics, String string, float x, float y, int color) {
        int line = 0;

        for (String newString : getNewLines(string)) {
            guiGraphics.drawString(font, newString, x, y + line, color, false);
            line += font.lineHeight + heightSpacing;
        }
    }

    protected static ArrayList<String> getNewLines(String string) {
        ArrayList<String> newLined = new ArrayList<>();

        if (string.contains("\n")) {
            for (String newString : string.split("\n")) {
                newLined.add(newString); 
            }
        }else {
            newLined.add(string);
        }

        return newLined;
    }

    // Wrap the text to fit the specified width
    // Ensures whole words per line
    public static void wordwrap(Font font, GuiGraphics guiGraphics, String string, float x, float y, int color, int width) {
        currentHeight = 0;

        for (String newString : getNewLines(string)) {
            wordwrapSingleLine(font, guiGraphics, newString, x, y + currentHeight, color, width, Integer.MAX_VALUE);
        }
    }

    public static int countLines(Font font, String string, int width) {
        int remaining = 0;

        for (String newString : getNewLines(string)) {
            remaining += wordwrapSkipOverLines(font, newString, width);
        }

        return remaining;
    }

    // Wrap the text to fit the specified width and height
    // Ensures whole words per line
    public static void wordwrap(Font font, GuiGraphics guiGraphics, String string, float x, float y, int color, int width, int height, int skipheight) {
        currentHeight = 0;

        for (String newString : getNewLines(string)) {
            if (currentHeight + font.lineHeight + heightSpacing > height) {
                return;
            }

            if (skipheight > 0) {
                skipheight -= wordwrapSkipOverLines(font, guiGraphics, newString, x, y + currentHeight, color, width, height, skipheight);
            } else {
                wordwrapSingleLine(font, guiGraphics, newString, x, y + currentHeight, color, width, height);
            }
        }
    }

    protected static int wordwrapSkipOverLines(Font font, String string, int width) {
        int skipped = 0;

        while (string.length() > 0) {
            String lineString = font.plainSubstrByWidth(string, width);
            // Always consume at least one char, otherwise a width smaller than a char loops forever
            if (lineString.isEmpty()) lineString = string.substring(0, 1);

            if (lineString.length() < string.length() && !string.substring(lineString.length()).startsWith(" ") && lineString.lastIndexOf(" ") > -1) {
                lineString = lineString.substring(0, lineString.lastIndexOf(" "));
            }

            string = string.substring(lineString.length());
            string = string.trim();

            skipped += font.lineHeight + heightSpacing;
        }

        return skipped;
    }

    protected static int wordwrapSkipOverLines(Font font, GuiGraphics guiGraphics, String string, float x, float y, int color, int width, int maxHeight, int heightToSkip) {
        int skipped = 0;

        while (skipped < heightToSkip && string.length() > 0) {
            String lineString = font.plainSubstrByWidth(string, width);
            // Always consume at least one char, otherwise a width smaller than a char loops forever
            if (lineString.isEmpty()) lineString = string.substring(0, 1);

            if (lineString.length() < string.length() && !string.substring(lineString.length()).startsWith(" ") && lineString.lastIndexOf(" ") > -1) {
                lineString = lineString.substring(0, lineString.lastIndexOf(" "));
            }

            string = string.substring(lineString.length());
            string = string.trim();

            skipped += font.lineHeight + heightSpacing;

            if (skipped >= heightToSkip) {
                wordwrapSingleLine(font, guiGraphics, string, x, y, color, width, maxHeight);
                return skipped;
            }
        }

        return skipped;
    }

    protected static void wordwrapSingleLine(Font font, GuiGraphics guiGraphics, String string, float x, float y, int color, int width, int maxHeight) {
        int line = 0;

        while (string.length() > 0) {
            // Don't draw extra lines if it will overflow the textfield
            if (currentHeight + font.lineHeight > maxHeight) {
                return;
            }

            String lineString = font.plainSubstrByWidth(string, width);
            // Always consume at least one char, otherwise a width smaller than a char loops forever
            if (lineString.isEmpty()) lineString = string.substring(0, 1);

            // Ensure the line string ends with a whole word
            // Don't bother adjusting the lineString if it contains everything string does or string starts with a space
            // Also don't try to adjust the lineString if it contains no spaces
            if (lineString.length() < string.length() && !string.substring(lineString.length()).startsWith(" ") && lineString.lastIndexOf(" ") > -1) {
                lineString = lineString.substring(0, lineString.lastIndexOf(" "));
            }

            guiGraphics.drawString(font, lineString, x, y + line, color, false);

            // Remove the lineString from the string and increase the line position
            string = string.substring(lineString.length());
            string = string.trim();

            currentHeight += font.lineHeight + heightSpacing;
            line += font.lineHeight + heightSpacing;
        }
    }

    // Wrap the text to fit the specified width
    public static void wrap(Font font, GuiGraphics guiGraphics, String string, float x, float y, int color, int width) {
        int line = 0;

        while (string.length() > 0) {
            String lineString = font.plainSubstrByWidth(string, width);
            // Always consume at least one char, otherwise a width smaller than a char loops forever
            if (lineString.isEmpty()) lineString = string.substring(0, 1);

            guiGraphics.drawString(font, lineString, x, y + line, color, false);

            // Remove the lineString from the string and increase the line position
            string = string.substring(lineString.length());
            line += font.lineHeight;
        }
    }
}
