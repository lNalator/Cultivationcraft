package DaoOfModding.Cultivationcraft.Client.Particles.Spit;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

public class SpitParticleType  extends ParticleType<SpitParticleData>
{
    public SpitParticleType()
    {
        super(true, null);
    }

    // What the HELL is this!?
    // I really don't know, it doesn't seem to get called, but it needs to be here...
    public Codec<SpitParticleData> codec()
    {
        return null;
    }
}
