/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("jade2.kotlin-application-conventions")
}

dependencies {
    implementation("org.apache.commons:commons-text")

    implementation("com.github.ajalt.clikt:clikt:3.4.0")

  // `.class` file parsing and analysis
  implementation("org.ow2.asm:asm:9.2")
  implementation("org.ow2.asm:asm-analysis:9.2")
  implementation("org.ow2.asm:asm-commons:9.2")
  //implementation("org.ow2.asm:asm-test:9.2")
  implementation("org.ow2.asm:asm-tree:9.2")
  implementation("org.ow2.asm:asm-util:9.2")

    implementation(project(":lib"))
}

application {
    // Define the main class for the application.
    mainClass.set("jade2.app.AppKt")
}
