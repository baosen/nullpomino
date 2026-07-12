package nullpomino.build.teavm;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.vm.TeaVMOptimizationLevel;

/** Bazel-facing TeaVM driver. Configuration is supplied by {@code teavm_js}. */
public final class TeaVMCompiler {
    private static final int FIXED_ARGUMENT_COUNT = 5;

    private TeaVMCompiler() {}

    public static void main(String[] args) throws Exception {
        if (args.length < FIXED_ARGUMENT_COUNT + 1) {
            throw new IllegalArgumentException(
                    "expected output, main class, optimization, strict, obfuscated, and classpath");
        }

        File output = new File(args[0]);
        String mainClass = args[1];
        TeaVMOptimizationLevel optimization = TeaVMOptimizationLevel.valueOf(args[2]);
        boolean strict = Boolean.parseBoolean(args[3]);
        boolean obfuscated = Boolean.parseBoolean(args[4]);

        List<File> applicationClassPath = new ArrayList<>();
        for (int i = FIXED_ARGUMENT_COUNT; i < args.length; i++) {
            applicationClassPath.add(new File(args[i]));
        }

        List<File> compilationClassPath = compilationClassPath(applicationClassPath);
        URL[] applicationUrls = applicationClassPath.stream()
                .map(TeaVMCompiler::toUrl)
                .toArray(URL[]::new);

        TeaVMToolLog log = new ConsoleTeaVMToolLog(false);
        try (URLClassLoader classLoader = new URLClassLoader(
                applicationUrls, TeaVMCompiler.class.getClassLoader())) {
            TeaVMTool tool = new TeaVMTool();
            tool.setClassLoader(classLoader);
            tool.setClassPath(compilationClassPath);
            tool.setLog(log);
            tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
            tool.setMainClass(mainClass);
            tool.setTargetDirectory(output.getParentFile());
            tool.setTargetFileName(output.getName());
            tool.setOptimizationLevel(optimization);
            tool.setJsModuleType(JSModuleType.UMD);
            tool.setStrict(strict);
            tool.setObfuscated(obfuscated);
            tool.setIncremental(false);
            tool.setDebugInformationGenerated(false);
            tool.setSourceMapsFileGenerated(false);
            tool.getProperties().putAll(System.getProperties());

            tool.generate();
            TeaVMProblemRenderer.describeProblems(
                    tool.getDependencyInfo().getCallGraph(), tool.getProblemProvider(), log);
            if (!tool.getProblemProvider().getSevereProblems().isEmpty()) {
                throw new IllegalStateException("TeaVM compilation failed with severe problems");
            }
        }
    }

    private static List<File> compilationClassPath(List<File> applicationClassPath) {
        Set<File> result = new LinkedHashSet<>(applicationClassPath);
        String javaClassPath = System.getProperty("java.class.path", "");
        if (!javaClassPath.isEmpty()) {
            for (String entry : javaClassPath.split(File.pathSeparator)) {
                if (!entry.isEmpty()) {
                    result.add(new File(entry));
                }
            }
        }
        return new ArrayList<>(result);
    }

    private static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid classpath entry: " + file, e);
        }
    }
}
