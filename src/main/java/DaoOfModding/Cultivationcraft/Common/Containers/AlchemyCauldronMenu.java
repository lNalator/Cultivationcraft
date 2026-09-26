package DaoOfModding.Cultivationcraft.Common.Containers;

import DaoOfModding.Cultivationcraft.Network.Packets.AlchemyPreviewPacket;
import DaoOfModding.Cultivationcraft.Common.Items.ItemRegister;
import DaoOfModding.Cultivationcraft.Common.Alchemy.AlchemyBatch;
import DaoOfModding.Cultivationcraft.Common.Alchemy.PillStacks;
import DaoOfModding.Cultivationcraft.Common.Knowledge.PlayerKnowledge;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
import java.util.ArrayList;
import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.entity.AlchemyCauldronBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Register;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class AlchemyCauldronMenu extends AbstractContainerMenu {
    private final Player viewer;
    private AlchemyPreviewPacket preview;
    private AlchemyPreviewPacket lastPreview;

    public void setPreview(AlchemyPreviewPacket preview) { this.preview = preview; }
    public ItemStack getPreviewItem() {
        if (preview != null && !preview.item().isEmpty()) return preview.item();
        ItemStack silhouette = new ItemStack(ItemRegister.ALCHEMY_PILL.get());
        silhouette.getOrCreateTag().putBoolean(DaoOfModding.Cultivationcraft.Common.Items.AlchemyPillItem.BLIND_PREVIEW_TAG, true);
        return silhouette;
    }
    public List<Component> previewTooltip() {
        var lines = new ArrayList<Component>();
        if (preview == null || preview.item().isEmpty()) { lines.add(Component.literal("???")); return lines; }
        lines.add(Component.literal(preview.item().getTag().getString("PillName")));
        lines.add(Component.translatable("cultivationcraft.jade.preview_qi", preview.qi()));
        if (!preview.valid()) lines.add(Component.translatable("cultivationcraft.jade.invalid_batch").withStyle(ChatFormatting.RED));
        if (preview.purityLow() >= 0) lines.add(Component.translatable("cultivationcraft.jade.purity_estimate", preview.purityLow(), preview.purityHigh()));
        return lines;
    }
    public static final int INGREDIENT_X = 32;
    public static final int INGREDIENT_Y = 30;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 18;
    public static final int HOTBAR_Y = 76;
    private static final int SLOT_COUNT = AlchemyCauldronBlockEntity.SLOT_COUNT;
    public static final int STATION_SLOTS = AlchemyCauldronBlockEntity.INVENTORY_SIZE;
    private final Container container;
    // Client rendering only: 0 = all slots, 1 = station, 2 = player inventory.
    private int visibleSection;
    private final ContainerData chargeData = new SimpleContainerData(3);

    public void setVisibleSection(int section) { visibleSection = section; }

    private Slot sectionSlot(Container owner, int index, int x, int y, int section) {
        return new Slot(owner, index, x, y) {
            @Override
            public boolean isActive() { return visibleSection == 0 || visibleSection == section; }
            @Override
            public boolean mayPlace(ItemStack stack) { return section != 1 || index < SLOT_COUNT; }
        };
    }

    public int getPreviewSlot() {
        boolean ingredients = false;
        for (int i = 0; i < SLOT_COUNT; i++) ingredients |= !container.getItem(i).isEmpty();
        if (ingredients) for (int i = SLOT_COUNT; i < STATION_SLOTS; i++) if (container.getItem(i).isEmpty()) return i;
        return -1;
    }

    public int getStoredQi() {
        return (chargeData.get(0) & 0xFFFF) | ((chargeData.get(1) & 0xFFFF) << 16);
    }

    public boolean isReceivingQi() { return chargeData.get(2) != 0; }
    private final ContainerLevelAccess access;
    private final ServerLevel serverLevel;
    // Menu data packets transmit signed shorts. Split each total into two halves.
    private final ContainerData qiData = new SimpleContainerData(AlchemyQi.ELEMENTS.size() * 2);

    public static AlchemyCauldronMenu createClient(int id, Inventory inventory, FriendlyByteBuf data) {
        return new AlchemyCauldronMenu(id, inventory, new SimpleContainer(STATION_SLOTS),
                ContainerLevelAccess.create(inventory.player.level, data.readBlockPos()));
    }

    public AlchemyCauldronMenu(int id, Inventory inventory, Container container, ContainerLevelAccess access) {
        super(Register.ALCHEMY_CAULDRON_MENU.get(), id);
        checkContainerSize(container, STATION_SLOTS);
        this.container = container;
        this.viewer = inventory.player;
        this.access = access;
        this.serverLevel = inventory.player.level instanceof ServerLevel server ? server : null;
        addDataSlots(qiData);
        addDataSlots(chargeData);
        updateQi();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(sectionSlot(container, row * 3 + column, INGREDIENT_X + column * 18, INGREDIENT_Y + row * 18, 1));
            }
        }
        for (int i = 0; i < 3; i++) addSlot(sectionSlot(container, SLOT_COUNT + i, 164 + i * 40, 30, 1));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(sectionSlot(inventory, 9 + row * 9 + column, INVENTORY_X + column * 18, INVENTORY_Y + row * 18, 2));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(sectionSlot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y, 2));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player) && stillValid(access, player, BlockRegister.ALCHEMY_CAULDRON.get());
    }

    private void updateQi() {
        if (serverLevel == null) return;
        if (container instanceof AlchemyCauldronBlockEntity cauldron) {
            chargeData.set(0, cauldron.getStoredQi() & 0xFFFF);
            chargeData.set(1, cauldron.getStoredQi() >>> 16);
            chargeData.set(2, cauldron.isReceivingQi() ? 1 : 0);
        }
        int[] totals = AlchemyQi.totals(container, serverLevel);
        for (int i = 0; i < totals.length; i++) {
            qiData.set(i * 2, totals[i] & 0xFFFF);
            qiData.set(i * 2 + 1, totals[i] >>> 16);
        }
    }

    @Override
    public void broadcastChanges() {
        updateQi();
        super.broadcastChanges();
        updatePreview();
    }

    private void updatePreview() {
        if (!(viewer instanceof ServerPlayer player) || serverLevel == null) return;
        var batch = AlchemyBatch.inspect(container, serverLevel);
        ItemStack item = ItemStack.EMPTY;
        int qi = 0, low = -1, high = -1;
        boolean valid = false;
        if (getPreviewSlot() >= 0 && batch != null
                && PlayerKnowledge.knowsRecipe(player, batch.definition())) {
            item = PillStacks.create(serverLevel, batch.definition(), 0, null);
            // Neither exact purity nor a randomly chosen affinity belongs in a preview.
            item.getTag().remove("Purity");
            item.getTag().remove("Affinity");
            qi = batch.definition().qi();
            valid = batch.valid();
            if (valid && PlayerKnowledge.knows(player,
                    PlayerKnowledge.PURITY_KNOWLEDGE)) {
                low = Math.min(90, batch.purity() / 10 * 10);
                high = low + 10;
            }
        }
        var next = new AlchemyPreviewPacket(containerId, item, qi, low, high, valid);
        if (!next.sameAs(lastPreview)) {
            lastPreview = next;
            PacketHandler.channel.send(PacketDistributor.PLAYER.with(() -> player), next);
        }
    }

    public int getElementalQi(int element) {
        return (qiData.get(element * 2) & 0xFFFF) | ((qiData.get(element * 2 + 1) & 0xFFFF) << 16);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < STATION_SLOTS) {
            if (!moveItemStackTo(stack, STATION_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
