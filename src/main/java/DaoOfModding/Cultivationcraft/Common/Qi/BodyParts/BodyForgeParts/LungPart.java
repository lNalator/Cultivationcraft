package DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.BodyForgeParts;

import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.BodyPartOption;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.Lungs.Lung.Lung;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.Lungs.Lungs;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public class LungPart extends BodyPartOption
{
    Lungs lungType = new Lungs();
    HashMap<ResourceLocation, Lung> lungs = new HashMap<ResourceLocation, Lung>();

    public LungPart(String partID, String position, String subPosition, String displayNamePos)
    {
        super(partID, position, subPosition, displayNamePos);
    }

    public void setLungType(Lungs newLung)
    {
        lungType = newLung;
    }

    public Lungs getLungType()
    {
        return lungType;
    }

    public void setLung(ResourceLocation location, Lung lung)
    {
        lungs.put(location, lung);
    }

    public Lung getLung(ResourceLocation location)
    {
        if (lungs.containsKey(location))
            return lungs.get(location);

        return null;
    }
}