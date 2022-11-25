plugins {
    alias(libs.plugins.buildconfig)
    groovy
    id("minecraft")
    id("publish")
}

repositories {
    maven("https://maven.accident.space/repository/maven-public/")
    maven("http://jenkins.usrv.eu:8081/nexus/content/groups/public/") { isAllowInsecureProtocol = true }
    maven("https://jitpack.io")
    maven("https://cursemaven.com") { content { includeGroup("curse.maven") } }
    maven("https://maven2.ic2.player.to/") { metadataSources { mavenPom(); artifact() } }
    mavenCentral()
    mavenLocal()
}

val modId: String by extra
val modName: String by extra
val modGroup: String by extra

buildConfig {
    packageName("space.impact.$modId")
    buildConfigField("String", "MODID", "\"${modId}\"")
    buildConfigField("String", "MODNAME", "\"${modName}\"")
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("String", "GROUPNAME", "\"${modGroup}\"")
    useKotlinOutput { topLevelConstants = true }
}

dependencies {
    api("space.impact:impactapi:0.0.+:dev") { isChanging = true }
    api("com.github.GTNewHorizons:NotEnoughItems:2.3.+:dev") { isChanging = true }
    api("com.github.GTNewHorizons:ModularUI:1.1.10:dev")
    api("net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev")
    api("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-213-GTNH:dev")
    api("com.github.GTNewHorizons:Nuclear-Control:2.4.+:dev") { isTransitive = false; isChanging = true }

    api("com.github.GTNewHorizons:EnderIO:2.4.16:dev") { isTransitive = false }
    api("com.github.GTNewHorizons:BuildCraft:7.1.33:dev") { isTransitive = false }
    api("com.github.GTNewHorizons:Railcraft:9.14.1:dev") { isTransitive = false }

    compileOnly("com.github.GTNewHorizons:Chisel:2.11.0-GTNH:dev") { isTransitive = false }
    compileOnly("com.github.GTNewHorizons:Translocators:1.1.2.21:dev") { isTransitive = false }
    compileOnly("curse.maven:cofh-core-69162:2388751") { isTransitive = false }
}

apply(from = "runConf.gradle")
