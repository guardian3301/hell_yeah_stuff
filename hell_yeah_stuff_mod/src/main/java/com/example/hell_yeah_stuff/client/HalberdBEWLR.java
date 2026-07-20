package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Рендерит алебарду разными моделями в зависимости от вида:
 * <ul>
 *   <li>в руке (1-е и 3-е лицо) — кастомная 3D-модель {@link #HELD_MODEL}
 *       (models/item/halberd_held.json) с текстурой {@code halberd_1.png};</li>
 *   <li>в GUI/инвентаре и прочих видах — кастомная 3D-модель
 *       {@link #INVENTORY_MODEL} (models/item/halberd_inventory.json)
 *       с текстурой {@code halberd_hand.png}.</li>
 * </ul>
 * Обе модели заданы вручную в Blockbench: собственная геометрия и
 * трансформации (display) лежат прямо в JSON моделей.
 *
 * Главная модель предмета ({@code models/item/halberd.json}) — {@code builtin/entity}
 * без собственных трансформаций (identity), поэтому трансформы не
 * накладываются дважды: их задаёт уже выбранная ниже модель.
 */
public class HalberdBEWLR extends BlockEntityWithoutLevelRenderer {

    /** Кастомная 3D-модель для руки (текстура halberd_1.png). */
    public static final ModelResourceLocation HELD_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "item/halberd_held"));

    /** Кастомная 3D-модель для GUI/инвентаря (текстура halberd_hand.png). */
    public static final ModelResourceLocation INVENTORY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "item/halberd_inventory"));

    public HalberdBEWLR() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean inHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

        boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(inHand ? HELD_MODEL : INVENTORY_MODEL);
        ItemRenderer itemRenderer = mc.getItemRenderer();

        // Внешняя (builtin/entity) модель имеет единичные трансформации, поэтому
        // здесь трансформы модели применяются ровно один раз. Выбранная модель
        // — обычная (не custom renderer), поэтому рекурсии нет.
        itemRenderer.render(stack, displayContext, leftHand, poseStack, buffer,
                packedLight, packedOverlay, model);
    }
}
