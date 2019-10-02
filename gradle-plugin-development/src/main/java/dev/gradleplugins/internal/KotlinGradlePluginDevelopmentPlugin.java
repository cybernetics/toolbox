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

package dev.gradleplugins.internal;

import dev.gradleplugins.GradlePlugin;
import dev.gradleplugins.internal.tasks.FakeAnnotationProcessorTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

@GradlePlugin(id = "dev.gradleplugins.kotlin-gradle-plugin")
public class KotlinGradlePluginDevelopmentPlugin extends AbstractGradlePluginDevelopmentPlugin {
    @Override
    public void doApply(Project project) {
        project.getPluginManager().apply(GradlePluginDevelopmentBasePlugin.class);

        // TODO: Not totally sure if this is required for building plugins in Kotlin
        //   Actually is doesn't seems like it. kotlin-dsl is an "alias" to java-gradle-plugin and other stuff
        //   Instead, let's check to see org.jetbrains.kotlin.jvm is applied so Kotlin can be compiled.
        //   Everything else will be handled by us
//        // There is no way to properly apply this plugin automatically
//        // project.pluginManager.apply("org.gradle.kotlin.kotlin-dsl")
//        // Instead we will crash the build if not applied manually
//        project.afterEvaluate( proj -> {
//            if (project.getPluginManager().findPlugin("org.gradle.kotlin.kotlin-dsl") == null) {
//                throw new GradleException("You need to manually apply the `kotlin-dsl` plugin inside the plugin block:\n" +
//                        "plugins {\n" +
//                        "    `kotlin-dsl`\n" +
//                        "}");
//            }
//        });

        // TODO: I think this is added by the kotlin-dsl, we will leave it out for the next versions
//        project.getDependencies().add("implementation", kotlin("gradle-plugin"));

        TaskProvider<FakeAnnotationProcessorTask> fakeAnnotationProcessorTask = project.getTasks().register("fakeAnnotationProcessing", FakeAnnotationProcessorTask.class, task -> {
            task.getPluginDescriptorDirectory().set(project.getLayout().getBuildDirectory().dir("pluginDescriptors"));
        });

        project.getExtensions().getByType(SourceSetContainer.class).getByName("main").getResources().srcDir(fakeAnnotationProcessorTask.flatMap(FakeAnnotationProcessorTask::getPluginDescriptorDirectory));

        // TODO: lint java-gradle-plugin extension VS the annotation

        project.getTasks().named("pluginDescriptors", it -> {
            it.dependsOn(fakeAnnotationProcessorTask);
            it.setEnabled(false);
        });
    }

    @Override
    protected String getPluginId() {
        return "dev.gradleplugins.kotlin-gradle-plugin";
    }
}