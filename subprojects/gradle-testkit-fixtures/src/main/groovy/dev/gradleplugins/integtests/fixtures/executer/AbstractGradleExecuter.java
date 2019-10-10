package dev.gradleplugins.integtests.fixtures.executer;

import dev.gradleplugins.test.fixtures.file.TestDirectoryProvider;
import dev.gradleplugins.test.fixtures.file.TestFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractGradleExecuter implements GradleExecuter {
    private final TestDirectoryProvider testDirectoryProvider;

    public AbstractGradleExecuter(TestDirectoryProvider testDirectoryProvider) {
        this.testDirectoryProvider = testDirectoryProvider;
    }

    @Override
    public TestDirectoryProvider getTestDirectoryProvider() {
        return testDirectoryProvider;
    }

    //region Working directory
    private File workingDirectory = null;
    public File getWorkingDirectory() {
        return workingDirectory == null ? getTestDirectoryProvider().getTestDirectory() : workingDirectory;
    }

    @Override
    public GradleExecuter inDirectory(File directory) {
        workingDirectory = directory;
        return this;
    }
    //endregion

    //region User home directory (java.home)
    private File userHomeDirectory = null;
    @Override
    public GradleExecuter withUserHomeDirectory(File userHomeDirectory) {
        this.userHomeDirectory = userHomeDirectory;
        return this;
    }
    //endregion

    //region Stacktrace (--stack-trace)
    private boolean showStacktrace = true;
    @Override
    public GradleExecuter withStacktraceDisabled() {
        showStacktrace = false;
        return this;
    }
    //endregion

    //region Settings file (--settings-file)
    private File settingsFile = null;

    @Override
    public GradleExecuter usingSettingsFile(File settingsFile) {
        this.settingsFile = settingsFile;
        return this;
    }

    // TODO: Maybe we should remove dependency on TestFile within this implementation
    private void ensureSettingsFileAvailable() {
        TestFile workingDirectory = new TestFile(getWorkingDirectory());
        TestFile directory = workingDirectory;
        while (directory != null && getTestDirectoryProvider().getTestDirectory().isSelfOrDescendent(directory)) {
            if (hasSettingsFile(directory)) {
                return;
            }
            directory = directory.getParentFile();
        }
        workingDirectory.createFile("settings.gradle");
    }

    private boolean hasSettingsFile(TestFile directory) {
        if (directory.isDirectory()) {
            return directory.file("settings.gradle").isFile() || directory.file("settings.gradle.kts").isFile();
        }
        return false;
    }
    //endregion

    //region Process arguments
    private final List<String> arguments = new ArrayList<>();

    @Override
    public GradleExecuter withArguments(String... args) {
        return withArguments(Arrays.asList(args));
    }

    @Override
    public GradleExecuter withArguments(List<String> args) {
        arguments.clear();
        arguments.addAll(args);
        return this;
    }

    @Override
    public GradleExecuter withArgument(String arg) {
        arguments.add(arg);
        return this;
    }
    //endregion

    protected void reset() {
        arguments.clear();

        workingDirectory = null;
        userHomeDirectory = null;
        settingsFile = null;

        showStacktrace = true;
    }

    protected List<String> getAllArguments() {
        List<String> allArguments = new ArrayList<>();

        // JVM arguments
        if (userHomeDirectory != null) {
            allArguments.add("-Duser.home=" + userHomeDirectory.getAbsolutePath());
        }

        // Gradle arguments
        if (settingsFile != null) {
            allArguments.add("--settings-file");
            allArguments.add(settingsFile.getAbsolutePath());
        }
        if (showStacktrace) {
            allArguments.add("--stacktrace");
        }

        // Deal with missing settings.gradle[.kts] file
        if (settingsFile == null) {
            ensureSettingsFileAvailable();
        }

        allArguments.addAll(arguments);

        return allArguments;
    }
}