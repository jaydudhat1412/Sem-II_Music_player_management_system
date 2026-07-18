import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    // --- User Queries ---
    public boolean registerUser(String username, String password, String name, int age, String question, String answer) throws DatabaseConnectionException {
        String sql = "INSERT INTO users (username, password, name, age, security_question, security_answer) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, name);
            ps.setInt(4, age);
            ps.setString(5, question);
            ps.setString(6, answer);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("\n[ERROR] Username already exists! Please choose another.");
                return false;
            }
            throw new DatabaseConnectionException("Database error during registration", e);
        }
    }

    public User loginUser(String username, String password) throws DatabaseConnectionException, InvalidLoginException {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"),
                            rs.getString("name"), rs.getInt("age"), rs.getString("security_question"), rs.getString("security_answer"));
                    createUserSpecificTable(username);
                    return user;
                }
                throw new InvalidLoginException("Invalid username or password.");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database error during login", e);
        }
    }

    public String getSecurityQuestion(String username) throws DatabaseConnectionException {
        String sql = "SELECT security_question FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("security_question");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching security question", e);
        }
        return null;
    }

    public boolean resetPassword(String username, String answer, String newPassword) throws DatabaseConnectionException {
        String checkSql = "SELECT security_answer FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setString(1, username);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next() && rs.getString("security_answer").equalsIgnoreCase(answer.trim())) {
                    String updateSql = "UPDATE users SET password = ? WHERE username = ?";
                    try (PreparedStatement psUp = conn.prepareStatement(updateSql)) {
                        psUp.setString(1, newPassword);
                        psUp.setString(2, username);
                        return psUp.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error resetting password", e);
        }
        return false;
    }

    public boolean updateProfile(int id, String name, int age, String password) throws DatabaseConnectionException {
        String sql = "UPDATE users SET name = ?, age = ?, password = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, age);
            ps.setString(3, password);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error updating profile", e);
        }
    }

    public boolean deleteUserAccount(int id) throws DatabaseConnectionException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error deleting account", e);
        }
    }

    public List<User> getAllUsers() throws DatabaseConnectionException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new User(rs.getInt("id"), rs.getString("username"), "", rs.getString("name"),
                        rs.getInt("age"), rs.getString("security_question"), rs.getString("security_answer")));
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error getting users", e);
        }
        return list;
    }

    // --- Admin Queries ---
    public Admin loginAdmin(String username, String password) throws DatabaseConnectionException, InvalidLoginException {
        String sql = "SELECT * FROM admins WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Admin(rs.getInt("id"), rs.getString("username"), rs.getString("name"));
                }
                throw new InvalidLoginException("Invalid admin credentials.");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database error during admin login", e);
        }
    }

    // --- Song catalog Queries ---
    public List<Song> getAllSongs() throws DatabaseConnectionException {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.id, s.title, ar.name AS artist_name, al.title AS album_title, " +
                "s.genre, s.language, s.duration, s.release_year, s.play_count " +
                "FROM songs s " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "LEFT JOIN albums al ON s.album_id = al.id " +
                "ORDER BY s.title";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                songs.add(new Song(rs.getInt("id"), rs.getString("title"), rs.getString("artist_name"),
                        rs.getString("album_title"), rs.getString("genre"), rs.getString("language"),
                        rs.getInt("duration"), rs.getInt("release_year"), rs.getInt("play_count")));
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error retrieving songs", e);
        }
        return songs;
    }

    public boolean addSong(String title, String artist, String album, String genre, String language, int duration, int year) throws DatabaseConnectionException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int artistId = getOrCreateArtist(conn, artist);
                int albumId = getOrCreateAlbum(conn, album, artistId);
                String sql = "INSERT INTO songs (title, artist_id, album_id, genre, language, duration, release_year) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, title);
                    ps.setInt(2, artistId);
                    ps.setInt(3, albumId);
                    ps.setString(4, genre);
                    ps.setString(5, language);
                    ps.setInt(6, duration);
                    ps.setInt(7, year);
                    if (ps.executeUpdate() > 0) {
                        conn.commit();
                        return true;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error adding song", e);
        }
        return false;
    }

    public boolean updateSong(int id, String title, String artist, String album, String genre, String language, int duration, int year) throws DatabaseConnectionException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int artistId = getOrCreateArtist(conn, artist);
                int albumId = getOrCreateAlbum(conn, album, artistId);
                String sql = "UPDATE songs SET title = ?, artist_id = ?, album_id = ?, genre = ?, language = ?, duration = ?, release_year = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, title);
                    ps.setInt(2, artistId);
                    ps.setInt(3, albumId);
                    ps.setString(4, genre);
                    ps.setString(5, language);
                    ps.setInt(6, duration);
                    ps.setInt(7, year);
                    ps.setInt(8, id);
                    if (ps.executeUpdate() > 0) {
                        conn.commit();
                        return true;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error updating song", e);
        }
        return false;
    }

    public boolean deleteSong(int id) throws DatabaseConnectionException {
        String sql = "DELETE FROM songs WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error deleting song", e);
        }
    }

    public void incrementPlayCount(int songId) {
        String sql = "UPDATE songs SET play_count = play_count + 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, songId);
            ps.executeUpdate();
        } catch (Exception e) {
            // Suppress error
        }
    }

    private int getOrCreateArtist(Connection conn, String name) throws SQLException {
        String sql = "SELECT id FROM artists WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        String insert = "INSERT INTO artists (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to resolve artist.");
    }

    private int getOrCreateAlbum(Connection conn, String title, int artistId) throws SQLException {
        String sql = "SELECT id FROM albums WHERE title = ? AND artist_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title.trim());
            ps.setInt(2, artistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        String insert = "INSERT INTO albums (title, artist_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title.trim());
            ps.setInt(2, artistId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to resolve album.");
    }

    // --- Playlist Queries ---
    public boolean createPlaylist(int userId, String name) throws DatabaseConnectionException {
        String sql = "INSERT INTO playlists (user_id, name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error creating playlist", e);
        }
    }

    public boolean deletePlaylist(int playlistId) throws DatabaseConnectionException {
        String sql = "DELETE FROM playlists WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error deleting playlist", e);
        }
    }

    public boolean renamePlaylist(int playlistId, String newName) throws DatabaseConnectionException {
        String sql = "UPDATE playlists SET name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, playlistId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error renaming playlist", e);
        }
    }

    public boolean addSongToPlaylist(int playlistId, int songId) throws DatabaseConnectionException, DuplicateSongException {
        String checkSql = "SELECT 1 FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setInt(1, playlistId);
            psCheck.setInt(2, songId);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) throw new DuplicateSongException("This song is already present in this playlist.");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error checking duplicates", e);
        }

        String sql = "INSERT INTO playlist_songs (playlist_id, song_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error adding song to playlist", e);
        }
    }

    public boolean removeSongFromPlaylist(int playlistId, int songId) throws DatabaseConnectionException {
        String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error removing song from playlist", e);
        }
    }

    public List<Playlist> getUserPlaylists(int userId) throws DatabaseConnectionException {
        List<Playlist> list = new ArrayList<>();
        String sql = "SELECT * FROM playlists WHERE user_id = ? ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Playlist p = new Playlist(rs.getInt("id"), rs.getString("name"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching user playlists", e);
        }

        // Fetch songs for each playlist after the connection for user playlists is closed
        for (Playlist p : list) {
            p.setSongs(getPlaylistSongs(p.getId()));
        }
        return list;
    }

    public List<Song> getPlaylistSongs(int playlistId) throws DatabaseConnectionException {
        List<Song> list = new ArrayList<>();
        String sql = "SELECT s.id, s.title, ar.name AS artist_name, al.title AS album_title, " +
                "s.genre, s.language, s.duration, s.release_year, s.play_count " +
                "FROM playlist_songs ps " +
                "INNER JOIN songs s ON ps.song_id = s.id " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "LEFT JOIN albums al ON s.album_id = al.id " +
                "WHERE ps.playlist_id = ? " +
                "ORDER BY s.title";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Song(rs.getInt("id"), rs.getString("title"), rs.getString("artist_name"),
                            rs.getString("album_title"), rs.getString("genre"), rs.getString("language"),
                            rs.getInt("duration"), rs.getInt("release_year"), rs.getInt("play_count")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching playlist songs", e);
        }
        return list;
    }

    // --- Favorites Queries ---
    public boolean addFavorite(int userId, int songId) throws DatabaseConnectionException {
        if (isFavorite(userId, songId)) return true;
        String sql = "INSERT INTO favorites (user_id, song_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error adding favorite", e);
        }
    }

    public boolean removeFavorite(int userId, int songId) throws DatabaseConnectionException {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND song_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error removing favorite", e);
        }
    }

    public List<Song> getFavorites(int userId) throws DatabaseConnectionException {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.id, s.title, ar.name AS artist_name, al.title AS album_title, " +
                "s.genre, s.language, s.duration, s.release_year, s.play_count " +
                "FROM favorites f " +
                "INNER JOIN songs s ON f.song_id = s.id " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "LEFT JOIN albums al ON s.album_id = al.id " +
                "WHERE f.user_id = ? " +
                "ORDER BY s.title";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(new Song(rs.getInt("id"), rs.getString("title"), rs.getString("artist_name"),
                            rs.getString("album_title"), rs.getString("genre"), rs.getString("language"),
                            rs.getInt("duration"), rs.getInt("release_year"), rs.getInt("play_count")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching favorites", e);
        }
        return songs;
    }

    public boolean isFavorite(int userId, int songId) throws DatabaseConnectionException {
        String sql = "SELECT 1 FROM favorites WHERE user_id = ? AND song_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error checking favorite", e);
        }
    }

    // --- History Queries ---
    public void addHistory(int userId, int songId) {
        String sql = "INSERT INTO history (user_id, song_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            ps.executeUpdate();
        } catch (Exception e) {
            // Ignore
        }
    }

    public List<String> getListeningHistory(int userId) throws DatabaseConnectionException {
        List<String> history = new ArrayList<>();
        String sql = "SELECT s.title, ar.name AS artist_name, h.played_at " +
                "FROM history h " +
                "INNER JOIN songs s ON h.song_id = s.id " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "WHERE h.user_id = ? " +
                "ORDER BY h.played_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    String artist = rs.getString("artist_name");
                    Timestamp ts = rs.getTimestamp("played_at");
                    String playedTime = ts != null ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
                    history.add(String.format("Played: %s by %s at [%s]", title, artist, playedTime));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error loading history", e);
        }
        return history;
    }

    public List<Song> getRecentlyPlayed(int userId, int limit) throws DatabaseConnectionException {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT DISTINCT s.id, s.title, ar.name AS artist_name, al.title AS album_title, " +
                "s.genre, s.language, s.duration, s.release_year, s.play_count " +
                "FROM history h " +
                "INNER JOIN songs s ON h.song_id = s.id " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "LEFT JOIN albums al ON s.album_id = al.id " +
                "WHERE h.user_id = ? " +
                "ORDER BY h.id DESC " +
                "LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(new Song(rs.getInt("id"), rs.getString("title"), rs.getString("artist_name"),
                            rs.getString("album_title"), rs.getString("genre"), rs.getString("language"),
                            rs.getInt("duration"), rs.getInt("release_year"), rs.getInt("play_count")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching recently played", e);
        }
        return songs;
    }

    public void createUserSpecificTable(String username) throws DatabaseConnectionException {
        String cleanUsername = username.replaceAll("[^a-zA-Z0-9_]", "");
        if (cleanUsername.isEmpty()) {
            throw new DatabaseConnectionException("Invalid username for table creation");
        }
        String sql = "CREATE TABLE IF NOT EXISTS `" + cleanUsername + "` ("
                   + "id INT AUTO_INCREMENT PRIMARY KEY, "
                   + "data_type VARCHAR(50) NOT NULL, "
                   + "name_or_title VARCHAR(150) NOT NULL, "
                   + "details VARCHAR(255) NOT NULL, "
                   + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                   + ")";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error creating user specific table: " + cleanUsername, e);
        }
    }

    public void logUserActivity(String username, String activityType, String details) {
        String cleanUsername = username.replaceAll("[^a-zA-Z0-9_]", "");
        if (cleanUsername.isEmpty()) return;
        String sql = "INSERT INTO `" + cleanUsername + "` (data_type, name_or_title, details) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "ACTIVITY");
            ps.setString(2, activityType);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("[WARNING] Failed to log activity to user table: " + e.getMessage());
        }
    }

    public void syncUserTable(int userId, String username) {
        String cleanUsername = username.replaceAll("[^a-zA-Z0-9_]", "");
        if (cleanUsername.isEmpty()) return;
        
        try {
            // First, make sure the table exists
            createUserSpecificTable(username);
            
            Connection conn = DatabaseConnection.getConnection();
            
            // Delete all existing entries that are NOT activity logs (or sync everything)
            String deleteSql = "DELETE FROM `" + cleanUsername + "` WHERE data_type IN ('PLAYLIST', 'FAVORITE', 'TOP_5_PLAYED')";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(deleteSql);
            }
            
            // 1. Playlists
            String playlistSql = "SELECT id, name FROM playlists WHERE user_id = ?";
            List<Integer> playlistIds = new ArrayList<>();
            List<String> playlistNames = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(playlistSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        playlistIds.add(rs.getInt("id"));
                        playlistNames.add(rs.getString("name"));
                    }
                }
            }
            
            String insertSql = "INSERT INTO `" + cleanUsername + "` (data_type, name_or_title, details) VALUES (?, ?, ?)";
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < playlistIds.size(); i++) {
                    int pId = playlistIds.get(i);
                    String pName = playlistNames.get(i);
                    // Count songs in playlist
                    int songCount = 0;
                    String countSql = "SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = ?";
                    try (PreparedStatement psCount = conn.prepareStatement(countSql)) {
                        psCount.setInt(1, pId);
                        try (ResultSet rs = psCount.executeQuery()) {
                            if (rs.next()) songCount = rs.getInt(1);
                        }
                    }
                    psInsert.setString(1, "PLAYLIST");
                    psInsert.setString(2, pName);
                    psInsert.setString(3, "Playlist ID: " + pId + " | Songs: " + songCount);
                    psInsert.executeUpdate();
                }
            }
            
            // 2. Favorite Songs
            String favSql = "SELECT s.id, s.title, ar.name AS artist_name FROM favorites f "
                          + "INNER JOIN songs s ON f.song_id = s.id "
                          + "LEFT JOIN artists ar ON s.artist_id = ar.id "
                          + "WHERE f.user_id = ?";
            List<Integer> favIds = new ArrayList<>();
            List<String> favNames = new ArrayList<>();
            List<String> favArtists = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(favSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        favIds.add(rs.getInt("id"));
                        favNames.add(rs.getString("title"));
                        favArtists.add(rs.getString("artist_name"));
                    }
                }
            }
            
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < favIds.size(); i++) {
                    psInsert.setString(1, "FAVORITE");
                    psInsert.setString(2, favNames.get(i));
                    psInsert.setString(3, "Song ID: " + favIds.get(i) + " | Artist: " + favArtists.get(i));
                    psInsert.executeUpdate();
                }
            }
            
            // 3. Top 5 Most Played Songs (based on user's history)
            String topSql = "SELECT s.id, s.title, ar.name AS artist_name, COUNT(h.id) AS play_count FROM history h "
                          + "INNER JOIN songs s ON h.song_id = s.id "
                          + "LEFT JOIN artists ar ON s.artist_id = ar.id "
                          + "WHERE h.user_id = ? "
                          + "GROUP BY s.id, s.title, ar.name "
                          + "ORDER BY play_count DESC "
                          + "LIMIT 5";
            List<Integer> topIds = new ArrayList<>();
            List<String> topNames = new ArrayList<>();
            List<String> topArtists = new ArrayList<>();
            List<Integer> topCounts = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(topSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        topIds.add(rs.getInt("id"));
                        topNames.add(rs.getString("title"));
                        topArtists.add(rs.getString("artist_name"));
                        topCounts.add(rs.getInt("play_count"));
                    }
                }
            }
            
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < topIds.size(); i++) {
                    psInsert.setString(1, "TOP_5_PLAYED");
                    psInsert.setString(2, topNames.get(i));
                    psInsert.setString(3, "Song ID: " + topIds.get(i) + " | Artist: " + topArtists.get(i) + " | User Play Count: " + topCounts.get(i));
                    psInsert.executeUpdate();
                }
            }
            
        } catch (Exception e) {
            System.out.println("[WARNING] Failed to sync user table: " + e.getMessage());
        }
    }

    public void displayUserSpecificTableData(String username) throws DatabaseConnectionException {
        String cleanUsername = username.replaceAll("[^a-zA-Z0-9_]", "");
        if (cleanUsername.isEmpty()) return;
        String sql = "SELECT * FROM `" + cleanUsername + "` ORDER BY data_type, id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n=============================================");
            System.out.println("      USER SPECIFIC DATABASE TABLE: " + cleanUsername.toUpperCase());
            System.out.println("=============================================");
            boolean empty = true;
            String currentType = "";
            while (rs.next()) {
                empty = false;
                String dataType = rs.getString("data_type");
                String title = rs.getString("name_or_title");
                String details = rs.getString("details");
                Timestamp ts = rs.getTimestamp("timestamp");
                String timeStr = ts != null ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
                
                if (!dataType.equals(currentType)) {
                    currentType = dataType;
                    System.out.println("\n--- Category: [" + currentType + "] ---");
                }
                if (dataType.equals("ACTIVITY")) {
                    System.out.println("  [" + timeStr + "] " + title + ": " + details);
                } else {
                    System.out.println("  - " + title + " (" + details + ")");
                }
            }
            if (empty) {
                System.out.println("  (No data stored in your specific table yet.)");
            }
            System.out.println("=============================================");
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error reading user specific table: " + cleanUsername, e);
        }
    }
}
