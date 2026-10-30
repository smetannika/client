package dev.client.gui;

import dev.client.Config;
import dev.client.module.BoolSetting;
import dev.client.module.HudModule;
import dev.client.module.Module;
import dev.client.module.ModuleManager;
import dev.client.module.NumSetting;
import dev.client.module.Setting;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;

/**
 * Rounded, animated ClickGUI. LMB module = toggle, RMB = expand settings, MMB = bind key
 * (then press a key; Backspace/Delete = unbind, Esc = cancel). Drag HUD blocks with LMB.
 * Right Shift closes.
 */
public class ClickGui extends Screen {
    private static final int ACCENT = 0xFF8B5CF6;
    private static final int ACCENT_DARK = 0xFF5B3FB3;
    private static final int PANEL_BG = 0xF0121216;
    private static final int ROW_BG = 0xFF1E1E26;
    private static final int SET_BG = 0xFF17171D;

    private static final int PW = 120, HEAD = 26, ROW = 18, GAP = 10;
    private static final int BOOL_H = 16, NUM_H = 24;

    private static class Hit {
        float x, y, w, h;
        float sx, sw; // slider track bounds
        Module m;
        Setting s;
    }

    private final List<Hit> hits = new ArrayList<>();
    private Module binding;
    private Hit dragging;
    private HudModule dragHud;
    private double dragOffX, dragOffY;

    private final long openedAt = System.nanoTime();
    private long lastNs = openedAt;

    public ClickGui() {
        super(new LiteralText("ClickGUI"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static float settingsHeight(Module m) {
        float h = 4;
        for (Setting s : m.settings) h += s instanceof NumSetting ? NUM_H : BOOL_H;
        return h;
    }

    private static void approach(float[] v, float target, float speed) {
        v[0] += (target - v[0]) * Math.min(1f, speed);
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastNs) / 1_000_000_000f, 0.1f);
        lastNs = now;

        float p = Math.min(1f, (now - openedAt) / 1_000_000_000f / 0.22f);
        float e = 1f - (1f - p) * (1f - p) * (1f - p); // ease-out cubic
        float k = dt * 14f;

        // animation state
        float[] tmp = new float[1];
        for (Module m : ModuleManager.modules) {
            tmp[0] = m.toggleAnim; approach(tmp, m.enabled ? 1f : 0f, k); m.toggleAnim = tmp[0];
            tmp[0] = m.openAnim; approach(tmp, m.open ? 1f : 0f, k); m.openAnim = tmp[0];
            for (Setting s : m.settings) {
                float target = 0f;
                if (s instanceof BoolSetting) target = ((BoolSetting) s).value ? 1f : 0f;
                else if (s instanceof NumSetting) {
                    NumSetting n = (NumSetting) s;
                    target = (float) ((n.value - n.min) / (n.max - n.min));
                }
                tmp[0] = s.anim; approach(tmp, target, k * 1.3f); s.anim = tmp[0];
            }
        }

        hits.clear();
        fill(ms, 0, 0, width, height, Render2D.ca(0xFF000000, 0.45f * e));

        ms.push();
        float cx = width / 2f, cy = height / 2f, sc = 0.92f + 0.08f * e;
        ms.translate(cx, cy, 0);
        ms.scale(sc, sc, 1f);
        ms.translate(-cx, -cy, 0);

        int cats = Module.Category.values().length;
        int total = cats * PW + (cats - 1) * GAP;
        int x = (width - total) / 2;
        int top = 28;

        for (Module.Category cat : Module.Category.values()) {
            // panel height
            float ph = HEAD + 4;
            for (Module m : ModuleManager.modules) {
                if (m.category != cat) continue;
                ph += ROW + 2 + settingsHeight(m) * m.openAnim;
            }
            ph += 2;

            Render2D.shadow(ms, 8, x, top, x + PW, top + ph, e);
            Render2D.rrect(ms, 8, x, top, x + PW, top + ph, Render2D.ca(PANEL_BG, e));
            Render2D.rrect(ms, 1, x + 10, top + HEAD - 3, x + PW - 10, top + HEAD - 2, Render2D.ca(ACCENT, e * 0.8f));
            drawText(ms, cat.name(), x + 10, top + 8, Render2D.ca(ACCENT, 1f), e);

            float y = top + HEAD;
            for (Module m : ModuleManager.modules) {
                if (m.category != cat) continue;

                Hit mh = new Hit();
                mh.x = x + 4; mh.y = y; mh.w = PW - 8; mh.h = ROW; mh.m = m;
                hits.add(mh);

                int bg = Render2D.lerp(ROW_BG, ACCENT_DARK, m.toggleAnim);
                if (inside(mh, mx, my)) bg = Render2D.lerp(bg, 0xFFFFFFFF, 0.08f);
                Render2D.rrect(ms, 5, mh.x, y, mh.x + mh.w, y + ROW, Render2D.ca(bg, e));

                int tc = Render2D.lerp(0xFFA8A8B4, 0xFFFFFFFF, m.toggleAnim);
                drawText(ms, m.name, x + 10, y + 5, tc, e);

                String tag = binding == m ? "[...]" : (m.key > 0 ? "[" + keyName(m.key) + "]" : "");
                if (!tag.isEmpty()) {
                    drawText(ms, tag, x + PW - 10 - textRenderer.getWidth(tag), y + 5, 0xFFB8B8C8, e);
                }
                y += ROW + 2;

                float sh = settingsHeight(m) * m.openAnim;
                if (m.openAnim > 0.01f && !m.settings.isEmpty()) {
                    boolean clip = m.openAnim < 0.99f;
                    if (clip) Render2D.scissor(x + 2, y - 1, PW - 4, sh + 1);

                    Render2D.rrect(ms, 5, x + 4, y - 2, x + PW - 4, y + settingsHeight(m) - 2, Render2D.ca(SET_BG, e));
                    float sy = y + 2;
                    for (Setting s : m.settings) {
                        boolean num = s instanceof NumSetting;
                        float rh = num ? NUM_H : BOOL_H;

                        if (m.openAnim > 0.9f) {
                            Hit sh2 = new Hit();
                            sh2.x = x + 4; sh2.y = sy; sh2.w = PW - 8; sh2.h = rh; sh2.m = m; sh2.s = s;
                            sh2.sx = x + 10; sh2.sw = PW - 20;
                            hits.add(sh2);
                        }

                        if (s instanceof BoolSetting) {
                            drawText(ms, s.name, x + 10, sy + 4, 0xFFC8C8D0, e);
                            float tx = x + PW - 32, ty = sy + 3;
                            Render2D.rrect(ms, 5, tx, ty, tx + 20, ty + 10,
                                    Render2D.ca(Render2D.lerp(0xFF3A3A46, ACCENT, s.anim), e));
                            float kx = tx + 1 + s.anim * 10;
                            Render2D.rrect(ms, 4, kx, ty + 1, kx + 8, ty + 9, Render2D.ca(0xFFFFFFFF, e));
                        } else if (num) {
                            NumSetting n = (NumSetting) s;
                            drawText(ms, s.name, x + 10, sy + 3, 0xFFC8C8D0, e);
                            String val = fmt(n.value);
                            drawText(ms, val, x + PW - 10 - textRenderer.getWidth(val), sy + 3, 0xFFFFFFFF, e);
                            float tx = x + 10, tw = PW - 20, ty = sy + 15;
                            Render2D.rrect(ms, 2, tx, ty, tx + tw, ty + 4, Render2D.ca(0xFF34343E, e));
                            float fw = Math.max(4f, tw * s.anim);
                            Render2D.rrect(ms, 2, tx, ty, tx + fw, ty + 4, Render2D.ca(ACCENT, e));
                            Render2D.rrect(ms, 4, tx + fw - 4, ty - 2, tx + fw + 4, ty + 6, Render2D.ca(0xFFFFFFFF, e));
                        }
                        sy += rh;
                    }
                    if (clip) Render2D.endScissor();
                }
                y += sh;
            }
            x += PW + GAP;
        }
        ms.pop();

        String hint = "LMB toggle   RMB settings   MMB bind   drag HUD blocks   Right Shift close";
        drawText(ms, hint, (width - textRenderer.getWidth(hint)) / 2, height - 14, 0xFFB0B0C0, e);
    }

    private void drawText(MatrixStack ms, String t, float x, float y, int color, float alpha) {
        int c = Render2D.ca(color, alpha);
        if (((c >>> 24) & 255) < 8) return; // vanilla treats very low alpha as opaque
        textRenderer.drawWithShadow(ms, t, x, y, c);
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
        if (button == 0) {
            for (Module m : ModuleManager.modules) {
                if (m instanceof HudModule && m.enabled && ((HudModule) m).contains(mx, my)) {
                    dragHud = (HudModule) m;
                    dragOffX = mx - dragHud.x;
                    dragOffY = my - dragHud.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            slide(dragging, mx);
            return true;
        }
        if (dragHud != null) {
            dragHud.x = (int) Math.max(0, Math.min(width - dragHud.w, mx - dragOffX));
            dragHud.y = (int) Math.max(0, Math.min(height - dragHud.h, my - dragOffY));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        dragHud = null;
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
        double frac = Math.max(0, Math.min(1, (mx - h.sx) / h.sw));
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
