public class MusicPlayerService {
    private final DatabaseHelper dbHelper = new DatabaseHelper();
    private PlaybackThread playbackThread;
    private Song currentSong;

    public synchronized void play(Song song, int userId) {
        if (song == null) return;
        if (isPlaying()) stop();
        this.currentSong = song;
        playbackThread = new PlaybackThread(song);
        playbackThread.start();

        dbHelper.addHistory(userId, song.getId());
        dbHelper.incrementPlayCount(song.getId());
        LoggerUtility.logInfo("Asynchronously started play simulation for song ID: " + song.getId());
    }

    public synchronized void stop() {
        if (playbackThread != null) {
            playbackThread.stopPlayback();
            try {
                playbackThread.join(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            playbackThread = null;
            System.out.println("\n[PLAYER] Stopped playing: \"" + (currentSong != null ? currentSong.getTitle() : "") + "\"");
            currentSong = null;
        } else {
            System.out.println("\n[PLAYER] No song is playing to stop.");
        }
    }

    public synchronized void pause() {
        if (playbackThread != null) {
            playbackThread.pausePlayback();
            System.out.println("\n[PLAYER] Paused: \"" + (currentSong != null ? currentSong.getTitle() : "") + "\"");
        }
    }

    public synchronized void resume() {
        if (playbackThread != null) {
            playbackThread.resumePlayback();
            System.out.println("\n[PLAYER] Resumed: \"" + (currentSong != null ? currentSong.getTitle() : "") + "\"");
        }
    }

    public boolean isPlaying() {
        return playbackThread != null && playbackThread.isAlive();
    }

    private class PlaybackThread extends Thread {
        private final Song song;
        private int elapsedSeconds = 0;
        private boolean running = true;

        public PlaybackThread(Song song) {
            super("Playback-" + song.getTitle());
            this.song = song;
        }

        private boolean paused = false;

        public void pausePlayback() {
            this.paused = true;
        }

        public void resumePlayback() {
            this.paused = false;
        }

        public void stopPlayback() {
            this.running = false;
            this.interrupt();
        }

        @Override
        public void run() {
            int total = song.getDuration();
            System.out.println("\n[PLAYER] Now Playing: \"" + song.getTitle() + "\" by " + song.getArtistName());
            System.out.print("[PLAYER] Progress: ");
            while (running && elapsedSeconds < total) {
                try {
                    Thread.sleep(1000); // Wait 1 second
                    if (!paused) {
                        elapsedSeconds++;
                    }
                    printProgressBar(elapsedSeconds, total);
                } catch (InterruptedException e) {
                    if (!running) break;
                }
            }
            if (running && elapsedSeconds >= total) {
                System.out.println("\n[PLAYER] Finished playing: \"" + song.getTitle() + "\"");
            }
        }

        private void printProgressBar(int elapsed, int total) {
            int width = 20;
            int progress = (int) (((double) elapsed / total) * width);
            StringBuilder sb = new StringBuilder("\r[PLAYER] [");
            for (int i = 0; i < width; i++) {
                if (i < progress) sb.append("=");
                else if (i == progress) sb.append(">");
                else sb.append(" ");
            }
            sb.append(String.format("] [%02d:%02d/%02d:%02d] (Choice 1=Pause, 2=Continue, 3=Stop): ",
                    elapsed / 60, elapsed % 60, total / 60, total % 60));
            System.out.print(sb.toString());
        }
    }
}
