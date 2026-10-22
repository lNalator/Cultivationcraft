package lNalator.Cultivationcraft.Common.Qi;

import lNalator.Cultivationcraft.Common.Qi.Techniques.*;
import lNalator.Cultivationcraft.Common.Qi.Techniques.BodyForgeTechniques.*;
import lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.*;
import lNalator.Cultivationcraft.Common.Qi.Techniques.BodyForgeTechniques.JetTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.*;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.function.Supplier;

public class TechniqueControl {

    // Initialises all techniques
    public static void init() {
        TechniqueControl.addTechnique(MeditateTechnique::new);
        TechniqueControl.addTechnique(DivineSenseTechnique::new);
        TechniqueControl.addTechnique(QiBarrierTechnique::new);
        TechniqueControl.addTechnique(ItemEnhanceTechnique::new);
        TechniqueControl.addTechnique(FlightTechnique::new);
        TechniqueControl.addTechnique(QiEmission::new);
        TechniqueControl.addTechnique(QiTransferTechnique::new);
        TechniqueControl.addTechnique(FlyingSwordFormationTechnique::new);
        TechniqueControl.addTechnique(IceWalkTechnique::new);
        TechniqueControl.addTechnique(AuraTechnique::new);
        TechniqueControl.addTechnique(LeapTechnique::new);
        TechniqueControl.addTechnique(BiteTechnique::new);
        TechniqueControl.addTechnique(RollTechnique::new);
        TechniqueControl.addTechnique(SpreadTechnique::new);
        TechniqueControl.addTechnique(FloatTechnique::new);
        TechniqueControl.addTechnique(DartTechnique::new);
        TechniqueControl.addTechnique(BounceTechnique::new);
        TechniqueControl.addTechnique(JetLegTechnique::new);
        TechniqueControl.addTechnique(JetTechnique::new);
        TechniqueControl.addTechnique(SpitTechnique::new);
        TechniqueControl.addTechnique(BeamTechnique::new);
        TechniqueControl.addTechnique(SpinPetalTechnique::new);
        TechniqueControl.addPassiveTechnique(ExpandingStomachTechnique::new);
        TechniqueControl.addPassiveTechnique(GlideTechnique::new);
    }

    // Factories of all techniques available in the game
    protected static ArrayList<Supplier<Technique>> techniques = new ArrayList<Supplier<Technique>>();

    // Factories of all passive techniques available in the game
    protected static ArrayList<Supplier<PassiveTechnique>> passiveTechniques = new ArrayList<Supplier<PassiveTechnique>>();

    public static void addTechnique(Supplier<Technique> technique) {
        techniques.add(technique);
    }

    public static void addPassiveTechnique(Supplier<PassiveTechnique> technique) {
        passiveTechniques.add(technique);
    }

    // Returns a new instance of every passive technique the specified player can use
    public static ArrayList<PassiveTechnique> getPassiveTechniques(Player player) {
        ArrayList<PassiveTechnique> available = new ArrayList<PassiveTechnique>();

        for (Supplier<PassiveTechnique> technique : passiveTechniques) {
            PassiveTechnique passive = technique.get();

            if (passive.isValid(player)) {
                available.add(passive);
            }
        }

        return available;
    }

    // Returns the factories of all the techniques that can be used by the specified player
    public static ArrayList<Supplier<Technique>> getAvailableTechnics(Player player) {
        ArrayList<Supplier<Technique>> available = new ArrayList<Supplier<Technique>>();

        for (Supplier<Technique> technique : techniques) {
            if (technique.get().isValid(player)) {
                available.add(technique);
            }
        }

        return available;
    }

    // Creates the technique saved under this class name (see Technique.writeNBT), or null if it is not registered
    public static Technique create(String className) {
        for (Supplier<Technique> technique : techniques) {
            Technique created = technique.get();

            if (created.getClass().getName().equals(className)) {
                return created;
            }
        }

        return null;
    }
}
