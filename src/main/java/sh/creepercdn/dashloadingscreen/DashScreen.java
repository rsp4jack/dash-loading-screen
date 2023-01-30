package sh.creepercdn.dashloadingscreen;

// https://github.com/TeamQuantumFusion/DashLoader/blob/fabric-1.18/src/main/java/dev/quantumfusion/dashloader/client/DashScreen.java
// LGPLv3

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.FontType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class DashScreen extends Screen {
    private static final Color FAILED_COLOR = new Color(255, 75, 69);
    private final Random random = new Random();
    private final UIDrawer drawer = new UIDrawer();
    private final List<Line> lines = new ArrayList<>();
    private final List<Pair<Color, Integer>> lineColorSelectors = new ArrayList<>();
    private final String fact = String.format("Minecraft %s", MinecraftClient.getInstance().getGame().getVersion().getName());
    public Status STATUS = Status.IDLE;
    public double progress = 0.0;
    public String task = "Loading";
    public Closeable closable;
    public TextRenderer tr;
    private float weight = 0;
    private boolean debug;
    private int padding;
    private int progressBarHeight = 0;
    private float lineSpeedDifference = 0;
    private boolean configRequiresUpdate = false;
    private long oldTime = System.currentTimeMillis();

    public DashScreen() {
        super(Text.of("Caching"));
        this.drawer.update(MinecraftClient.getInstance());
        this.updateConfig();
    }

    private static void drawVertex(Matrix4f m4f, BufferBuilder bb, int x, int y, Color color) {
        bb.vertex(m4f, (float) x, (float) y, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
    }

    public TextRenderer getTextRenderer() {
        if (tr == null) {
            MinecraftClient minecraft = MinecraftClient.getInstance();
            ResourceManager manager = MinecraftGraphicsUtils.createResourceManager(minecraft.getResourcePackProvider().getPack());
            TextureManager textureManager = minecraft.getTextureManager();
            FontStorage fontSet = new FontStorage(textureManager, new Identifier("default"));
            JsonElement element = JsonParser.parseString("""
                    {"type":"bitmap","file":"minecraft:font/ascii.png","ascent":7,"chars":["\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000","\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000"," !\\"#$%&'()*+,-./","0123456789:;<=>?","@ABCDEFGHIJKLMNO","PQRSTUVWXYZ[\\\\]^_","`abcdefghijklmno","pqrstuvwxyz{|}~\\u0000","\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000","\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000£\\u0000\\u0000ƒ","\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000ªº\\u0000\\u0000¬\\u0000\\u0000\\u0000«»","░▒▓│┤╡╢╖╕╣║╗╝╜╛┐","└┴┬├─┼╞╟╚╔╩╦╠═╬╧","╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀","\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000∅∈\\u0000","≡±≥≤⌠⌡÷≈°∙\\u0000√\\u207f²■\\u0000"]}""");
            fontSet.setFonts(List.of(
                    Objects.requireNonNull(FontType.BITMAP.createLoader(element.getAsJsonObject()).load(manager))
            ));
            tr = new TextRenderer(resourceLocation -> fontSet);
            closable = () -> {
                fontSet.close();
                if (manager instanceof Closeable closeable) {
                    closeable.close();
                }
            };
        }

        return tr;
    }

    private void updateConfig() {
        final ModConfig config = DashLoadingScreenMod.config;
        ColorUtils.loadConfig(config);

        this.padding = config.paddingSize;
        this.debug = config.debug;
        this.progressBarHeight = config.progressBarHeight;
        this.lineSpeedDifference = config.line.lineSpeedDifference;
        int lineAmount = config.line.lineAmount;

        // lines
        this.weight = 0;
        this.lineColorSelectors.clear();
        config.color.lineColors.forEach((s, integer) -> {
            this.weight += integer;
            this.lineColorSelectors.add(Pair.of(ColorUtils.parseColor(s), integer));
        });

        UIDrawer.GradientOrientation lineOrientation = null;

        final ModConfig.Direction lineDirection = config.line.lineDirection;
        switch (lineDirection) {
            case UP -> lineOrientation = UIDrawer.GradientOrientation.GRADIENT_TOP;
            case LEFT -> lineOrientation = UIDrawer.GradientOrientation.GRADIENT_LEFT;
            case RIGHT -> lineOrientation = UIDrawer.GradientOrientation.GRADIENT_RIGHT;
            case DOWN -> lineOrientation = UIDrawer.GradientOrientation.GRADIENT_DOWN;
        }

        if (lineAmount > this.lines.size()) {
            for (int i = 0; i < (lineAmount - this.lines.size()); i++) {
                this.lines.add(new Line());
            }
        } else if (lineAmount < this.lines.size()) {
            final int toRemove = this.lines.size() - lineAmount;
            if (toRemove > 0) {
                this.lines.subList(0, toRemove).clear();
            }
        }

        for (Line line : this.lines) {
            line.speed = config.line.lineSpeed;
            line.width = config.line.lineWidth;
            final int bound = config.line.lineMaxHeight - config.line.lineMinHeight;
            line.height = config.line.lineMinHeight + (bound > 0 ? this.random.nextInt(bound) : 0);
            line.orientation = lineOrientation;
            line.updateStep();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    public Color getLineColor() {
        float target = this.random.nextFloat() * this.weight;
        float countWeight = 0.0f;
        for (Pair<Color, Integer> item : this.lineColorSelectors) {
            countWeight += item.getValue();
            if (countWeight >= target) {
                return item.getKey();
            }
        }
        throw new RuntimeException("Could not get line color.");
    }

    @Override
    protected void init() {

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER && STATUS == Status.CRASHED) {
            STATUS = Status.DONE;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if ((STATUS == Status.DONE) && !this.debug) {
            // MinecraftClient.getInstance().setScreen(this.previousScreen);
            STATUS = Status.IDLE;
        }

        if (this.configRequiresUpdate) {
            this.updateConfig();
            this.configRequiresUpdate = false;
        }

        this.drawer.push(matrices, getTextRenderer());
        final int width = this.drawer.getWidth();
        final int height = this.drawer.getHeight();
        final int barY = height - this.padding - this.progressBarHeight;

        double currentProgress;
        Color currentProgressColor;
        if (this.debug) {
            currentProgress = Math.max(Math.min(1, mouseX / (double) width), 0);
            currentProgressColor = ColorUtils.getProgressColor(currentProgress);
        } else if (STATUS == Status.CRASHED) {
            task = "Internal crash. Please check logs or press ENTER.";
            currentProgress = 1.0;
            currentProgressColor = FAILED_COLOR;
        } else {
            currentProgress = progress;
            currentProgressColor = ColorUtils.getProgressColor(currentProgress);
        }

        this.drawer.drawQuad(ColorUtils.BACKGROUND_COLOR, 0, 0, width, height);

        while (this.oldTime < System.currentTimeMillis()) {
            for (Line line : this.lines) {
                line.tick();
            }
            // about 60fps
            this.oldTime += 17;
        }

        this.drawLines(this.lines, matrices);
        this.drawer.drawQuad(ColorUtils.PROGRESS_LANE_COLOR, 0, barY, width, this.progressBarHeight); // progress back
        this.drawer.drawQuad(currentProgressColor, 0, barY, (int) (width * currentProgress), this.progressBarHeight); // the progress bar
        this.drawer.drawText(UIDrawer.TextOrientation.TEXT_LEFT, task, ColorUtils.TEXT_COLOR, this.padding, barY - this.padding); // current task

        // fun fact
        this.drawer.drawText(UIDrawer.TextOrientation.TEXT_LEFT, this.fact, ColorUtils.TEXT_COLOR, this.padding, this.padding+getTextRenderer().fontHeight);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.drawer.update(client);
    }

    private void drawLines(List<Line> lines, MatrixStack ms) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Line line : lines) {
            Color color = line.color;
            Color end = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
            if (STATUS == Status.CRASHED) {
                color = FAILED_COLOR.darker().darker();
            }
            this.fillGradient(line.orientation, ms.peek().getPositionMatrix(), bufferBuilder, (int) line.x, (int) line.y, (int) line.x + line.width, (int) line.y + line.height, color, end);
        }

        tessellator.draw();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    private void fillGradient(UIDrawer.GradientOrientation orientation, Matrix4f b4, BufferBuilder bb, int startX, int startY, int endX, int endY, Color colorStart, Color colorEnd) {
        final int ordinal = orientation.ordinal();
        drawVertex(b4, bb, endX, startY, ordinal == 0 || ordinal == 3 ? colorStart : colorEnd); // right top
        drawVertex(b4, bb, startX, startY, ordinal == 1 || ordinal == 0 ? colorStart : colorEnd); // left top
        drawVertex(b4, bb, startX, endY, ordinal == 2 || ordinal == 1 ? colorStart : colorEnd); // left bottom
        drawVertex(b4, bb, endX, endY, ordinal == 3 || ordinal == 4 ? colorStart : colorEnd); // right bottom
    }

    public enum Status {
        IDLE,
        CRASHED,
        DONE
    }

    private final class Line {
        public UIDrawer.GradientOrientation orientation;
        public Color color;
        public float x;
        public float y;
        public int width;
        public int height;
        public float speed;

        public float xStep;
        public float yStep;

        public Line() {
            this.orientation = UIDrawer.GradientOrientation.GRADIENT_LEFT;
            this.x = -100;
            this.y = -100;
            this.width = 0;
            this.height = 0;
            this.color = Color.WHITE;
            this.speed = 1;
        }

        public void tick() {
            this.x += this.xStep;
            this.y += this.yStep;

            final int screenHeight = DashScreen.this.drawer.getHeight();
            final int screenWidth = DashScreen.this.drawer.getWidth();
            if (this.x - this.width > screenWidth) {
                this.x = 0;
                this.y = DashScreen.this.random.nextInt(screenHeight);
                this.update();
            } else if (this.x + this.width < 0) {
                this.x = screenWidth;
                this.y = DashScreen.this.random.nextInt(screenHeight);
                this.update();
            } else if (this.y - this.height > screenHeight) {
                this.y = -this.height;
                this.x = DashScreen.this.random.nextInt(screenWidth);
                this.update();
            } else if (this.y + this.height < 0) {
                this.y = screenHeight;
                this.x = DashScreen.this.random.nextInt(screenWidth);
                this.update();
            }
        }

        private void update() {
            this.updateColor();
            this.updateStep();
        }

        private void updateColor() {
            this.color = DashScreen.this.getLineColor();
        }

        private void updateStep() {
            this.xStep = (this.orientation.xDir * (this.speed * (1 + (DashScreen.this.random.nextFloat() * DashScreen.this.lineSpeedDifference)))) / 2f;
            this.yStep = (this.orientation.yDir * (this.speed * (1 + (DashScreen.this.random.nextFloat() * DashScreen.this.lineSpeedDifference)))) / 2f;
        }
    }

}