import java.util.ArrayList;
import java.util.List;

public class RecentStack {
    private static final int MAX_SIZE = 10;
    private final Song[] arr = new Song[MAX_SIZE];
    private int top = -1;

    public void push(Song song) {
        if (song == null) return;
        
        // Remove duplicate if it exists in the stack
        removeSong(song.getId());
        
        // If stack is full, shift elements left by 1 to make room at the top
        if (top == MAX_SIZE - 1) {
            for (int i = 0; i < top; i++) {
                arr[i] = arr[i + 1];
            }
            arr[top] = song;
        } else {
            top++;
            arr[top] = song;
        }
    }

    private void removeSong(int songId) {
        int index = -1;
        for (int i = 0; i <= top; i++) {
            if (arr[i].getId() == songId) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            for (int i = index; i < top; i++) {
                arr[i] = arr[i + 1];
            }
            arr[top] = null;
            top--;
        }
    }

    public void clear() {
        for (int i = 0; i <= top; i++) {
            arr[i] = null;
        }
        top = -1;
    }

    public List<Song> getRecent(int count) {
        List<Song> list = new ArrayList<>();
        int limit = Math.min(count, top + 1);
        for (int i = 0; i < limit; i++) {
            list.add(arr[top - i]);
        }
        return list;
    }
}
