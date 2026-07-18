import java.util.ArrayList;
import java.util.List;

public class SongSort {
    public static void bubbleSortByReleaseYear(List<Song> songs) {
        int n = songs.size();
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (songs.get(j).getReleaseYear() > songs.get(j + 1).getReleaseYear()) {
                    Song temp = songs.get(j);
                    songs.set(j, songs.get(j + 1));
                    songs.set(j + 1, temp);
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }

    public static void mergeSortByDuration(List<Song> songs) {
        if (songs.size() <= 1) return;
        List<Song> sorted = mergeSortHelper(songs);
        for (int i = 0; i < songs.size(); i++) songs.set(i, sorted.get(i));
    }

    private static List<Song> mergeSortHelper(List<Song> list) {
        if (list.size() <= 1) return list;
        int mid = list.size() / 2;
        List<Song> left = new ArrayList<>(list.subList(0, mid));
        List<Song> right = new ArrayList<>(list.subList(mid, list.size()));
        left = mergeSortHelper(left);
        right = mergeSortHelper(right);
        return merge(left, right);
    }

    private static List<Song> merge(List<Song> left, List<Song> right) {
        List<Song> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).getDuration() <= right.get(j).getDuration()) {
                result.add(left.get(i));
                i++;
            } else {
                result.add(right.get(j));
                j++;
            }
        }
        while (i < left.size()) result.add(left.get(i++));
        while (j < right.size()) result.add(right.get(j++));
        return result;
    }
}
