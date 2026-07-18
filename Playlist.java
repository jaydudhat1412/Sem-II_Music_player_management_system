import java.util.List;
import java.util.ArrayList;

public class Playlist {
    private int id;
    private String name;
    private List<Song> songs;

    public Playlist(int id, String name) {
        this.id = id;
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public int getTotalDuration() {
        int total = 0;
        for (Song s : songs) total += s.getDuration();
        return total;
    }

    @Override
    public String toString() {
        int totalSec = getTotalDuration();
        return String.format("Playlist: %s [Songs: %d | Total Duration: %d:%02d]",
                name, songs.size(), totalSec / 60, totalSec % 60);
    }
}
