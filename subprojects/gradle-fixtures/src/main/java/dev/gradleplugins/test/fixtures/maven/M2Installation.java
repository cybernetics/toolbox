/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.gradleplugins.test.fixtures.maven;

import dev.gradleplugins.test.fixtures.file.TestFile;
import dev.gradleplugins.test.fixtures.gradle.executer.GradleExecuter;

import java.io.File;
import java.util.Collections;
import java.util.function.Function;

public class M2Installation implements Function<GradleExecuter, GradleExecuter> {
    private final TestFile testDirectory;
    private boolean initialized = false;
    private TestFile userHomeDirectory;
    private TestFile userM2Directory;
    private TestFile userSettingsFile;
    private TestFile globalMavenDirectory;
    private TestFile globalSettingsFile;
    private TestFile isolatedMavenRepoForLeakageChecks;
    private boolean isolateMavenLocal = true;

    public M2Installation(TestFile testDirectory) {
        this.testDirectory = testDirectory;
    }

    private void init() {
        if (!initialized) {
            userHomeDirectory = testDirectory.createDirectory("maven_home");
            userM2Directory = userHomeDirectory.createDirectory(".m2");
            userSettingsFile = userM2Directory.file("settings.xml");
            globalMavenDirectory = userHomeDirectory.createDirectory("m2_home");
            globalSettingsFile = globalMavenDirectory.file("conf/settings.xml");
            System.out.println("M2 home: " + userHomeDirectory);

            initialized = true;
        }
    }

    public TestFile getUserHomeDir() {
        init();
        return userHomeDirectory;
    }

    public TestFile getUserM2Directory() {
        init();
        return userM2Directory;
    }

    public TestFile getUserSettingsFile() {
        init();
        return userSettingsFile;
    }

    public TestFile getGlobalMavenDirectory() {
        init();
        return globalMavenDirectory;
    }

    public TestFile getGlobalSettingsFile() {
        init();
        return globalSettingsFile;
    }

    public MavenLocalRepository mavenRepo() {
        init();
        return new MavenLocalRepository(userM2Directory.file("repository"));
    }

    public M2Installation generateUserSettingsFile(MavenLocalRepository userRepository) {
        init();
        userSettingsFile.write("<settings>\n"
                        + "    <localRepository>${userRepository.rootDir.absolutePath}</localRepository>\n"
                        + "</settings>");
        return this;
    }

    public M2Installation generateGlobalSettingsFile() {
        return generateGlobalSettingsFile(mavenRepo());
    }

    public M2Installation generateGlobalSettingsFile(MavenLocalRepository globalRepository) {
        init();
        globalSettingsFile.createFile().write("<settings>\n"
                + "    <localRepository>${globalRepository.rootDir.absolutePath}</localRepository>\n"
                + "</settings>");
        return this;
    }

    @Override
    public GradleExecuter apply(GradleExecuter gradleExecuter) {
        init();
        GradleExecuter result = gradleExecuter.withUserHomeDirectory(userHomeDirectory);
        // if call `using m2`, then we disable the automatic isolation of m2
        isolateMavenLocal = false;
        if (globalMavenDirectory.exists()) {
            result = result.withEnvironmentVars(Collections.singletonMap("M2_HOME", globalMavenDirectory.getAbsolutePath()));
        }
        return result;
    }

    public GradleExecuter isolateMavenLocalRepo(GradleExecuter gradleExecuter) {
        gradleExecuter = gradleExecuter.beforeExecute(executer -> {
            if (isolateMavenLocal) {
                isolatedMavenRepoForLeakageChecks = executer.getTestDirectory().createDirectory("m2-home-should-not-be-filled");
                return setMavenLocalLocation(executer, isolatedMavenRepoForLeakageChecks);
            }
            return executer;
        });
        gradleExecuter = gradleExecuter.afterExecute(executer -> {
            if (isolateMavenLocal) {
                isolatedMavenRepoForLeakageChecks.assertIsEmptyDirectory();
            }
        });

        return gradleExecuter;
    }

    private static GradleExecuter setMavenLocalLocation(GradleExecuter gradleExecuter, File destination) {
        return gradleExecuter.withArgument("-Dmaven.repo.local=" + destination.getAbsolutePath());
    }
}

