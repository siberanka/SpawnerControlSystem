package com.siberanka.spawnercontrolsystem.managers;

import com.siberanka.spawnercontrolsystem.SpawnerControlSystem;
import com.siberanka.spawnercontrolsystem.utils.BlockPos;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {

    private final SpawnerControlSystem plugin;
    private HikariDataSource dataSource;
    private final CacheManager cacheManager;

    public DatabaseManager(SpawnerControlSystem plugin, CacheManager cacheManager) {
        this.plugin = plugin;
        this.cacheManager = cacheManager;
    }

    public void connect() {
        if (!plugin.getConfigurationManager().isDbEnabled()) {
            plugin.getLogger().info("Database is disabled in config. Spawners will not persist.");
            return;
        }

        File dbFile = new File(plugin.getDataFolder(), plugin.getConfigurationManager().getDbFileName());

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("SCC-SQLitePool");
        // SQLite does not handle concurrent writes well without transaction queueing.
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");

        // Optimize for SQLite concurrency and reliability
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        dataSource = new HikariDataSource(config);

        createTable();
        loadAllSpawners();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS placed_spawners (" +
                "world VARCHAR(64) NOT NULL, " +
                "x INT NOT NULL, " +
                "y INT NOT NULL, " +
                "z INT NOT NULL, " +
                "PRIMARY KEY (world, x, y, z))";

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create SQLite table", e);
        }
    }

    public void loadAllSpawners() {
        if (dataSource == null || dataSource.isClosed())
            return;

        cacheManager.clear();
        String query = "SELECT world, x, y, z FROM placed_spawners";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                BlockPos pos = new BlockPos(
                        rs.getString("world"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"));
                cacheManager.addSpawner(pos);
            }
            plugin.getLogger().info("Loaded " + cacheManager.size() + " spawners from database into RAM.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load spawners from database", e);
        }
    }

    public void addSpawnerAsync(BlockPos pos) {
        // We add to RAM instantly over the main thread before calling async
        cacheManager.addSpawner(pos);

        if (!plugin.getConfigurationManager().isDbEnabled() || dataSource == null)
            return;

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            String query = "INSERT OR IGNORE INTO placed_spawners (world, x, y, z) VALUES (?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, pos.world);
                stmt.setInt(2, pos.x);
                stmt.setInt(3, pos.y);
                stmt.setInt(4, pos.z);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save spawner: " + pos, e);
            }
        });
    }

    public void removeSpawnerAsync(BlockPos pos) {
        // Instantly remove from memory without awaiting database operation
        cacheManager.removeSpawner(pos);

        if (!plugin.getConfigurationManager().isDbEnabled() || dataSource == null)
            return;

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            String query = "DELETE FROM placed_spawners WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, pos.world);
                stmt.setInt(2, pos.x);
                stmt.setInt(3, pos.y);
                stmt.setInt(4, pos.z);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove spawner: " + pos, e);
            }
        });
    }
}
