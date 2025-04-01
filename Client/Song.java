import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.*;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Image;


public class Song {


    private MusicPlayerGUI gui;
    private String filePath;
    private int duration;
    public SourceDataLine audioLine;
    private volatile boolean isPaused = false;
    private ByteArrayOutputStream pauseBuffer = new ByteArrayOutputStream();
    private Socket socket;
    private volatile boolean playbackCompleted = false;
    private AtomicLong currentPosition = new AtomicLong(0);
    private long totalDurationMicros = 0;
  
    public Song() {
        // Default constructor
    }

    // Constructor to initialize song and load audio file
    public Song(String filePath, MusicPlayerGUI gui) {
        this.gui = gui;
        this.filePath = filePath;

    }




    
    

    // Pause the song
    public synchronized void pause() {
        if (!isPaused) {
            isPaused = true;
            System.out.println("Playback paused.");
        }
    }

    // Resume the song
    public synchronized void resumePlayback() {
        if (isPaused) {
            isPaused = false;
            notify(); // Notify thread to resume playback
            System.out.println("Playback resumed.");
        }
    }


    // Stop the song and reset playback
    public void stop() {
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
            isPaused = false;
        }
    }
    
    // Get duration from database (not from audio line)
    private int getSongDuration() {
        return this.duration; // Get this from your database
    }

    

    // Update progress and slider position
    private void updateProgress(long bytesStreamed, long totalBytes) {
        if (totalBytes > 0) {
            int progress = (int) ((bytesStreamed * 100) / totalBytes);
            SwingUtilities.invokeLater(() -> gui.updateSlider(progress));
        }
    }

    

    public void streamAndPlaySong(String songTitle) {
        new Thread(() -> {
            try {
   
                socket = new Socket("localhost", 9806);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                out.println("getSongFile:" + songTitle);
                System.out.println("Request sent to server: " + songTitle);

                InputStream inputStream = socket.getInputStream();
                if (inputStream == null) {
                    System.err.println("Error: Could not get InputStream from socket.");
                    return;
                }
                playAudioFromStream(inputStream);

            } catch (Exception e) {
                System.err.println("Error streaming song: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

     

    private void playAudioFromStream(InputStream inputStream) {
        if (inputStream == null) {
            System.err.println("Error: Input stream is null. Cannot play audio.");
            return;
        }
    
        try {
            AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100,    // Sample rate
                16,       // Sample size in bits
                2,        // Channels (Stereo)
                4,        // Frame size
                44100,    // Frame rate
                false     // Little-endian
            );
    
            AudioInputStream audioInputStream = new AudioInputStream(inputStream, targetFormat, AudioSystem.NOT_SPECIFIED);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
    
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Error: Unsupported audio format.");
                return;
            }
    
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(targetFormat);
            audioLine.start();
    
            byte[] buffer = new byte[8192];
            int bytesRead;
    
          
            long totalDurationMicros = audioLine.getMicrosecondPosition();


            long totalBytes = audioInputStream.getFrameLength() * targetFormat.getFrameSize();
    
            
            SwingUtilities.invokeLater(() -> gui.startSliderUpdater(audioLine, totalDurationMicros));
    
            System.out.println("Audio playback started.");
    
            long bytesStreamed = 0;  // Track streamed bytes
    
            while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                synchronized (this) {
                    while (isPaused) {
                        audioLine.stop();
                        try {
                            wait(); // Pause thread until notified
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    audioLine.start();
                }
                audioLine.write(buffer, 0, bytesRead);
                bytesStreamed += bytesRead;
    
               
                updateProgress(bytesStreamed, totalBytes);
            }
    
            audioLine.drain();
            audioLine.close();
    
            
            if (gui.sliderTimer != null && gui.sliderTimer.isRunning()) {
                gui.sliderTimer.stop();
            }
    
            System.out.println("Audio playback finished.");
        } catch (IOException e) {
            System.err.println("Error reading audio stream.");
        } catch (LineUnavailableException e) {
            System.err.println("Error: Audio line unavailable.");
        }
    }
    
    
    


    // Update playback slider with current song position
    private void updateSlider(FileInputStream fis) {
        try {
            int progress = duration - fis.available();
            gui.updateSlider(progress);
        } catch (IOException e) {
            System.err.println("Error updating slider: " + e.getMessage());
        }
    }

    


    // Send a request to get details about the selected song
    public void sendGetSongDetailsRequest(String songTitle) {
        try (Socket clientSocket = new Socket("localhost", 9806);
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            String request = "getSongDetails:" + songTitle;
            out.println(request);
            System.out.println("Request sent to server: " + request);

            Object response = in.readObject();

            if (response instanceof ArrayList) {
                ArrayList<String> songDetails = (ArrayList<String>) response;

                if (songDetails.size() == 5) {
                    String songPath = songDetails.get(0);
                    String duration = songDetails.get(1);
                    String artist = songDetails.get(2);
                    String coverPath = songDetails.get(3);
                    String songName = songDetails.get(4);

                   
                    SwingUtilities.invokeLater(() -> {
                    gui.updateSongDetails(songName, artist, duration, coverPath);

                    
                    try {
                        if (!coverPath.isEmpty()) {
                            ImageIcon coverIcon = new ImageIcon(new ImageIcon(coverPath).getImage().getScaledInstance(290, 290, Image.SCALE_SMOOTH));
                            // gui.updateCoverImage(coverIcon);  
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading cover image: " + e.getMessage());
                    }

                    // loadAudioFile(songPath);  
                });
                } else {
                    System.err.println("Invalid response format from server.");
                }
            } else {
                System.err.println("Unexpected response from server.");
            }
        } catch (Exception e) {
            System.err.println("Error communicating with server: " + e.getMessage());
        }
    }
    
    // learn about this *************************************
    public void sendGetSongRequest(String songTitle) {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 9806);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {
                
                out.println("getSongFile:" + songTitle);
                
                // Read duration first
                ObjectInputStream objIn = new ObjectInputStream(in);
                totalDurationMicros = (Long) objIn.readObject();
                
                // Read audio format
                long duration = in.readLong();
                float sampleRate = in.readFloat();
                int sampleSize = in.readInt();
                int channels = in.readInt();
                boolean signed = in.readBoolean();
                boolean bigEndian = in.readBoolean();
                
                AudioFormat format = new AudioFormat(
                    signed ? AudioFormat.Encoding.PCM_SIGNED : AudioFormat.Encoding.PCM_UNSIGNED,
                    sampleRate, sampleSize, channels, 
                    (sampleSize / 8) * channels, sampleRate, bigEndian);
                
                // Start playback
                playAudioStream(in, format);
                
            } catch (Exception e) {
                System.err.println("Playback error: " + e.getMessage());
            }
        }).start();
    }
    
    private void playAudioStream(InputStream stream, AudioFormat format) {
        try {
            audioLine = AudioSystem.getSourceDataLine(format);
            audioLine.open(format);
            
            // Start GUI updates
            SwingUtilities.invokeLater(() -> {
                gui.startSliderUpdater(audioLine, totalDurationMicros);
            });

            audioLine.start();
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = stream.read(buffer)) != -1) {
                synchronized (this) {
                    while (isPaused) {
                        audioLine.stop();
                        wait();
                    }
                    audioLine.start();
                }
                audioLine.write(buffer, 0, bytesRead);
                currentPosition.set(audioLine.getMicrosecondPosition());
            }
            
            audioLine.drain();
            playbackCompleted = true;
            
        } catch (Exception e) {
            System.err.println("Playback error: " + e.getMessage());
        } finally {
            if (audioLine != null) {
                audioLine.close();
            }
            SwingUtilities.invokeLater(() -> {
                if (gui.sliderTimer != null) {
                    gui.sliderTimer.stop();
                }
            });
        }
    }
    
    public synchronized void seek(long positionMicros) {
        if (audioLine != null && audioLine.isOpen()) {
            try {
                audioLine.stop();
                audioLine.flush();
                currentPosition.set(positionMicros);
                audioLine.start();
            } catch (Exception e) {
                System.err.println("Seek error: " + e.getMessage());
            }
        }
    }
    

    // Receive a file (audio or cover image) from the server
    private void receiveFile(InputStream in, String fileName) {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("File received: " + fileName);
        } catch (IOException e) {
            System.err.println("Error receiving file: " + e.getMessage());
        }
    }

    
}
