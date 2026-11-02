package lab.melostudy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/** Keeps uploaded files in one directory, each under a name we generate. */
@ApplicationScoped
public class FileStorage {

    private final Path directory;

    FileStorage(@ConfigProperty(name = "docs.storage.dir") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    /** Moves the uploaded temp file into the storage directory and returns the generated name. */
    public String store(Path uploaded) {
        try {
            Files.createDirectories(directory);
            String storedName = UUID.randomUUID().toString();
            Files.move(uploaded, directory.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            return storedName;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path locate(String storedName) {
        return directory.resolve(storedName);
    }

    public Path directory() {
        return directory;
    }
}
