package jdbc;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3307/music_player";
    private static final String USER = "root";
    private static final String PASSWORD = "eric";
    private static Connection connection;


    private DatabaseConnection() {}

    // Singleton pattern for database connection
    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Database connected successfully.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}
