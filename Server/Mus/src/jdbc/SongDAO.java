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

    public void addSong(String name, String artist, String genre, String path, int duration, String coverPath) {
        if (songExists(name, artist)) {
            System.out.println("Song already exists in the database! Skipping insertion.");
            return;
        }

        String sql = "INSERT INTO songs (name, artist, genre, path, duration, cover_path) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.toLowerCase());
            pstmt.setString(2, artist.toLowerCase());
            pstmt.setString(3, genre.toLowerCase());
            pstmt.setString(4, path);
            pstmt.setInt(5, duration);
            pstmt.setString(6, coverPath);
            pstmt.executeUpdate();
            System.out.println("Song added successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean songExists(String name, String artist) {
        String sql = "SELECT COUNT(*) FROM songs WHERE LOWER(name) = LOWER(?) AND LOWER(artist) = LOWER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, artist);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }



    public static String getSongPath(String songTitle) {
        // Correct base path with double backslashes for Windows
        String basePath = "Mus\\src\\songs\\";

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

    public List<String> searchSong(String keyword) {
        String sql = "SELECT name FROM songs WHERE LOWER(name) LIKE LOWER(?)";
        List<String> songList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword.toLowerCase() + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                songList.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return songList;
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

    public void deleteSong(String name) {
        String sql = "DELETE FROM songs WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.toLowerCase());
            int rowsDeleted = pstmt.executeUpdate();
            System.out.println(rowsDeleted > 0 ? "Song deleted successfully." : "Song not found.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    public List<String> searchSongByGenre(String genre) {
        String sql = "SELECT name FROM songs WHERE LOWER(genre) = LOWER(?)";
        List<String> songList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, genre.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                songList.add(rs.getString("name"));
            }
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


    public int getTotalSongs() {
        String sql = "SELECT COUNT(*) AS total FROM songs";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> searchSongByArtist(String artist) {
        String sql = "SELECT name FROM songs WHERE LOWER(artist) = LOWER(?)";
        List<String> songList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, artist.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                songList.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return songList;
    }
}
