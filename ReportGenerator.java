import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.PriorityQueue;

public class ReportGenerator {
    private final DatabaseHelper dbHelper = new DatabaseHelper();

    public void displayTop10Songs() {
        try {
            List<Song> all = dbHelper.getAllSongs();
            PriorityQueue<Song> pq = new PriorityQueue<>((s1, s2) -> Integer.compare(s2.getPlayCount(), s1.getPlayCount()));
            pq.addAll(all);
            System.out.println("\nTop 10 Most Played Songs:");
            int count = Math.min(10, pq.size());
            for (int i = 0; i < count; i++) {
                Song s = pq.poll();
                System.out.println(String.format("  %d. %s - %s | Plays: %d", i + 1, s.getTitle(), s.getArtistName(), s.getPlayCount()));
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to load top songs report: " + e.getMessage());
        }
    }

    public void displayPlaylistStats() {
        try {
            System.out.println("\nPlaylist Metrics (From DB):");
            int total = 0;
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM playlists")) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
            System.out.println("  Total playlists on system: " + total);
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to load playlist stats: " + e.getMessage());
        }
    }

    public void displayActiveUsers() {
        String sql = "SELECT u.username, COUNT(h.id) AS plays FROM users u LEFT JOIN history h ON u.id = h.user_id GROUP BY u.username ORDER BY plays DESC LIMIT 5";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\nMost Active Users (Play Counts):");
            int idx = 1;
            while (rs.next()) {
                System.out.println(String.format("  %d. User: %-15s | Plays: %d", idx++, rs.getString("username"), rs.getInt("plays")));
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Restricted database metrics error: " + e.getMessage());
        }
    }

    public boolean exportSystemReport(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("==================================================\n");
            writer.write("         SYSTEM MUSIC REPORT & STATISTICS         \n");
            writer.write("==================================================\n\n");
            writer.write("Generated at: " + java.time.LocalDateTime.now() + "\n");
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
