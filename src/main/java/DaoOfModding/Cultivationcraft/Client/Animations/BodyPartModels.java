package DaoOfModding.Cultivationcraft.Client.Animations;

import DaoOfModding.mlmanimator.Client.Models.*;
import DaoOfModding.mlmanimator.Client.Models.Quads.Quad;
import DaoOfModding.mlmanimator.Client.Models.Quads.QuadLinkage;
import DaoOfModding.mlmanimator.Client.Poses.PoseHandler;
import com.mojang.math.Vector4f;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;

public class BodyPartModels
{
    private HashMap<String, ExtendableModelRenderer> models = new HashMap<String, ExtendableModelRenderer>();
    private HashMap<String, HashMap<String, ExtendableModelRenderer>> nonRenderingModels = new HashMap<String, HashMap<String, ExtendableModelRenderer>>();
    private HashMap<String, QuadCollection> quads = new HashMap<String, QuadCollection>();

    private boolean slim = false;

    public BodyPartModels(UUID playerID)
    {
        slim = PoseHandler.getPlayerPoseHandler(playerID).isSlim();

        setupBodyModels();
        setupHeadModels();
        setupLegModels();
        setupArmModels();

        setupBackModels();
    }

    private void setupBodyModels()
    {
        ExtendableModelRenderer body = new ExtendableModelRenderer(16, 16, GenericLimbNames.body);
        body.setPos(0.0F, 0.0F, 0.0F);
        body.setRotationPoint(new Vec3(0.5, 0.5, 0.5));
        body.extend(GenericResizers.getBodyResizer());

        addModel(GenericLimbNames.body, body);


        ExtendableModelRenderer shortbody = new ExtendableModelRenderer(16, 16, BodyPartModelNames.shortBody);
        shortbody.setPos(0.0F, 0.0F, 0.0F);
        shortbody.setRotationPoint(new Vec3(0.5, 0.5, 0.5));
        shortbody.setDefaultResize(new Vec3(1, 0.5, 1));
        shortbody.extend(GenericResizers.getBodyResizer());

        addModel(BodyPartModelNames.shortBody, shortbody);
    }

    private void setupArmModels()
    {
        ExtendableModelRenderer rightArm = new ExtendableModelRenderer(40, 16, GenericLimbNames.rightArm);
        rightArm.setRotationPoint(new Vec3(0.5D, 0.66D, 0.5D));
        rightArm.setPos(0.0F, 0.0F, 0.5F);

        if (slim)
            rightArm.setFixedPosAdjustment(-1.5F, 2F, 0.0F);
        else
            rightArm.setFixedPosAdjustment(-2.0F, 2F, 0.0F);

        if (slim)
            rightArm.extend(GenericResizers.getSlimArmResizer());
        else
            rightArm.extend(GenericResizers.getArmResizer());

        ExtendableModelRenderer leftArm = new ExtendableModelRenderer(32, 48, GenericLimbNames.leftArm);
        leftArm.setRotationPoint(new Vec3(0.5D, 0.66D, 0.5D));
        leftArm.setPos(1.0F, 0.0F, 0.5F);
        if (slim)
            leftArm.setFixedPosAdjustment(1.5F, 2F, 0.0F);
        else
            leftArm.setFixedPosAdjustment(2.0F, 2F, 0.0F);

        if (slim)
            leftArm.extend(GenericResizers.getSlimArmResizer());
        else
            leftArm.extend(GenericResizers.getArmResizer());
        leftArm.mirror = true;

        addModel(GenericLimbNames.leftArm, leftArm);
        addReference(GenericLimbNames.leftArm, GenericLimbNames.lowerLeftArm, leftArm.getChildren().get(0));
        addModel(GenericLimbNames.rightArm, rightArm);
        addReference(GenericLimbNames.rightArm, GenericLimbNames.lowerRightArm, rightArm.getChildren().get(0));


        ExtendableModelRenderer lFlipper = leftArm.clone();
        ExtendableModelRenderer rFlipper = rightArm.clone();
        lFlipper.setDefaultResize(new Vec3(0.1, 1, 1));
        rFlipper.setDefaultResize(new Vec3(0.1, 1, 1));
        lFlipper.getChildren().get(0).setDefaultResize(new Vec3(0.1, 1, 1));
        rFlipper.getChildren().get(0).setDefaultResize(new Vec3(0.1, 1, 1));
        lFlipper.setFixedPosAdjustment(0F, 2F, 0.0F);
        rFlipper.setFixedPosAdjustment(0F, 2F, 0.0F);
        lFlipper.generateCube();
        rFlipper.generateCube();

        addModel(BodyPartModelNames.flipperLeftModel, lFlipper);
        addReference(BodyPartModelNames.flipperLeftModel, BodyPartModelNames.flipperLowerLeftModel, lFlipper.getChildren().get(0));
        addModel(BodyPartModelNames.flipperRightModel, rFlipper);
        addReference(BodyPartModelNames.flipperRightModel, BodyPartModelNames.flipperLowerRightModel, rFlipper.getChildren().get(0));


        ExtendableModelRenderer shortRightArm = new ExtendableModelRenderer(40, 16, BodyPartModelNames.shortArmRightModel);
        shortRightArm.setRotationPoint(new Vec3(0.5D, 0.66D, 0.5D));
        shortRightArm.setPos(0.0F, 0.0F, 0.5F);
        shortRightArm.setDefaultResize(new Vec3(1, 0.5, 1));
        if (slim)
            shortRightArm.setFixedPosAdjustment(-1.5F, 2F, 0.0F);
        else
            shortRightArm.setFixedPosAdjustment(-2.0F, 2F, 0.0F);

        if (slim)
            shortRightArm.extend(new defaultResizeModule(new Vec3(3.0D, 12.0D ,4.0D)));
        else
            shortRightArm.extend(new defaultResizeModule(new Vec3(4.0D, 12.0D ,4.0D)));


        ExtendableModelRenderer shortLeftArm = new ExtendableModelRenderer(32, 48, BodyPartModelNames.shortArmLeftModel);
        shortLeftArm.setRotationPoint(new Vec3(0.5D, 0.66D, 0.5D));
        shortLeftArm.setPos(1.0F, 0.0F, 0.5F);
        shortLeftArm.setDefaultResize(new Vec3(1, 0.5, 1));
        if (slim)
            shortLeftArm.setFixedPosAdjustment(1.5F, 2F, 0.0F);
        else
            shortLeftArm.setFixedPosAdjustment(1.5F, 2F, 0.0F);

        if (slim)
            shortLeftArm.extend(new defaultResizeModule(new Vec3(3.0D, 12.0D ,4.0D)));
        else
            shortLeftArm.extend(new defaultResizeModule(new Vec3(4.0D, 12.0D ,4.0D)));


        shortLeftArm.mirror = true;

        addModel(BodyPartModelNames.shortArmLeftModel, shortLeftArm);
        addModel(BodyPartModelNames.shortArmRightModel, shortRightArm);


        Quad larmConnector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        Quad rarmConnector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        getModel(GenericLimbNames.body).addQuadLinkage(new QuadLinkage(larmConnector, Quad.QuadVertex.TopLeft, new Vec3(1, 1, 0.5)));
        getModel(GenericLimbNames.body).addQuadLinkage(new QuadLinkage(larmConnector, Quad.QuadVertex.TopRight, new Vec3(1, 0, 0.5)));

        getModel(GenericLimbNames.body).addQuadLinkage(new QuadLinkage(rarmConnector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        getModel(GenericLimbNames.body).addQuadLinkage(new QuadLinkage(rarmConnector, Quad.QuadVertex.BottomLeft, new Vec3(0, 1, 0.5)));

        getModel(GenericLimbNames.leftArm).addQuadLinkage(new QuadLinkage(larmConnector, Quad.QuadVertex.BottomLeft, new Vec3(0, 1, 0.5)));
        getModel(GenericLimbNames.leftArm).getChildren().get(0).addQuadLinkage(new QuadLinkage(larmConnector, Quad.QuadVertex.BottomRight, new Vec3(0, 0.5, 0.5)));

        getModel(GenericLimbNames.rightArm).addQuadLinkage(new QuadLinkage(rarmConnector, Quad.QuadVertex.TopRight, new Vec3(0, 1, 0.5)));
        getModel(GenericLimbNames.rightArm).getChildren().get(0).addQuadLinkage(new QuadLinkage(rarmConnector, Quad.QuadVertex.BottomRight, new Vec3(1, 0.5, 0.5)));


        QuadCollection glidingArms = new QuadCollection();
        glidingArms.addQuad(larmConnector);
        glidingArms.addQuad(rarmConnector);

        addQuadCollection(BodyPartModelNames.armglidequad, glidingArms);

        setupArmOptions();
    }

    private void setupArmOptions()
    {
    }

    private void setupHeadModels()
    {
        defaultResizeModule neckResizer = new defaultResizeModule(9, new Vec3(0, -1, 0), new Vec3(0.5, 1, 0.5), new Vec3(8, 8, 8), new Vec3(0.5, -1, 0.5));

        ExtendableModelRenderer neckPart = new ExtendableModelRenderer(0, 7, BodyPartModelNames.longNeckModel);
        neckPart.setRotationPoint(new Vec3(0.5D, 0.0D, 0.5D));
        neckPart.setPos(0.5F, 0.0F, 0.5F);
        neckPart.setDefaultResize(new Vec3(0.5, 1, 0.5));
        neckPart.extend(neckResizer);

        ExtendableModelRenderer neckExplorer = neckPart;

        while (neckExplorer.getChildren().size() > 0)
        {
            neckExplorer = neckExplorer.getChildren().get(0);
            neckExplorer.setDefaultResize(new Vec3(0.5, 1, 0.5));
        }

        addModel(BodyPartModelNames.longNeckModel, neckPart);
        addReference(BodyPartModelNames.longNeckModel, BodyPartModelNames.longNeckModelEnd, neckExplorer);


        defaultResizeModule semiHeadResizer = new defaultResizeModule(new Vec3(8, 5, 8));
        defaultResizeModule jawResizer = new defaultResizeModule(new Vec3(8, 3, 8));

        // Create the top half of the head
        ExtendableModelRenderer semiHeadPart = new ExtendableModelRenderer(0, 0,BodyPartModelNames.jawModel + "1");
        semiHeadPart.setRotationPoint(new Vec3(0.5D, 0.0D, 0.5D));
        semiHeadPart.setPos(0.5F, 0.0F, 0.5F);
        semiHeadPart.setFixedPosAdjustment(0, -3, 0);
        semiHeadPart.extend(semiHeadResizer);
        semiHeadPart.setLooking(true);
        semiHeadPart.setFirstPersonRender(false);

        // Create the jaw
        ExtendableModelRenderer jawPart = new ExtendableModelRenderer(0, 5, BodyPartModelNames.jawModel + "2");
        jawPart.setRotationPoint(new Vec3(0.5D, 1, 0.3));
        jawPart.setPos(0.5F, 1F, 0.7F);
        jawPart.extend(jawResizer);
        jawPart.setFirstPersonRender(false);

        // Attach the jaw to the head
        semiHeadPart.addChild(jawPart);

        addModel(BodyPartModelNames.jawModel, semiHeadPart);
        addReference(BodyPartModelNames.jawModel, BodyPartModelNames.jawModelLower, jawPart);

        setupTeethModels();

        // TODO: Fix first person animations
        // Setup first person jaw models
        semiHeadResizer = new defaultResizeModule(new Vec3(8, 5, 8));
        jawResizer = new defaultResizeModule(new Vec3(8, 3, 8));

        // Create the top half of the head
        ExtendableModelRenderer FPsemiHeadPart = new ExtendableModelRenderer(0, 0, BodyPartModelNames.FPjawModel + "1");
        FPsemiHeadPart.setRotationPoint(new Vec3(0.5D, 0.0D, 0.5D));
        FPsemiHeadPart.setPos(0.5F, 0.0F, 0.5F);
        FPsemiHeadPart.setFixedPosAdjustment(0, -3, 0);
        FPsemiHeadPart.extend(semiHeadResizer);
        FPsemiHeadPart.setLooking(true);

        // Create the jaw
        ExtendableModelRenderer FPjawPart = new ExtendableModelRenderer(0, 5, BodyPartModelNames.FPjawModel + "2");
        FPjawPart.setRotationPoint(new Vec3(0.5D, 1, 0));
        FPjawPart.setPos(0.5F, 1F, 1F);
        FPjawPart.extend(jawResizer);

        // Attach the jaw to the head
        FPsemiHeadPart.addChild(FPjawPart);

        addModel(BodyPartModelNames.FPjawModel, FPsemiHeadPart);
        addReference(BodyPartModelNames.FPjawModel, BodyPartModelNames.FPjawModelLower, FPjawPart);
    }

    private void setupBackModels()
    {
        setupWingModels();
        setupInsectWingModels();
        setupJetModels();
    }

    private void setupJetModels()
    {
        defaultResizeModule jet = new defaultResizeModule(new Vec3(0.1, 0.1, 4));

        ExtendableModelRenderer JetModel = new ExtendableModelRenderer(16, 16, BodyPartModelNames.jetLeft);
        JetModel.setPos(0.25F, 0.25F, 1F);
        JetModel.setRotationPoint(new Vec3(0.5, 0.5f, 1f));
        JetModel.setDefaultResize(new Vec3(20, 5, 1));
        JetModel.extend(jet);

        jet = new defaultResizeModule(new Vec3(0.1, 0.1, 4));

        ExtendableModelRenderer JetModelRight = new ExtendableModelRenderer(16, 16, BodyPartModelNames.jetRight);
        JetModelRight.setPos(1F, 1F, 0F);
        JetModelRight.setRotationPoint(new Vec3(0, 1f, 1f));
        JetModelRight.setDefaultResize(new Vec3(5, 10, 1));
        JetModelRight.extend(jet);

        jet = new defaultResizeModule(new Vec3(0.1, 0.1, 4));

        ExtendableModelRenderer JetModelBottom = new ExtendableModelRenderer(16, 16, BodyPartModelNames.jetLeft + "2");
        JetModelBottom.setPos(1F, 1F, 0F);
        JetModelBottom.setRotationPoint(new Vec3(0, 1f, 1f));
        JetModelBottom.setDefaultResize(new Vec3(20, 5, 1));
        JetModelBottom.extend(jet);

        jet = new defaultResizeModule(new Vec3(0.1, 0.1, 4));

        ExtendableModelRenderer JetModelLeft = new ExtendableModelRenderer(16, 16, BodyPartModelNames.jetRight + "2");
        JetModelLeft.setPos(0F, 0F, 0F);
        JetModelLeft.setRotationPoint(new Vec3(1, 0f, 1f));
        JetModelLeft.setDefaultResize(new Vec3(5, 10, 1));
        JetModelLeft.extend(jet);

        JetModelBottom.addChild(JetModelLeft);
        JetModelRight.addChild(JetModelBottom);
        JetModel.addChild(JetModelRight);

        ExtendableModelRenderer JetRight = JetModel.clone();
        JetRight.setPos(0.75F, 0.25F, 1F);

        ParticleEmitter jetSmoke = new ParticleEmitter(ParticleTypes.SMOKE, BodyPartModelNames.jetLeftSmoke);
        jetSmoke.setInterval(20);
        jetSmoke.setVelocity(new Vec3(0, 0f, 0f));
        jetSmoke.setPos(0.5f, 2, 1);

        ParticleEmitter jetSmokeRight = new ParticleEmitter(ParticleTypes.SMOKE, BodyPartModelNames.jetRightSmoke);
        jetSmokeRight.setInterval(20);
        jetSmokeRight.setVelocity(new Vec3(0, 0f, 0f));
        jetSmokeRight.setPos(0.5f, 2, 1);

        ParticleEmitter jetFlame = new ParticleEmitter(ParticleTypes.FLAME, BodyPartModelNames.jetLeftFlame);
        jetFlame.setInterval(1);
        jetFlame.setPos(0.5f, 2, 1);
        jetFlame.setVelocity(new Vec3(0, 0, -0.4f));
        jetFlame.getModelPart().visible = false;

        ParticleEmitter jetFlameRight = new ParticleEmitter(ParticleTypes.FLAME, BodyPartModelNames.jetRightFlame);
        jetFlameRight.setInterval(1);
        jetFlameRight.setVelocity(new Vec3(0, 0f, -0.4f));
        jetFlameRight.setPos(0.5f, 2, 1);
        jetFlameRight.getModelPart().visible = false;

        JetModel.addChild(jetSmoke);
        JetRight.addChild(jetSmokeRight);
        JetModel.addChild(jetFlame);
        JetRight.addChild(jetFlameRight);

        addModel(BodyPartModelNames.jetLeft, JetModel);
        addModel(BodyPartModelNames.jetRight, JetRight);
        addReference(BodyPartModelNames.jetLeft, BodyPartModelNames.jetLeftSmoke, jetSmoke);
        addReference(BodyPartModelNames.jetRight, BodyPartModelNames.jetRightSmoke, jetSmokeRight);
        addReference(BodyPartModelNames.jetLeft, BodyPartModelNames.jetLeftFlame, jetFlame);
        addReference(BodyPartModelNames.jetRight, BodyPartModelNames.jetRightFlame, jetFlameRight);
    }

    private void setupWingModels()
    {
        defaultResizeModule wingTop = new defaultResizeModule(new Vec3(16, 0.5, 1));
        defaultResizeModule lwingTop = new defaultResizeModule(new Vec3(-16, 0.5, 1));

        ExtendableModelRenderer wingTopModel = new ExtendableModelRenderer(0, 0, BodyPartModelNames.rwingUpperArmModel);
        wingTopModel.setPos(0.5F, 0.25F, 1F);
        wingTopModel.setRotationPoint(new Vec3(1, 0.5f, 0f));
        wingTopModel.extend(wingTop);

        ExtendableModelRenderer lwingTopModel = new ExtendableModelRenderer(0, 0, BodyPartModelNames.lwingUpperArmModel);
        lwingTopModel.setPos(0.5F, 0.25F, 1F);
        lwingTopModel.setRotationPoint(new Vec3(1, 0.5f, 0f));
        lwingTopModel.extend(lwingTop);


        defaultResizeModule wingStrand = new defaultResizeModule(new Vec3(0.5, 16, 1));
        defaultResizeModule lwingStrand = new defaultResizeModule(new Vec3(0.5, 16, 1));

        ExtendableModelRenderer wingStrandModel = new ExtendableModelRenderer(0, 0, BodyPartModelNames.rwingStrand1Model);
        wingStrandModel.setPos(1, 1F, 0.5F);
        wingStrandModel.setRotationPoint(new Vec3(0.5F, 1, 0.5f));
        wingStrandModel.extend(wingStrand);

        ExtendableModelRenderer lwingStrandModel = new ExtendableModelRenderer(0, 0, BodyPartModelNames.lwingStrand1Model);
        lwingStrandModel.setPos(1, 1F, 0.5F);
        lwingStrandModel.setRotationPoint(new Vec3(0.5F, 1, 0.5f));
        lwingStrandModel.extend(lwingStrand);


        ExtendableModelRenderer wingStrandModel2 = wingStrandModel.clone();
        ExtendableModelRenderer wingStrandModel3 = wingStrandModel.clone();
        ExtendableModelRenderer wingStrandModel4 = wingStrandModel.clone();
        ExtendableModelRenderer lwingStrandModel2 = lwingStrandModel.clone();
        ExtendableModelRenderer lwingStrandModel3 = lwingStrandModel.clone();
        ExtendableModelRenderer lwingStrandModel4 = lwingStrandModel.clone();

        wingTopModel.addChild(wingStrandModel);
        wingTopModel.addChild(wingStrandModel2);
        wingTopModel.addChild(wingStrandModel3);
        wingTopModel.addChild(wingStrandModel4);
        lwingTopModel.addChild(lwingStrandModel);
        lwingTopModel.addChild(lwingStrandModel2);
        lwingTopModel.addChild(lwingStrandModel3);
        lwingTopModel.addChild(lwingStrandModel4);


        Quad wing12Connector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        wingStrandModel.addQuadLinkage(new QuadLinkage(wing12Connector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        wingStrandModel.addQuadLinkage(new QuadLinkage(wing12Connector, Quad.QuadVertex.BottomLeft, new Vec3(0, 0.95, 0.5)));
        wingStrandModel2.addQuadLinkage(new QuadLinkage(wing12Connector, Quad.QuadVertex.TopRight, new Vec3(1, 0, 0.5)));
        wingStrandModel2.addQuadLinkage(new QuadLinkage(wing12Connector, Quad.QuadVertex.BottomRight, new Vec3(1, 0.95, 0.5)));

        Quad wing23Connector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        wingStrandModel2.addQuadLinkage(new QuadLinkage(wing23Connector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        wingStrandModel2.addQuadLinkage(new QuadLinkage(wing23Connector, Quad.QuadVertex.BottomLeft, new Vec3(0, 0.95, 0.5)));
        wingStrandModel3.addQuadLinkage(new QuadLinkage(wing23Connector, Quad.QuadVertex.TopRight, new Vec3(1, 0, 0.5)));
        wingStrandModel3.addQuadLinkage(new QuadLinkage(wing23Connector, Quad.QuadVertex.BottomRight, new Vec3(1, 0.95, 0.5)));

        Quad wing34Connector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        wingStrandModel3.addQuadLinkage(new QuadLinkage(wing34Connector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        wingStrandModel3.addQuadLinkage(new QuadLinkage(wing34Connector, Quad.QuadVertex.BottomLeft, new Vec3(0, 0.95, 0.5)));
        wingStrandModel4.addQuadLinkage(new QuadLinkage(wing34Connector, Quad.QuadVertex.TopRight, new Vec3(1, 0, 0.5)));
        wingStrandModel4.addQuadLinkage(new QuadLinkage(wing34Connector, Quad.QuadVertex.BottomRight, new Vec3(1, 0.95, 0.5)));

        Quad wing45Connector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        wingStrandModel4.addQuadLinkage(new QuadLinkage(wing45Connector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        wingStrandModel4.addQuadLinkage(new QuadLinkage(wing45Connector, Quad.QuadVertex.BottomLeft, new Vec3(0, 0.95, 0.5)));
        wingTopModel.addQuadLinkage(new QuadLinkage(wing45Connector, Quad.QuadVertex.TopRight, new Vec3(1, 0, 0.5)));
        wingTopModel.addQuadLinkage(new QuadLinkage(wing45Connector, Quad.QuadVertex.BottomRight, new Vec3(0, 0, 0.5)));


        Quad lwing12Connector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        lwingStrandModel.addQuadLinkage(new QuadLinkage(lwing12Connector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        lwingStrandModel.addQuadLinkage(new QuadLinkage(lwing12Connector, Quad.QuadVertex.TopRight, new Vec3(0, 0.95, 0.5)));
        lwingStrandModel2.addQuadLinkage(new QuadLinkage(lwing12Connector, Quad.QuadVertex.BottomLeft, new Vec3(1, 0, 0.5)));
        lwingStrandModel2.addQuadLinkage(new QuadLinkage(lwing12Connector, Quad.QuadVertex.BottomRight, new Vec3(1, 0.95, 0.5)));

        Quad lwing23Connector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        lwingStrandModel2.addQuadLinkage(new QuadLinkage(lwing23Connector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        lwingStrandModel2.addQuadLinkage(new QuadLinkage(lwing23Connector, Quad.QuadVertex.TopRight, new Vec3(0, 0.95, 0.5)));
        lwingStrandModel3.addQuadLinkage(new QuadLinkage(lwing23Connector, Quad.QuadVertex.BottomLeft, new Vec3(1, 0, 0.5)));
        lwingStrandModel3.addQuadLinkage(new QuadLinkage(lwing23Connector, Quad.QuadVertex.BottomRight, new Vec3(1, 0.95, 0.5)));

        Quad lwing34Connector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        lwingStrandModel3.addQuadLinkage(new QuadLinkage(lwing34Connector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        lwingStrandModel3.addQuadLinkage(new QuadLinkage(lwing34Connector, Quad.QuadVertex.TopRight, new Vec3(0, 0.95, 0.5)));
        lwingStrandModel4.addQuadLinkage(new QuadLinkage(lwing34Connector, Quad.QuadVertex.BottomLeft, new Vec3(1, 0, 0.5)));
        lwingStrandModel4.addQuadLinkage(new QuadLinkage(lwing34Connector, Quad.QuadVertex.BottomRight, new Vec3(1, 0.95, 0.5)));

        Quad lwing45Connector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        lwingStrandModel4.addQuadLinkage(new QuadLinkage(lwing45Connector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        lwingStrandModel4.addQuadLinkage(new QuadLinkage(lwing45Connector, Quad.QuadVertex.TopRight, new Vec3(0, 0.95, 0.5)));
        lwingTopModel.addQuadLinkage(new QuadLinkage(lwing45Connector, Quad.QuadVertex.BottomLeft, new Vec3(1, 0, 0.5)));
        lwingTopModel.addQuadLinkage(new QuadLinkage(lwing45Connector, Quad.QuadVertex.BottomRight, new Vec3(0, 0, 0.5)));


        QuadCollection wingQuads = new QuadCollection();
        wingQuads.addQuad(wing12Connector);
        wingQuads.addQuad(wing23Connector);
        wingQuads.addQuad(wing34Connector);
        wingQuads.addQuad(wing45Connector);
        wingQuads.addQuad(lwing12Connector);
        wingQuads.addQuad(lwing23Connector);
        wingQuads.addQuad(lwing34Connector);
        wingQuads.addQuad(lwing45Connector);


        addModel(BodyPartModelNames.rwingUpperArmModel, wingTopModel);
        addReference(BodyPartModelNames.rwingUpperArmModel, BodyPartModelNames.rwingStrand1Model, wingStrandModel);
        addReference(BodyPartModelNames.rwingUpperArmModel, BodyPartModelNames.rwingStrand2Model, wingStrandModel2);
        addReference(BodyPartModelNames.rwingUpperArmModel, BodyPartModelNames.rwingStrand3Model, wingStrandModel3);
        addReference(BodyPartModelNames.rwingUpperArmModel, BodyPartModelNames.rwingStrand4Model, wingStrandModel4);
        addModel(BodyPartModelNames.lwingUpperArmModel, lwingTopModel);
        addReference(BodyPartModelNames.lwingUpperArmModel, BodyPartModelNames.lwingStrand1Model, lwingStrandModel);
        addReference(BodyPartModelNames.lwingUpperArmModel, BodyPartModelNames.lwingStrand2Model, lwingStrandModel2);
        addReference(BodyPartModelNames.lwingUpperArmModel, BodyPartModelNames.lwingStrand3Model, lwingStrandModel3);
        addReference(BodyPartModelNames.lwingUpperArmModel, BodyPartModelNames.lwingStrand4Model, lwingStrandModel4);
        addQuadCollection(BodyPartModelNames.wingquad, wingQuads);
    }

    private void setupInsectWingModels()
    {
        defaultResizeModule wingTop = new defaultResizeModule(new Vec3(16, 0.25, 0.25));
        defaultResizeModule lwingTop = new defaultResizeModule(new Vec3(-16, 0.25, 0.25));

        ExtendableModelRenderer wingTopModel = new ExtendableModelRenderer(0, 0, BodyPartModelNames.rinsectWing);
        wingTopModel.setPos(0.5F, 0.1F, 1F);
        wingTopModel.setRotationPoint(new Vec3(1, 0.5f, 0f));
        wingTopModel.extend(wingTop);

        ExtendableModelRenderer lwingTopModel = new ExtendableModelRenderer(0, 0, BodyPartModelNames.linsectWing);
        lwingTopModel.setPos(0.5F, 0.1F, 1F);
        lwingTopModel.setRotationPoint(new Vec3(1, 0.5f, 0f));
        lwingTopModel.extend(lwingTop);


        defaultResizeModule innerWingTop = new defaultResizeModule(new Vec3(-14, 0.25, 0.25));
        defaultResizeModule linnerWingTop = new defaultResizeModule(new Vec3(14, 0.25, 0.25));

        ExtendableModelRenderer innerWingModel = new ExtendableModelRenderer(0, 0, BodyPartModelNames.rinsectWingInner);
        innerWingModel.setPos(0, 0F, 0.5F);
        innerWingModel.setRotationPoint(new Vec3(0F, 1, 0.5f));
        innerWingModel.extend(innerWingTop);

        ExtendableModelRenderer linnerWingModel = new ExtendableModelRenderer(0, 0, BodyPartModelNames.linsectWingInner);
        linnerWingModel.setPos(0, 0F, 0.5F);
        linnerWingModel.setRotationPoint(new Vec3(0F, 1, 0.5f));
        linnerWingModel.extend(linnerWingTop);

        wingTopModel.addChild(innerWingModel);
        lwingTopModel.addChild(linnerWingModel);


        Quad lwingConnector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        lwingTopModel.addQuadLinkage(new QuadLinkage(lwingConnector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        lwingTopModel.addQuadLinkage(new QuadLinkage(lwingConnector, Quad.QuadVertex.BottomLeft, new Vec3(1, 0, 0.5)));
        linnerWingModel.addQuadLinkage(new QuadLinkage(lwingConnector, Quad.QuadVertex.TopRight, new Vec3(1, 0, 0.5)));
        linnerWingModel.addQuadLinkage(new QuadLinkage(lwingConnector, Quad.QuadVertex.BottomRight, new Vec3(0, 0, 0.5)));
        lwingConnector.setColor(new Vector4f(1, 1, 1, 0.33f));

        Quad rwingConnector = new Quad(new Vec3(0, 0, 0), new Vec3(0, 0, 0),new Vec3(0, 0, 0),new Vec3(0, 0, 0));
        wingTopModel.addQuadLinkage(new QuadLinkage(rwingConnector, Quad.QuadVertex.TopLeft, new Vec3(0, 0, 0.5)));
        wingTopModel.addQuadLinkage(new QuadLinkage(rwingConnector, Quad.QuadVertex.BottomLeft, new Vec3(1, 0, 0.5)));
        innerWingModel.addQuadLinkage(new QuadLinkage(rwingConnector, Quad.QuadVertex.TopRight, new Vec3(1, 0, 0.5)));
        innerWingModel.addQuadLinkage(new QuadLinkage(rwingConnector, Quad.QuadVertex.BottomRight, new Vec3(0, 0, 0.5)));
        rwingConnector.setColor(new Vector4f(1, 1, 1, 0.33f));

        QuadCollection wingQuads = new QuadCollection();
        wingQuads.addQuad(lwingConnector);
        wingQuads.addQuad(rwingConnector);


        addModel(BodyPartModelNames.rinsectWing, wingTopModel);
        addReference(BodyPartModelNames.rinsectWing, BodyPartModelNames.rinsectWingInner, innerWingModel);

        addModel(BodyPartModelNames.linsectWing, lwingTopModel);
        addReference(BodyPartModelNames.linsectWing, BodyPartModelNames.linsectWingInner, linnerWingModel);

        addQuadCollection(BodyPartModelNames.insectQuads, wingQuads);
    }

    private void setupTeethModels()
    {
        setupFlatTeethModels();
        setupSharpTeethModels();
    }

    private void setupFlatTeethModels()
    {
        // Create row of top teeth
        defaultResizeModule flatToothResizer = QiResizers.getTeethResizer(8, 8f, 1, 0.1f);

        ExtendableModelRenderer flatToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.flatToothModel);
        flatToothPart.setPos(0f, 1f, 0f);
        flatToothPart.setFixedPosAdjustment(0.05f, 0, 0.05f);
        flatToothPart.setRotationPoint(new Vec3(1f, 1, 1));
        flatToothPart.extend(flatToothResizer);

        ExtendableModelRenderer lastTooth = flatToothPart;

        while (lastTooth.getChildren().size() > 0)
            lastTooth = lastTooth.getChildren().get(0);


        //Setup side teeth
        flatToothResizer = QiResizers.getSideTeethResizer(6, 5.8f, 1, 0.1f);

        ExtendableModelRenderer flatToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.flatToothModel + "1");
        flatToothSidePart.setPos(1f, 0.5f, 1f);
        flatToothSidePart.setFixedPosAdjustment(0f, 0, 0.1f);
        flatToothSidePart.setRotationPoint(new Vec3(0f, 0.5, 1));
        flatToothSidePart.extend(flatToothResizer);

        flatToothPart.addChild(flatToothSidePart);

        flatToothResizer = QiResizers.getSideTeethResizer(6, 5.8f, 1, 0.1f);

        flatToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.flatToothModel + "2");
        flatToothSidePart.setPos(1f, 0.5f, 1f);
        flatToothSidePart.setFixedPosAdjustment(0f, 0, 0.1f);
        flatToothSidePart.setRotationPoint(new Vec3(0f, 0.5, 1));
        flatToothSidePart.extend(flatToothResizer);

        lastTooth.addChild(flatToothSidePart);


        addModel(BodyPartModelNames.flatToothModel, flatToothPart);

        // Create row of bottom teeth
        flatToothResizer = QiResizers.getTeethResizer(8, 7.8f, 1, 0.1f);

        flatToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.flatToothLowerModel);
        flatToothPart.setPos(0f, 0, 0.3f);
        flatToothPart.setFixedPosAdjustment(0.05f, 0, 0.05f);
        flatToothPart.setRotationPoint(new Vec3(1f, 0, 1));
        flatToothPart.extend(flatToothResizer);
        flatToothPart.setRotationOffset(new Vec3(Math.toRadians(-20), 0, 0));

        lastTooth = flatToothPart;

        while (lastTooth.getChildren().size() > 0)
            lastTooth = lastTooth.getChildren().get(0);

        // Create row of bottom teeth
        flatToothResizer = QiResizers.getSideTeethResizer(6, 5.8f, 1, 0.1f);

        flatToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.flatToothLowerModel + "1");
        flatToothSidePart.setPos(1f, 0.5f, 1f);
        flatToothSidePart.setFixedPosAdjustment(0f, 0, 0.1f);
        flatToothSidePart.setRotationPoint(new Vec3(0f, 0.5, 1));
        flatToothSidePart.extend(flatToothResizer);

        flatToothPart.addChild(flatToothSidePart);

        flatToothResizer = QiResizers.getSideTeethResizer(6, 5.8f, 1, 0.1f);

        flatToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.flatToothLowerModel + "2");
        flatToothSidePart.setPos(1f, 0.5f, 1f);
        flatToothSidePart.setFixedPosAdjustment(0f, 0, 0.1f);
        flatToothSidePart.setRotationPoint(new Vec3(0f, 0.5, 1));
        flatToothSidePart.extend(flatToothResizer);

        lastTooth.addChild(flatToothSidePart);

        flatToothPart.setFirstPersonRenderForSelfAndChildren(false);

        addModel(BodyPartModelNames.flatToothLowerModel, flatToothPart);
    }

    private void setupSharpTeethModels()
    {
        // Create row of top teeth
        defaultResizeModule ToothResizer = QiResizers.getTeethResizer(8, 7.9f, 0.5f, 0.1f);
        ExtendableModelRenderer ToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel);
        ToothPart.setPos(0f, 1f, 0f);
        ToothPart.setFixedPosAdjustment(0.05f, 0, 0.05f);
        ToothPart.setRotationPoint(new Vec3(1f, 1, 1));
        ToothPart.extend(ToothResizer);

        ToothResizer = QiResizers.getTeethResizer(8, 7.9f, 0.5f, 0.3f);
        ExtendableModelRenderer LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "1");
        LowerToothPart.setPos(0.1f, 0.5f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        ToothResizer = QiResizers.getTeethResizer(8, 7.9f, 0.5f, 0.5f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "2");
        LowerToothPart.setPos(0.2f, 1f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        ToothResizer = QiResizers.getTeethResizer(8, 7.9f, 0.49f, 0.7f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "3");
        LowerToothPart.setPos(0.3f, 1.5f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        //Setup side teeth
        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.1f);

        ExtendableModelRenderer ToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "4");
        ToothSidePart.setPos(0f, 0f, 1.1f);
        ToothSidePart.extend(ToothResizer);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.3f);
        ExtendableModelRenderer LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "5");
        LowerToothSidePart.setPos(0f, 0.5f, 0.1f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.5f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "6");
        LowerToothSidePart.setPos(0f, 1f, 0.2f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.49f, 0.7f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "7");
        LowerToothSidePart.setPos(0f, 1.5f, 0.3f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothPart.addChild(ToothSidePart);


        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.1f);

        ToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "8");
        ToothSidePart.setPos(7.8f, 0f, 1.1f);
        ToothSidePart.extend(ToothResizer);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.3f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "9");
        LowerToothSidePart.setPos(0f, 0.5f, 0.1f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.5f, 0.5f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "10");
        LowerToothSidePart.setPos(0f, 1f, 0.2f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(5, 4.8f, 0.49f, 0.7f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothModel + "11");
        LowerToothSidePart.setPos(0f, 1.5f, 0.3f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothPart.addChild(ToothSidePart);
        ToothPart.setFirstPersonRenderForSelfAndChildren(false);


        addModel(BodyPartModelNames.sharpToothModel, ToothPart);


        // Create row of bottom teeth
        ToothResizer = QiResizers.getTeethResizer(7, 7f, 0.5f, 0.1f);
        ToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel);
        ToothPart.setPos(0f, 0f, 0.3f);
        ToothPart.setFixedPosAdjustment(0.55f, 0, 0.1f);
        ToothPart.setRotationPoint(new Vec3(1f, 0, 1));
        ToothPart.extend(ToothResizer);
        ToothPart.setRotationOffset(new Vec3(Math.toRadians(-20), 0, 0));

        ToothResizer = QiResizers.getTeethResizer(7, 7f, 0.5f, 0.3f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "1");
        LowerToothPart.setPos(0.1f, -0.5f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        ToothResizer = QiResizers.getTeethResizer(7, 7f, 0.5f, 0.5f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "2");
        LowerToothPart.setPos(0.2f, -1f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);

        ToothResizer = QiResizers.getTeethResizer(7, 7f, 0.49f, 0.7f);
        LowerToothPart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "3");
        LowerToothPart.setPos(0.3f, -1.5f, 0f);
        LowerToothPart.extend(ToothResizer);

        ToothPart.addChild(LowerToothPart);


        float sideTeethLength = 3.7f;

        //Setup side teeth
        ToothResizer = QiResizers.getSideTeethResizer(4, sideTeethLength, 0.5f, 0.1f);

        ToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "4");
        ToothSidePart.setPos(-0.5f, 0f, 0.4f);
        ToothSidePart.extend(ToothResizer);

        ToothResizer = QiResizers.getSideTeethResizer(4, sideTeethLength, 0.5f, 0.3f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "5");
        LowerToothSidePart.setPos(0f, -0.5f, 0.1f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(4, sideTeethLength, 0.5f, 0.5f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "6");
        LowerToothSidePart.setPos(0f, -1f, 0.2f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(4, sideTeethLength, 0.49f, 0.7f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "7");
        LowerToothSidePart.setPos(0f, -1.5f, 0.3f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothPart.addChild(ToothSidePart);


        ToothResizer = QiResizers.getSideTeethResizer(4, sideTeethLength, 0.5f, 0.1f);

        ToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "8");
        ToothSidePart.setPos(7.2f, 0f, 0.4f);
        ToothSidePart.extend(ToothResizer);

        ToothResizer = QiResizers.getSideTeethResizer(4, sideTeethLength, 0.5f, 0.3f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "9");
        LowerToothSidePart.setPos(0f, -0.5f, 0.1f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(4, sideTeethLength, 0.5f, 0.5f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "10");
        LowerToothSidePart.setPos(0f, -1f, 0.2f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothResizer = QiResizers.getSideTeethResizer(4, sideTeethLength, 0.49f, 0.7f);
        LowerToothSidePart = new ExtendableModelRenderer(64, 64, 0, 0, BodyPartModelNames.sharpToothLowerModel + "11");
        LowerToothSidePart.setPos(0f, -1.5f, 0.3f);
        LowerToothSidePart.extend(ToothResizer);
        ToothSidePart.addChild(LowerToothSidePart);

        ToothPart.addChild(ToothSidePart);
        ToothPart.setFirstPersonRenderForSelfAndChildren(false);

        addModel(BodyPartModelNames.sharpToothLowerModel, ToothPart);
    }

    private void setupLegModels()
    {
        defaultResizeModule reverseJointResizer = new defaultResizeModule(2, new Vec3(0, 1, 0), new Vec3(0.5, 1, 1), new Vec3(3.98, 10, 3.98), new Vec3(0.5, 1, 0));
        defaultResizeModule footResizer = new defaultResizeModule(new Vec3(4, 2, 4));

        ExtendableModelRenderer leftReverseJoint = new ExtendableModelRenderer(0, 16, BodyPartModelNames.reverseJointLeftLegModel);
        leftReverseJoint.setPos(0.75F, 1.0F, 0.5F);
        leftReverseJoint.setRotationPoint(new Vec3(0.5D, 1D, 0.5D));
        leftReverseJoint.setFixedPosAdjustment(0.0F, 0.0F, 0.0F);
        leftReverseJoint.extend(reverseJointResizer);
        leftReverseJoint.mirror = true;

        ExtendableModelRenderer leftFoot = new ExtendableModelRenderer(0, 26, BodyPartModelNames.reverseJointLeftFootModel);
        leftFoot.setPos(0.5F, 1F, 0F);
        leftFoot.setRotationPoint(new Vec3(0.5D, 1D, 1D));
        leftFoot.extend(footResizer);
        leftFoot.mirror = true;

        leftReverseJoint.getChildren().get(0).addChild(leftFoot);

        // Reinitialize the resizers
        reverseJointResizer = new defaultResizeModule(2, new Vec3(0, 1, 0), new Vec3(0.5, 1, 1), new Vec3(3.98, 10, 3.98), new Vec3(0.5, 1, 0));
        footResizer = new defaultResizeModule(new Vec3(4, 2, 4));

        ExtendableModelRenderer rightReverseJoint = new ExtendableModelRenderer(0, 16, BodyPartModelNames.reverseJointRightLegModel);
        rightReverseJoint.setPos(0.25F, 1.0F, 0.5F);
        rightReverseJoint.setRotationPoint(new Vec3(0.5D, 1, 0.5));
        rightReverseJoint.setFixedPosAdjustment(0.0F, 0.0F, 0F);
        rightReverseJoint.extend(reverseJointResizer);

        ExtendableModelRenderer rightFoot = new ExtendableModelRenderer(0, 26, BodyPartModelNames.reverseJointRightFootModel);
        rightFoot.setPos(0.5F, 1F, 0F);
        rightFoot.setRotationPoint(new Vec3(0.5D, 1D, 1D));
        rightFoot.extend(footResizer);

        rightReverseJoint.getChildren().get(0).addChild(rightFoot);

        addModel(BodyPartModelNames.reverseJointLeftLegModel, leftReverseJoint);
        addReference(BodyPartModelNames.reverseJointLeftLegModel, BodyPartModelNames.reverseJointLeftLegLowerModel, leftReverseJoint.getChildren().get(0));
        addReference(BodyPartModelNames.reverseJointLeftLegModel, BodyPartModelNames.reverseJointLeftFootModel, leftFoot);

        addModel(BodyPartModelNames.reverseJointRightLegModel, rightReverseJoint);
        addReference(BodyPartModelNames.reverseJointRightLegModel, BodyPartModelNames.reverseJointRightLegLowerModel, rightReverseJoint.getChildren().get(0));
        addReference(BodyPartModelNames.reverseJointRightLegModel, BodyPartModelNames.reverseJointRightFootModel, rightFoot);


        footResizer = new defaultResizeModule(new Vec3(4, 2, 4));
        ExtendableModelRenderer leftFootModel = new ExtendableModelRenderer(0, 26, BodyPartModelNames.footLeftModel);
        leftFootModel.setPos(0.75F, 1F, 0.5F);
        leftFootModel.setRotationPoint(new Vec3(0.5D, 1D, 0.5D));
        leftFootModel.extend(footResizer);
        leftFootModel.mirror = true;

        footResizer = new defaultResizeModule(new Vec3(4, 2, 4));
        ExtendableModelRenderer rightFootModel = new ExtendableModelRenderer(0, 26, BodyPartModelNames.footRightModel);
        rightFootModel.setPos(0.25F, 1F, 0.5F);
        rightFootModel.setRotationPoint(new Vec3(0.5D, 1D, 0.5D));
        rightFootModel.extend(footResizer);
        rightFootModel.mirror = true;

        addModel(BodyPartModelNames.footLeftModel, leftFootModel);
        addModel(BodyPartModelNames.footRightModel, rightFootModel);


        ExtendableModelRenderer singleLeg = new ExtendableModelRenderer(0, 16, BodyPartModelNames.singleLegModel);
        singleLeg.setPos(0.5F, 1.0F, 0.5F);
        singleLeg.setRotationPoint(new Vec3(0.5, 0.66, 0.5));
        singleLeg.setFixedPosAdjustment(0F, 2F, 0.0F);
        singleLeg.extend(GenericResizers.getLegResizer());

        addModel(BodyPartModelNames.singleLegModel, singleLeg);
        addReference(BodyPartModelNames.singleLegModel, BodyPartModelNames.singleLegLowerModel, singleLeg.getChildren().get(0));


        defaultResizeModule hexaResizer = new defaultResizeModule(2, new Vec3(0, 1, 0), new Vec3(0.5, 1, 0.5), new Vec3(4, 10, 4), new Vec3(0.5, 1, 0.5));

        ExtendableModelRenderer hexaLeft = new ExtendableModelRenderer(0, 16, BodyPartModelNames.hexaLeftLegModel);
        hexaLeft.setPos(1F, 1.0F, 0.5F);
        hexaLeft.setRotationPoint(new Vec3(0.5D, 1D, 0.5D));
        hexaLeft.extend(hexaResizer);
        hexaLeft.mirror = true;
        hexaLeft.setDefaultResize(new Vec3(0.5, 1.5, 0.5));
        hexaLeft.getChildren().get(0).setDefaultResize(new Vec3(0.5, 1.5, 0.5));

        ExtendableModelRenderer hexaLeftTwo = hexaLeft.clone();
        ExtendableModelRenderer hexaLeftThree = hexaLeft.clone();

        hexaResizer = new defaultResizeModule(2, new Vec3(0, 1, 0), new Vec3(0.5, 1, 0.5), new Vec3(4, 10, 4), new Vec3(0.5, 1, 0.5));

        ExtendableModelRenderer hexaRight = new ExtendableModelRenderer(0, 16, BodyPartModelNames.hexaRightLegModel);
        hexaRight.setPos(0F, 1.0F, 0.5F);
        hexaRight.setRotationPoint(new Vec3(0.5D, 1D, 0.5D));
        hexaRight.extend(hexaResizer);
        hexaRight.mirror = true;
        hexaRight.setDefaultResize(new Vec3(0.5, 1.5, 0.5));
        hexaRight.getChildren().get(0).setDefaultResize(new Vec3(0.5, 1.5, 0.5));

        ExtendableModelRenderer hexaRightTwo = hexaRight.clone();
        ExtendableModelRenderer hexaRightThree = hexaRight.clone();

        addModel(BodyPartModelNames.hexaLeftLegModel, hexaLeft);
        addReference(BodyPartModelNames.hexaLeftLegModel, BodyPartModelNames.hexaLowerLeftLegModel, hexaLeft.getChildren().get(0));
        addModel(BodyPartModelNames.hexaLeftLegModelTwo, hexaLeftTwo);
        addReference(BodyPartModelNames.hexaLeftLegModelTwo, BodyPartModelNames.hexaLowerLeftLegModelTwo, hexaLeftTwo.getChildren().get(0));
        addModel(BodyPartModelNames.hexaLeftLegModelThree, hexaLeftThree);
        addReference(BodyPartModelNames.hexaLeftLegModelThree, BodyPartModelNames.hexaLowerLeftLegModelThree, hexaLeftThree.getChildren().get(0));

        addModel(BodyPartModelNames.hexaRightLegModel, hexaRight);
        addReference(BodyPartModelNames.hexaRightLegModel, BodyPartModelNames.hexaLowerRightLegModel, hexaRight.getChildren().get(0));
        addModel(BodyPartModelNames.hexaRightLegModelTwo, hexaRightTwo);
        addReference(BodyPartModelNames.hexaRightLegModelTwo, BodyPartModelNames.hexaLowerRightLegModelTwo, hexaRightTwo.getChildren().get(0));
        addModel(BodyPartModelNames.hexaRightLegModelThree, hexaRightThree);
        addReference(BodyPartModelNames.hexaRightLegModelThree, BodyPartModelNames.hexaLowerRightLegModelThree, hexaRightThree.getChildren().get(0));


        ExtendableModelRenderer jLeftLeg = new ExtendableModelRenderer(0, 16, BodyPartModelNames.jetLegRightModel);
        jLeftLeg.setPos(0.25F, 1.0F, 0.5F);
        jLeftLeg.setRotationPoint(new Vec3(0.5, 0.66, 0.5));
        jLeftLeg.setFixedPosAdjustment(0F, 2F, 0.0F);
        jLeftLeg.extend(GenericResizers.getLegResizer());

        ExtendableModelRenderer jRightLeg = new ExtendableModelRenderer(0, 16, BodyPartModelNames.jetLegLeftModel);
        jRightLeg.setPos(0.75F, 1.0F, 0.5F);
        jRightLeg.setRotationPoint(new Vec3(0.5, 0.66, 0.5));
        jRightLeg.setFixedPosAdjustment(0F, 2F, 0.0F);
        jRightLeg.extend(GenericResizers.getLegResizer());
        jRightLeg.mirror = true;

        defaultResizeModule rightLegJetResizer = new defaultResizeModule(new Vec3(2, 0.5, -0.1));
        ExtendableModelRenderer rightLegJet = new ExtendableModelRenderer(1, 22, BodyPartModelNames.jetLegRightModel + "1");
        rightLegJet.setPos(0.75F, 0F, 1F);
        rightLegJet.setRotationPoint(new Vec3(0, 0, 0));
        rightLegJet.setDefaultResize(new Vec3(1, 1, 20));
        rightLegJet.extend(rightLegJetResizer);

        rightLegJetResizer = new defaultResizeModule(new Vec3(0.5, -4, -0.1));
        ExtendableModelRenderer rightLegJetLeft = new ExtendableModelRenderer(1, 26, BodyPartModelNames.jetLegRightModel + "2");
        rightLegJetLeft.setPos(1F, 0F, 1F);
        rightLegJetLeft.setRotationPoint(new Vec3(0, 0, 0));
        rightLegJetLeft.setDefaultResize(new Vec3(1, 1, 20));
        rightLegJetLeft.extend(rightLegJetResizer);

        rightLegJetResizer = new defaultResizeModule(new Vec3(0.5, -4, -0.1));
        ExtendableModelRenderer rightLegJetRight = new ExtendableModelRenderer(3, 26, BodyPartModelNames.jetLegRightModel + "3");
        rightLegJetRight.setPos(0F, 0F, 1F);
        rightLegJetRight.setRotationPoint(new Vec3(1, 0, 0));
        rightLegJetRight.setDefaultResize(new Vec3(1, 1, 20));
        rightLegJetRight.extend(rightLegJetResizer);

        rightLegJetResizer = new defaultResizeModule(new Vec3(2, -4, -0.1));
        ExtendableModelRenderer rightLegJetFront = new ExtendableModelRenderer(1, 26, BodyPartModelNames.jetLegRightModel + "4");
        rightLegJetFront.setPos(1F, 0F, 0F);
        rightLegJetFront.setRotationPoint(new Vec3(0, 0, 1));
        rightLegJetFront.setDefaultResize(new Vec3(1, 1, 5));
        rightLegJetFront.extend(rightLegJetResizer);

        rightLegJet.addChild(rightLegJetLeft);
        rightLegJet.addChild(rightLegJetRight);
        rightLegJet.addChild(rightLegJetFront);
        jRightLeg.getChildren().get(0).addChild(rightLegJet);


        defaultResizeModule leftLegJetResizer = new defaultResizeModule(new Vec3(2, 0.5, -0.1));
        ExtendableModelRenderer leftLegJet = new ExtendableModelRenderer(1, 22, BodyPartModelNames.jetLegLeftModel + "1");
        leftLegJet.setPos(0.75F, 0F, 1F);
        leftLegJet.setRotationPoint(new Vec3(0, 0, 0));
        leftLegJet.setDefaultResize(new Vec3(1, 1, 20));
        leftLegJet.extend(leftLegJetResizer);

        leftLegJetResizer = new defaultResizeModule(new Vec3(0.5, -4, -0.1));
        ExtendableModelRenderer leftLegJetLeft = new ExtendableModelRenderer(1, 26, BodyPartModelNames.jetLegLeftModel + "2");
        leftLegJetLeft.setPos(1F, 0F, 1F);
        leftLegJetLeft.setRotationPoint(new Vec3(0, 0, 0));
        leftLegJetLeft.setDefaultResize(new Vec3(1, 1, 20));
        leftLegJetLeft.extend(leftLegJetResizer);

        leftLegJetResizer = new defaultResizeModule(new Vec3(0.5, -4, -0.1));
        ExtendableModelRenderer leftLegJetRight = new ExtendableModelRenderer(3, 26, BodyPartModelNames.jetLegLeftModel + "3");
        leftLegJetRight.setPos(0F, 0F, 1F);
        leftLegJetRight.setRotationPoint(new Vec3(1, 0, 0));
        leftLegJetRight.setDefaultResize(new Vec3(1, 1, 20));
        leftLegJetRight.extend(leftLegJetResizer);

        leftLegJetResizer = new defaultResizeModule(new Vec3(2, -4, -0.1));
        ExtendableModelRenderer leftLegJetFront = new ExtendableModelRenderer(1, 26, BodyPartModelNames.jetLegLeftModel + "4");
        leftLegJetFront.setPos(1F, 0F, 0F);
        leftLegJetFront.setRotationPoint(new Vec3(0, 0, 1));
        leftLegJetFront.setDefaultResize(new Vec3(1, 1, 5));
        leftLegJetFront.extend(leftLegJetResizer);

        leftLegJet.addChild(leftLegJetLeft);
        leftLegJet.addChild(leftLegJetRight);
        leftLegJet.addChild(leftLegJetFront);
        jLeftLeg.getChildren().get(0).addChild(leftLegJet);

        ParticleEmitter leftLegEmitter = new ParticleEmitter(ParticleTypes.FLAME, BodyPartModelNames.jetLegLeftEmitter);
        leftLegEmitter.setPos(0.5f, 8, 0.5f);
        leftLegEmitter.setVelocity(new Vec3(0, -0.5f, 0));
        leftLegEmitter.setInterval(1);
        leftLegEmitter.getModelPart().visible = false;

        ParticleEmitter rightLegEmitter = new ParticleEmitter(ParticleTypes.FLAME, BodyPartModelNames.jetLegRightEmitter);
        rightLegEmitter.setPos(0.5f, 8, 0.5f);
        rightLegEmitter.setVelocity(new Vec3(0, -0.5f, 0));
        rightLegEmitter.setInterval(1);
        rightLegEmitter.getModelPart().visible = false;

        leftLegJet.addChild(leftLegEmitter);
        rightLegJet.addChild(rightLegEmitter);

        addModel(BodyPartModelNames.jetLegLeftModel, jLeftLeg);
        addModel(BodyPartModelNames.jetLegRightModel, jRightLeg);
        addReference(BodyPartModelNames.jetLegLeftModel, BodyPartModelNames.jetLegLeftLowerModel, jLeftLeg.getChildren().get(0));
        addReference(BodyPartModelNames.jetLegRightModel, BodyPartModelNames.jetLegRightLowerModel, jRightLeg.getChildren().get(0));
        addReference(BodyPartModelNames.jetLegLeftModel, BodyPartModelNames.jetLegLeftEmitter, leftLegEmitter);
        addReference(BodyPartModelNames.jetLegRightModel, BodyPartModelNames.jetLegRightEmitter, rightLegEmitter);


        ExtendableModelRenderer largeRightLeg = new ExtendableModelRenderer(0, 16, BodyPartModelNames.largeLegRightModel);
        largeRightLeg.setPos(0F, 0.25F, 0.5F);
        largeRightLeg.setRotationPoint(new Vec3(0, 0.75, 0.5));
        largeRightLeg.setDefaultResize(new Vec3(2, 2, 2));
        //largeRightLeg.setFixedPosAdjustment(0F, 2F, 0.0F);
        largeRightLeg.extend(GenericResizers.getLegResizer());
        largeRightLeg.getChildren().get(0).setDefaultResize(new Vec3(2, 2, 2));

        ExtendableModelRenderer largeLeftLeg = new ExtendableModelRenderer(0, 16, BodyPartModelNames.largeLegLeftModel);
        largeLeftLeg.setPos(1F, 0.25F, 0.5F);
        largeLeftLeg.setRotationPoint(new Vec3(1, 0.75, 0.5));
        largeLeftLeg.setDefaultResize(new Vec3(2, 2, 2));
        //largeLeftLeg.setFixedPosAdjustment(0F, 2F, 0.0F);
        largeLeftLeg.extend(GenericResizers.getLegResizer());
        largeLeftLeg.getChildren().get(0).setDefaultResize(new Vec3(2, 2, 2));
        largeLeftLeg.mirror = true;

        addModel(BodyPartModelNames.largeLegLeftModel, largeLeftLeg);
        addModel(BodyPartModelNames.largeLegRightModel, largeRightLeg);
        addReference(BodyPartModelNames.largeLegLeftModel, BodyPartModelNames.largeLegLeftLowerModel, largeLeftLeg.getChildren().get(0));
        addReference(BodyPartModelNames.largeLegRightModel, BodyPartModelNames.largeLegRightLowerModel, largeRightLeg.getChildren().get(0));
    }

    public void addQuadCollection(String ID, QuadCollection quad)
    {
        quads.put(ID, quad);
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

    // Returns the specified quad
    public QuadCollection getQuadCollection(String ID)
    {
        return quads.get(ID);
    }
}
