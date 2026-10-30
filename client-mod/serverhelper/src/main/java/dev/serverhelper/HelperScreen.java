package dev.serverhelper;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

public class HelperScreen extends Screen {
    private static final String[] NAMES = {"Трапка", "Пласт", "Зелье киллера", "Зелье медика"};

    private int waiting = -1;
    private final ButtonWidget[] bindButtons = new ButtonWidget[4];
    private ButtonWidget killerDownBtn;
    private ButtonWidget medicDownBtn;

    public HelperScreen() {
        super(new LiteralText("Server Helper"));
    }

    private static int getKey(int i) {
        Config c = Config.INSTANCE;
        switch (i) {
            case 0:
                return c.trapKey;
            case 1:
                return c.plastKey;
            case 2:
                return c.killerKey;
            default:
                return c.medicKey;
        }
    }

    private static void setKey(int i, int code) {
        Config c = Config.INSTANCE;
        switch (i) {
            case 0:
                c.trapKey = code;
                break;
            case 1:
                c.plastKey = code;
                break;
            case 2:
                c.killerKey = code;
                break;
            default:
                c.medicKey = code;
                break;
        }
        Config.save();
    }

    private static String keyName(int code) {
        if (code < 0) {
            return "—";
        }
        if (code >= 1000) {
            return "Мышь " + (code - 1000 + 1);
        }
        return InputUtil.fromKeyCode(code, -1).getLocalizedText().getString();
    }

    private String label(int i) {
        if (waiting == i) {
            return NAMES[i] + ": > нажми клавишу <";
        }
        return NAMES[i] + ": " + keyName(getKey(i));
    }

    private void refresh() {
        for (int i = 0; i < 4; i++) {
            bindButtons[i].setMessage(new LiteralText(label(i)));
        }
        killerDownBtn.setMessage(new LiteralText("Киллер: кидать под ноги — " + (Config.INSTANCE.killerDown ? "ДА" : "НЕТ")));
        medicDownBtn.setMessage(new LiteralText("Медик: кидать под ноги — " + (Config.INSTANCE.medicDown ? "ДА" : "НЕТ")));
    }

    @Override
    protected void init() {
        int w = 220;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 80;

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            bindButtons[i] = this.addButton(new ButtonWidget(x, y + i * 24, w, 20, new LiteralText(""), b -> {
                waiting = idx;
                refresh();
            }));
        }

        killerDownBtn = this.addButton(new ButtonWidget(x, y + 4 * 24 + 8, w, 20, new LiteralText(""), b -> {
            Config.INSTANCE.killerDown = !Config.INSTANCE.killerDown;
            Config.save();
            refresh();
        }));
        medicDownBtn = this.addButton(new ButtonWidget(x, y + 5 * 24 + 8, w, 20, new LiteralText(""), b -> {
            Config.INSTANCE.medicDown = !Config.INSTANCE.medicDown;
            Config.save();
            refresh();
        }));

        this.addButton(new ButtonWidget(x, y + 6 * 24 + 16, w, 20, new LiteralText("Закрыть"), b -> this.onClose()));
        refresh();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waiting >= 0) {
            int i = waiting;
            waiting = -1;
            // Esc — убрать бинд
            setKey(i, keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode);
            refresh();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_INSERT) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waiting >= 0) {
            if (button >= 2) {
                int i = waiting;
                waiting = -1;
                setKey(i, 1000 + button);
                refresh();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, "Server Helper", this.width / 2, this.height / 2 - 108, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer,
                "Нажми на кнопку и затем клавишу. Esc — убрать бинд.", this.width / 2, this.height / 2 - 96, 0xAAAAAA);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
