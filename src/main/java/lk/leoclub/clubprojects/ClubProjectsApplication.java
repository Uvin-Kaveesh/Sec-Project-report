package lk.leoclub.clubprojects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClubProjectsApplication {

    public static void main(String[] args) {
        ensureDataDirectory();
        SpringApplication.run(ClubProjectsApplication.class, args);
    }

    /**
     * The SQLite driver creates the database file but not the folder holding it,
     * so the directory has to exist before the DataSource is built.
     */
    private static void ensureDataDirectory() {
        String url = System.getProperty("CLUB_DB_PATH", System.getenv("CLUB_DB_PATH"));
        Path file = Path.of(url == null || url.isBlank() ? "data/club-projects.db" : url);
        Path dir = file.getParent();
        if (dir == null) {
            return;
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the database directory: " + dir, e);
        }
    }
}
