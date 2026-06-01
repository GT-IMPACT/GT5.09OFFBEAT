import settings.getVersionMod

plugins {
    alias(libs.plugins.setup.minecraft)
    alias(libs.plugins.setup.publish)
    id(libs.plugins.buildconfig.get().pluginId)
}

val modId: String by extra
val modName: String by extra
val modGroup: String by extra

extra.set("modVersion", getVersionMod())

buildConfig {
    packageName("space.impact.$modId")
    buildConfigField("String", "MODID", "\"${modId}\"")
    buildConfigField("String", "MODNAME", "\"${modName}\"")
    buildConfigField("String", "VERSION", "\"${getVersionMod()}\"")
    buildConfigField("String", "GROUPNAME", "\"${modGroup}\"")
    useKotlinOutput { topLevelConstants = true }
}

repositories {
    maven("https://maven.accident.space/repository/maven-public/") {
        mavenContent {
            includeGroup("space.impact")
            includeGroup("com.github.GTNewHorizons")
            includeGroupByRegex("space\\.impact\\..+")
        }
        credentials {
            username = System.getenv("MAVEN_USER") ?: "NONE"
            password = System.getenv("MAVEN_PASSWORD") ?: "NONE"
        }
    }
}

dependencies {
    // impact
    api("space.impact:ImpactAPI:0.0.4:dev")
    api("space.impact:VirtualWorld:2.0.0d.dirty:dev")
    compileOnlyApi("space.impact:impact-core:1.1.0.25-1-gb6211b5.dirty:dev") { isTransitive = false }
    runtimeOnlyNonPublishable("space.impact:VisualProspecting:1.3.2")

    // maven impact
    api("com.github.GTNewHorizons:CodeChickenCore:1.3.11:dev") {
        version { strictly("1.3.11") }
    }
    api("com.github.GTNewHorizons:NotEnoughItems:2.6.0-GTNH:dev")
    api("com.github.GTNewHorizons:modularui:1.1.24:dev")
    api("com.github.GTNewHorizons:appliedenergistics2:rv3-beta-400-GTNH:dev")
    api("com.github.GTNewHorizons:IC2NuclearControl:2.6.2:dev")
    compileOnlyApi("com.github.GTNewHorizons:Translocator:1.1.2.21:dev")
    compileOnlyApi("com.github.GTNewHorizons:chisel:2.11.0-GTNH")

    // other
    api("net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev")
    compileOnly("curse.maven:cofh-core-69162:2388751") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:AppleCore:3.3.4:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:BuildCraft:7.1.42:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:EnderIO:2.9.3:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:ForestryMC:4.10.1:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:ProjectRed:4.11.1-GTNH:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:Railcraft:9.16.3:dev") { isTransitive = false }
    runtimeOnlyNonPublishable("com.github.GTNewHorizons:DuraDisplay:1.3.2:dev")
}
