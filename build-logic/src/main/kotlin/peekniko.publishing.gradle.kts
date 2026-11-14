import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.vanniktech.maven.publish")
    signing
}

val baseVersion = extensions.getByType<VersionCatalogsExtension>()
    .named("libs")
    .findVersion("peekniko")
    .get()
    .requiredVersion
val snapshotVersion = "$baseVersion-SNAPSHOT"
val peeknikoVersion = providers.gradleProperty("peeknikoVersion").orNull ?: snapshotVersion

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    signAllPublications()

    coordinates(
        groupId = "io.github.otacon.peekniko",
        version = peeknikoVersion,
    )

    pom {
        name = "Peekniko"
        description = "A network monitoring library"
        url = "https://github.com/Otacon/peekniko"

        developers {
            developer {
                id = "Otacon"
                name = "Orfeo Ciano"
                email = "orfeo18+peekniko@gmail.com"
                organization = "Peekniko"
                organizationUrl = "https://github.com/Otacon/peekniko"
            }
        }

        scm {
            connection = "scm:git:https://github.com/Otacon/peekniko.git"
            developerConnection = "scm:git:ssh://git@github.com/Otacon/peekniko.git"
            url = "https://github.com/Otacon/peekniko"
        }

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingInMemoryKey") as? String
    val signingPassword = findProperty("signingInMemoryKeyPassword") as? String
    useInMemoryPgpKeys(signingKey, signingPassword)
}
