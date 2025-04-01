package de.vynora.blockBreak;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockBreak extends JavaPlugin implements CommandExecutor {

    private final List<Player> players = new ArrayList<>();
    private final int towerHeight = 20;
    private final int towerSpacing = 10; // Abstand zwischen den Türmen
    private final Material[] blockTypes = {Material.STONE, Material.DIRT, Material.OAK_LOG};

    @Override
    public void onEnable() {
        this.getCommand("startblockbreak").setExecutor(this);
        Bukkit.getLogger().info("BlockBreak Plugin aktiviert!");
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("BlockBreak Plugin deaktiviert!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler können dieses Kommando ausführen.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("startblockbreak")) {
            players.clear();
            players.addAll(Bukkit.getOnlinePlayers());
            startGame();
            return true;
        }
        return false;
    }

    private void startGame() {
        if (players.isEmpty()) {
            Bukkit.broadcastMessage("Es sind keine Spieler online!");
            return;
        }

        World world = players.get(0).getWorld();
        Random random = new Random();

        // Erstellen der 4 Türme in einem Viereck
        int[] xOffsets = {-towerSpacing, 0, towerSpacing, 0};
        int[] zOffsets = {0, towerSpacing, 0, -towerSpacing};

        for (int i = 0; i < 4; i++) {
            int x = xOffsets[i];
            int z = zOffsets[i];
            Player player = players.get(i);

            int y = world.getHighestBlockYAt(x, z) + towerHeight;

            // Erstelle den Turm aus Goldblöcken
            for (int j = 0; j < towerHeight; j++) {
                world.getBlockAt(x, y - j, z).setType(Material.GOLD_BLOCK);
            }

            // Setze grünen Wollblock am Boden des Turms
            world.getBlockAt(x, y - towerHeight, z).setType(Material.GREEN_WOOL);

            // Errichte eine Barriere um den Turm
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    for (int dy = 0; dy < towerHeight; dy++) {
                        world.getBlockAt(x + dx, y - dy, z + dz).setType(Material.BARRIER);
                    }
                }
            }

            // Teleportiere den Spieler auf einen der Türme
            player.teleport(new org.bukkit.Location(world, x + 0.5, y + 1, z + 0.5));

            // Gib dem Spieler die benötigten Werkzeuge
            giveToolsToPlayer(player);

            // Nachricht, dass das Spiel in 5 Sekunden startet
            player.sendMessage("Das Spiel startet in 5 Sekunden!");
        }

        // Verzögerung von 5 Sekunden vor Spielstart
        new BukkitRunnable() {
            @Override
            public void run() {
                startGameLoop();
            }
        }.runTaskLater(this, 100); // 100 Ticks = 5 Sekunden
    }

    private void giveToolsToPlayer(Player player) {
        // Erstelle die benötigten Werkzeuge für den Spieler
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        pickaxeMeta.setDisplayName("Spitzhacke");
        pickaxe.setItemMeta(pickaxeMeta);

        ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
        ItemMeta shovelMeta = shovel.getItemMeta();
        shovelMeta.setDisplayName("Schaufel");
        shovel.setItemMeta(shovelMeta);

        ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta axeMeta = axe.getItemMeta();
        axeMeta.setDisplayName("Axt");
        axe.setItemMeta(axeMeta);

        // Gib dem Spieler die Werkzeuge
        player.getInventory().addItem(pickaxe, shovel, axe);
    }

    private void startGameLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Random random = new Random();

                for (Player player : players) {
                    int x = player.getLocation().getBlockX();
                    int y = player.getLocation().getBlockY();
                    int z = player.getLocation().getBlockZ();

                    Block blockBelow = player.getWorld().getBlockAt(x, y - 1, z);

                    // Wenn der Spieler den grünen Wollblock erreicht hat
                    if (blockBelow.getType() == Material.GREEN_WOOL) {
                        Bukkit.broadcastMessage(player.getName() + " hat gewonnen!");
                        this.cancel();
                        return;
                    }

                    // Überprüfe, ob der Spieler das richtige Werkzeug für den Block hat
                    if (!isCorrectToolForBlock(player, blockBelow)) {
                        player.sendMessage("Du hast das falsche Werkzeug!");
                        continue;
                    }

                    // Ändere die zwei Blöcke unter dem Spieler
                    for (int i = 1; i <= 2; i++) {
                        Material newBlock = blockTypes[random.nextInt(blockTypes.length)];
                        player.getWorld().getBlockAt(x, y - i, z).setType(newBlock);
                    }
                }
            }
        }.runTaskTimer(this, 20, 20); // Alle 20 Ticks = 1 Sekunde
    }

    private boolean isCorrectToolForBlock(Player player, Block block) {
        Material blockType = block.getType();
        Material toolType = player.getInventory().getItemInMainHand().getType();

        switch (blockType) {
            case STONE:
                return toolType == Material.DIAMOND_PICKAXE;
            case DIRT:
                return toolType == Material.DIAMOND_SHOVEL;
            case OAK_LOG:
                return toolType == Material.DIAMOND_AXE;
            default:
                return false;
        }
    }
}
