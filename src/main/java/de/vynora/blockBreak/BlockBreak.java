package de.vynora.blockBreak;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockBreak extends JavaPlugin implements CommandExecutor {

    private final List<Player> players = new ArrayList<>();
    private final int towerHeight = 20;
    private final int towerSpacing = 5;
    private final Material[] blockTypes = {Material.STONE, Material.OAK_LOG, Material.DIRT};

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

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            int x = i * towerSpacing;
            int y = world.getHighestBlockYAt(x, 0) + towerHeight;
            int z = 0;

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

            // Teleportiere den Spieler auf seinen Turm
            player.teleport(new org.bukkit.Location(world, x + 0.5, y + 1, z + 0.5));
            player.sendMessage("Das Spiel startet in 5 Sekunden!");
        }

        // Countdown von 5 Sekunden vor Spielstart
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown > 0) {
                    Bukkit.broadcastMessage("Spiel startet in " + countdown + " Sekunden!");
                    countdown--;
                } else {
                    Bukkit.broadcastMessage("Los geht's!");
                    startGameLoop();
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0, 20); // 20 Ticks = 1 Sekunde
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

                    if (player.getWorld().getBlockAt(x, y - 1, z).getType() == Material.GREEN_WOOL) {
                        Bukkit.broadcastMessage(player.getName() + " hat gewonnen!");
                        this.cancel();
                        return;
                    }

                    // Ändere die zwei Blöcke unter dem Spieler
                    for (int i = 1; i <= 2; i++) {
                        Material newBlock = blockTypes[random.nextInt(blockTypes.length)];
                        player.getWorld().getBlockAt(x, y - i, z).setType(newBlock);
                    }
                }
            }
        }.runTaskTimer(this, 20, 20);
    }
}
