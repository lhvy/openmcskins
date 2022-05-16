package net.zatrit.openmcskins.render;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.zatrit.openmcskins.OpenMCSkins;
import net.zatrit.openmcskins.mixin.AbstractClientPlayerEntityAccessor;
import net.zatrit.openmcskins.resolvers.loader.CosmeticsManager;
import net.zatrit.openmcskins.resolvers.loader.TextureLoader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CosmeticsFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    public CosmeticsFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Contract(pure = true)
    private static ModelPart getPartByName(@NotNull PlayerEntityModel<AbstractClientPlayerEntity> model, String name) {
        return switch (name) {
            case "head" -> model.head;
            case "body" -> model.body;
            case "leftArm" -> model.leftArm;
            case "leftLeg" -> model.leftLeg;
            case "rightArm" -> model.rightArm;
            case "rightLeg" -> model.rightLeg;
            default -> null;
        };
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, @NotNull AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!OpenMCSkins.getConfig().cosmetics || !FabricLoader.getInstance().isModLoaded("cem"))
            return;

        String name = entity.getEntityName();
        List<CosmeticsManager.CosmeticsItem> items = CosmeticsManager.COSMETICS.get(name);
        if (CosmeticsManager.COSMETICS.get(name) == null) {
            TextureLoader.resolveCosmetics(((AbstractClientPlayerEntityAccessor) entity).invokeGetPlayerListEntry());
            return;
        }

        items.forEach(item -> {
            if (item != null) {
                VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(item.texture()));
                for (int i = 0; i < item.parts().size(); i++) {
                    ModelPart part = item.parts().get(i);
                    ModelPart attachPart = getPartByName(getContextModel(), item.attaches().get(i));
                    if (attachPart != null)
                        attachPart.rotate(matrices);
                    part.render(matrices, buffer, light, OverlayTexture.DEFAULT_UV);
                }
            }
        });
    }
}
