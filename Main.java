import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// =========================================================================
// MAIN ENTRY POINT
// =========================================================================
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("     MUSIC PLAYER MANAGEMENT SYSTEM INITIALIZING  ");
        System.out.println("=================================================");

        // Test Database connection on startup
        try {
            System.out.println("[INFO] Connecting to MySQL database...");
            DatabaseConnection.getConnection();
            System.out.println("[SUCCESS] Connected to MySQL database successfully.");
            LoggerUtility.logInfo("System initialized and connected to database.");
        } catch (DatabaseConnectionException e) {
            System.out.println("\n=================================================");
            System.out.println("  [WARNING] DATABASE CONNECTION FAILED!");
            System.out.println("=================================================");
            System.out.println("  Please check that:");
            System.out.println("  1. Your MySQL server is running.");
            System.out.println("  2. Database 'music_player_db' exists.");
            System.out.println("  3. Credentials in DatabaseConnection class match your server.");
            System.out.println("    =================================================");
            System.out.println("  Detailed Error: " + e.getMessage());
            System.out.println("=================================================\n");

            // Log the error
            LoggerUtility.logError("System startup database check failed: " + e.getMessage());
        }

        // Initialize and start the menu system controller
        MenuController controller = new MenuController();
        controller.startApp();

        // Close Database connection on exit
        DatabaseConnection.closeConnection();
        System.out.println("[INFO] Database connection closed. Application terminated.");
    }
}
