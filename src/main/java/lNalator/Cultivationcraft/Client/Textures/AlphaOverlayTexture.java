package lNalator.Cultivationcraft.Client.Textures;

import net.minecraft.util.FastColor;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;

public class AlphaOverlayTexture extends SimpleTexture {

    ResourceLocation alpha;
    float alphaSize = 1f;

    public AlphaOverlayTexture(ResourceLocation textureLocation, ResourceLocation alphaLocation) {
        super(textureLocation);

        alpha = alphaLocation;
    }

    public void setAlphaSize(Float newSize) {
        if (alphaSize != newSize) {
            alphaSize = newSize;

            try {
                this.load(Minecraft.getInstance().getResourceManager());
            } catch (java.io.IOException e) {
                Cultivationcraft.LOGGER.error("Error loading alpha texture: " + e);
            }
        }
    }

    protected SimpleTexture.TextureImage adjustAlpha(ResourceManager resourceManager) throws IOException {
        // Save the nativeImage of the alpha texture here
        NativeImage alphaImage = SimpleTexture.TextureImage.load(resourceManager, this.alpha).getImage();

        SimpleTexture.TextureImage textureImage = SimpleTexture.TextureImage.load(resourceManager, this.location);
        NativeImage image = textureImage.getImage();

        // Center the transparency texture over the image texture
        float adjustedX = ((alphaImage.getWidth() * alphaSize) - image.getWidth()) / 2.0f;
        float adjustedY = ((alphaImage.getWidth() * alphaSize) - image.getHeight()) / 2.0f;

        // Go through each pixel of the base texture and apply the alpha value from the alpha texture to it
        for (int x = 0; x < image.getWidth(); x++) {
            int finalX = x;

            for (int y = 0; y < image.getHeight(); y++) {
                int finalY = y;

                int alpha = 0;
                int alphaX = (int) ((finalX + adjustedX) / alphaSize);
                int alphaY = (int) ((finalY + adjustedY) / alphaSize);

                // Alpha is 0 if the alpha texture is out of bounds
                if (alphaX > -1 && alphaX < alphaImage.getWidth() && alphaY > -1 && alphaY < alphaImage.getHeight()) {
                    alpha = FastColor.ABGR32.alpha(alphaImage.getPixelRGBA(alphaX, alphaY));
                }

                int baseRgba = image.getPixelRGBA(finalX, finalY);

                // Set the alpha value to be the lowest alpha of the two images
                alpha = Math.min(alpha, FastColor.ABGR32.alpha(baseRgba));

                int rgba = FastColor.ABGR32.color(alpha, FastColor.ABGR32.blue(baseRgba), FastColor.ABGR32.green(baseRgba), FastColor.ABGR32.red(baseRgba));

                if (!RenderSystem.isOnRenderThreadOrInit()) {
                    RenderSystem.recordRenderCall(()
                            -> {
                        image.setPixelRGBA(finalX, finalY, rgba);
                    });
                } else {
                    image.setPixelRGBA(finalX, finalY, rgba);
                }
            }
        }

        return textureImage;
    }

    @Override
    protected SimpleTexture.TextureImage getTextureImage(ResourceManager resourceManager) {
        try {
            return adjustAlpha(resourceManager);
        } catch (java.io.IOException e) {
            return SimpleTexture.TextureImage.load(resourceManager, this.location);
        }
    }
}
