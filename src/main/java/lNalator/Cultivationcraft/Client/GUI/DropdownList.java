package lNalator.Cultivationcraft.Client.GUI;

import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.util.LinkedHashMap;

// T: the type of value stored behind each displayed name
public class DropdownList<T> {

    protected static final ResourceLocation TEXTURE = new ResourceLocation(Cultivationcraft.MODID, "textures/gui/dropdownlist.png");

    public static final int width = 85;
    public static final int closedHeight = 11;
    public static final int openHeight = 71;

    public static final int scrollbarXStart = 75;
    public static final int scrollbarYDown = 60;

    public static final int scrollBarTop = 11;
    public static final int scrollBarBottom = 51;

    protected static final int scrollBarSize = 9;

    protected static final int MaxSize = 6;

    protected int scrollOffset = 0;

    // Map String, String = Name, Value
    // Name is what the entry displays
    // Value is the data stored in the entry
    protected LinkedHashMap<String, T> items = new LinkedHashMap<String, T>();

    protected String selected = null;

    boolean isOpen = false;

    public DropdownList() {
    }

    public void addItem(String name, T value) {
        items.put(name, value);

        if (selected == null) {
            selected = name;
        }
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    // Return the Value of the selected list entry
    public T getSelected() {
        return items.get(selected);
    }

    public boolean mouseScroll(double mouseX, double mouseY, double direction) {
        if (!isOpen) {
            return false;
        }

        scroll((int) -direction);

        return true;
    }

    public T mouseClick(int mouseX, int mouseY, int buttonPressed) {
        if (isOpen) {
            return mouseClickOpen(mouseX, mouseY, buttonPressed); 
        }else {
            mouseClickClosed(mouseX, mouseY, buttonPressed);
            return null;
        }
    }

    public void mouseClickClosed(int mouseX, int mouseY, int buttonPressed) {
        // Do nothing if the left button isn't pressed
        if (buttonPressed != 0) {
            return;
        }

        // If the mouse is pressed on the selected item open the list
        if (mouseX >= 0 && mouseX <= width
                && mouseY >= 0 && mouseY <= closedHeight) {
            setOpen(true);
        }
    }

    // Process mouse click. Return new selected object if an object is selected.
    public T mouseClickOpen(int mouseX, int mouseY, int buttonPressed) {
        // Do nothing if the left button isn't pressed
        if (buttonPressed != 0) {
            return null;
        }

        // If the mouse is pressed outside the list then close the list
        if (mouseX < 0 || mouseX > width
                || mouseY < 0 || mouseY > openHeight) {
            setOpen(false);
            return null;
        }

        // If an item on the list is clicked, change selected item
        if (mouseX >= 0 && mouseX < scrollbarXStart) {
            if (mouseY > scrollBarTop && mouseY < openHeight) {
                if (changeSelection((float) (mouseY - scrollBarTop) / (float) (openHeight - scrollBarTop)));
            }
            {
                setOpen(false);
                return items.get(selected);
            }
        } // Else if the scrollbar is clicked, adjust scroll position based on where
        else if (mouseX >= scrollbarXStart && mouseX < width) {
            if (mouseY >= 0 && mouseY < closedHeight) {
                scroll(-1); 
            }else if (mouseY >= scrollbarYDown && mouseY < openHeight) {
                scroll(1); 
            }else if (mouseY > 0 && mouseY < openHeight) {
                scrollTo((float) (mouseY - scrollBarTop) / (float) (scrollbarYDown - scrollBarTop));
            }
        }

        return null;
    }

    public void changeSelection(String selection) {
        selected = selection;
        scrollOffset = Math.min(scrollOffset, maxScroll());
    }

    // Change the selected item based on location clicked on dropdown list
    public boolean changeSelection(float percent) {
        int selection = (int) (percent * MaxSize);

        int i = 0;
        int count = 0;

        for (String name : items.keySet()) {
            if (!name.contentEquals(selected)) {
                // Only start counting once the scroll offset has been reached
                if (scrollOffset <= i) {
                    if (count == selection) {
                        changeSelection(name);
                        return true;
                    }

                    count++;
                } else {
                    i++;
                }
            }
        }

        return false;
    }

    // The open list shows every item except the selected one. The selected name is not always an item
    // (e.g. a technique already placed in the slot is left out of the list), so count what is really shown;
    // assuming the selected item was always hidden made the last entry unreachable.
    protected int maxScroll() {
        int shown = items.size() - (items.containsKey(selected) ? 1 : 0);
        return Math.max(0, shown - MaxSize);
    }

    public void scrollTo(float percent) {
        scrollOffset = Math.max(0, Math.min(maxScroll(), (int) (maxScroll() * percent + 0.5f)));
    }

    // Scroll the scroll bar
    public void scroll(int amount) {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset + amount));
    }

    public void render(GuiGraphics guiGraphics, int xpos, int ypos, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw the list open or closed
        if (isOpen) {
            guiGraphics.blit(TEXTURE, xpos, ypos, width + 1, 0, width, openHeight, 255, 255);

            drawScrollBar(guiGraphics, xpos, ypos, mouseX, mouseY);
        } else {
            guiGraphics.blit(TEXTURE, xpos, ypos, 0, 0, width, closedHeight, 255, 255);
        }

        // Draw the selected item
        guiGraphics.drawString(Minecraft.getInstance().font, selected, xpos + 2, ypos + 2, Color.white.getRGB(), false);

        // Draw all items if open
        if (isOpen) {
            renderListText(guiGraphics, xpos, ypos);
        }

    }

    public void drawScrollBar(GuiGraphics guiGraphics, int xpos, int ypos, int mouseX, int mouseY) {
        // Highlight the scroll buttons if mouse is over them
        if (mouseX - xpos >= scrollbarXStart && mouseX - xpos < width) {
            if (mouseY - ypos >= 0 && mouseY - ypos < closedHeight) {
                guiGraphics.blit(TEXTURE, xpos + scrollbarXStart, ypos, width * 2 + 2, 0, width - scrollbarXStart, closedHeight, 255, 255); 
            }else if (mouseY - ypos >= scrollbarYDown && mouseY - ypos < openHeight) {
                guiGraphics.blit(TEXTURE, xpos + scrollbarXStart, ypos + scrollbarYDown, width * 2 + 2, scrollbarYDown, width - scrollbarXStart, closedHeight, 255, 255);
            }
        }

        // Draw the scrollbar
        float percent = maxScroll() == 0 ? 0 : (float) scrollOffset / maxScroll();
        int scrollPosition = scrollBarTop + (int) ((scrollBarBottom - scrollBarTop) * percent);

        guiGraphics.blit(TEXTURE, xpos + scrollbarXStart, ypos + scrollPosition, width * 2 + 2, closedHeight, width - scrollbarXStart, scrollBarSize, 255, 255);
    }

    // Draw visible list items
    protected void renderListText(GuiGraphics guiGraphics, int xpos, int ypos) {
        int i = 0;

        for (String name : items.keySet()) {
            if (!name.contentEquals(selected)) {
                // Only start displaying list items from the scroll offset
                if (scrollOffset <= i) {
                    guiGraphics.drawString(Minecraft.getInstance().font, name, xpos + 2, ypos + 2 + (i + 1 - scrollOffset) * (closedHeight - 1), Color.white.getRGB(), false);
                }

                i++;

                // Stop displaying items once MaxSize items have been displayed
                if (i == MaxSize + scrollOffset) {
                    break;
                }
            }
        }
    }
}
