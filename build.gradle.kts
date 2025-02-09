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
            includeGroupByRegex("space\\.impact\\..+")
        }
    }
}

dependencies {
    api("space.impact:Impact-API:0.0.4:dev")
    api("space.impact:VirtualWorld:1.4.1:dev")
    api("com.github.GTNewHorizons:NotEnoughItems:2.3.+:dev") { isChanging = true }
    api("com.github.GTNewHorizons:ModularUI:1.1.10:dev")
    api("net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev")
    api("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-213-GTNH:dev")
    api("com.github.GTNewHorizons:Nuclear-Control:2.4.+:dev") { isTransitive = false; isChanging = true }

    compileOnly("com.github.GTNewHorizons:Chisel:2.11.0-GTNH:dev") { isTransitive = false }
    compileOnly("com.github.GTNewHorizons:Translocators:1.1.2.21:dev") { isTransitive = false }
    compileOnly("curse.maven:cofh-core-69162:2388751") { isTransitive = false }

    compileOnlyApi("com.github.GTNewHorizons:AppleCore:3.3.4:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:BuildCraft:7.1.42:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:EnderIO:2.9.3:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:ForestryMC:4.10.1:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:ProjectRed:4.11.1-GTNH:dev") { isTransitive = false }
    compileOnlyApi("com.github.GTNewHorizons:Railcraft:9.16.3:dev") { isTransitive = false }

    runtimeOnlyNonPublishable("space.impact:VisualProspecting:1.3.2")
}
