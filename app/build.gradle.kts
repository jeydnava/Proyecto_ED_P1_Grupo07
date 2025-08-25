plugins {
    id 'java';
    id 'application';
    id 'org.openjfx.javafxplugin' version '0.1.0'
}

group = 'ed_p1_grupo07'
version = '1.0-SNAPSHOT'

repositories {
    mavenCentral()
}


javafx {
    version = '24'
    modules = ['javafx.controls', 'javafx.fxml']
}


application {
    mainClass = 'ed_p1_grupo07.MainApplication'
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}