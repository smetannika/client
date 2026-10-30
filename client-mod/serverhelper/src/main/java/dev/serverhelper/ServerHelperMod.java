package dev.serverhelper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class ServerHelperMod implements ClientModInitializer {
    private static boolean insertWas = false;
    private static final boolean[] was = new boolean[4];
    private static final long[] lastUse = new long[4];
    private static final long COOLDOWN_MS = 150;

    @Override
    public void onInitializeClient() {
        Config.load();
        ClientTickEvents.END_CLIENT_TICK.register(ServerHelperMod::tick);
    }

    static boolean isDown(long window, int code) {
        if (code < 0) {
            return false;
        }
        if (code >= 1000) {
            return GLFW.glfwGetMouseButton(window, code - 1000) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }

    private static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        long window = mc.getWindow().getHandle();

        boolean ins = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_INSERT) == GLFW.GLFW_PRESS;
        if (ins && !insertWas && mc.currentScreen == null) {
            mc.openScreen(new HelperScreen());
        }
        insertWas = ins;

        Config c = Config.INSTANCE;
        int[] keys = {c.trapKey, c.plastKey, c.killerKey, c.medicKey};
        boolean canAct = mc.currentScreen == null;
        long now = System.currentTimeMillis();

        for (int i = 0; i < 4; i++) {
            boolean down = isDown(window, keys[i]);
            if (down && !was[i] && canAct && now - lastUse[i] >= COOLDOWN_MS) {
                lastUse[i] = now;
                switch (i) {
                    case 0:
                        ItemUser.useByName("Трапка", false, ItemUser.TRAP, false);
                        break;
                    case 1:
                        ItemUser.useByName("Пласт", false, ItemUser.PLAST, false);
                        break;
                    case 2:
                        ItemUser.useByName("Зелье киллера", true, ItemUser.KILLER, c.killerDown);
                        break;
                    default:
                        ItemUser.useByName("Зелье медика", true, ItemUser.MEDIC, c.medicDown);
                        break;
                }
            }
            was[i] = down;
        }
    }
}
