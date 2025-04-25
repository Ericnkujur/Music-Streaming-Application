package jdbc;
import java.io.*;
import java.net.*;
import java.nio.file.Files;

import jdbc.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class server {
    private static final int PORT = 9806;

    //Learn about this *****************************
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Pass clientSocket to SongDAO to handle requests
                threadPool.submit(() -> handleClientRequest(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handle client requests
    private static void handleClientRequest(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream())) {
            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Received request: " + request);

                if (request.equalsIgnoreCase("listAllSongs")) {
                    // Correctly create SongDAO instance with clientSocket
                    SongDAO songDAO = new SongDAO(clientSocket);
                    listAllSongs(songDAO, out); // Send song list to client
                } else if (request.startsWith("getSongDetails:")) {
                    String songTitle = request.split(":")[1];
                    System.out.println("Fetching song details for: " + songTitle);
                    sendSongDetails(songTitle, out);
                } else if (request.startsWith("getSongFile:")) {
                    String songTitle = request.split(":")[1];

                    // Send cover image first
                    System.out.println("Sending cover image");
                    sendCoverImage(songTitle, out);

                    // After the cover image, stream the song file
                    System.out.println("Streaming song");
                    streamSongFile(songTitle, dataOut, out);
                } else {
                    out.writeObject("Unknown request");
                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                clientSocket.close();
                System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }


    private static void streamSongFile(String songTitle, DataOutputStream dataOut, ObjectOutputStream out) {
        String filePath = SongDAO.getSongPath(songTitle);
        if (filePath == null || filePath.isEmpty()) {
            System.err.println("Invalid file path for: " + songTitle);
            return;
        }
    
        File songFile = new File(filePath);
        if (!songFile.exists()) {
            System.err.println("File not found: " + filePath);
            return;
        }
    
        System.out.println("Streaming song: " + songTitle);
    
        // Step 1: Get Duration and Send it to Client
        long durationMicros = getDurationMicros(songFile);
        try {
            out.writeObject(durationMicros);  // Send duration to client before streaming audio
            out.flush();
            System.out.println("Duration sent: " + durationMicros + " µs");
        } catch (IOException e) {
            System.err.println("Error sending duration: " + e.getMessage());
            return;
        }
    
        // Step 2: Stream Audio as Before
        try (AudioInputStream baseAudioStream = AudioSystem.getAudioInputStream(songFile)) {
            if (baseAudioStream == null) {
                System.err.println("Error: Base audio stream is null.");
                return;
            }
    
            AudioFormat baseFormat = baseAudioStream.getFormat();
    
            try (AudioInputStream pcmInputStream = getPCMStream(baseAudioStream, baseFormat)) {
                if (pcmInputStream == null) {
                    System.err.println("Error: PCM conversion failed.");
                    return;
                }
                
                //send meta data.
                long durationSeconds = getDurationSeconds(pcmInputStream);
                AudioFormat format = pcmInputStream.getFormat();
                dataOut.writeLong(durationSeconds);
                sendAudioFormat(dataOut,format);

                // Send audio data to the client
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = pcmInputStream.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
    
                System.out.println("Song streamed successfully: " + songTitle);
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("Error streaming song: " + e.getMessage());
        }
    }

    private static long getDurationSeconds(AudioInputStream audioStream) {
        long frames = audioStream.getFrameLength();
        return (long) (frames / audioStream.getFormat().getFrameRate());
    }

    private static void sendAudioFormat(DataOutputStream dos, AudioFormat format) throws IOException {
        dos.writeFloat(format.getSampleRate());
        dos.writeInt(format.getSampleSizeInBits());
        dos.writeInt(format.getChannels());
        dos.writeBoolean(format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED);
        dos.writeBoolean(format.isBigEndian());
        dos.flush();
    }

    private static long getDurationMicros(File audioFile) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat format = audioInputStream.getFormat();
            long audioFileLength = audioFile.length();
            float frameSize = format.getFrameSize();
            float frameRate = format.getFrameRate();
    
            // Duration in microseconds
            return (long) ((audioFileLength / (frameSize * frameRate)) * 1_000_000);
        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("Error calculating duration: " + e.getMessage());
            return -1;
        }
    }
    
    
    // Helper method to handle PCM conversion
    private static AudioInputStream getPCMStream(AudioInputStream baseAudioStream, AudioFormat baseFormat) {
        AudioFormat targetFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.getSampleRate(),
            16,
            baseFormat.getChannels(),
            baseFormat.getChannels() * 2,
            baseFormat.getSampleRate(),
            false
        );
    
        if (!AudioSystem.isConversionSupported(targetFormat, baseFormat)) {
            System.err.println("Conversion to PCM format not supported.");
            return null;
        }
    
        return AudioSystem.getAudioInputStream(targetFormat, baseAudioStream);
    }
    
    
    

    // Send the list of all songs to the client
    private static void listAllSongs(SongDAO songDAO,ObjectOutputStream out) {
        System.out.println("Fetching song list from database...");
        try {

            List<String> li = songDAO.listAllSongs();

            out.writeObject(li);

            out.writeObject("END"); // Mark the end of the list
            System.out.println("Song list sent to client.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendSongDetails(String songTitle, ObjectOutputStream out) {
        try {
            List<String> songDetails = SongDAO.getSongDetails(songTitle);
            if (!songDetails.isEmpty()) {
                out.writeObject(songDetails);
            } else {
                out.writeObject("Song not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    
    private static void sendCoverImage(String songTitle, ObjectOutputStream out) {
        String coverPath = SongDAO.getSongCoverPath(songTitle);
        try {
            File coverFile = new File(coverPath);
            if (coverFile.exists()) {
                byte[] coverBytes = Files.readAllBytes(coverFile.toPath());
                out.writeObject(coverBytes); // Send cover image as byte[]
                out.flush();
                System.out.println("Cover image sent for: " + songTitle);
            } else {
                System.out.println("NO COVER FOUND!");
                out.writeObject(new byte[0]); // Send empty byte array if cover not found
            }
        } catch (IOException e) {
            System.err.println("Error sending cover image: " + e.getMessage());
        }
    }
    

    

}
