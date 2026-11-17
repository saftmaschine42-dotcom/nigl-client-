package net.nigl.client.module.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.nigl.client.module.Category;
import net.nigl.client.module.Module;
import net.nigl.client.render.EspRenderer.Box;
import net.nigl.client.render.EspRenderer.Line;
import net.nigl.client.setting.BoolSetting;
import net.nigl.client.setting.NumberSetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockEsp extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 24, 8, 48, 2));
    private final NumberSetting max = add(new NumberSetting("Max Blocks", 120, 20, 300, 10));
    private final BoolSetting diamond = add(new BoolSetting("Diamond", true));
    private final BoolSetting emerald = add(new BoolSetting("Emerald", true));
    private final BoolSetting gold = add(new BoolSetting("Gold", true));
    private final BoolSetting iron = add(new BoolSetting("Iron", false));
    private final BoolSetting coal = add(new BoolSetting("Coal", false));
    private final BoolSetting redstone = add(new BoolSetting("Redstone", false));
    private final BoolSetting lapis = add(new BoolSetting("Lapis", false));
    private final BoolSetting copper = add(new BoolSetting("Copper", false));
    private final BoolSetting debris = add(new BoolSetting("Ancient Debris", true));
    private final BoolSetting quartz = add(new BoolSetting("Quartz", false));
    private final BoolSetting containers = add(new BoolSetting("Containers", true));
    private final BoolSetting spawners = add(new BoolSetting("Spawners", true));
    private final BoolSetting fill = add(new BoolSetting("Fill", true));
    private final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    private final NumberSetting thickness = add(new NumberSetting("Thickness", 1.5, 1, 4, 0.5));

    private record Type(BoolSetting setting, int color) { }
    private record Found(int x, int y, int z, int color) { }

    private final Map<Block, Type> types = new HashMap<>();
    private static final int CYCLE = 10;
    private volatile List<Found> result = List.of();
    private List<Found> building = new ArrayList<>();
    private int phase = 0, cx, cy, cz;

    public BlockEsp() {
        super("BlockESP", "Markiert Erze, Kisten und Spawner durch Waende", Category.RENDER);
        reg(diamond, 0x4DE1FF, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        reg(emerald, 0x4DFF7A, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        reg(gold, 0xFFD54D, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        reg(iron, 0xE8C4A8, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        reg(coal, 0x9A9AA8, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        reg(redstone, 0xFF3B3B, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        reg(lapis, 0x4D6BFF, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        reg(copper, 0xFF8A4D, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        reg(debris, 0xD08A5C, Blocks.ANCIENT_DEBRIS);
        reg(quartz, 0xFFFFFF, Blocks.NETHER_QUARTZ_ORE);
        reg(containers, 0xFFB74D, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL);
        reg(spawners, 0xB04DFF, Blocks.SPAWNER);
    }

    private void reg(BoolSetting s, int color, Block... blocks) {
        for (Block b : blocks) types.put(b, new Type(s, color));
    }

    @Override
    public void onTick() {
        var p = mc.player;
        int R = (int) range.value;
        if (phase == 0) {
            building = new ArrayList<>();
            cx = Mth.floor(p.getX()); cy = Mth.floor(p.getY()); cz = Mth.floor(p.getZ());
        }
        int limit = (int) max.value;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -R; dx <= R; dx++) {
            if (Math.floorMod(dx + R, CYCLE) != phase) continue;
            for (int dz = -R; dz <= R; dz++) {
                for (int dy = -R; dy <= R; dy++) {
                    if (building.size() >= limit) break;
                    pos.set(cx + dx, cy + dy, cz + dz);
                    var state = mc.level.getBlockState(pos);
                    if (state.isAir()) continue;
                    Type t = types.get(state.getBlock());
                    if (t != null && t.setting().value) building.add(new Found(pos.getX(), pos.getY(), pos.getZ(), t.color()));
                }
            }
        }
        if (++phase >= CYCLE) { phase = 0; result = building; }
    }

    @Override public void onDisable() { result = List.of(); building = new ArrayList<>(); phase = 0; }

    public void collect(List<Box> boxes, List<Line> lines, float pt) {
        List<Found> found = result;
        if (found.isEmpty()) return;
        var p = mc.player;
        double ex = Mth.lerp(pt, p.xo, p.getX());
        double ey = Mth.lerp(pt, p.yo, p.getY()) + p.getEyeHeight();
        double ez = Mth.lerp(pt, p.zo, p.getZ());
        Vec3 view = p.getViewVector(pt);
        double sx = ex + view.x * 2, sy = ey + view.y * 2, sz = ez + view.z * 2;
        float px = (float) thickness.value;
        for (Found f : found) {
            float r = ((f.color() >> 16) & 255) / 255f, g = ((f.color() >> 8) & 255) / 255f, b = (f.color() & 255) / 255f;
            boxes.add(new Box(f.x() - 0.002, f.y() - 0.002, f.z() - 0.002, f.x() + 1.002, f.y() + 1.002, f.z() + 1.002,
                    r, g, b, fill.value, true, px));
            if (tracers.value) lines.add(new Line(sx, sy, sz, f.x() + 0.5, f.y() + 0.5, f.z() + 0.5, r, g, b, 0.9f, px));
        }
    }
}
