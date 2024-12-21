package htmlcompiler.compilers;

public enum JsCompiler {;

    public static CodeCompiler newTypescriptCompiler() {
        return CodeCompiler.newExternalToolCompiler("tsc", ".tsc",
            (outputFile, inputFile) -> "--outFile " + outputFile.toAbsolutePath() + " " + inputFile.toAbsolutePath());
    }

    public static CodeCompiler newJsppCompiler() {
        return CodeCompiler.newExternalToolCompiler("js++", ".jspp",
            (outputFile, inputFile) -> inputFile.toAbsolutePath() + " -o " + outputFile.toAbsolutePath());
    }

    public static CodeCompiler newDartCompiler() {
        return CodeCompiler.newExternalToolCompiler("dart2js", ".dart",
            (outputFile, inputFile) -> "-o " + outputFile.toAbsolutePath() + " " + inputFile.toAbsolutePath());
    }

}
