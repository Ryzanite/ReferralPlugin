package io.ryzanite.referral;

import java.sql.*;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Referral extends JavaPlugin implements Listener {

    // Replace these values with your own MySQL database credentials
    private static final String DB_URL = "jdbc:mysql://u1_wukhbT5wK2:LB6W%40%2B%40oiOpArGH%5EoVpXARBQ@129.146.120.114:3306/s1_Referral?autoReconnect=true";
    private static final String DB_USERNAME = "u1_wukhbT5wK2";
    private static final String DB_PASSWORD = "LB6W@+@oiOpArGH^oVpXARBQ";

    private Connection connection;

    @Override
    public void onEnable() {
        try {
            openConnection();
            createTable();
        } catch (SQLException e) {
            log("X");
            e.printStackTrace();
        }

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /referral <player>");
            return true;
        }

        String referredPlayerName = args[0];
        UUID referredPlayerUuid = Bukkit.getOfflinePlayer(referredPlayerName).getUniqueId();

        if (referredPlayerUuid == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        try {
            if (hasReferral(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You already have a referral. You can only have one referral.");
                return true;
            }
            if (referredPlayerUuid.equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot refer yourself.");
                return true;
            }
            try {
                addReferral(player.getUniqueId(), referredPlayerUuid);
            } catch (SQLException e) {
                e.printStackTrace();
                log("D");
            }

            player.sendMessage(ChatColor.GREEN + "Successfully referred " + referredPlayerName + ".");
            giveReward(player);

            Player referredPlayer = Bukkit.getPlayer(referredPlayerUuid);
            if (referredPlayer != null && referredPlayer.isOnline()) {
                giveReward(referredPlayer);
                referredPlayer.sendMessage(ChatColor.GREEN + "You have been referred by " + player.getName() + " and have received a reward.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "An error occurred while trying to refer the player. Please try again later.");
            log("C");
        }
        closeConnection();
        return true;
    }

    private void openConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            try {
                connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            } catch (SQLException e) {
                e.printStackTrace();
                log("B");
            }
        }
    }

    private void closeConnection() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException e) {
            log("A");
        }
    }

    private void createTable() throws SQLException {
        openConnection();
        PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS referrals (id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, referring_player_uuid VARCHAR(36) NOT NULL, referred_player_uuid VARCHAR(36) NOT NULL);");
        statement.executeUpdate();
        closeConnection();
    }

    private boolean hasReferral(UUID playerUuid) throws SQLException {
        openConnection();
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM referrals WHERE referring_player_uuid=? OR referred_player_uuid=?");
        statement.setString(1, playerUuid.toString());
        statement.setString(2, playerUuid.toString());
        ResultSet resultSet = statement.executeQuery();
        closeConnection();
        return resultSet.next();
    }

    private void addReferral(UUID referringPlayerUuid, UUID referredPlayerUuid) throws SQLException {
        openConnection();
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO referrals (referring_player_uuid, referred_player_uuid) VALUES (?, ?)");
        statement.setString(1, referringPlayerUuid.toString());
        statement.setString(2, referredPlayerUuid.toString());
        statement.executeUpdate();
        closeConnection();
    }

    private void giveReward(Player player) {
        String playername = player.getName();
        CommandSender console = Bukkit.getServer().getConsoleSender();
        Bukkit.dispatchCommand(console, "tokens give " + playername + " 2"); // Give player 2 tokens
    }

    private void log(String a) {
        Bukkit.getServer().getLogger().info(a);
    }
}