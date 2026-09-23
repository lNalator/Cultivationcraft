package DaoOfModding.Cultivationcraft.Common.Containers;

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
    public static final int INGREDIENT_X = 32;
    public static final int INGREDIENT_Y = 30;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 18;
    public static final int HOTBAR_Y = 76;
    private static final int SLOT_COUNT = AlchemyCauldronBlockEntity.SLOT_COUNT;
    private final Container container;
    // Client rendering only: 0 = all slots, 1 = station, 2 = player inventory.
    private int visibleSection;
    private final ContainerData chargeData = new SimpleContainerData(3);

    public void setVisibleSection(int section) { visibleSection = section; }

    private Slot sectionSlot(Container owner, int index, int x, int y, int section) {
        return new Slot(owner, index, x, y) {
            @Override
            public boolean isActive() { return visibleSection == 0 || visibleSection == section; }
        };
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
        return new AlchemyCauldronMenu(id, inventory, new SimpleContainer(SLOT_COUNT),
                ContainerLevelAccess.create(inventory.player.level, data.readBlockPos()));
    }

    public AlchemyCauldronMenu(int id, Inventory inventory, Container container, ContainerLevelAccess access) {
        super(Register.ALCHEMY_CAULDRON_MENU.get(), id);
        checkContainerSize(container, SLOT_COUNT);
        this.container = container;
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
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
