package lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats;

import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;


public interface ICultivatorStats {

    public int getCultivationType();

    public void setCultivationType(int newType);

    public CultivationType getCultivation();

    public void setCultivation(CultivationType newCultivation);

    public ResourceLocation getTechniqueFocus(Technique tech);

    public void setTechniqueFocus(String tech, ResourceLocation focus);

    public double getConceptProgress(ResourceLocation ID);

    public void setConceptProgress(ResourceLocation ID, double amount);

    public void reset();

    public void setDisconnected(boolean value);

    public boolean isDisconnected();

    public CompoundTag getPillData();

    public CompoundTag getKnowledgeData();

    public CompoundTag writeNBT();

    public void readNBT(CompoundTag nbt);
}
