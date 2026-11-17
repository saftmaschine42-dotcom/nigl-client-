package net.nigl.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Zeichenhelfer: runde Formen, iPhone-artiges "Liquid Glass", Block-Logo. */
public final class Draw {
    private static final String[][] LOGO = {
            {"10001", "11001", "10101", "10101", "10011", "10001", "10001"}, // N
            {"11111", "00100", "00100", "00100", "00100", "00100", "11111"}, // I
            {"01110", "10001", "10000", "10111", "10001", "10001", "01110"}, // G
            {"10000", "10000", "10000", "10000", "10000", "10000", "11111"}  // L
    };

    public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * (double) r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        if (h - 2 * r > 0) g.fill(x, y + r, x + w, y + h - r, color);
    }

    /** Durchsichtiges Milchglas: heller Rand, Frost-Schicht, Glanz oben, Specular-Linie. */
    public static void glass(GuiGraphicsExtractor g, int x, int y, int w, int h, int r) {
        float o = Theme.glass();
        roundRect(g, x - 1, y - 1, w + 2, h + 2, r + 1, Theme.scaleAlpha(0x78FFFFFF, o));   // Rand
        roundRect(g, x, y, w, h, r, 0x38101420);                                              // dunkle Basis (Lesbarkeit)
        roundRect(g, x, y, w, h, r, Theme.scaleAlpha(0x2EFFFFFF, o));                         // Frost
        roundRect(g, x + 1, y + 1, w - 2, Math.max(3, (int) (h * 0.42f)), Math.max(2, r - 1), Theme.scaleAlpha(0x1EFFFFFF, o)); // Glanz
        g.fill(x + r, y + h - 2, x + w - r, y + h - 1, 0x1CFFFFFF);                           // unterer Innenschein
        g.fill(x + r, y, x + w - r, y + 1, 0xC0FFFFFF);                                       // Specular oben
    }

    /** Wandernder Lichtreflex auf der Oberkante. */
    public static void shimmer(GuiGraphicsExtractor g, int x, int y, int w, int r) {
        int span = Math.max(1, w * 2);
        int t = (int) ((System.currentTimeMillis() / 14) % span);
        int a = Math.max(x + r, x + t - 26), b = Math.min(x + w - r, x + t);
        if (b > a) g.fill(a, y, b, y + 1, 0xFFFFFFFF);
    }

    public static int logoWidth(int cell) { return 23 * cell; }

    public static void logo(GuiGraphicsExtractor g, int x, int y, int c) {
        for (int pass = 0; pass < 3; pass++) {
            int cx = 0;
            for (String[] letter : LOGO) {
                for (int row = 0; row < 7; row++) {
                    for (int col = 0; col < 5; col++) {
                        if (letter[row].charAt(col) != '1') continue;
                        int px = x + (cx + col) * c, py = y + row * c;
                        int color = Theme.accent((cx + col) / 22f);
                        switch (pass) {
                            case 0 -> g.fill(px - 2, py - 2, px + c + 2, py + c + 2, Theme.withAlpha(color, 0x20));
                            case 1 -> g.fill(px + 2, py + 3, px + c + 2, py + c + 3, 0x55000000);
                            default -> {
                                g.fill(px, py, px + c, py + c, Theme.withAlpha(color, 0xE8));
                                g.fill(px, py, px + c, py + Math.max(1, c / 4), 0x80FFFFFF);
                            }
                        }
                    }
                }
                cx += 6;
            }
        }
    }

    private Draw() { }
}
