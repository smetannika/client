package dev.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Small immediate-mode 2D helpers: anti-aliased-looking rounded rects, shadows, colour math, scissor. */
public final class Render2D {
    private static final int SEGMENTS = 10;

    private Render2D() {}

    public static int ca(int color, float alpha) {
        int a = (int) (((color >>> 24) & 255) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Rounded rectangle from (x1,y1) to (x2,y2). */
    public static void rrect(MatrixStack ms, float r, float x1, float y1, float x2, float y2, int color) {
        float w = x2 - x1, h = y2 - y1;
        if (w <= 0 || h <= 0 || ((color >>> 24) & 255) == 0) return;
        r = Math.max(0f, Math.min(r, Math.min(w, h) / 2f));

        float a = ((color >>> 24) & 255) / 255f;
        float cr = ((color >> 16) & 255) / 255f;
        float cg = ((color >> 8) & 255) / 255f;
        float cb = (color & 255) / 255f;

        Matrix4f m = ms.peek().getModel();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.defaultBlendFunc();

        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        b.vertex(m, x1 + w / 2f, y1 + h / 2f, 0f).color(cr, cg, cb, a).next();

        float[][] corners = {
                {x1 + r, y1 + r, 180f},
                {x2 - r, y1 + r, 270f},
                {x2 - r, y2 - r, 0f},
                {x1 + r, y2 - r, 90f}
        };
        float firstX = 0, firstY = 0;
        for (int c = 0; c < 4; c++) {
            for (int i = 0; i <= SEGMENTS; i++) {
                double ang = Math.toRadians(corners[c][2] + 90.0 * i / SEGMENTS);
                float px = corners[c][0] + (float) Math.cos(ang) * r;
                float py = corners[c][1] + (float) Math.sin(ang) * r;
                if (c == 0 && i == 0) { firstX = px; firstY = py; }
                b.vertex(m, px, py, 0f).color(cr, cg, cb, a).next();
            }
        }
        b.vertex(m, firstX, firstY, 0f).color(cr, cg, cb, a).next();

        b.end();
        BufferRenderer.draw(b);

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /** Soft drop shadow made of a few expanding translucent rounded rects. */
    public static void shadow(MatrixStack ms, float r, float x1, float y1, float x2, float y2, float alpha) {
        for (int i = 6; i >= 1; i--) {
            float e = i * 1.6f;
            rrect(ms, r + e, x1 - e, y1 - e + 2, x2 + e, y2 + e + 2, ca(0xFF000000, alpha * 0.045f));
        }
    }

    /** Clip drawing to a GUI-space rectangle. Call endScissor() afterwards. */
    public static void scissor(float x, float y, float w, float h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double sf = mc.getWindow().getScaleFactor();
        int fbH = mc.getWindow().getFramebufferHeight();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * sf), (int) (fbH - (y + h) * sf), Math.max(0, (int) (w * sf)), Math.max(0, (int) (h * sf)));
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
