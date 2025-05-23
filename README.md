# Music-Streaming-Application

To run the Project follow the below steps:

### step 1:
Update the USER and PASSWORD and enter your root username and password in the Music-Streaming-Application\Server\Mus\src\jdbc\DatabaseConnection.java file

then,
open command prompt and type:
```
mysql -u root -p
```
then enter and then press enter your password

### step 2:
then paste the below command:
```
CREATE DATABASE music_player;

USE music_player;

CREATE TABLE songs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    path VARCHAR(500) NOT NULL,
    cover_path VARCHAR(500) NOT NULL,
    duration INT NOT NULL
);

INSERT INTO songs (name, artist, genre, path, cover_path, duration) VALUES
('shape of you', 'ed sheeran', 'pop', 'songs/Shape Of You.wav', 'covers/Shape Of You.jpg', 233),
('believer', 'imagine dragons', 'rock', 'songs/Believer.wav', 'covers/Believer.jpg', 204),
('someone like you', 'adele', 'pop', 'songs/Someone Like You.wav', 'covers/Someone Like You.jpg', 285),
('bohemian rhapsody', 'queen', 'rock', 'songs/Bohemian Rhapsody.wav', 'covers/Bohemian Rhapsody.jpg', 355),
('blinding lights', 'the weeknd', 'pop', 'songs/Blinding Lights.wav', 'covers/Blinding Lights.jpg', 200),
('bad guy', 'billie eilish', 'pop', 'songs/Bad Guy.wav', 'covers/Bad Guy.jpg', 195),
('hotel california', 'eagles', 'rock', 'songs/Hotel California.wav', 'covers/Hotel California.jpg', 391),
('smells like teen spirit', 'nirvana', 'grunge', 'songs/Smells Like Teen Spirit.wav', 'covers/Smells Like Teen Spirit.jpg', 301),
('see you again', 'wiz khalifa', 'hip-hop', 'songs/See You Again.wav', 'covers/See You Again.jpg', 245),
('closer', 'the chainsmokers', 'electronic', 'songs/Closer.wav', 'covers/Closer.jpg', 244),
('levitating', 'dua lipa', 'pop', 'songs/Levitating.wav', 'covers/Levitating.jpg', 203),
('faded', 'alan walker', 'edm', 'songs/Faded.wav', 'covers/Faded.jpg', 212),
('radioactive', 'imagine dragons', 'rock', 'songs/Radioactive.wav', 'covers/Radioactive.jpg', 186),
('rolling in the deep', 'adele', 'pop', 'songs/Rolling In The Deep.wav', 'covers/Rolling In The Deep.jpg', 228),
('havana', 'camila cabello', 'pop', 'songs/Havana.wav', 'covers/Havana.jpg', 216),
('let me love you', 'dj snake', 'pop', 'songs/Let Me Love You.wav', 'covers/Let Me Love You.jpg', 204);
```
## To start server

### step 3:
open the \Music-Streaming-Application folder in your file explorer,
then right click and select open in terminal

your working directory should be \Music-Streaming-Application

### step 4:
type below command to compile the code:
```
javac -cp ".;Client/lib/flatlaf-intellij-themes-3.5.4.jar;Client/lib/flatlaf-3.5.4.jar" -d out Client/*.java Server/Mus/src/jdbc/*.java
```
then run the server:
```
java -cp ".;out;Client/lib/flatlaf-intellij-themes-3.5.4.jar;Client/lib/flatlaf-3.5.4.jar;Server/mysql-connector-j-9.2.0.jar" jdbc.server
```

## To start GUI

### step 5: 
open the \Music-Streaming-Application folder in your file explorer,
then right click and select open in terminal

your working directory should be \Music-Streaming-Application

### step 6:
type below command to compile the code:
```
javac -cp ".;Client/lib/flatlaf-intellij-themes-3.5.4.jar;Client/lib/flatlaf-3.5.4.jar" -d out Client/*.java Server/Mus/src/jdbc/*.java
```
then run the GUI:
```
java -cp ".;out;Client/lib/flatlaf-intellij-themes-3.5.4.jar;Client/lib/flatlaf-3.5.4.jar" MusicPlayerGUI
```

finaly the project should be running