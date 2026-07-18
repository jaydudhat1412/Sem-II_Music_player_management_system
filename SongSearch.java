import java.util.ArrayList;
import java.util.List;

public class SongSearch {
    public static Song binarySearchByTitle(List<Song> sortedSongs, String targetTitle) {
        if (sortedSongs == null || sortedSongs.isEmpty() || targetTitle == null) return null;
        int low = 0, high = sortedSongs.size() - 1;
        String target = targetTitle.trim().toLowerCase();

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int cmp = sortedSongs.get(mid).getTitle().toLowerCase().compareTo(target);
            if (cmp == 0) return sortedSongs.get(mid);
            else if (cmp < 0) low = mid + 1;
            else high = mid - 1;
        }
        return null;
    }

    public static List<Song> linearSearch(List<Song> songs, String query, String searchField) {
        List<Song> results = new ArrayList<>();
        if (songs == null || query == null || searchField == null) return results;
        String q = query.trim().toLowerCase();
        for (Song song : songs) {
            boolean match = false;
            switch (searchField.toLowerCase()) {
                case "title":
                    match = song.getTitle().toLowerCase().startsWith(q);
                    break;
                case "artist":
                    match = song.getArtistName() != null && song.getArtistName().toLowerCase().contains(q);
                    break;
                case "album":
                    match = song.getAlbumTitle() != null && song.getAlbumTitle().toLowerCase().contains(q);
                    break;
                case "genre":
                    match = song.getGenre().toLowerCase().contains(q);
                    break;
                case "language":
                    match = song.getLanguage().toLowerCase().contains(q);
                    break;
            }
            if (match) results.add(song);
        }
        return results;
    }
}
