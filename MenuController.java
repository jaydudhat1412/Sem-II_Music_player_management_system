import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class MenuController {
    private final Scanner scanner = new Scanner(System.in);
    private final DatabaseHelper dbHelper = new DatabaseHelper();
    private final PlayQueue playQueue = new PlayQueue();
    private final RecentStack recentStack = new RecentStack();
    private final MusicPlayerService playerService = new MusicPlayerService();
    private final ReportGenerator reportGenerator = new ReportGenerator();

    private User currentUser = null;
    private Admin currentAdmin = null;

    public void startApp() throws Exception {
        LoggerUtility.logInfo("Music Player Application Started.");
        boolean exit = false;
        while (!exit) {
            if (currentUser == null && currentAdmin == null) {
                printPreLoginMenu();
                String choice = readInput("Choice: ");
                switch (choice) {
                    case "1":
                        handleUserRegistration();
                        break;
                    case "2":
                        handleUserLogin();
                        break;
                    case "3":
                        exit = true;
                        playerService.stop();
                        System.out.println("\nThank you for using Music Player Management System. Good bye!");
                        break;
                    default:
                        System.out.println("[ERROR] Invalid choice!");
                }
            } else if (currentAdmin != null) {
                handleAdminMenu();
            } else {
                printMainMenu();
                String choice = readInput("Enter your choice: ");
                if (!playerService.isPlaying() && choice.equals("8")) {
                    System.out.println("[INFO] Logging out...");
                    if (currentUser != null) {
                        dbHelper.logUserActivity(currentUser.getUsername(), "LOGOUT", "User logged out.");
                    }
                    currentUser = null;
                    currentAdmin = null;
                    playQueue.clear();
                } else if (playerService.isPlaying() && choice.equals("9")) {
                    System.out.println("[INFO] Logging out...");
                    if (currentUser != null) {
                        dbHelper.logUserActivity(currentUser.getUsername(), "LOGOUT", "User logged out.");
                    }
                    currentUser = null;
                    currentAdmin = null;
                    playQueue.clear();
                } else {
                    switch (choice) {
                        case "1":
                            handleUserProfileMenu();
                            break;
                        case "2":
                            handlePlaylistMenu();
                            break;
                        case "3":
                            handleSongsMenu();
                            break;
                        case "4":
                            handleQueueMenu();
                            break;
                        case "5":
                            handleFavoritesMenu();
                            break;
                        case "6":
                            handleHistoryMenu();
                            break;
                        case "7":
                            handleReportsMenu();
                            break;
                        case "8":
                            if (playerService.isPlaying()) {
                                playerService.stop();
                                if (currentUser != null) {
                                    dbHelper.logUserActivity(currentUser.getUsername(), "STOP_PLAYBACK", "Stopped playback.");
                                }
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            }
        }
    }

    private void printPreLoginMenu() {
        System.out.println("\n=============================================");
        System.out.println("        MUSIC PLAYER MANAGEMENT SYSTEM        ");
        System.out.println("=============================================");
        System.out.println("  1. Register");
        System.out.println("  2. Login");
        System.out.println("  3. Exit");
        System.out.println("=============================================");
    }

    private void printMainMenu() {
        System.out.println("\n=============================================");
        System.out.println("        MUSIC PLAYER MANAGEMENT SYSTEM        ");
        System.out.println("=============================================");
        if (currentUser != null) {
            System.out.println("  [Logged in as User: " + currentUser.getName() + " (" + currentUser.getUsername() + ")]");
        } else if (currentAdmin != null) {
            System.out.println("  [Logged in as Admin: " + currentAdmin.getName() + "]");
        }
        System.out.println("---------------------------------------------");
        System.out.println("  1. User Profile Menu");
        System.out.println("  2. Playlists Menu");
        System.out.println("  3. Music Catalog Menu (Play, Search, Sort)");
        System.out.println("  4. Play Queue Menu");
        System.out.println("  5. Favorites Menu");
        System.out.println("  6. Listening History Menu");
        System.out.println("  7. Reports Module");
        if (playerService.isPlaying()) {
            System.out.println("  8. Stop Playback");
            System.out.println("  9. Logout");
        } else {
            System.out.println("  8. Logout");
        }
        System.out.println("=============================================");
    }

    private void handleUserRegistration() {
        System.out.println("\n--- USER REGISTRATION ---");
        String username = readInput("Enter Username: ");
        if (username == null || username.length() < 3) {
            System.out.println("[ERROR] Username must be at least 3 characters long.");
            return;
        }
        if (!Character.isLetter(username.charAt(0))) {
            System.out.println("[ERROR] Username must start with an alphabet character.");
            return;
        }
        String password = readAndValidatePassword();
        String name = readInput("Enter Full Name: ");
        int age = readIntInput("Enter Age: ");
        if (age <= 10) {
            System.out.println("[ERROR] Age must be greater than 10 to register.");
            return;
        }
        String question = readInput("Security Question: ");
        String answer = readInput("Answer: ");
        try {
            if (dbHelper.registerUser(username, password, name, age, question, answer)) {
                System.out.println("[SUCCESS] Registration completed! Logging you in automatically...");
                dbHelper.createUserSpecificTable(username);
                dbHelper.logUserActivity(username, "REGISTRATION", "User registered successfully with name: " + name + ", age: " + age);
                
                // Auto-login logic
                try {
                    currentUser = dbHelper.loginUser(username, password);
                    currentAdmin = null;
                    System.out.println("[SUCCESS] Welcome, " + currentUser.getName() + "!");
                    dbHelper.logUserActivity(currentUser.getUsername(), "LOGIN", "Successfully logged into the system after registration.");
                    dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                    recentStack.clear(); // Fresh user, no history to load yet
                } catch (Exception loginEx) {
                    System.out.println("[ERROR] Auto-login failed: " + loginEx.getMessage() + ". Please login manually.");
                }
            } else {
                System.out.println("[ERROR] Registration failed. Username might exist.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void handleUserLogin() {
        System.out.println("\n--- USER LOGIN ---");
        String username = readInput("Username: ");
        String password = readPasswordInput("Password: ");
        try {
            currentAdmin = dbHelper.loginAdmin(username, password);
            currentUser = null;
            System.out.println("[SUCCESS] Welcome Admin, " + currentAdmin.getName() + "!");
            return;
        } catch (InvalidLoginException e) {
            // Not an admin, proceed to try user
        } catch (Exception e) {
            System.out.println("[ERROR] Database error: " + e.getMessage());
            return;
        }

        try {
            currentUser = dbHelper.loginUser(username, password);
            currentAdmin = null;
            System.out.println("[SUCCESS] Welcome, " + currentUser.getName() + "!");
            dbHelper.logUserActivity(currentUser.getUsername(), "LOGIN", "Successfully logged into the system.");
            dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
            List<Song> hist = dbHelper.getRecentlyPlayed(currentUser.getId(), 5);
            recentStack.clear();
            for (int i = hist.size() - 1; i >= 0; i--) recentStack.push(hist.get(i));
        } catch (Exception e) {
            System.out.println("[ERROR] Login failed: " + e.getMessage());
            System.out.print("Forgot password? Recover using security question? (y/n): ");
            if (readInput("").equalsIgnoreCase("y")) {
                handleForgotPassword(username);
            }
        }
    }

    private void handleForgotPassword(String username) {
        try {
            String question = dbHelper.getSecurityQuestion(username);
            if (question == null) {
                System.out.println("[ERROR] User does not exist or has no security question.");
                return;
            }
            System.out.println("Security Question: " + question);
            String answer = readInput("Your Answer: ");
            String newPassword = readPasswordInput("Enter New Password: ");
            if (dbHelper.resetPassword(username, answer, newPassword)) {
                System.out.println("[SUCCESS] Password reset successfully. Log in using your new password.");
            } else {
                System.out.println("[ERROR] Recovery failed. Incorrect answer.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private boolean checkUserSession() {
        if (currentUser == null) {
            System.out.println("\n[ACCESS DENIED] Please login as User first.");
            return false;
        }
        return true;
    }

    private void handleUserProfileMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- USER PROFILE MENU ---");
            System.out.println("1. View Profile Details");
            System.out.println("2. Update Profile Details");
            System.out.println("3. Delete Account");
            System.out.println("4. Logout");
            System.out.println("5. View My Personal Database Table Content");
            System.out.println("6. Back");
            String choice = readInput("Choice: ");
            switch (choice) {
                case "1":
                    System.out.println("\nProfile Information:");
                    System.out.println("Username: " + currentUser.getUsername());
                    System.out.println("Name: " + currentUser.getName());
                    System.out.println("Age: " + currentUser.getAge());
                    break;
                case "2":
                    System.out.println("\nUpdate Details (Leave empty to keep current):");
                    String newName = readInput("Enter new name [" + currentUser.getName() + "]: ");
                    if (newName.trim().isEmpty()) newName = currentUser.getName();
                    String ageStr = readInput("Enter new age [" + currentUser.getAge() + "]: ");
                    int newAge = ageStr.trim().isEmpty() ? currentUser.getAge() : Integer.parseInt(ageStr);
                    String newPass = readPasswordInput("Enter new password: ");
                    if (newPass.trim().isEmpty()) newPass = currentUser.getPassword();
                    try {
                        if (dbHelper.updateProfile(currentUser.getId(), newName, newAge, newPass)) {
                            currentUser.setName(newName);
                            currentUser.setAge(newAge);
                            currentUser.setPassword(newPass);
                            System.out.println("[SUCCESS] Profile updated successfully!");
                            dbHelper.logUserActivity(currentUser.getUsername(), "UPDATE_PROFILE", "Updated profile name to: " + newName + ", age: " + newAge);
                        }
                    } catch (Exception e) {
                        System.out.println("[ERROR] Update failed: " + e.getMessage());
                    }
                    break;
                case "3":
                    System.out.print("[WARNING] Are you sure you want to delete your account? (y/n): ");
                    if (readInput("").equalsIgnoreCase("y")) {
                        try {
                            String uname = currentUser.getUsername();
                            if (dbHelper.deleteUserAccount(currentUser.getId())) {
                                System.out.println("[SUCCESS] Account deleted.");
                                dbHelper.logUserActivity(uname, "DELETE_ACCOUNT", "Account deleted.");
                                currentUser = null;
                                playQueue.clear();
                                back = true;
                            }
                        } catch (Exception e) {
                            System.out.println("[ERROR] Delete failed: " + e.getMessage());
                        }
                    }
                    break;
                case "4":
                    System.out.println("Logged out successfully.");
                    dbHelper.logUserActivity(currentUser.getUsername(), "LOGOUT", "Successfully logged out.");
                    currentUser = null;
                    playQueue.clear();
                    back = true;
                    break;
                case "5":
                    System.out.println("\nRetrieving your personal database table content...");
                    try {
                        dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                        dbHelper.displayUserSpecificTableData(currentUser.getUsername());
                    } catch (Exception e) {
                        System.out.println("[ERROR] Failed to fetch table content: " + e.getMessage());
                    }
                    break;
                case "6":
                    back = true;
                    break;
            }
        }
    }

    private void handlePlaylistMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- PLAYLISTS MENU ---");
            System.out.println("1. Create New Playlist");
            System.out.println("2. Display All My Playlists");
            System.out.println("3. Rename Playlist");
            System.out.println("4. Add Song to Playlist");
            System.out.println("5. Remove Song from Playlist");
            System.out.println("6. Export Playlist to File");
            System.out.println("7. Delete Playlist");
            if (playerService.isPlaying()) {
                System.out.println("8. Stop Playback");
                System.out.println("9. Back");
            } else {
                System.out.println("8. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("8")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("9")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            String name = readInput("Enter playlist name: ");
                            if (dbHelper.createPlaylist(currentUser.getId(), name)) {
                                System.out.println("[SUCCESS] Playlist created!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "CREATE_PLAYLIST", "Created playlist: " + name);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "2":
                            displayPlaylists();
                            break;
                        case "3":
                            displayPlaylists();
                            int renameId = readIntInput("Enter Playlist ID to rename: ");
                            String newName = readInput("Enter new name: ");
                            if (dbHelper.renamePlaylist(renameId, newName)) {
                                System.out.println("[SUCCESS] Playlist renamed!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "RENAME_PLAYLIST", "Renamed playlist ID: " + renameId + " to: " + newName);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "4":
                            displayPlaylists();
                            int pId = readIntInput("Enter Playlist ID: ");
                            displayCatalog();
                            int sId = readIntInput("Enter Song ID to add: ");
                            if (dbHelper.addSongToPlaylist(pId, sId)) {
                                System.out.println("[SUCCESS] Song added!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "PLAYLIST_ADD_SONG", "Added song ID " + sId + " to playlist ID " + pId);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "5":
                            displayPlaylists();
                            int pIdRem = readIntInput("Enter Playlist ID: ");
                            displayPlaylistSongs(pIdRem);
                            int sIdRem = readIntInput("Enter Song ID to remove: ");
                            if (dbHelper.removeSongFromPlaylist(pIdRem, sIdRem)) {
                                System.out.println("[SUCCESS] Song removed!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "PLAYLIST_REMOVE_SONG", "Removed song ID " + sIdRem + " from playlist ID " + pIdRem);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "6":
                            displayPlaylists();
                            int pIdExp = readIntInput("Enter Playlist ID to export: ");
                            String pName = readInput("Playlist name: ");
                            List<Song> songs = dbHelper.getPlaylistSongs(pIdExp);
                            if (FileExporter.exportPlaylist(pName, songs)) {
                                System.out.println("[SUCCESS] Playlist exported!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "EXPORT_PLAYLIST", "Exported playlist: " + pName);
                            }
                            break;
                        case "7":
                            displayPlaylists();
                            int deleteId = readIntInput("Enter Playlist ID to delete: ");
                            if (dbHelper.deletePlaylist(deleteId)) {
                                System.out.println("[SUCCESS] Playlist deleted!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "DELETE_PLAYLIST", "Deleted playlist ID: " + deleteId);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "8":
                            if (playerService.isPlaying()) {
                                playerService.stop();
                                dbHelper.logUserActivity(currentUser.getUsername(), "STOP_PLAYBACK", "Stopped playback.");
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Operation failed: " + e.getMessage());
            }
        }
    }

    private void displayPlaylists() throws Exception {
        List<Playlist> list = dbHelper.getUserPlaylists(currentUser.getId());
        System.out.println("\nYour Playlists:");
        if (list.isEmpty()) System.out.println("No playlists found.");
        else {
            for (Playlist p : list) System.out.println("ID: " + p.getId() + " | " + p);
        }
    }

    private void displayPlaylistSongs(int pId) throws Exception {
        List<Song> songs = dbHelper.getPlaylistSongs(pId);
        System.out.println("\nPlaylist Songs:");
        if (songs.isEmpty()) System.out.println("Empty playlist.");
        else {
            for (Song s : songs) System.out.println("Song ID: " + s.getId() + " | " + s);
        }
    }

    private void handleSongsMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MUSIC CATALOG MENU ---");
            System.out.println("1. View All Songs (Alphabetical)");
            System.out.println("2. Sort Songs (Merge/Bubble Sort)");
            System.out.println("3. Search Songs");
            System.out.println("4. Play Song (Simulate)");
            if (playerService.isPlaying()) {
                System.out.println("5. Stop Playback");
                System.out.println("6. Back");
            } else {
                System.out.println("5. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("5")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("6")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            displayCatalog();
                            break;
                        case "2":
                            handleSortingSubmenu();
                            break;
                        case "3":
                            handleSearchSubmenu();
                            break;
                        case "4":
                            displayCatalog();
                            int sId = readIntInput("Enter Song ID to Play: ");
                            Song songToPlay = getSongFromCatalog(sId);
                            if (songToPlay != null) {
                                playSong(songToPlay);
                            } else {
                                System.out.println("[ERROR] Invalid Song ID.");
                            }
                            break;
                        case "5":
                            if (playerService.isPlaying()) {
                                playerService.stop();
                                if (currentUser != null) {
                                    dbHelper.logUserActivity(currentUser.getUsername(), "STOP_PLAYBACK", "Stopped playback.");
                                    dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                                }
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private void offerPlayOrQueue(Song song) {
        System.out.println("Options: [1] Play immediately | [2] Add to Play Queue | [3] Add to Favorites | [4] Do nothing");
        String opt = readInput("Enter option: ");
        try {
            if (opt.equals("1")) {
                playSong(song);
            } else if (opt.equals("2")) {
                playQueue.enqueue(song);
                System.out.println("Song queued.");
                dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_ADD", "Queued song: " + song.getTitle());
            } else if (opt.equals("3")) {
                if (dbHelper.addFavorite(currentUser.getId(), song.getId())) {
                    System.out.println("Added to favorites.");
                    dbHelper.logUserActivity(currentUser.getUsername(), "ADD_FAVORITE", "Added song to favorites: " + song.getTitle());
                    dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void displayCatalog() throws Exception {
        List<Song> catalog = dbHelper.getAllSongs();
        Collections.sort(catalog);
        System.out.println("\nAll Songs Catalog:");
        if (catalog.isEmpty()) System.out.println("No songs available.");
        else {
            for (Song s : catalog) System.out.println("ID: " + s.getId() + " | " + s);
        }
    }

    private Song getSongFromCatalog(int id) throws Exception {
        for (Song s : dbHelper.getAllSongs()) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    private void handleSortingSubmenu() throws Exception {
        System.out.println("\nSort Catalog by:");
        System.out.println("1. Duration (Merge Sort)");
        System.out.println("2. Release Year (Bubble Sort)");
        String ch = readInput("Choice: ");
        List<Song> sorted = dbHelper.getAllSongs();
        if (ch.equals("1")) {
            SongSort.mergeSortByDuration(sorted);
            System.out.println("\nSorted by Duration:");
        } else if (ch.equals("2")) {
            SongSort.bubbleSortByReleaseYear(sorted);
            System.out.println("\nSorted by Release Year:");
        } else {
            System.out.println("Invalid choice.");
            return;
        }
        for (Song s : sorted) System.out.println(s);
    }

    private void handleSearchSubmenu() throws Exception {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- SEARCH SONG ---");
            System.out.println("1. Search using Name");
            System.out.println("2. Search using ID");
            System.out.println("3. Back");
            String choice = readInput("Choice: ");
            switch (choice) {
                case "1":
                    String query = readInput("Enter song name to search: ");
                    List<Song> results = SongSearch.linearSearch(dbHelper.getAllSongs(), query, "title");
                    System.out.println("\nSearch Results (" + results.size() + " matching):");
                    if (results.isEmpty()) {
                        System.out.println("No matching songs found.");
                    } else {
                        for (Song s : results) {
                            System.out.println("ID: " + s.getId() + " | " + s);
                        }
                        int selectId = readIntInput("Enter Song ID to select (or 0 to cancel): ");
                        if (selectId > 0) {
                            Song selected = getSongFromCatalog(selectId);
                            if (selected != null) offerPlayOrQueue(selected);
                        }
                    }
                    break;
                case "2":
                    int selectId = readIntInput("Enter Song ID: ");
                    if (selectId > 0) {
                        Song selected = getSongFromCatalog(selectId);
                        if (selected != null) {
                            System.out.println("\n[FOUND] " + selected);
                            offerPlayOrQueue(selected);
                        } else {
                            System.out.println("Song not found.");
                        }
                    }
                    break;
                case "3":
                    back = true;
                    break;
                default:
                    System.out.println("[ERROR] Invalid choice!");
            }
        }
    }

    private void handleQueueMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- PLAY QUEUE MENU ---");
            System.out.println("1. Add Song to Queue");
            System.out.println("2. Play Next (Insert song at front)");
            System.out.println("3. View Current Queue & Upcoming");
            System.out.println("4. Skip to Next Song (Circular Traversal)");
            System.out.println("5. Play Previous Song (Circular Traversal)");
            System.out.println("6. Remove Song from Queue");
            System.out.println("7. Clear Queue");
            System.out.println("8. Shuffle Queue");
            if (playerService.isPlaying()) {
                System.out.println("9. Stop Playback");
                System.out.println("10. Back");
            } else {
                System.out.println("9. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("9")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("10")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            displayCatalog();
                            int addId = readIntInput("Enter Song ID to queue: ");
                            Song addSong = getSongFromCatalog(addId);
                            if (addSong != null) {
                                playQueue.enqueue(addSong);
                                System.out.println("[SUCCESS] Song added to queue.");
                                dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_ADD", "Queued song: " + addSong.getTitle());
                                displayQueueStatus();
                            }
                            break;
                        case "2":
                            displayCatalog();
                            int nextId = readIntInput("Enter Song ID to play next: ");
                            Song nextSong = getSongFromCatalog(nextId);
                            if (nextSong != null) {
                                playQueue.playNext(nextSong);
                                System.out.println("[SUCCESS] Song will play next.");
                                dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_PLAY_NEXT", "Set song to play next: " + nextSong.getTitle());
                                displayQueueStatus();
                            }
                            break;
                        case "3":
                            displayQueueStatus();
                            break;
                        case "4":
                            Song n = playQueue.getNextSong();
                            if (n != null) {
                                playSong(n);
                                displayQueueStatus();
                            } else System.out.println("End of queue reached.");
                            break;
                        case "5":
                            Song prev = playQueue.getPreviousSong();
                            if (prev != null) {
                                playSong(prev);
                                displayQueueStatus();
                            } else System.out.println("Beginning of queue reached.");
                            break;
                        case "6":
                            List<Song> qList = playQueue.getAllSongs();
                            for (int i = 0; i < qList.size(); i++) {
                                System.out.println("[" + i + "] " + qList.get(i));
                            }
                            int remIdx = readIntInput("Enter position index to remove: ");
                            if (playQueue.remove(remIdx)) {
                                System.out.println("[SUCCESS] Removed!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_REMOVE", "Removed song at index: " + remIdx);
                                displayQueueStatus();
                            } else System.out.println("[ERROR] Invalid queue index.");
                            break;
                        case "7":
                            playQueue.clear();
                            System.out.println("Queue cleared.");
                            dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_CLEAR", "Cleared the play queue.");
                            displayQueueStatus();
                            break;
                        case "8":
                            playQueue.shuffle();
                            System.out.println("Queue shuffled.");
                            dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_SHUFFLE", "Shuffled the play queue.");
                            displayQueueStatus();
                            break;
                        case "9":
                            if (playerService.isPlaying()) {
                                playerService.stop();
                                dbHelper.logUserActivity(currentUser.getUsername(), "STOP_PLAYBACK", "Stopped playback.");
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private void handleFavoritesMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- FAVORITES MENU ---");
            System.out.println("1. Add Song to Favorites");
            System.out.println("2. Remove Song from Favorites");
            System.out.println("3. View My Favorites");
            if (playerService.isPlaying()) {
                System.out.println("4. Stop Playback");
                System.out.println("5. Back");
            } else {
                System.out.println("4. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("4")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("5")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            displayCatalog();
                            int favId = readIntInput("Enter Song ID to favorite: ");
                            if (dbHelper.addFavorite(currentUser.getId(), favId)) {
                                System.out.println("[SUCCESS] Song added!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "ADD_FAVORITE", "Added song ID " + favId + " to favorites");
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "2":
                            displayFavorites();
                            int remId = readIntInput("Enter Song ID to unfavorite: ");
                            if (dbHelper.removeFavorite(currentUser.getId(), remId)) {
                                System.out.println("[SUCCESS] Song removed!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "REMOVE_FAVORITE", "Removed song ID " + remId + " from favorites");
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "3":
                            displayFavorites();
                            break;
                        case "4":
                            if (playerService.isPlaying()) {
                                playerService.stop();
                                dbHelper.logUserActivity(currentUser.getUsername(), "STOP_PLAYBACK", "Stopped playback.");
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private void displayFavorites() throws Exception {
        List<Song> favs = dbHelper.getFavorites(currentUser.getId());
        System.out.println("\nYour Favorites:");
        if (favs.isEmpty()) System.out.println("No favorite songs.");
        else {
            for (Song s : favs) System.out.println("ID: " + s.getId() + " | " + s);
        }
    }

    private void handleHistoryMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- LISTENING HISTORY MENU ---");
            System.out.println("1. View Recently Played ");
            System.out.println("2. View Full History Log ");
            System.out.println("3. Export History Log to File");
            if (playerService.isPlaying()) {
                System.out.println("4. Stop Playback");
                System.out.println("5. Back");
            } else {
                System.out.println("4. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("4")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("5")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            System.out.println("\nRecently Played (Memory Stack - last 5 plays):");
                            List<Song> memoryRecent = recentStack.getRecent(5);
                            if (memoryRecent.isEmpty()) System.out.println("No songs played recently in this session.");
                            else {
                                for (Song s : memoryRecent) System.out.println(s);
                            }
                            break;
                        case "2":
                            System.out.println("\nFull Listening History (From Database):");
                            List<String> hist = dbHelper.getListeningHistory(currentUser.getId());
                            if (hist.isEmpty()) System.out.println("No history logs.");
                            else {
                                for (String h : hist) System.out.println(h);
                            }
                            break;
                        case "3":
                            List<String> records = dbHelper.getListeningHistory(currentUser.getId());
                            if (FileExporter.exportListeningHistory(currentUser.getUsername(), records)) {
                                System.out.println("[SUCCESS] History exported to " + currentUser.getUsername() + "_listening_history.txt");
                                dbHelper.logUserActivity(currentUser.getUsername(), "EXPORT_HISTORY", "Exported history log to file");
                            }
                            break;
                        case "4":
                            if (playerService.isPlaying()) {
                                playerService.stop();
                                dbHelper.logUserActivity(currentUser.getUsername(), "STOP_PLAYBACK", "Stopped playback.");
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private void handleAdminMenu() {
        if (currentAdmin == null) {
            System.out.println("\n--- ADMIN PORTAL LOGIN ---");
            String username = readInput("Admin Username: ");
            String password = readPasswordInput("Admin Password: ");
            try {
                currentAdmin = dbHelper.loginAdmin(username, password);
                currentUser = null; // Clear user session
                System.out.println("[SUCCESS] Welcome, Admin " + currentAdmin.getName() + "!");
            } catch (Exception e) {
                System.out.println("[ERROR] Admin authentication failed: " + e.getMessage());
                return;
            }
        }
        boolean back = false;
        while (!back) {
            System.out.println("\n=================================");
            System.out.println("           ADMIN PORTAL          ");
            System.out.println("=================================");
            System.out.println("1. Add Song to Catalog");
            System.out.println("2. Update Song Details");
            System.out.println("3. Delete Song");
            System.out.println("4. View All Registered Users");
            System.out.println("5. View Database Statistics");
            System.out.println("6. Logout Admin");
            System.out.println("7. Back");
            String choice = readInput("Choice: ");
            try {
                switch (choice) {
                    case "1":
                        String title = readInput("Song Title: ");
                        String artist = readInput("Artist: ");
                        String album = readInput("Album: ");
                        String genre = readInput("Genre: ");
                        String lang = readInput("Language: ");
                        int duration = readIntInput("Duration (seconds): ");
                        int year = readIntInput("Release Year: ");
                        if (dbHelper.addSong(title, artist, album, genre, lang, duration, year)) {
                            System.out.println("[SUCCESS] Song successfully added to catalog!");
                        }
                        break;
                    case "2":
                        displayCatalog();
                        int upId = readIntInput("Enter Song ID to update: ");
                        String upTitle = readInput("Song Title: ");
                        String upArtist = readInput("Artist: ");
                        String upAlbum = readInput("Album: ");
                        String upGenre = readInput("Genre: ");
                        String upLang = readInput("Language: ");
                        int upDur = readIntInput("Duration: ");
                        int upYear = readIntInput("Release Year: ");
                        if (dbHelper.updateSong(upId, upTitle, upArtist, upAlbum, upGenre, upLang, upDur, upYear)) {
                            System.out.println("[SUCCESS] Song updated!");
                        }
                        break;
                    case "3":
                        displayCatalog();
                        int delId = readIntInput("Enter Song ID to delete: ");
                        if (dbHelper.deleteSong(delId)) System.out.println("[SUCCESS] Song removed!");
                        break;
                    case "4":
                        System.out.println("\nRegistered Users:");
                        for (User u : dbHelper.getAllUsers()) System.out.println(u);
                        break;
                    case "5":
                        System.out.println("\nDatabase Statistics:");
                        System.out.println("Total Users: " + dbHelper.getAllUsers().size());
                        System.out.println("Total Songs: " + dbHelper.getAllSongs().size());
                        break;
                    case "6":
                        System.out.println("Admin logged out.");
                        currentAdmin = null;
                        back = true;
                        break;
                    case "7":
                        back = true;
                        break;
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private void handleReportsMenu() {
        if (currentUser == null && currentAdmin == null) {
            System.out.println("[ACCESS DENIED] Please login first to view reports.");
            return;
        }
        boolean back = false;
        while (!back) {
            System.out.println("\n--- REPORTS MODULE ---");
            System.out.println("1. Top 10 Most Played Songs");
            System.out.println("2. Favorite Songs Statistics (Most Liked)");
            System.out.println("3. Playlist Statistics (Summary)");
            System.out.println("4. Most Active Users (Admin Only)");
            System.out.println("5. Back");
            String choice = readInput("Choice: ");
            switch (choice) {
                case "1":
                    reportGenerator.displayTop10Songs();
                    break;
                case "2":
                    if (currentUser != null) {
                        try {
                            displayFavorites();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    } else System.out.println("Login as a user to view favorites.");
                    break;
                case "3":
                    reportGenerator.displayPlaylistStats();
                    break;
                case "4":
                    if (currentAdmin != null) reportGenerator.displayActiveUsers();
                    else System.out.println("[ACCESS DENIED] Administrators only.");
                    break;
                case "5":
                    back = true;
                    break;
            }
        }
    }

    private void playSong(Song song) {
        if (song == null) return;
        playerService.play(song, currentUser.getId());
        recentStack.push(song);
        if (currentUser != null) {
            dbHelper.logUserActivity(currentUser.getUsername(), "PLAY_SONG", "Started playing song: " + song.getTitle() + " by " + song.getArtistName());
            dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
        }
        System.out.println("\n=============================================");
        System.out.println("  1. Pause");
        System.out.println("  2. Continue");
        System.out.println("  3. Stop");
        System.out.println("=============================================");
        while (playerService.isPlaying()) {
            String choice = readInput("");
            if (choice.trim().equals("1")) {
                playerService.pause();
            } else if (choice.trim().equals("2")) {
                playerService.resume();
            } else if (choice.trim().equals("3")) {
                playerService.stop();
                if (currentUser != null) {
                    dbHelper.logUserActivity(currentUser.getUsername(), "STOP_PLAYBACK", "Stopped song: " + song.getTitle());
                    dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                }
                break;
            } else {
                if (!playerService.isPlaying()) {
                    break;
                }
                System.out.println("[ERROR] Invalid choice!");
            }
        }
    }

    private void displayQueueStatus() {
        System.out.println("\nPlay Queue Status (Size: " + playQueue.size() + "):");
        Song current = playQueue.getCurrentSong();
        System.out.println("Currently Active: " + (current != null ? current : "None"));
        List<Song> upcoming = playQueue.getUpcomingSongs();
        System.out.println("\nUpcoming Songs in ArrayDeque:");
        if (upcoming.isEmpty()) System.out.println("  No upcoming songs.");
        else {
            int idx = 1;
            for (Song s : upcoming) System.out.println("  " + (idx++) + ". " + s);
        }
    }

    // --- Inputs Helpers ---
    private String readInput(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return "12"; // Graceful default exit choice (Exit option)
        }
        return scanner.nextLine();
    }

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return -1; // Default/sentinel value indicating exit/empty
            }
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid number format! Please enter an integer.");
            }
        }
    }

    private class EraserThread implements Runnable {
        private volatile boolean stop = false;

        public void run() {
            while (!stop) {
                System.out.print("\010*");
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public void stopMasking() {
            this.stop = true;
        }
    }

    private String readPasswordInput(String prompt) {
        System.out.print(prompt);
        EraserThread et = new EraserThread();
        Thread mask = new Thread(et);
        mask.start();

        String password = "";
        try {
            if (scanner.hasNextLine()) {
                password = scanner.nextLine();
            }
        } finally {
            et.stopMasking();
        }
        return password;
    }

    private String readAndValidatePassword() {
        System.out.println("\n--- Password Requirements ---");
        System.out.println("- At least 8 characters long");
        System.out.println("- Contains at least one uppercase letter");
        System.out.println("- Contains at least one number");
        System.out.println("- Contains at least one special character (@, #, $, !, etc.)");
        System.out.println("-----------------------------");

        while (true) {
            String password = readPasswordInput("Enter Password: ");

            if (!isValidPassword(password)) {
                System.out.println("\n[ERROR] Password does not meet the requirements. Please try again.");
                continue;
            }

            String confirm = readPasswordInput("Re-enter Password to Confirm: ");

            if (password.equals(confirm)) {
                return password;
            } else {
                System.out.println("\n[ERROR] Passwords do not match. Please start over.");
            }
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        return hasUpper && hasDigit && hasSpecial;
    }
}
