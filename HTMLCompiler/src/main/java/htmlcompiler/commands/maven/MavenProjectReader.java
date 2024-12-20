package htmlcompiler.commands.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.nio.file.Files;

public enum MavenProjectReader {;

    public static Path toInputDirectory(final MavenProject project) throws MojoFailureException {
        final Path inputDir = project.getBasedir().toPath().resolve("src").resolve("main").resolve("websrc");
        if (!Files.exists(inputDir)) throw new MojoFailureException("Input directory must exist: " + inputDir);
        if (!Files.isDirectory(inputDir)) throw new MojoFailureException("Input directory must be a directory");
        return inputDir;
    }

    public static Path toOutputDirectory(final String subdir, final MavenProject project) throws MojoFailureException {
        final Path outputDir = Paths.get(project.getBuild().getOutputDirectory()).resolve(subdir);
        outputDir.toFile().mkdirs();
        if (!Files.exists(outputDir)) throw new MojoFailureException("Output directory must exist: " + outputDir);
        if (!Files.isDirectory(outputDir)) throw new MojoFailureException("Output directory must be a directory");
        return outputDir;
    }

    public static Path toStaticDirectory(final MavenProject project) throws MojoFailureException {
        final Path outputDir = Paths.get(project.getBuild().getOutputDirectory()).resolve("wwwroot");
        outputDir.toFile().mkdirs();
        if (!Files.exists(outputDir))
            throw new MojoFailureException("Output directory must exist: " + outputDir);
        if (!Files.isDirectory(outputDir))
            throw new MojoFailureException("Output directory must be a directory");
        return outputDir;
    }

    public static Map<String, String> newTemplateContext(final MavenProject project) {
        return applyMavenProjectContext(applyEnvironmentContext(new HashMap<>()), project);
    }

    public static Map<String, String> applyMavenProjectContext(final Map<String, String> context, final MavenProject project) {
        for (final Entry<Object, Object> entry : project.getProperties().entrySet()) {
            context.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return context;
    }
    public static Map<String, String> applyEnvironmentContext(final Map<String, String> context) {
        for (final Entry<String, String> entry : System.getenv().entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }
        return context;
    }

}
