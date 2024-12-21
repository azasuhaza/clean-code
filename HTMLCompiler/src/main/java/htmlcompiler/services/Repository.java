package htmlcompiler.services;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public enum Repository {;

    public static Path getRepositoryDirectory() throws IOException {
        final Path repo = Paths.get(System.getProperty("user.home")).resolve(".htmlcompiler");
        if (!Files.exists(repo)) Files.createDirectories(repo);
        if (!Files.isDirectory(repo)) throw new IOException("'~/.htmlcompiler' is not a directory");
        return repo;
    }

}
