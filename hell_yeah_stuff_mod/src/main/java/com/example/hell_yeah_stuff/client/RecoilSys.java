package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Отдача камеры при выстреле — порт RecoilSys из мода bren (anothergunmod):
 * резкий кик тангажа вверх со случайным сносом по рысканью,
 * затухающий за пару тиков. Плавность per-frame, как в оригинале.
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID, value = Dist.CLIENT)
public final class RecoilSys {

    private static float cameraRecoil = 0.0F;
    private static float sideRecoil = 0.0F;
    private static int cameraRecoilProgress = 0;
    private static int lastCameraRecoilProgress = 0;

    /** Вызывается на клиенте в момент выстрела. */
    public static void shotEvent(Player player, float camRecoil) {
        cameraRecoil = camRecoil;
        // Случайный увод в сторону, как в bren: (rand - 0.5) / 2.
        sideRecoil = (player.getRandom().nextFloat() - 0.5F) / 2.0F;
        cameraRecoilProgress = 2;
    }

    @SubscribeEvent
    static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || (cameraRecoilProgress <= 0 && lastCameraRecoilProgress <= 0)) {
            return;
        }

        float partial = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float progress = Mth.lerp(partial, lastCameraRecoilProgress, cameraRecoilProgress);
        float frameDelta = mc.getTimer().getRealtimeDeltaTicks();

        float pitch = player.getXRot();
        float yaw = player.getYRot();
        float recoil = progress * cameraRecoil * frameDelta;

        player.setXRot(pitch - (Float.isNaN(recoil) ? 0.0F : recoil));
        player.setYRot(yaw - (Float.isNaN(recoil * sideRecoil) ? 0.0F : recoil * sideRecoil));
        player.xRotO = pitch;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        lastCameraRecoilProgress = cameraRecoilProgress;
        cameraRecoilProgress = Math.max(0, cameraRecoilProgress - 1);
    }

    private RecoilSys() {}
}
