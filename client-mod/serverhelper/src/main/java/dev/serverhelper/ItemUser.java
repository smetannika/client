package dev.serverhelper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;

import java.util.Locale;

/** Поиск предмета по названию и его использование (из хотбара или из инвентаря). */
public final class ItemUser {
    private ItemUser() {
    }

    public static final String[] TRAP = {"трапк", "trap"};
    public static final String[] PLAST = {"пласт", "plast"};
    public static final String[] KILLER = {"киллер", "killer"};
    public static final String[] MEDIC = {"медик", "medic"};

    private static String clean(String s) {
        return s.replaceAll("§.", "").toLowerCase(Locale.ROOT);
    }

    /** Индекс слота инвентаря 0..35 (0..8 хотбар) или -1. Сначала ищет в хотбаре. */
    public static int find(ClientPlayerEntity p, boolean potion, String[] keys) {
        for (int i = 0; i < 36; i++) {
            ItemStack st = p.inventory.getStack(i);
            if (st.isEmpty()) {
                continue;
            }
            if (potion && !(st.getItem() instanceof PotionItem)) {
                continue;
            }
            String name = clean(st.getName().getString());
            for (String k : keys) {
                if (name.contains(k)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static void useByName(String title, boolean potion, String[] keys, boolean lookDown) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        ClientPlayerInteractionManager im = mc.interactionManager;
        if (p == null || im == null) {
            return;
        }
        int idx = find(p, potion, keys);
        if (idx < 0) {
            p.sendMessage(new LiteralText("§c[Server Helper] Не найден предмет: " + title), true);
            return;
        }
        use(mc, p, im, idx, lookDown);
    }

    private static void use(MinecraftClient mc, ClientPlayerEntity p, ClientPlayerInteractionManager im,
                            int idx, boolean lookDown) {
        int sel = p.inventory.selectedSlot;
        boolean fromHotbar = idx < 9;

        if (fromHotbar) {
            p.inventory.selectedSlot = idx;
        } else {
            // слот инвентаря -> слот хотбара, где сейчас держим руку
            im.clickSlot(0, idx, sel, SlotActionType.SWAP, p);
        }

        float yaw = p.yaw;
        float pitch = p.pitch;
        if (lookDown) {
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(yaw, 90.0F, p.isOnGround()));
        }

        im.interactItem(p, mc.world, Hand.MAIN_HAND);
        p.swingHand(Hand.MAIN_HAND);

        if (lookDown) {
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(yaw, pitch, p.isOnGround()));
        }

        if (fromHotbar) {
            p.inventory.selectedSlot = sel;
        } else {
            im.clickSlot(0, idx, sel, SlotActionType.SWAP, p);
        }
    }
}
