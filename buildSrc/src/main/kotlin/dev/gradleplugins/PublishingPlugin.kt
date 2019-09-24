package dev.gradleplugins

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import java.text.SimpleDateFormat
import java.util.*

class PublishingPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        applyPublishingPlugin()
        configurePublishingExtension()
        configureBintrayExtension()
    }

    private
    fun Project.applyPublishingPlugin() {
        apply<PublishingBasePlugin>()
        apply<MavenPublishPlugin>()
        apply<BintrayPlugin>()
    }

    private
    fun Project.configurePublishingExtension() {
        val shadowJar = tasks.named<ShadowJar>("shadowJar")
        val sourcesJar = tasks.named<Jar>("sourcesJar")
        val groovydocJar = tasks.named<Jar>("groovydocJar")
        val javadocJar = tasks.named<Jar>("javadocJar")

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    artifact(shadowJar.get())
                    artifact(sourcesJar.get())
                    artifact(groovydocJar.get())
                    artifact(javadocJar.get())

                    pom {
                        name.set("Gradle TestKit Fixtures")
                        description.set(project.provider { project.description })
                        inceptionYear.set("2019")

                        developers {
                            developer {
                                id.set("lacasseio")
                                name.set("Daniel Lacasse")
                                url.set("https://github.com/lacasseio")
                            }
                        }
                    }
                }
            }
        }
    }

    private
    fun Project.configureBintrayExtension() {
        afterEvaluate {
            val packageName = "${project.group}:${project.name}"

            configure<BintrayExtension> {
                setPublications("mavenJava")

                pkg(closureOf<BintrayExtension.PackageConfig> {
                    name = packageName
                    setLabels("gradle", "gradle-plugins")
                    publicDownloadNumbers = true
                })
            }
        }
    }
}