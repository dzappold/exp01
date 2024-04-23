plugins {
    `java-base`
}

allprojects{
    tasks.withType<Jar>{
        //archiveAppendix = rootProject.name
        archiveBaseName = rootProject.name +"-"+ project.name
    }
}
