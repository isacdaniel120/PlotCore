package me.plotcore.managers;

import me.plotcore.models.Plot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

public class SignManager {

    public static Location placeSign(Plot plot, Location base, Material signMaterial) {
        Location signLoc = base.clone();
        signLoc.setY(Math.floor(base.getY()));
        Block block = signLoc.getBlock();

        if (!block.isEmpty() && !block.isLiquid()) return null;

        Material mat = signMaterial != null ? signMaterial : Material.OAK_SIGN;
        block.setType(mat);

        if (block.getBlockData() instanceof org.bukkit.block.data.Rotatable rotatable) {
            float yaw = base.getYaw();
            if (yaw < 0) yaw += 360;
            int rotation = (int) Math.round(yaw / 22.5f) % 16;
            BlockFace[] faces = {
                BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
                BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST,
                BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
                BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST
            };
            rotatable.setRotation(faces[rotation]);
            block.setBlockData(rotatable);
        }

        updateSign(block, plot);
        return signLoc;
    }

    public static void updateSign(Block block, Plot plot) {
        if (!(block.getState() instanceof Sign sign)) return;

        var face = sign.getSide(Side.FRONT);

        if (plot.isRented()) {
            face.line(0, Component.text("[" + plot.getName() + "]")
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            face.line(1, Component.text("Rented by")
                .color(NamedTextColor.GRAY));
            face.line(2, Component.text(plot.getOwnerName() != null ? plot.getOwnerName() : "Unknown")
                .color(NamedTextColor.WHITE));
            face.line(3, Component.text(plot.getTimeLeftFormatted())
                .color(NamedTextColor.AQUA));
        } else {
            face.line(0, Component.text("[" + plot.getName() + "]")
                .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
            face.line(1, Component.text("Unrented")
                .color(NamedTextColor.GREEN));
            face.line(2, Component.text("Cost: " + (int) plot.getRentPrice())
                .color(NamedTextColor.YELLOW));
            face.line(3, Component.text("Click to rent")
                .color(NamedTextColor.GRAY));
        }
        sign.update(true);
    }

    public static void refreshSign(Plot plot) {
        Location loc = plot.getSignLocation();
        if (loc == null || loc.getWorld() == null) return;
        Block block = loc.getBlock();
        if (block.getState() instanceof Sign) {
            updateSign(block, plot);
        }
    }

    public static boolean isSignBlock(Block block) {
        return block.getState() instanceof Sign;
    }
}
