package com.example.hell_yeah_stuff.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Подключает алебарде собственный рендер ({@link HalberdBEWLR}), чтобы
 * текстура в руке и в GUI отличалась.
 */
public class HalberdClientExtensions implements IClientItemExtensions {

    private HalberdBEWLR renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            renderer = new HalberdBEWLR();
        }
        return renderer;
    }
}
