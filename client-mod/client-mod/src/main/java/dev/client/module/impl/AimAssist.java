package dev.client.module.impl;

import dev.client.module.BoolSetting;
import dev.client.module.Module;
import dev.client.module.NumSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssist extends Module {
    private final NumSetting range = add(new NumSetting("Range", 4.5, 1.0, 8.0, 0.1));
    private final NumSetting fov = add(new NumSetting("FOV", 90, 10, 180, 5));
    private final NumSetting smooth = add(new NumSetting("Smoothness", 6, 1, 20, 0.5)); // higher = faster
    private final NumSetting maxSpeed = add(new NumSetting("Max deg/s", 240, 30, 720, 10));
    private final BoolSetting vertical = add(new BoolSetting("Vertical", true));
    private final BoolSetting onlyClick = add(new BoolSetting("Only when attacking", true));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", true));

    public AimAssist() {
        super("AimAssist", Category.COMBAT);
    }

    @Override
    public void onFrame(float dt) {
        if (mc.currentScreen != null || mc.player == null || mc.world == null) return;
        if (onlyClick.value && !mc.options.keyAttack.isPressed()) return;

        Vec3d eye = mc.player.getCameraPosVec(1.0f);
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        float bestDy = 0, bestDp = 0;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity) || e == mc.player || !e.isAlive()) continue;
            if (playersOnly.value && !(e instanceof PlayerEntity)) continue;
            if (mc.player.distanceTo(e) > range.value) continue;

            Vec3d p = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.7, e.getZ());
            double dx = p.x - eye.x, dy = p.y - eye.y, dz = p.z - eye.z;
            float tYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float tPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            float dYaw = MathHelper.wrapDegrees(tYaw - mc.player.yaw);
            float dPitch = tPitch - mc.player.pitch;
            double ang = Math.hypot(dYaw, dPitch);
            if (ang > fov.value / 2.0) continue;
            if (ang < bestScore) {
                bestScore = ang;
                best = e;
                bestDy = dYaw;
                bestDp = dPitch;
            }
        }
        if (best == null) return;

        // exponential smoothing, frame-rate independent, with a speed cap
        float k = 1.0f - (float) Math.exp(-smooth.value * dt);
        float cap = maxSpeed.f() * dt;
        float stepYaw = MathHelper.clamp(bestDy * k, -cap, cap);
        float stepPitch = MathHelper.clamp(bestDp * k, -cap, cap);

        mc.player.yaw += stepYaw;
        if (vertical.value) {
            mc.player.pitch = MathHelper.clamp(mc.player.pitch + stepPitch, -90.0f, 90.0f);
        }
    }
}
