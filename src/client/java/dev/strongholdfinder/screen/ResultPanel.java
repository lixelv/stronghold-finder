package dev.strongholdfinder.screen;

import dev.strongholdfinder.core.Advice;
import dev.strongholdfinder.core.AdviceLine;
import dev.strongholdfinder.core.Compass;
import dev.strongholdfinder.core.Geometry;
import dev.strongholdfinder.core.Solution;
import java.util.Arrays;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class ResultPanel {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int GREEN = 0xFF55FF55;
    private static final int GOLD = 0xFFFFAA00;
    private static final int RED = 0xFFFF5555;
    private static final int LINE = 11;
    private static final int ADVICE_LINE = 10;

    private ResultPanel() {
    }

    static void extract(GuiGraphicsExtractor graphics, Font font, Solution s, int x, int y, int width, int maxY) {
        int at = y;
        if (s.mean() == null) {
            int missing = 2 - s.on().size();
            Component title = missing > 0
                    ? Component.translatable("screen.stronghold_finder.need_more", missing)
                    : Component.translatable("screen.stronghold_finder.no_crossing");
            graphics.text(font, title, x, at, YELLOW);
            at += LINE + 2;
        } else {
            at = extractResult(graphics, font, s, x, at, width);
        }
        for (AdviceLine line : Advice.of(s)) {
            Component text = Component.translatable(
                    "stronghold_finder." + line.key(),
                    Arrays.stream(line.args()).map(ResultPanel::argument).toArray());
            for (FormattedCharSequence wrapped : font.split(text, width)) {
                if (at + ADVICE_LINE > maxY) {
                    return;
                }
                graphics.text(font, wrapped, x, at, line.warn() ? GOLD : GRAY);
                at += ADVICE_LINE;
            }
        }
    }

    private static int extractResult(GuiGraphicsExtractor graphics, Font font, Solution s, int x, int y, int width) {
        int at = y;
        String coordinates = "X " + Math.round(s.mean().x()) + "  Z " + Math.round(s.mean().z());
        int scale = font.width(coordinates) * 2 <= width - 60 ? 2 : 1;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, at);
        graphics.pose().scale(scale, scale);
        graphics.text(font, coordinates, 0, 0, YELLOW);
        graphics.pose().popMatrix();

        String grade = s.err() <= 30 ? "good" : s.err() <= 100 ? "fair" : "rough";
        int gradeColor = s.err() <= 30 ? GREEN : s.err() <= 100 ? YELLOW : RED;
        Component gradeText = Component.translatable("screen.stronghold_finder.grade." + grade);
        graphics.text(font, gradeText, x + width - font.width(gradeText), at + (scale == 2 ? 4 : 0), gradeColor);
        at += 9 * scale + 5;

        int gap = width / 2;
        at = row(graphics, font, x, at, gap, Component.translatable("screen.stronghold_finder.walk", s.last().id()),
                Component.translatable("screen.stronghold_finder.walk_value",
                        Math.round(s.toLast()), argument(Geometry.compass(s.yawNext()))));
        at = row(graphics, font, x, at, gap, Component.translatable("screen.stronghold_finder.yaw"),
                Component.literal(String.format(java.util.Locale.ROOT, "%.1f°", s.yawNext())));
        at = row(graphics, font, x, at, gap, Component.translatable("screen.stronghold_finder.nether"),
                Component.literal(Math.floorDiv(Math.round(s.mean().x()), 8) + ", " + Math.floorDiv(Math.round(s.mean().z()), 8)));
        Component ring = s.ring().inside()
                ? Component.translatable("screen.stronghold_finder.ring_value",
                        s.ring().n(), Geometry.RINGS[s.ring().n() - 1][0], Geometry.RINGS[s.ring().n() - 1][1])
                : Component.translatable("screen.stronghold_finder.ring_outside");
        at = row(graphics, font, x, at, gap, Component.translatable("screen.stronghold_finder.ring"), ring);
        at = row(graphics, font, x, at, gap, Component.translatable("screen.stronghold_finder.chunk"),
                Component.literal(Math.floorDiv(Math.round(s.mean().x()), 16) + ", " + Math.floorDiv(Math.round(s.mean().z()), 16)));
        if (s.lsq() != null) {
            at = row(graphics, font, x, at, gap, Component.translatable("screen.stronghold_finder.check"),
                    Component.translatable("screen.stronghold_finder.blocks", Math.round(s.lsq().off())));
        }
        return at + 3;
    }

    private static int row(GuiGraphicsExtractor graphics, Font font, int x, int y, int gap, Component label, Component value) {
        graphics.text(font, font.plainSubstrByWidth(label.getString(), gap - 4), x, y, GRAY);
        graphics.text(font, value, x + gap, y, WHITE);
        return y + LINE;
    }

    private static Object argument(Object arg) {
        return arg instanceof Compass side ? Component.translatable("stronghold_finder." + side.key()) : arg;
    }
}
