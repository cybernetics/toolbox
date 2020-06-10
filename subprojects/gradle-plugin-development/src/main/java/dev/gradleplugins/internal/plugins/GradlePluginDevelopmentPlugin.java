package dev.gradleplugins.internal.plugins;

import dev.gradleplugins.internal.GradlePluginDevelopmentDependencyExtensionInternal;
import dev.gradleplugins.internal.GradlePluginDevelopmentRepositoryExtensionInternal;
import lombok.RequiredArgsConstructor;
import org.gradle.BuildAdapter;
import org.gradle.BuildResult;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.exceptions.DefaultMultiCauseException;

import java.util.Arrays;
import java.util.List;

public abstract class GradlePluginDevelopmentPlugin implements Plugin<Object> {
    private static final Logger LOGGER = Logging.getLogger(GradlePluginDevelopmentPlugin.class);

    @Override
    public void apply(Object target) {
        if (!Settings.class.isInstance(target)) {
            throw new IllegalArgumentException("Please apply 'dev.gradleplugins.gradle-plugin-development' plugin inside the settings.gradle[.kts] script.");
        }
        doApply((Settings)target);
    }

    private void doApply(Settings settings) {
        settings.getGradle().addBuildListener(new BuildAdapter() {
            @Override
            public void buildFinished(BuildResult result) {
                if (result.getFailure() != null) {
                    MissingGradlePluginDevelopmentArtifactsErrorReporter missingArtifacts = new MissingGradlePluginDevelopmentArtifactsErrorReporter();
                    MissingGradleApiRuntimeDependenciesErrorReporter missingGradleApiRuntimeDependencies = new MissingGradleApiRuntimeDependenciesErrorReporter();

                    GradleFailureVisitor root = new UnwrappingGradleFailureVisitor(MultiGradleFailureVisitor.of(missingArtifacts, missingGradleApiRuntimeDependencies));
                    root.visitCause(result.getFailure());

                    missingArtifacts.report();
                    missingGradleApiRuntimeDependencies.report();
                }
            }
        });
        settings.getGradle().rootProject(rootProject -> {
            rootProject.allprojects(this::applyToProject);
        });
    }

    private void applyToProject(Project project) {
        applyToRepositories(project.getRepositories());
        applyToDependencies(project.getDependencies());
        project.afterEvaluate(this::warnWhenUsingCoreGradlePluginDevelopment);
    }

    private void applyToRepositories(RepositoryHandler repositories) {
        GradlePluginDevelopmentRepositoryExtensionInternal extension = new GradlePluginDevelopmentRepositoryExtensionInternal(repositories);
        extension.applyTo(repositories);
    }

    private void applyToDependencies(DependencyHandler dependencies) {
        GradlePluginDevelopmentDependencyExtensionInternal extension = new GradlePluginDevelopmentDependencyExtensionInternal(dependencies);
        extension.applyTo(dependencies);
    }

    private void warnWhenUsingCoreGradlePluginDevelopment(Project project) {
        if (project.getPluginManager().hasPlugin("java-gradle-plugin") && !project.getPluginManager().hasPlugin("dev.gradleplugins.java-gradle-plugin")) {
            LOGGER.warn(String.format("The Gradle Plugin Development team recommends using 'dev.gradleplugins.java-gradle-plugin' instead of 'java-gradle-plugin' in project '%s'.", project.getPath()));
        }
    }

    private interface GradleFailureVisitor {
        void visitCause(Throwable cause);
    }

    @RequiredArgsConstructor
    private static class UnwrappingGradleFailureVisitor implements GradleFailureVisitor {
        private final GradleFailureVisitor delegate;

        @Override
        public void visitCause(Throwable cause) {
            while (cause != null) {
                if (cause instanceof DefaultMultiCauseException) {
                    for (Throwable c : ((DefaultMultiCauseException) cause).getCauses()) {
                        visitCause(c);
                    }
                }
                delegate.visitCause(cause);
                cause = cause.getCause();
            }
        }
    }

    @RequiredArgsConstructor
    private static class MultiGradleFailureVisitor implements GradleFailureVisitor {
        private final List<GradleFailureVisitor> delegates;

        @Override
        public void visitCause(Throwable cause) {
            for (GradleFailureVisitor delegate : delegates) {
                delegate.visitCause(cause);
            }
        }

        public static MultiGradleFailureVisitor of(GradleFailureVisitor... delegates) {
            return new MultiGradleFailureVisitor(Arrays.asList(delegates));
        }
    }

    private static class MissingGradleApiRuntimeDependenciesErrorReporter implements GradleFailureVisitor {
        private boolean missingKotlinStdlib = false;
        private boolean missingGroovyAll = false;

        @Override
        public void visitCause(Throwable cause) {
            if (cause.getMessage().startsWith("Could not find org.codehaus.groovy:groovy-all") && cause.getMessage().contains("dev.gradleplugins:gradle-api:")) {
                missingGroovyAll = true;
            } else if (cause.getMessage().startsWith("Could not find org.jetbrains.kotlin:kotlin-stdlib") && cause.getMessage().contains("dev.gradleplugins:gradle-api:")) {
                missingKotlinStdlib = true;
            }
        }

        public void report() {
            if (missingGroovyAll || missingKotlinStdlib) {
                LOGGER.error("Please verify Gradle API was intentionally declared for runtime usage, see https://nokee.dev/docs/current/manual/gradle-plugin-development.html#sec:gradle-dev-compileonly-vs-implementation.");
            }

            if (missingGroovyAll && missingKotlinStdlib) {
                LOGGER.error("If runtime usage of the Gradle API is expected, please declare a repository containing org.codehaus.groovy:groovy-all and org.jetbrains.kotlin:kotlin-stdlib artifacts, i.e. repositories.mavenCentral().");
            } else if (missingGroovyAll) {
                LOGGER.error("If runtime usage of the Gradle API is expected, Please declare a repository containing org.codehaus.groovy:groovy-all artifacts, i.e. repositories.mavenCentral().");
            } else if (missingKotlinStdlib) {
                LOGGER.error("If runtime usage of the Gradle API is expected, Please declare a repository containing org.jetbrains.kotlin:kotlin-stdlib artifacts, i.e. repositories.mavenCentral().");
            }
        }
    }

    private static class MissingGradlePluginDevelopmentArtifactsErrorReporter implements GradleFailureVisitor {
        private boolean shouldReport = false;
        @Override
        public void visitCause(Throwable cause) {
            if (cause.getMessage().startsWith("Cannot resolve external dependency dev.gradleplugins:") || cause.getMessage().startsWith("Could not find dev.gradleplugins:")) {
                shouldReport = true;
            }
        }

        public void report() {
            if (shouldReport) {
                LOGGER.error("Please declare a repository using repositories.gradlePluginDevelopment().");
            }
        }
    }
}
