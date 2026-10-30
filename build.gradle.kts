plugins {
    id("minecraft-setup")
    id("publish-setup")
}

impactMod {
    modId.set("gregtech")
    modName.set("GregTech")
    modGroup.set("gregtech")
    mavenGroup.set("space.impact")
    mavenUrl.set("https://maven.accident.space/repository/maven-releases/")
    shadeDependencies.set(true)
    minimizeShadedDependencies.set(true)
    relocateShadedDependencies.set(true)
}

val modId = impactMod.modId.get()
val modName = impactMod.modName.get()
val modGroup = impactMod.modGroup.get()

buildConfig {
    packageName("space.impact.$modId")
    buildConfigField("String", "MODID", "\"${modId}\"")
    buildConfigField("String", "MODNAME", "\"${modName}\"")
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("String", "GROUPNAME", "\"${modGroup}\"")
    useKotlinOutput {
        topLevelConstants = true
        internalVisibility = false
    }
}

dependencies {

    // impact
    api("space.impact:ImpactStructures:1.0.7:dev")
    implementation("space.impact:VirtualWorld:2.0.4:dev")

    compileOnlyApi("space.impact:impact-core:1.1.0.25-1-gb6211b5.dirty:dev") { isTransitive = false }
    runtimeOnlyNonPublishable("space.impact:VisualProspecting:1.3.2")


    // maven impact

    api("com.github.GTNewHorizons:modularui:1.1.24:dev")
    api("com.github.GTNewHorizons:IC2NuclearControl:2.6.2:dev")
    compileOnlyApi("com.github.GTNewHorizons:Translocator:1.1.2.21:dev")
    compileOnlyApi("com.github.GTNewHorizons:chisel:2.11.0-GTNH")

    // other
    compileOnlyApi("com.github.GTNewHorizons:waila:1.19.34:dev") { isTransitive = false }
    runtimeOnlyNonPublishable("com.github.GTNewHorizons:waila:1.19.34:dev") { isTransitive = false }
    api("com.github.GTNewHorizons:CodeChickenCore:1.4.22:dev")
    api("com.github.GTNewHorizons:NotEnoughItems:2.8.158-GTNH:dev")
    api("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-1082-GTNH:dev")


    api("net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev")
    compileOnly("curse.maven:cofh-core-69162:2388751") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:AppleCore:3.3.4:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:BuildCraft:7.1.42:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:EnderIO:2.9.3:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:ForestryMC:4.10.1:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:ProjectRed:4.11.1-GTNH:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:Railcraft:9.16.3:dev") { isTransitive = false }
    runtimeOnlyNonPublishable("space.impact:GregTechBridge:1.0.2:dev") { isTransitive = false }
    runtimeOnlyNonPublishable("com.github.GTNewHorizons:DuraDisplay:1.4.6:dev")
    runtimeOnlyNonPublishable(modUtils.deobfuscate("curse.maven:journeymap-32274:4500659"))

    testImplementation("junit:junit:4.13.2")
    // AE stack interfaces reference Minecraft client types even in server-side storage tests.
    testRuntimeOnly("org.lwjgl.lwjgl:lwjgl:2.9.3") { isTransitive = false }
    testRuntimeOnly("it.unimi.dsi:fastutil:8.5.18")
}

tasks.test {
    // Forge 1.7.10 tests require the Java 8 URLClassLoader.
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}
