package DaoOfModding.Cultivationcraft.Client.Renderers;

import DaoOfModding.Cultivationcraft.Client.Particles.BloodParticleData;
import DaoOfModding.mlmanimator.Client.Poses.PoseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class BloodRenderer
{
    protected static final int maxBloodSpawn = 200;
    protected static final double maxSpeed = 1;

    public static void spawnBlood(Player player, Vec3 source, double amount)
    {
        double percent = amount / player.getMaxHealth();
        int toSpawn =  (int)(percent * maxBloodSpawn);

        for (int i = 0; i <toSpawn; i++)
        {
            Vec3 direction = getBloodDirection(player, source);
            double speed = (percent * maxSpeed) * (Math.random() * 0.5 + 0.5);
            direction = direction.scale(speed);

            BloodParticleData particle = new BloodParticleData(player);

            float height = PoseHandler.getPlayerPoseHandler(player.getUUID()).getPlayerModel().getHeightAdjustment();

            Minecraft.getInstance().level.addParticle(particle, player.getX(), player.getY() + height, player.getZ(), direction.x, direction.y, direction.z);
        }
    }

    protected static Vec3 getBloodDirection(Player player, Vec3 source)
    {
        Vec3 direction;

        if (source != null)
        {
            double x = player.getX() - source.x();
            double z = player.getZ() - source.z();

            direction = new Vec3(x, 0 , z);
        }
        else
            direction = new Vec3(Math.random()* 2 - 1, 0, Math.random() * 2 - 1);

        double rand = Math.random() * 0.3 - 0.15;

        direction = direction.normalize();
        return direction.add(rand, 0, -rand);
    }
}