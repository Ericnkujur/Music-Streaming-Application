import javax.imageio.ImageIO;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme;
import com.formdev.flatlaf.util.ColorFunctions;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.event.ListSelectionListener;


public class MusicPlayerGUI implements ActionListener {

    Color bg = UIManager.getColor("Panel.background");
    Color lighter = ColorFunctions.lighten(bg, 0.05f);  // Slightly lighter
    Color darker = ColorFunctions.darken(bg, 0.05f);    // Slightly darker

    JFrame frame;
    JButton playButton, forwardButton, backwardButton, volumeButton, themeButton;
    ImageIcon playIcon, forwardIcon, backwardIcon, musicIcon, pauseIcon, volumeIcon, lightIcon, darkIcon;
    JSlider playBackSlider, volumeSlider;
    JTable songTable;
    JLabel songTitle, songArtist, timeLabel, durationLabel, musicIconLabel;
    JPanel panel1, panel2;
    DefaultTableModel tableModel;
    JTableHeader header;
    Color iconColor;
    Song song;
    boolean isPlaying = false;
    private String selectedSongPath = "";
    private String selectedSongTitle = "";
    private String selectedSongArtist = "";
    public Timer sliderTimer;
    private AtomicLong currentDurationMicros = new AtomicLong(0);
    private volatile boolean sliderBeingAdjusted = false;
    private boolean isDarkTheme = true;

    public MusicPlayerGUI() {

        iconColor = Color.WHITE;
        // Initialize icons
        playIcon = new ImageIcon(recolorAndScale("Client\\play-solid.png", 55, 75, iconColor));
        forwardIcon = new ImageIcon(recolorAndScale("Client\\forward-solid.png", 75, 75, iconColor));
        backwardIcon = new ImageIcon(recolorAndScale("Client\\backward-solid.png", 75, 75, iconColor));
        musicIcon = new ImageIcon("Client\\musicIcon.jpg"); // No need to recolor
        pauseIcon = new ImageIcon(recolorAndScale("Client\\pause-solid.png", 55, 75, iconColor));
        volumeIcon = new ImageIcon(recolorAndScale("Client\\volume.png", 30, 30, iconColor));
        lightIcon = new ImageIcon(recolorAndScale("Client\\sun.png", 30, 30, iconColor));
        darkIcon = new ImageIcon(recolorAndScale("Client\\moon.png", 30, 30, iconColor));

        // Create frame
        frame = new JFrame("Music Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 620);
        frame.setResizable(false);
        frame.setLayout(null);

        panel1 = new JPanel();
        panel1.setBounds(0, 0, 400, 600);
        panel1.setLayout(null);

        panel2 = new JPanel();
        panel2.setBounds(400, 0, 600, 600);
        panel2.setLayout(new BorderLayout());

        // Create song table
        String[] columnNames = {"Title", "Artist", "Genre", "Duration"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        songTable = new JTable(tableModel);
        songTable.setRowHeight(30);
        songTable.setFont(new Font("Comic Sans", Font.PLAIN, 22));
        songTable.setShowHorizontalLines(true);
        songTable.setOpaque(true);
        songTable.setBorder(null);
        songTable.setSelectionBackground(new Color(0xBF616A)); 
        Font headerFont = new Font("Comic Sans", Font.BOLD, 17);
        songTable.getTableHeader().setFont(headerFont);
        // Set fixed column widths
        songTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Title
        songTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Artist
        songTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Genre
        songTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Duration
        
        // Disable column resizing
        for (int i = 0; i < songTable.getColumnCount(); i++) {
            songTable.getColumnModel().getColumn(i).setResizable(false);
        }
        songTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        songTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                getSelectedSong();
            }
        });

        JLabel text1 = new JLabel("Popular Songs");
        text1.setBounds(50, 50, 200, 100);
        text1.setForeground(new Color(0xBF616A));
        text1.setOpaque(false);
        text1.setFont(new Font("Comic Sans", Font.BOLD, 55));

        themeButton = new JButton(lightIcon);
        themeButton.setBounds(5, 410, 40, 30);
        themeButton.setFocusable(false);
        themeButton.setFocusPainted(false);
        themeButton.setContentAreaFilled(false);
        themeButton.setBorderPainted(false);
        themeButton.addActionListener(e -> toggleTheme());

        JScrollPane scrollPane = new JScrollPane(songTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setOpaque(true);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBorder(null);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(580, 400));

        musicIconLabel = new JLabel(musicIcon);
        musicIconLabel.setBounds(57, 50, 290, 290);

        // Song title and artist labels
        songTitle = new JLabel("Title");
        songTitle.setBounds(160, 310, 200, 100);
        songTitle.setFont(new Font("Comic Sans", Font.BOLD, 35));

        songArtist = new JLabel("Artist");
        songArtist.setBounds(170, 360, 100, 50);
        songArtist.setFont(new Font("Comic Sans", Font.BOLD, 20));

        // Playback slider
        playBackSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 0);
        playBackSlider.setBounds(50, 400, 300, 50);
        playBackSlider.setOpaque(false);

        volumeButton = new JButton(volumeIcon); // Replace with your icon path
        volumeButton.setBounds(350, 410, 40, 30);
        volumeButton.setFocusPainted(false);
        volumeButton.setContentAreaFilled(false);
        volumeButton.setBorderPainted(false);
        volumeButton.addActionListener(e -> {
            volumeSlider.setVisible(!volumeSlider.isVisible());
            panel1.revalidate();
            panel1.repaint();
        });

        volumeSlider = new JSlider(SwingConstants.VERTICAL,50, 100, 90); // volume from 0 (mute) to 100 (max), default to 80
        volumeSlider.setBounds(355, 310, 30, 100); 
        volumeSlider.setVisible(false);
        volumeSlider.setOpaque(false);
        volumeSlider.addChangeListener(e -> {
            if (song != null) {
                float volume = volumeSlider.getValue() / 100f; // convert to 0.0 - 1.0
                song.setVolume(volume);
            }
        });
        

        timeLabel = new JLabel("00:00", SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timeLabel.setBounds(20, 430, 100, 30);

        durationLabel = new JLabel("00:00", SwingConstants.CENTER);
        durationLabel.setFont(new Font("Arial", Font.BOLD, 16));
        durationLabel.setBounds(270, 430, 100, 30);

        // Buttons
        playButton = new JButton(playIcon);
        playButton.setBounds(160, 470, 75, 75);
        playButton.setFocusable(false);
        playButton.setOpaque(false);
        playButton.setContentAreaFilled(false);
        playButton.setBorderPainted(false);
        playButton.addActionListener(this);

        backwardButton = new JButton(backwardIcon);
        backwardButton.setBounds(60, 470, 75, 75);
        backwardButton.setFocusable(false);
        backwardButton.setOpaque(false);
        backwardButton.setContentAreaFilled(false);
        backwardButton.setBorderPainted(false);
        backwardButton.addActionListener(this);
        
        forwardButton = new JButton(forwardIcon);
        forwardButton.setBounds(255, 470, 75, 75);
        forwardButton.setFocusable(false);
        forwardButton.setOpaque(false);
        forwardButton.setContentAreaFilled(false);
        forwardButton.setBorderPainted(false);
        forwardButton.addActionListener(this);
        
        panel1.add(playBackSlider);
        panel1.add(songTitle);
        panel1.add(songArtist);
        panel1.add(playButton);
        panel1.add(forwardButton);
        panel1.add(backwardButton);
        panel1.add(musicIconLabel);
        panel1.add(timeLabel);
        panel1.add(durationLabel);
        panel1.add(volumeSlider);
        panel1.add(volumeButton);
        panel1.add(themeButton);
        header = songTable.getTableHeader();
        header.setBackground(darker);
        header.setOpaque(true);
        header.setForeground(Color.white);
        songTable.setBackground(darker);
        panel2.setBackground(darker);
        panel2.setOpaque(true);
        panel2.add(text1, BorderLayout.NORTH);
        panel2.add(scrollPane, BorderLayout.CENTER);
        frame.add(panel1);
        frame.add(panel2);
        frame.setVisible(true);
        
        // Fetch songs from server dynamically
        fetchSongsFromServer();
        // getSelectedSong();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == playButton) {
            if (!isPlaying && song != null) {
                song.resumePlayback(); // Resume if paused
                playButton.setIcon(pauseIcon);
                isPlaying = true;
            } else if (isPlaying && song != null) {
                song.pause(); // Pause if playing
                playButton.setIcon(playIcon);
                isPlaying = false;
            }
        } else if (e.getSource() == backwardButton) {
            previousSong();
        } else if (e.getSource() == forwardButton) {
            nextSong();
        }
    }

    public void getSelectedSong() {
        int row = songTable.getSelectedRow();
        // System.out.println("getSelectedSong() triggered. Selected row: " + row);
        
        if (row != -1) {
            selectedSongTitle = songTable.getValueAt(row, 0).toString();
            selectedSongArtist = songTable.getValueAt(row, 1).toString();
            
            System.out.println("Selected Song Title: " + selectedSongTitle + ", Artist: " + selectedSongArtist);
            
            if (song != null) {
                song.stop(); // Stop the current song before playing a new one
            }
    
            playBackSlider.setValue(0);
            song = new Song(selectedSongPath, this);
            song.sendGetSongRequest(selectedSongTitle);
            playButton.setIcon(pauseIcon);
            isPlaying = true;
            updateSongDetails(selectedSongTitle, selectedSongArtist);
        } else {
            System.out.println("No row selected. Cannot play song.");
        }
    }
    
    
    public void toggleTheme() {
        try {
            if (isDarkTheme) {
                UIManager.setLookAndFeel(new FlatSolarizedLightIJTheme());
                header.setForeground(Color.black);
                darkIcon = new ImageIcon(recolorAndScale("Client\\moon.png", 30, 30, Color.BLACK));
                themeButton.setIcon(darkIcon);
                iconColor = Color.BLACK;
            } else {
                UIManager.setLookAndFeel(new FlatSpacegrayIJTheme());
                header.setForeground(Color.white);
                themeButton.setIcon(lightIcon);
                iconColor = Color.WHITE;
            }
            // UIManager.put("Slider.trackColor", new Color(0x1C1F26));          // Light track
            UIManager.put("Slider.thumbColor", new Color(0xBF616A));          // Thumb
            UIManager.put("Slider.trackValueColor", new Color(0xBF616A));          
            // UIManager.put("Slider.thumbBorderColor", new Color(0xA3BE8C));    // Thumb border
            UIManager.put("Slider.trackBorderColor", new Color(0x4C566A));    // Track border
            UIManager.put("Slider.hoverTrackColor", new Color(0x88C0D0));     // On hover
            UIManager.put("Slider.focusedTrackColor", new Color(0x81A1C1));   // On focus
    
            isDarkTheme = !isDarkTheme;
            SwingUtilities.updateComponentTreeUI(frame);
    
            // Update icons with the new icon color
            playIcon = new ImageIcon(recolorAndScale("Client\\play-solid.png", 55, 75, iconColor));
            pauseIcon = new ImageIcon(recolorAndScale("Client\\pause-solid.png", 55, 75, iconColor));
            forwardIcon = new ImageIcon(recolorAndScale("Client\\forward-solid.png", 75, 75, iconColor));
            backwardIcon = new ImageIcon(recolorAndScale("Client\\backward-solid.png", 75, 75, iconColor));
            volumeIcon = new ImageIcon(recolorAndScale("Client\\volume.png", 30, 30, iconColor));

            // Apply the new icons to buttons
            playButton.setIcon(playIcon);
            forwardButton.setIcon(forwardIcon);
            backwardButton.setIcon(backwardIcon);
            volumeButton.setIcon(volumeIcon);
            

            // Recalculate background colors based on new theme
            bg = UIManager.getColor("Panel.background");
            lighter = ColorFunctions.lighten(bg, 0.05f);
            darker = ColorFunctions.darken(bg, 0.05f);

            iconColor = new JLabel().getForeground(); 
    
            // Update component colors
            panel1.setBackground(bg);
            panel2.setBackground(darker);
            header.setBackground(darker);
            songTable.setBackground(darker);
            panel1.repaint();
            panel1.revalidate();
            panel2.repaint();
            panel2.revalidate();
    
            // Update the entire UI
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
    
    public void updateSongDetails(String songName, String artist) {
        updateSongDetails(songName, artist, "", ""); // Call 4-arg version with default values
    }

    public void updateSongDetails(String songName, String artist, String duration, String coverPath) {
        if (songTitle != null && songArtist != null) {
            songTitle.setText(songName);
            songArtist.setText(artist);
            int titleWidth = songTitle.getPreferredSize().width;
            int artistWidth = songArtist.getPreferredSize().width;
        
            int panelWidth = panel1.getWidth();
            songTitle.setBounds((panelWidth - titleWidth) / 2, 310, titleWidth, 100);
            songArtist.setBounds((panelWidth - artistWidth) / 2, 360, artistWidth, 50);
        } else {
            System.err.println("Error: songTitle or songArtist is not initialized.");
        }
    
        // Load and display cover image if available
        if (coverPath != null && !coverPath.isEmpty()) {
            try {
                ImageIcon coverIcon = new ImageIcon(new ImageIcon(coverPath).getImage().getScaledInstance(290, 290, Image.SCALE_SMOOTH));
                JLabel musicIconLabel = new JLabel(coverIcon);
                musicIconLabel.setBounds(50, 50, 290, 290);
                panel1.add(musicIconLabel);
                panel1.repaint(); // Refresh GUI
            } catch (Exception e) {
                System.err.println("Error loading cover image: " + e.getMessage());
            }
        }
    }
    
    

    public void fetchSongsFromServer() {
        new Thread(() -> {
            try (Socket clientSocket = new Socket("localhost", 9806);
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

                out.println("listAllSongs");
                Object response = in.readObject();

                if (response instanceof ArrayList) {
                    ArrayList<String> songList = (ArrayList<String>) response;
                    SwingUtilities.invokeLater(() -> {
                        updateSongTable(songList);
                        
                    });
                } else {
                    System.err.println("Unexpected response from server.");
                }

            } catch (Exception e) {
                System.err.println("Error communicating with server: " + e.getMessage());
            }
        }).start();
    }

    public void updateSongTable(ArrayList<String> songList) {
        tableModel.setRowCount(0);
        for (int i = 0; i < songList.size(); i += 4) {
            String name = songList.get(i);
            String artist = songList.get(i + 1);
            String genre = songList.get(i + 2);
            String duration = songList.get(i + 3);

            // Format the duration from seconds to hh:mm:ss
            long durationInSeconds = Long.parseLong(duration);
            String formattedDuration = formatDuration(durationInSeconds);

            tableModel.addRow(new Object[]{name, artist, genre, formattedDuration});
        }
    }

    public void previousSong() {
        System.out.println("Previous Song Triggered!");
        int rowCount = songTable.getRowCount();
        int selectedRow = songTable.getSelectedRow();
        
        System.out.println("Total Songs: " + rowCount);
        System.out.println("Currently Selected Row: " + selectedRow);
    
        if (selectedRow > 0) {
            songTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
        } else {
            System.out.println("Reached start. Looping to the last song.");
            songTable.setRowSelectionInterval(songTable.getRowCount() - 1, songTable.getRowCount() - 1);
        }
        // getSelectedSong();
        
        
        int newRow = songTable.getSelectedRow();
        System.out.println("Selecting Previous Song: Row " + newRow);
    

    }
    
    
    
    
    public void nextSong() {
        System.out.println("Next Song Triggered!");
        int rowCount = songTable.getRowCount();
        int selectedRow = songTable.getSelectedRow();

        
        System.out.println("Total Songs: " + rowCount);
        System.out.println("Currently Selected Row: " + selectedRow);
    
        if (selectedRow < songTable.getRowCount() - 1) {
            songTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
        } else {
            System.out.println("Reached end. Looping to the first song.");
            songTable.setRowSelectionInterval(0, 0);
        }
        // getSelectedSong();
        
        
        int newRow = songTable.getSelectedRow();
        System.out.println("Selecting Next Song: Row " + newRow);
        
    }
    
    public void updateCoverImage(ImageIcon newCoverImage) {
        if (musicIconLabel != null && newCoverImage != null) {
            musicIconLabel.setIcon(new ImageIcon(newCoverImage.getImage().getScaledInstance(290, 290, Image.SCALE_SMOOTH)));
            panel1.repaint();
        }
    }
    
    public static BufferedImage recolorIcon(BufferedImage image, Color newColor) {
        BufferedImage tinted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgba = image.getRGB(x, y);
                Color col = new Color(rgba, true);
    
                // Recolor only dark (likely black) pixels, keep alpha
                if (col.getAlpha() > 0 && col.getRed() < 100 && col.getGreen() < 100 && col.getBlue() < 100) {
                    Color newCol = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), col.getAlpha());
                    tinted.setRGB(x, y, newCol.getRGB());
                } else {
                    tinted.setRGB(x, y, rgba);
                }
            }
        }
        return tinted;
    }

    public static Image recolorAndScale(String path, int width, int height, Color color) {
        try {
            BufferedImage original = ImageIO.read(new File(path));
            BufferedImage recolored = recolorIcon(original, color);
            return recolored.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void startSliderUpdater(SourceDataLine audioLine, long totalDurationMicros) {
        this.currentDurationMicros.set(totalDurationMicros);
        
        // Update total duration label
        SwingUtilities.invokeLater(() -> {
            durationLabel.setText(formatMicroseconds(totalDurationMicros));
        });

        if (sliderTimer != null) {
            sliderTimer.stop();
        }

        sliderTimer = new Timer(50, e -> {
            if (!sliderBeingAdjusted && audioLine != null && audioLine.isOpen()) {
                long currentPos = audioLine.getMicrosecondPosition();
                int progress = (int) ((currentPos * 100) / totalDurationMicros);
                
                SwingUtilities.invokeLater(() -> {
                    playBackSlider.setValue(Math.min(progress, 100));
                    timeLabel.setText(formatMicroseconds(currentPos));
                });
            }
        });
        sliderTimer.start();
    }
    
    
    private String formatMicroseconds(long micros) {
        long seconds = micros / 1_000_000;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static String formatDuration(long seconds) {
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
    
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    // Update slider position
    public void updateSlider(int progress) {
        System.out.println("Updating Slider to: " + progress);
        playBackSlider.setValue(progress);
    }

    public void setSliderMax(int max) {
        playBackSlider.setMaximum(max);
    }

    public static void main(String[] args) {
        FlatSpacegrayIJTheme.setup();
        new MusicPlayerGUI();
    }
}
