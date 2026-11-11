package lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques;

import lNalator.Cultivationcraft.Common.Qi.TechniqueControl;
import lNalator.Cultivationcraft.Common.Qi.Techniques.DivineSenseTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.PassiveTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

public class CultivatorTechniques implements ICultivatorTechniques {

    public static final int numberOfTechniques = 9;

    protected Technique[] techniques = new Technique[numberOfTechniques];

    protected ArrayList<PassiveTechnique> passives = new ArrayList<PassiveTechnique>();

    // Divine Sense is innate: it never takes a technique slot and is toggled with its own key (see KeybindingControl)
    protected static final String DIVINE_SENSE_KEY = "divineSense";
    protected DivineSenseTechnique divineSense = new DivineSenseTechnique();

    public DivineSenseTechnique getDivineSense() {
        return divineSense;
    }

    public void setDivineSense(DivineSenseTechnique sense) {
        divineSense = sense;
    }

    public Technique getTechnique(int slot) {
        return techniques[slot];
    }

    public void setTechnique(int slot, Technique tech) {
        techniques[slot] = tech;
    }

    public boolean canBreath(Player player) {
        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            Technique tech = getTechnique(i);

            if (tech != null && tech.isValid(player) && tech.isActive() && tech.disableBreathing()) {
                return false;
            }
        }

        for (PassiveTechnique passive : getPassives()) {
            if (passive.isValid(player) && passive.isActive() && passive.disableBreathing()) {
                return false;
            }
        }

        return true;
    }

    public void determinePassives(Player player) {
        passives.clear();
        passives.addAll(TechniqueControl.getPassiveTechniques(player));
    }

    public ArrayList<PassiveTechnique> getPassives() {
        return passives;
    }

    public boolean techniqueExists(Technique exist) {
        for (Technique tech : techniques) {
            if (tech != null && tech.getClass() == exist.getClass()) {
                return true;
            }
        }

        return false;
    }

    public Technique getTechniqueByName(String name) {
        for (int i = 0; i < numberOfTechniques; i++) {
            if (techniques[i] != null) {
                if (techniques[i].isCalled(name)) {
                    return techniques[i];
                }
            }
        }

        for (PassiveTechnique passive : passives) {
            if (passive.isCalled(name)) {
                return passive;
            }
        }

        if (divineSense != null && divineSense.isCalled(name)) {
            return divineSense;
        }

        return null;
    }

    // Return a specified players CultivatorTechniques
    public static ICultivatorTechniques getCultivatorTechniques(Player player) {
        return player.getCapability(CultivatorTechniquesCapability.INSTANCE).orElseThrow(() -> new IllegalArgumentException("getting cultivator techniques"));
    }

    public CompoundTag writeNBT() {
        CompoundTag nbt = new CompoundTag();

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            Technique sending = getTechnique(i);

            if (sending != null) {
                nbt.put(Integer.toString(i), sending.writeNBT());
            }
        }

        nbt.put(DIVINE_SENSE_KEY, divineSense.writeNBT());

        return nbt;
    }

    public void readNBT(CompoundTag nbt) {
        boolean senseSaved = false;

        if (nbt.contains(DIVINE_SENSE_KEY) && Technique.readNBT(nbt.getCompound(DIVINE_SENSE_KEY)) instanceof DivineSenseTechnique sense) {
            divineSense = sense;
            senseSaved = true;
        }

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            if (nbt.contains(Integer.toString(i))) {
                Technique tech = Technique.readNBT(nbt.getCompound(Integer.toString(i)));

                // Saves from before Divine Sense became innate: take it out of its slot, keeping its categories and progress
                if (tech instanceof DivineSenseTechnique sense) {
                    if (!senseSaved) {
                        divineSense = sense;
                        senseSaved = true;
                    }

                    tech = null;
                }

                setTechnique(i, tech);
            }
        }
    }
}
