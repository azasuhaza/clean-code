package htmlcompiler.compilers;

import htmlcompiler.utils.Logger;
import htmlcompiler.utils.OnlyFileVisitor;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;

import htmlcompiler.compilers.FileCompiler;
import htmlcompiler.utils.Filenames;
import java.lang.Integer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;


public interface TemplateThenCompile {

    void compileTemplate(final Path inFile) throws Exception;

    public static TemplateThenCompile newTemplateThenCompile(final Logger logger, final Path inputDir
            , final Path outputDir, final boolean replaceExtension, final Map<String, String> variables
            , final HtmlCompiler html) {
        final var compilers = FileCompiler.newFileCompilerMap(logger, html, variables);

        return inFile -> {
            if (inFile == null || !Files.isRegularFile(inFile)) return;

            final String extension = Filenames.toExtension(inFile, null);
            if (extension == null) return;
            final var compiler = compilers.get(extension);
            if (compiler == null) return;

            final String output = compiler.compile(inFile);
            final Path outputFile = renameFile(inFile, inputDir, outputDir, replaceExtension, compiler.outputExtension());
            Files.writeString(outputFile, output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        };
    }

    private static Path renameFile(final Path inputFile, final Path inputDir, final Path outputDir
            , final boolean replaceExtension, final String replacement) {
        final Path outFile = outputDir.resolve(extensionize(inputDir.relativize(inputFile).toString(), replaceExtension, replacement));
        outFile.getParent().toFile().mkdirs();
        return outFile;
    }

    private static String extensionize(final String filename, final boolean replaceExtension, final String replacement) {
        return replaceExtension ? filename.substring(0, filename.lastIndexOf('.')) + replacement : filename+replacement;
    }

    public static void compileDirectories(final Path inputDir, final TemplateThenCompile ttc, final boolean recursive) throws IOException {
        final int maxDepth = recursive ? Integer.MAX_VALUE : 1;
        Files.walkFileTree(inputDir, EnumSet.noneOf(FileVisitOption.class), maxDepth, (OnlyFileVisitor) (file, attrs) -> {
            try {
                ttc.compileTemplate(file);
            } catch (Exception e) {
                throw new IOException("Exception occurred while parsing " + inputDir.relativize(file), e);
            }
            return FileVisitResult.CONTINUE;
        });
    }

}