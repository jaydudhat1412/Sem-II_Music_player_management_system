import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileExporter {
    public static boolean exportPlaylist(String playlistName, List<Song> songs) {
        String fileName = playlistName + "_playlist.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Playlist: " + playlistName + "\n");
            writer.write("==============================\n");
            for (Song s : songs) {
                writer.write(s.toString() + "\n");
            }
            return true;
        } catch (IOException e) {
            LoggerUtility.logError("Failed to export playlist: " + e.getMessage());
            return false;
        }
    }

    public static boolean exportListeningHistory(String username, List<String> records) {
        String fileName = username + "_listening_history.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Listening History for: " + username + "\n");
            writer.write("==============================\n");
            for (String r : records) {
                writer.write(r + "\n");
            }
            return true;
        } catch (IOException e) {
            LoggerUtility.logError("Failed to export listening history: " + e.getMessage());
            return false;
        }
    }
}
