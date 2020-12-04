package DaoOfModding.Cultivationcraft.Client.AnimationFramework;

import DaoOfModding.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

import java.util.*;

public class MultiLimbedModel
{
    private float sizeScale = 1;
    private double defaultHeight = 1.5;

    PlayerModel baseModel;

    HashMap<String, ModelRenderer> limbs = new HashMap<String, ModelRenderer>();
    ArrayList<String> toRender = new ArrayList<String>();

    public MultiLimbedModel(PlayerModel model)
    {
        // TODO: Setup so armor displays on player
        baseModel = model;

        ExtendableModelRenderer rightArm = new ExtendableModelRenderer(model, 40, 16);
        baseModel.bipedRightArm = rightArm;

        ExtendableModelRenderer leftArm = new ExtendableModelRenderer(model, 32, 48);
        baseModel.bipedLeftArm = leftArm;
        leftArm.mirror = true;

        ExtendableModelRenderer rightLeg = new ExtendableModelRenderer(model, 0, 16);
        baseModel.bipedRightLeg = rightLeg;

        ExtendableModelRenderer leftLeg = new ExtendableModelRenderer(model, 0, 16);
        baseModel.bipedLeftLeg = leftLeg;
        leftLeg.mirror = true;

        addLimb("LEFTARM", baseModel.bipedLeftArm);
        addLimb("RIGHTARM", baseModel.bipedRightArm);
        addLimb("LEFTLEG", baseModel.bipedLeftLeg);
        addLimb("RIGHTLEG", baseModel.bipedRightLeg);

        rightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        addNonRenderingLimb("LOWERRIGHTARM", rightArm.extend(2, new Vector3d(0, 1, 0), new Vector3d(-3, -2, -2), new Vector3d(4, 12, 4), new Vector3d(1, 1, 0), 0));

        leftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        addNonRenderingLimb("LOWERLEFTARM", leftArm.extend(2, new Vector3d(0, 1, 0), new Vector3d(-1, -2, -2), new Vector3d(4, 12, 4), new Vector3d(-1, 1, 0), 0));

        rightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);
        addNonRenderingLimb("LOWERRIGHTLEG", rightLeg.extend(2, new Vector3d(0, 1, 0), new Vector3d(-2, 0, -2), new Vector3d(4, 12, 4), new Vector3d(0, 1, 1), 0));

        leftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
        addNonRenderingLimb("LOWERLEFTLEG", leftLeg.extend(2, new Vector3d(0, 1, 0), new Vector3d(-2, 0, -2), new Vector3d(4, 12, 4), new Vector3d(0, 1, 1), 0));
    }

    // Returns a list of all limbs on this model
    public Set<String> getLimbs()
    {
        return limbs.keySet();
    }

    // Apply the supplied rotations to the specified limb
    public void rotateLimb(String limb, Vector3d angles)
    {
        if (hasLimb(limb))
        {
            ModelRenderer limbModel = getLimb(limb);

            limbModel.rotateAngleX = (float) angles.x;
            limbModel.rotateAngleY = (float) angles.y;
            limbModel.rotateAngleZ = (float) angles.z;
        }
    }

    // Returns true if this model contains the specified limb
    public boolean hasLimb(String limb)
    {
        return limbs.containsKey(limb);
    }

    public ModelRenderer getLimb(String limb)
    {
        return limbs.get(limb);
    }

    public void addLimb(String limb, ModelRenderer limbModel)
    {
        toRender.add(limb);
        limbs.put(limb, limbModel);
    }

    // Add a limb for reference purposes, don't render it
    // Usually used for referencing child limbs
    public void addNonRenderingLimb(String limb, ModelRenderer limbModel)
    {
        limbs.put(limb, limbModel);
    }

    public void setLivingAnimations(PlayerEntity entityIn, float limbSwing, float limbSwingAmount, float partialTick)
    {
        baseModel.setLivingAnimations(entityIn, limbSwing, limbSwingAmount, partialTick);
    }

    public void setRotationAngles(PlayerEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        baseModel.setRotationAngles(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    public RenderType getRenderType(ResourceLocation resourcelocation)
    {
        return baseModel.getRenderType(resourcelocation);
    }

    public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha)
    {
        matrixStackIn.push();

        // Scale the model to match the scale size, and move it up or down so it's standing at the right height
        matrixStackIn.translate(0.0D, (1-sizeScale) * defaultHeight, 0.0D);
        matrixStackIn.scale(sizeScale, sizeScale, sizeScale);

        baseModel.bipedHead.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
        baseModel.bipedBody.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);

        for (Map.Entry<String, ModelRenderer> limb: limbs.entrySet())
            if (toRender.contains(limb.getKey()))
                limb.getValue().render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);

        baseModel.bipedHeadwear.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);

        matrixStackIn.pop();
    }


    public double getHeightAdjustment()
    {
        double MaxAdjustment = defaultHeight / 2.3 * sizeScale;

        ModelRenderer LeftLeg = getLimb("LEFTLEG");
        ModelRenderer RightLeg = getLimb("RIGHTLEG");

        // Get the largest angle change for both legs
        float largestLeft = Math.abs(LeftLeg.rotateAngleX);
        /*if (largestLeft < Math.abs(LeftLeg.rotateAngleZ))
            largestLeft = Math.abs(LeftLeg.rotateAngleZ);*/

        float largestRight = Math.abs(RightLeg.rotateAngleX);
        /*if (largestRight < Math.abs(RightLeg.rotateAngleZ))
            largestRight = Math.abs(RightLeg.rotateAngleZ);*/

        // Determine which leg has the smallest angle change
        float smallest = largestLeft;

        if (smallest > largestRight)
            smallest = largestRight;

        if (smallest >= Math.toRadians(90))
            return MaxAdjustment;

        return Math.pow(smallest / Math.toRadians(90), 2) * MaxAdjustment;
    }
}
