package DaoOfModding.Cultivationcraft.Client.Tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record PlantBadgeTooltipData(int speciesId, boolean host, int dynTier) implements TooltipComponent {}
