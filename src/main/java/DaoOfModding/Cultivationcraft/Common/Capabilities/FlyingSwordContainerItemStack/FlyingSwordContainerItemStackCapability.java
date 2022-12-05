package DaoOfModding.Cultivationcraft.Common.Capabilities.FlyingSwordContainerItemStack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FlyingSwordContainerItemStackCapability implements ICapabilityProvider, INBTSerializable<CompoundTag>
{
    public static final Capability<IFlyingSwordContainerItemStack> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});

    private final IFlyingSwordContainerItemStack backend = new FlyingSwordContainerItemStack();
    private final LazyOptional<IFlyingSwordContainerItemStack> optionalData = LazyOptional.of(() -> backend);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return INSTANCE.orEmpty(cap, this.optionalData);
    }

    void invalidate() {
        this.optionalData.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.backend.writeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.backend.readNBT(nbt);
    }

    public static void register(RegisterCapabilitiesEvent event)
    {
        event.register(IFlyingSwordContainerItemStack.class);
    }
}
