package DaoOfModding.Cultivationcraft.Client.Animations;

import DaoOfModding.mlmanimator.Client.Models.*;
import net.minecraft.util.math.vector.Vector3d;

import java.util.HashMap;

public class BodyPartModels
{
    private HashMap<String, ExtendableModelRenderer> models = new HashMap<String, ExtendableModelRenderer>();
    private HashMap<String, HashMap<String, ExtendableModelRenderer>> nonRenderingModels = new HashMap<String, HashMap<String, ExtendableModelRenderer>>();

    public BodyPartModels()
    {
        setupHeadModels();
        setupLegModels();
    }

    private void setupHeadModels()
    {
        defaultResizeModule semiHeadResizer = new defaultResizeModule(1, new Vector3d(0, 1, 0), new Vector3d(-4, -8, -4), new Vector3d(8, 5, 8), new Vector3d(0, 1, 1));
        defaultResizeModule jawResizer = new defaultResizeModule(1, new Vector3d(0, 0, 0), new Vector3d(-4, -1, -6), new Vector3d(8, 3, 8), new Vector3d(0, 1, 1));

        // Create the top half of the head
        ExtendableModelRenderer semiHeadPart = new ExtendableModelRenderer(0, 0);
        semiHeadPart.setPos(0.0F, 0.0F, 0.0F);
        semiHeadPart.extend(semiHeadResizer);
        semiHeadPart.setLooking(true);
        semiHeadPart.setFirstPersonRender(false);

        // Create the jaw
        ExtendableModelRenderer jawPart = new ExtendableModelRenderer(0, 5);
        jawPart.setPos(0.0F, -2F, 2F);
        jawPart.extend(jawResizer);
        jawPart.setFirstPersonRender(false);

        // Attach the jaw to the head
        semiHeadPart.addChild(jawPart);

        addModel(BodyPartModelNames.jawModel, semiHeadPart);
        addReference(BodyPartModelNames.jawModel, BodyPartModelNames.jawModelLower, jawPart);

        setupTeethModels();

        // Setup first person jaw models
        defaultResizeModule FPsemiHeadResizer = new defaultResizeModule(1, new Vector3d(0, 1, 0), new Vector3d(-4, -8, -4), new Vector3d(8, 5, 8), new Vector3d(0, 1, 1));
        defaultResizeModule FPjawResizer = new defaultResizeModule(1, new Vector3d(0, 0, 0), new Vector3d(-4, -1, -6), new Vector3d(8, 3, 8), new Vector3d(0, 1, 1));

        // Create the top half of the head
        ExtendableModelRenderer FPsemiHeadPart = new ExtendableModelRenderer(0, 0);
        FPsemiHeadPart.setPos(0.0F, 2.5F, -1F);
        FPsemiHeadPart.extend(FPsemiHeadResizer);
        FPsemiHeadPart.setLooking(true);

        // Create the jaw
        ExtendableModelRenderer FPjawPart = new ExtendableModelRenderer(0, 5);
        FPjawPart.setPos(0.0F, -2F, 2F);
        FPjawPart.extend(FPjawResizer);

        // Attach the jaw to the head
        FPsemiHeadPart.addChild(FPjawPart);

        addModel(BodyPartModelNames.FPjawModel, FPsemiHeadPart);
        addReference(BodyPartModelNames.FPjawModel, BodyPartModelNames.FPjawModelLower, FPjawPart);
    }

    private void setupTeethModels()
    {
        setupFlatTeethModels();
        setupSharpTeethModels();
    }

    private void setupFlatTeethModels()
    {
        // Create row of top teeth
        defaultResizeModule flatToothResizer = QiResizers.getTeethResizer(8, 7.8f, 1, 0.1f);

        ExtendableModelRenderer flatToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        flatToothPart.setPos(-3.9f, -3f, -3.9f);
        flatToothPart.extend(flatToothResizer);

        //Setup side teeth
        flatToothResizer = QiResizers.getSideTeethResizer(6, 5.8f, 1, 0.1f);

        ExtendableModelRenderer flatToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        flatToothSidePart.setPos(0f, 0f, 1.1f);
        flatToothSidePart.extend(flatToothResizer);

        flatToothPart.addChild(flatToothSidePart);

        flatToothResizer = QiResizers.getSideTeethResizer(6, 5.8f, 1, 0.1f);

        flatToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        flatToothSidePart.setPos(6.8f, 0f, 1.1f);
        flatToothSidePart.extend(flatToothResizer);

        flatToothPart.addChild(flatToothSidePart);
        flatToothPart.setFirstPersonRenderForSelfAndChildren(false);

        addModel(BodyPartModelNames.flatToothModel, flatToothPart);

        // Create row of bottom teeth
        flatToothResizer = QiResizers.getTeethResizer(8, 7.8f, 1, 0.1f);

        flatToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        flatToothPart.setPos(-3.9f, -2, -5.6f);
        flatToothPart.extend(flatToothResizer);
        flatToothPart.setRotationOffset(new Vector3d(Math.toRadians(-20), 0, 0));

        flatToothResizer = QiResizers.getSideTeethResizer(6, 5.8f, 1, 0.1f);

        flatToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        flatToothSidePart.setPos(0f, 0f, 1f);
        flatToothSidePart.extend(flatToothResizer);

        flatToothPart.addChild(flatToothSidePart);

        flatToothResizer = QiResizers.getSideTeethResizer(6, 5.8f, 1, 0.1f);

        flatToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        flatToothSidePart.setPos(6.8f, 0f, 1f);
        flatToothSidePart.extend(flatToothResizer);

        flatToothPart.addChild(flatToothSidePart);
        flatToothPart.setFirstPersonRenderForSelfAndChildren(false);

        addModel(BodyPartModelNames.flatToothLowerModel, flatToothPart);
    }

    private void setupSharpTeethModels()
    {
        // Create row of top teeth
        defaultResizeModule ToothResizer = QiResizers.getTeethResizer(8, 7.8f, 0.5f, 0.1f);
        ExtendableModelRenderer ToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        ToothPart.setPos(-3.9f, -3f, -3.9f);
        ToothPart.extend(ToothResizer);

        ToothResizer = QiResizers.getTeethResizer(8, 7.8f, 0.5f, 0.3f);
        ExtendableModelRenderer LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothPart.setPos(0.1f, 0.5f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        ToothResizer = QiResizers.getTeethResizer(8, 7.8f, 0.5f, 0.5f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothPart.setPos(0.2f, 1f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        ToothResizer = QiResizers.getTeethResizer(8, 7.8f, 0.49f, 0.7f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothPart.setPos(0.3f, 1.5f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        //Setup side teeth
        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.1f);

        ExtendableModelRenderer ToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        ToothSidePart.setPos(0f, 0f, 1.1f);
        ToothSidePart.extend(ToothResizer);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.3f);
        ExtendableModelRenderer LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, 0.5f, 0.1f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.5f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, 1f, 0.2f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.49f, 0.7f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, 1.5f, 0.3f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothPart.addChild(ToothSidePart);


        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.1f);

        ToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        ToothSidePart.setPos(6.8f, 0f, 1.1f);
        ToothSidePart.extend(ToothResizer);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.3f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, 0.5f, 0.1f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.5f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, 1f, 0.2f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.49f, 0.7f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, 1.5f, 0.3f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothPart.addChild(ToothSidePart);
        ToothPart.setFirstPersonRenderForSelfAndChildren(false);


        //ToothPart.setCustomTextureForFamily(new ResourceLocation(Cultivationcraft.MODID, "textures/models/tooth.png"));
        addModel(BodyPartModelNames.sharpToothModel, ToothPart);


        // Create row of bottom teeth
        ToothResizer = QiResizers.getTeethResizer(7, 6.8f, 0.5f, 0.1f);
        ToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        ToothPart.setPos(-3.4f, -1.5f, -5.8f);
        ToothPart.extend(ToothResizer);
        ToothPart.setRotationOffset(new Vector3d(Math.toRadians(-20), 0, 0));

        ToothResizer = QiResizers.getTeethResizer(7, 6.8f, 0.5f, 0.3f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothPart.setPos(0.1f, -0.5f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        ToothResizer = QiResizers.getTeethResizer(7, 6.8f, 0.5f, 0.5f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothPart.setPos(0.2f, -1f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        ToothResizer = QiResizers.getTeethResizer(7, 6.8f, 0.49f, 0.7f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothPart.setPos(0.3f, -1.5f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);


        //Setup side teeth
        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.1f);

        ToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        ToothSidePart.setPos(-0.5f, 0f, 0.6f);
        ToothSidePart.extend(ToothResizer);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.3f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, -0.5f, 0.1f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.5f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, -1f, 0.2f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.49f, 0.7f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, -1.5f, 0.3f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothPart.addChild(ToothSidePart);


        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.1f);

        ToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        ToothSidePart.setPos(6.3f, 0f, 0.6f);
        ToothSidePart.extend(ToothResizer);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.3f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, -0.5f, 0.1f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.5f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, -1f, 0.2f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.49f, 0.7f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0);
        LowerToothSidePart.setPos(0f, -1.5f, 0.3f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothPart.addChild(ToothSidePart);
        ToothPart.setFirstPersonRenderForSelfAndChildren(false);

        addModel(BodyPartModelNames.sharpToothLowerModel, ToothPart);
    }

    private void setupLegModels()
    {
        defaultResizeModule reverseJointResizer = new defaultResizeModule(2, new Vector3d(0, 1, 0), new Vector3d(-1.99, 0, -1.99), new Vector3d(3.98, 10, 3.98), new Vector3d(0, 1, 1));
        defaultResizeModule footResizer = new defaultResizeModule(2, new Vector3d(0, 1, 0), new Vector3d(-1.99, 0, 0.01), new Vector3d(4, 2, 4), new Vector3d(0, 1, -1));

        ExtendableModelRenderer leftReverseJoint = new ExtendableModelRenderer(0, 16);
        leftReverseJoint.setPos(1.9F, 11.0F, 0.0F);
        leftReverseJoint.extend(reverseJointResizer);
        leftReverseJoint.mirror = true;

        ExtendableModelRenderer leftFoot = new ExtendableModelRenderer(0, 26);
        leftFoot.setPos(0F, 5.0F, -4F);
        leftFoot.extend(footResizer);
        leftFoot.mirror = true;

        leftReverseJoint.getChildren().get(0).addChild(leftFoot);

        // Reinitialize the resizers
        reverseJointResizer = new defaultResizeModule(2, new Vector3d(0, 1, 0), new Vector3d(-1.99, 0, -1.99), new Vector3d(3.98, 10, 3.98), new Vector3d(0, 1, 1));
        footResizer = new defaultResizeModule(2, new Vector3d(0, 1, 0), new Vector3d(-1.99, 0, 0.01), new Vector3d(4, 2, 4), new Vector3d(0, 1, -1));

        ExtendableModelRenderer rightReverseJoint = new ExtendableModelRenderer(0, 16);
        rightReverseJoint.setPos(-1.9F, 11.0F, 0.0F);
        rightReverseJoint.extend(reverseJointResizer);

        ExtendableModelRenderer rightFoot = new ExtendableModelRenderer(0, 26);
        rightFoot.setPos(0F, 5.0F, -4F);
        rightFoot.extend(footResizer);

        rightReverseJoint.getChildren().get(0).addChild(rightFoot);

        addModel(BodyPartModelNames.reverseJointLeftLegModel, leftReverseJoint);
        addReference(BodyPartModelNames.reverseJointLeftLegModel, BodyPartModelNames.reverseJointLeftLegLowerModel, leftReverseJoint.getChildren().get(0));
        addReference(BodyPartModelNames.reverseJointLeftLegModel, BodyPartModelNames.reverseJointLeftFootModel, leftFoot);

        addModel(BodyPartModelNames.reverseJointRightLegModel, rightReverseJoint);
        addReference(BodyPartModelNames.reverseJointRightLegModel, BodyPartModelNames.reverseJointRightLegLowerModel, rightReverseJoint.getChildren().get(0));
        addReference(BodyPartModelNames.reverseJointRightLegModel, BodyPartModelNames.reverseJointRightFootModel, rightFoot);
    }

    public void addModel(String ID, ExtendableModelRenderer model)
    {
        models.put(ID, model);
    }

    public void addReference(String ID, String referenceID, ExtendableModelRenderer model)
    {
        if (!nonRenderingModels.containsKey(ID))
            nonRenderingModels.put(ID, new HashMap<String, ExtendableModelRenderer>());

        nonRenderingModels.get(ID).put(referenceID, model);
    }

    public HashMap<String, ExtendableModelRenderer> getReferences(String ID)
    {
        if (!nonRenderingModels.containsKey(ID))
            nonRenderingModels.put(ID, new HashMap<String, ExtendableModelRenderer>());

        return nonRenderingModels.get(ID);
    }

    // Returns the specified model
    public ExtendableModelRenderer getModel(String ID)
    {
        return models.get(ID);
    }
}
