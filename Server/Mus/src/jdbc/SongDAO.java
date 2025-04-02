package jdbc;
import jdbc.DatabaseConnection;

import java.io.File;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;



public class SongDAO {
    private static Connection conn;
    private Socket clientSocket;

    public SongDAO(Socket socket) {
        this.clientSocket = socket;
    }

    static {
        conn = DatabaseConnection.getConnection();
    }





    public static String getSongPath(String songTitle) {
        // Correct base path with double backslashes for Windows
        String basePath = "Server\\Mus\\src\\songs\\";

        // Prepare the file path correctly
        String filePath = basePath + songTitle.toLowerCase() + ".wav";

        return filePath;
    }


    

    public static String getSongCoverPath(String name) {
        String sql = "SELECT cover_path FROM songs WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("cover_path");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Cover not found";
    }



    public static List<String> getSongDetails(String songTitle) {
        List<String> songDetails = new ArrayList<>();
        try {
            String query = "SELECT path AS file_path, duration, artist, cover_path, name AS title FROM songs WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, songTitle);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                songDetails.add(rs.getString("file_path"));
                songDetails.add(rs.getString("duration"));
                songDetails.add(rs.getString("artist"));
                songDetails.add(rs.getString("cover_path"));
                songDetails.add(rs.getString("title"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return songDetails;
    }


    public List<String> listAllSongs() {
        String sql = "SELECT name,artist,genre,duration FROM songs";
        List<String> songList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                songList.add(rs.getString("name"));
                songList.add(rs.getString("artist"));
                songList.add(rs.getString("genre"));
                songList.add(rs.getString("duration"));
            }
            System.out.println("Total songs fetched: " + songList.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return songList;
    }



    public int getSongDuration(String name) {
        String sql = "SELECT duration FROM songs WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("duration");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }


}
