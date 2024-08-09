plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.dokka") version "1.9.20"
    id("io.github.jeadyx.sonatype-uploader") version "2.8"
}

android {
    namespace = "io.github.jeadyx.sqlitebackuprestore"
    compileSdk = 34

    defaultConfig {
        minSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}


group = "io.github.jeadyx"
version = "1.2"
val tokenUsername:String by project
val tokenPassword:String by project
sonatypeUploader {
    bundleName = "SqliteBackupRestore-$version"
    tokenName = tokenUsername
    tokenPasswd = tokenPassword
    pom = Action<MavenPom> {
        name.set("SqliteBackupRestore")
        description.set("A library for backup and restore sqlite database")
        url.set("https://github.com/jeadyx/SqliteBackupRestore")
        scm {
            connection.set("scm:git:git://github.com/jeadyx/SqliteBackupRestore.git")
            developerConnection.set("scm:git:ssh://github.com/jeadyx/SqliteBackupRestore.git")
            url.set("https://github.com/jeadyx/SqliteBackupRestore")
        }
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id.set("jeadyx")
                name.set("Jeady")
            }
        }
        withXml {
            val dependenciesNode = asNode().appendNode("dependencies")
            val dependencyGitManager = dependenciesNode.appendNode("dependency")
            dependencyGitManager.appendNode("groupId", "io.github.jeadyx.compose")
            dependencyGitManager.appendNode("artifactId", "gitVersionManager")
            dependencyGitManager.appendNode("version", "1.3")
            val dependencyUtil = dependenciesNode.appendNode("dependency")
            //pkg:maven/io.github.jeadyx.compose/util
            dependencyUtil.appendNode("groupId", "io.github.jeadyx.compose")
            dependencyUtil.appendNode("artifactId", "util")
            dependencyUtil.appendNode("version", "1.1")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.gitversionmanager)
    implementation(libs.util)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}