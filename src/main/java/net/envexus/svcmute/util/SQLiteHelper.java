package net.envexus.svcmute.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.envexus.svcmute.SVCMute;

public class SQLiteHelper {
    private final String url;

    public SQLiteHelper(SVCMute svcMute) {
        svcMute.getDataFolder().mkdirs();
        File databaseFile = new File(svcMute.getDataFolder(), "mutes.db");
        this.url = "jdbc:sqlite:" + databaseFile.getAbsolutePath().replace('\\', '/');

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS mutes (" +
                    "uuid TEXT NOT NULL PRIMARY KEY, " +
                    "unmute_time INTEGER NOT NULL)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mutes_unmute_time ON mutes(unmute_time)");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize mute database", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public void addMute(String uuid, long unmuteTime) {
        String sql = "INSERT OR REPLACE INTO mutes(uuid, unmute_time) VALUES(?, ?)";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setLong(2, unmuteTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to store mute for uuid " + uuid, e);
        }
    }

    public Long getUnmuteTime(String uuid) {
        String sql = "SELECT unmute_time FROM mutes WHERE uuid = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("unmute_time");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read mute for uuid " + uuid, e);
        }
        return null;
    }

    public void removeMute(String uuid) {
        String sql = "DELETE FROM mutes WHERE uuid = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove mute for uuid " + uuid, e);
        }
    }

    public boolean isMuted(String string) {
        long now = System.currentTimeMillis();
        String sql = "SELECT 1 FROM mutes WHERE uuid = ? AND unmute_time > ? LIMIT 1";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, string);
            pstmt.setLong(2, now);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check mute for uuid " + string, e);
        }
    }

    public void removeExpiredMutes(long now) {
        String sql = "DELETE FROM mutes WHERE unmute_time <= ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, now);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to prune expired mutes", e);
        }
    }

    public Map<UUID, Long> getActiveMutes(long now) {
        String sql = "SELECT uuid, unmute_time FROM mutes WHERE unmute_time > ?";
        Map<UUID, Long> activeMutes = new HashMap<>();

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, now);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activeMutes.put(UUID.fromString(rs.getString("uuid")), rs.getLong("unmute_time"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load active mutes", e);
        }

        return activeMutes;
    }
}
