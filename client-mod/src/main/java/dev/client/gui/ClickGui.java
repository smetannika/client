package dev.client.gui;

import dev.client.Config;
import dev.client.module.BoolSetting;
import dev.client.module.Module;
import dev.client.module.ModuleManager;
import dev.client.module.NumSetting;
import dev.client.module.Setting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;

/**
 * Left click module = toggle, right click = expand settings, middle click = bind key
 * (then press a key; Backspace/Delete = unbind, Esc = cancel). Right Shift closes.
 */
public class ClickGui extends Screen {
    private static final int PANEL_W = 110, ROW_H = 14, SET_H = 13, GAP = 8;

    private static class Hit {
        int x, y, w, h;
        Module m;
        Setting s;
    }

    private final List<Hit> hits = new ArrayList<>();
    private Module binding;
    private Hit dragging;

    public ClickGui() {
        super(new LiteralText("ClickGUI"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        hits.clear();
        int x = 12;
        for (Module.Category cat : Module.Category.values()) {
            int y = 12;
            fill(ms, x, y, x + PANEL_W, y + ROW_H, 0xFF202020);
            textRenderer.drawWithShadow(ms, cat.name(), x + 4, y + 3, 0xFFFFFFFF);
            y += ROW_H;

            for (Module m : ModuleManager.modules) {
                if (m.category != cat) continue;

                Hit mh = new Hit();
                mh.x = x; mh.y = y; mh.w = PANEL_W; mh.h = ROW_H; mh.m = m;
                hits.add(mh);

                int bg = m.enabled ? 0xFF2E7D32 : 0xCC101010;
                if (inside(mh, mx, my)) bg = m.enabled ? 0xFF388E3C : 0xDD303030;
                fill(ms, x, y, x + PANEL_W, y + ROW_H, bg);
                textRenderer.drawWithShadow(ms, m.name, x + 4, y + 3, 0xFFFFFFFF);

                String tag = binding == m ? "[...]" : (m.key > 0 ? "[" + keyName(m.key) + "]" : "");
                if (!tag.isEmpty()) {
                    textRenderer.drawWithShadow(ms, tag, x + PANEL_W - 4 - textRenderer.getWidth(tag), y + 3, 0xFFBBBBBB);
                }
                y += ROW_H;

                if (m.open) {
                    for (Setting s : m.settings) {
                        Hit sh = new Hit();
                        sh.x = x; sh.y = y; sh.w = PANEL_W; sh.h = SET_H; sh.m = m; sh.s = s;
                        hits.add(sh);
                        fill(ms, x, y, x + PANEL_W, y + SET_H, 0xDD181818);

                        if (s instanceof BoolSetting) {
                            boolean v = ((BoolSetting) s).value;
                            textRenderer.drawWithShadow(ms, s.name, x + 8, y + 2, 0xFFDDDDDD);
                            fill(ms, x + PANEL_W - 14, y + 3, x + PANEL_W - 5, y + SET_H - 3, v ? 0xFF4CAF50 : 0xFF555555);
                        } else if (s instanceof NumSetting) {
                            NumSetting n = (NumSetting) s;
                            double frac = (n.value - n.min) / (n.max - n.min);
                            fill(ms, x, y, x + (int) (PANEL_W * frac), y + SET_H, 0xAA2E7D32);
                            String txt = s.name + ": " + fmt(n.value);
                            textRenderer.drawWithShadow(ms, txt, x + 4, y + 2, 0xFFFFFFFF);
                        }
                        y += SET_H;
                    }
                }
            }
            x += PANEL_W + GAP;
        }

        String hint = "LMB toggle | RMB settings | MMB bind | Right Shift close";
        textRenderer.drawWithShadow(ms, hint, 12, height - 14, 0xFFAAAAAA);
        super.render(ms, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Hit h : hits) {
            if (!inside(h, mx, my)) continue;
            if (h.s == null) {
                if (button == 0) h.m.toggle();
                else if (button == 1) h.m.open = !h.m.open;
                else if (button == 2) binding = h.m;
            } else if (h.s instanceof BoolSetting) {
                if (button == 0) ((BoolSetting) h.s).value = !((BoolSetting) h.s).value;
            } else if (h.s instanceof NumSetting) {
                if (button == 0) {
                    dragging = h;
                    slide(h, mx);
                }
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            slide(dragging, mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode == 259 || keyCode == 261) binding.key = -1; // Backspace / Delete
            else if (keyCode != 256) binding.key = keyCode;          // Esc cancels
            binding = null;
            return true;
        }
        if (keyCode == ModuleManager.OPEN_GUI_KEY) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Config.save();
        super.onClose();
    }

    private void slide(Hit h, double mx) {
        NumSetting n = (NumSetting) h.s;
        double frac = Math.max(0, Math.min(1, (mx - h.x) / h.w));
        n.set(n.min + frac * (n.max - n.min));
    }

    private static boolean inside(Hit h, double mx, double my) {
        return mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }

    private static String keyName(int key) {
        return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
    }
}
