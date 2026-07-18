import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayQueue {
    private static final int CAPACITY = 50;
    private final Song[] arr = new Song[CAPACITY];
    private int front = -1;
    private int rear = -1;
    private Song currentSong = null;

    public void enqueue(Song song) {
        if (song == null) return;
        if (currentSong == null) {
            currentSong = song;
            return;
        }

        // Circular queue enqueue at rear
        if (isFull()) {
            System.out.println("[WARNING] Play Queue is full! Cannot enqueue.");
            return;
        }
        if (isEmpty()) {
            front = 0;
            rear = 0;
        } else {
            rear = (rear + 1) % CAPACITY;
        }
        arr[rear] = song;
    }

    public void playNext(Song song) {
        if (song == null) return;
        if (currentSong == null) {
            currentSong = song;
            return;
        }

        // Deque push front (circular decrement)
        if (isFull()) {
            System.out.println("[WARNING] Play Queue is full! Cannot play next.");
            return;
        }
        if (isEmpty()) {
            front = 0;
            rear = 0;
        } else {
            front = (front - 1 + CAPACITY) % CAPACITY;
        }
        arr[front] = song;
    }

    public Song getNextSong() {
        if (currentSong != null) {
            enqueue(currentSong); // Circular next traversal logic
        }
        if (isEmpty()) {
            currentSong = null;
            return null;
        }
        currentSong = dequeue();
        return currentSong;
    }

    public Song getPreviousSong() {
        if (currentSong != null) {
            playNext(currentSong); // Put current song back at front
        }
        // Dequeue from rear to get the last added element (circular previous traversal)
        if (isEmpty()) {
            currentSong = null;
            return null;
        }

        currentSong = arr[rear];
        arr[rear] = null;
        if (front == rear) {
            front = -1;
            rear = -1;
        } else {
            rear = (rear - 1 + CAPACITY) % CAPACITY;
        }
        return currentSong;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public List<Song> getUpcomingSongs() {
        List<Song> upcoming = new ArrayList<>();
        if (isEmpty()) return upcoming;
        int i = front;
        while (true) {
            upcoming.add(arr[i]);
            if (i == rear) break;
            i = (i + 1) % CAPACITY;
        }
        return upcoming;
    }

    public void shuffle() {
        List<Song> upcoming = getUpcomingSongs();
        if (upcoming.size() <= 1) return;
        Collections.shuffle(upcoming);
        clearQueueOnly();
        for (Song s : upcoming) {
            enqueue(s);
        }
    }

    public void clear() {
        clearQueueOnly();
        currentSong = null;
    }

    private void clearQueueOnly() {
        for (int i = 0; i < CAPACITY; i++) {
            arr[i] = null;
        }
        front = -1;
        rear = -1;
    }

    public boolean remove(int index) {
        if (index < 0) return false;
        if (index == 0) {
            if (currentSong != null) {
                currentSong = isEmpty() ? null : dequeue();
                return true;
            }
            return false;
        }

        int listIndex = index - 1;
        List<Song> list = getUpcomingSongs();
        if (listIndex < list.size()) {
            list.remove(listIndex);
            clearQueueOnly();
            for (Song s : list) {
                enqueue(s);
            }
            return true;
        }
        return false;
    }

    public int size() {
        int queueSize = 0;
        if (!isEmpty()) {
            if (rear >= front) {
                queueSize = rear - front + 1;
            } else {
                queueSize = CAPACITY - front + rear + 1;
            }
        }
        return queueSize + (currentSong != null ? 1 : 0);
    }

    public List<Song> getAllSongs() {
        List<Song> all = new ArrayList<>();
        if (currentSong != null) all.add(currentSong);
        all.addAll(getUpcomingSongs());
        return all;
    }

    private boolean isEmpty() {
        return front == -1;
    }

    private boolean isFull() {
        return (rear + 1) % CAPACITY == front;
    }

    private Song dequeue() {
        if (isEmpty()) return null;
        Song song = arr[front];
        arr[front] = null;
        if (front == rear) {
            front = -1;
            rear = -1;
        } else {
            front = (front + 1) % CAPACITY;
        }
        return song;
    }
}
