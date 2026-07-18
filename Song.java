public class Song implements Comparable<Song> {
    private int id;
    private String title;
    private String artistName;
    private String albumTitle;
    private String genre;
    private String language;
    private int duration; // in seconds
    private int releaseYear;
    private int playCount;

    public Song(int id, String title, String artistName, String albumTitle, String genre, String language, int duration, int releaseYear, int playCount) {
        this.id = id;
        this.title = title;
        this.artistName = artistName;
        this.albumTitle = albumTitle;
        this.genre = genre;
        this.language = language;
        this.duration = duration;
        this.releaseYear = releaseYear;
        this.playCount = playCount;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public String getGenre() {
        return genre;
    }

    public String getLanguage() {
        return language;
    }

    public int getDuration() {
        return duration;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public int getPlayCount() {
        return playCount;
    }

    @Override
    public int compareTo(Song other) {
        if (other == null) return 1;
        return this.title.compareToIgnoreCase(other.title);
    }

    @Override
    public String toString() {
        int min = duration / 60;
        int sec = duration % 60;
        return String.format("%s - %s [Album: %s | Genre: %s | Lang: %s | Duration: %d:%02d | Play Count: %d | Year: %d]",
                title, artistName, albumTitle, genre, language, min, sec, playCount, releaseYear);
    }
}
