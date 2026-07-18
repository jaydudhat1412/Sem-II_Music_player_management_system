-- ==========================================
-- MUSIC PLAYER MANAGEMENT SYSTEM - DATABASE
-- ==========================================

CREATE DATABASE IF NOT EXISTS music_player_db;
USE music_player_db;

-- 1. Artists Table
CREATE TABLE IF NOT EXISTS artists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 2. Albums Table
CREATE TABLE IF NOT EXISTS albums (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    artist_id INT,
    FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE CASCADE
);

-- 3. Songs Table
CREATE TABLE IF NOT EXISTS songs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    artist_id INT,
    album_id INT,
    genre VARCHAR(50) NOT NULL,
    language VARC
    
    HAR(50) NOT NULL,
    duration INT NOT NULL, -- in seconds
    release_year INT NOT NULL,
    play_count INT DEFAULT 0,
    FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE SET NULL,
    FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE SET NULL
);

-- 4. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    age INT,
    security_question VARCHAR(150) NOT NULL,
    security_answer VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Admins Table
CREATE TABLE IF NOT EXISTS admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL
);

-- 6. Playlists Table
CREATE TABLE IF NOT EXISTS playlists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 7. PlaylistSongs Table (Junction Table)
CREATE TABLE IF NOT EXISTS playlist_songs (
    playlist_id INT NOT NULL,
    song_id INT NOT NULL,
    PRIMARY KEY (playlist_id, song_id),
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

-- 8. Favorites Table (Junction Table)
CREATE TABLE IF NOT EXISTS favorites (
    user_id INT NOT NULL,
    song_id INT NOT NULL,
    PRIMARY KEY (user_id, song_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

-- 9. History Table
CREATE TABLE IF NOT EXISTS history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    song_id INT NOT NULL,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

-- ==========================================
-- DBMS TRIGGERS & LOGS
-- ==========================================

-- 10. System Logs Table
CREATE TABLE IF NOT EXISTS system_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trigger: Log new user registrations
DELIMITER //
CREATE TRIGGER after_user_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO system_logs (action_type, description)
    VALUES ('USER_REGISTRATION', CONCAT('New user registered: ', NEW.username));
END;
//
DELIMITER ;

-- ==========================================
-- INSERT SAMPLE DATA
-- ==========================================

-- Insert Admins
INSERT INTO admins (username, password, name) 
VALUES ('admin', 'admin123', 'System Administrator')
ON DUPLICATE KEY UPDATE name=name;

-- Insert Users
INSERT INTO users (username, password, name, age, security_question, security_answer)
VALUES 
('john_doe', 'password123', 'John Doe', 20, 'What is your favorite color?', 'blue'),
('jane_smith', 'pass456', 'Jane Smith', 22, 'What is your mother''s maiden name?', 'taylor')
ON DUPLICATE KEY UPDATE name=name;

-- Insert Artists
INSERT INTO artists (name) VALUES 
('The Beatles'),
('A.R. Rahman'),
('Taylor Swift'),
('Ed Sheeran'),
('Diljit Dosanjh')
ON DUPLICATE KEY UPDATE name=name;

-- Insert Albums
-- Fetching artist_id dynamically is safer, but since this is a clean script we can do simple queries
INSERT INTO albums (title, artist_id) VALUES 
('Abbey Road', (SELECT id FROM artists WHERE name = 'The Beatles')),
('Rockstar', (SELECT id FROM artists WHERE name = 'A.R. Rahman')),
('1989', (SELECT id FROM artists WHERE name = 'Taylor Swift')),
('Divide', (SELECT id FROM artists WHERE name = 'Ed Sheeran')),
('G.O.A.T.', (SELECT id FROM artists WHERE name = 'Diljit Dosanjh'))
ON DUPLICATE KEY UPDATE title=title;

-- Insert Songs
INSERT INTO songs (title, artist_id, album_id, genre, language, duration, release_year, play_count) VALUES
('Let It Be', 
 (SELECT id FROM artists WHERE name = 'The Beatles'), 
 (SELECT id FROM albums WHERE title = 'Abbey Road'), 
 'Rock', 'English', 230, 1970, 15),

('Here Comes the Sun', 
 (SELECT id FROM artists WHERE name = 'The Beatles'), 
 (SELECT id FROM albums WHERE title = 'Abbey Road'), 
 'Rock', 'English', 185, 1969, 25),

('Kun Faya Kun', 
 (SELECT id FROM artists WHERE name = 'A.R. Rahman'), 
 (SELECT id FROM albums WHERE title = 'Rockstar'), 
 'Sufi', 'Hindi', 470, 2011, 50),

('Nadaan Parindey', 
 (SELECT id FROM artists WHERE name = 'A.R. Rahman'), 
 (SELECT id FROM albums WHERE title = 'Rockstar'), 
 'Rock', 'Hindi', 386, 2011, 35),

('Blank Space', 
 (SELECT id FROM artists WHERE name = 'Taylor Swift'), 
 (SELECT id FROM albums WHERE title = '1989'), 
 'Pop', 'English', 231, 2014, 42),

('Shake It Off', 
 (SELECT id FROM artists WHERE name = 'Taylor Swift'), 
 (SELECT id FROM albums WHERE title = '1989'), 
 'Pop', 'English', 219, 2014, 30),

('Shape of You', 
 (SELECT id FROM artists WHERE name = 'Ed Sheeran'), 
 (SELECT id FROM albums WHERE title = 'Divide'), 
 'Pop', 'English', 233, 2017, 60),

('Perfect', 
 (SELECT id FROM artists WHERE name = 'Ed Sheeran'), 
 (SELECT id FROM albums WHERE title = 'Divide'), 
 'Romance', 'English', 263, 2017, 55),

('G.O.A.T.', 
 (SELECT id FROM artists WHERE name = 'Diljit Dosanjh'), 
 (SELECT id FROM albums WHERE title = 'G.O.A.T.'), 
 'Punjabi Pop', 'Punjabi', 223, 2020, 40),

('Clash', 
 (SELECT id FROM artists WHERE name = 'Diljit Dosanjh'), 
 (SELECT id FROM albums WHERE title = 'G.O.A.T.'), 
 'Punjabi Pop', 'Punjabi', 176, 2020, 28);
