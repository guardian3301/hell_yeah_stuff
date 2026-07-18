package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Бинд перезарядки пистолета (по умолчанию R).
 *
 * Пока R удержана и в руке НЕзаряженный Contender с патронами
 * в инвентаре — виртуально «зажимается» клавиша использования:
 * перезарядка идёт тем же ванильным путём, что и удержание ПКМ
 * (startUsingItem -> releaseUsing), со всеми кадрами анимации
 * и звуками. Отпустил R раньше — перезарядка отменяется.
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID, value = Dist.CLIENT)
public final class ModKeyMappings {

    public static final KeyMapping RELOAD_KEY = new KeyMapping(
            "key.hell_yeah_stuff.reload",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.hell_yeah_stuff");

    /** Рывок в поножах с зачарованием «Рывок» (по умолчанию V). */
    public static final KeyMapping DASH_KEY = new KeyMapping(
            "key.hell_yeah_stuff.dash",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.hell_yeah_stuff");

    /** true, если клавишу использования зажали мы, а не игрок. */
    private static boolean useForced = false;

    @EventBusSubscriber(modid = HellYeahStuffMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(RELOAD_KEY);
            event.register(DASH_KEY);
        }

        private Registration() {}
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        boolean shouldForce = false;
        if (player != null && mc.screen == null && RELOAD_KEY.isDown()) {
            ItemStack held = player.getMainHandItem();
            if (held.is(ModItems.CONTENDER_PISTOL.get())
                    && !player.getCooldowns().isOnCooldown(held.getItem())) {
                ChargedProjectiles loaded = held.get(DataComponents.CHARGED_PROJECTILES);
                boolean isLoaded = loaded != null && !loaded.isEmpty();
                shouldForce = !isLoaded && hasAmmo(player);
            }
        }

        if (shouldForce) {
            // Виртуально держим «использовать» — ваниль сама начнёт
            // и продолжит перезарядку, как при удержании ПКМ.
            mc.options.keyUse.setDown(true);
            useForced = true;
        } else if (useForced) {
            // Возвращаем клавише реальное физическое состояние,
            // чтобы не сломать обычный ПКМ.
            useForced = false;
            mc.options.keyUse.setDown(isPhysicallyDown(mc, mc.options.keyUse));
        }
    }

    /** Физически ли нажата клавиша/кнопка, привязанная к маппингу. */
    private static boolean isPhysicallyDown(Minecraft mc, KeyMapping mapping) {
        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    /** Есть ли патрон .44 в инвентаре (клиентская проверка для бинда). */
    private static boolean hasAmmo(LocalPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.CARTRIDGE.get())) {
                return true;
            }
        }
        return false;
    }

    private ModKeyMappings() {}
}
