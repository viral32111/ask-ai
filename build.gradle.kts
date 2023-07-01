plugins {
	id( "fabric-loom" )
	id( "maven-publish" )
	kotlin( "jvm" ).version( System.getProperty( "kotlin_version" ) )
	kotlin( "plugin.serialization" ) version( System.getProperty( "kotlin_version" ) )
}

base {
	archivesName.set( project.extra[ "archives_base_name" ] as String )
}
version = project.extra[ "mod_version" ] as String
group = project.extra[ "maven_group" ] as String

repositories {}

// https://github.com/Crec0/McServerApi/blob/main/build.gradle.kts
val transitiveInclude: Configuration by configurations.creating {
	exclude( group = "org.jetbrains.kotlin" )
	exclude( group = "com.mojang" )
}

dependencies {

	// Minecraft
	minecraft( "com.mojang", "minecraft", project.extra[ "minecraft_version" ] as String )

	// Minecraft source mappings - https://github.com/FabricMC/yarn
	mappings( "net.fabricmc", "yarn", project.extra[ "yarn_mappings" ] as String, null, "v2" )

	// Fabric Loader - https://github.com/FabricMC/fabric-loader
	modImplementation( "net.fabricmc", "fabric-loader", project.extra[ "loader_version" ] as String )

	// Fabric API - https://github.com/FabricMC/fabric
	modImplementation( "net.fabricmc.fabric-api", "fabric-api", project.extra[ "fabric_version" ] as String )

	// Kotlin support for Fabric - https://github.com/FabricMC/fabric-language-kotlin
	modImplementation( "net.fabricmc", "fabric-language-kotlin", project.extra[ "fabric_language_kotlin_version" ] as String )

	// Kotlin serialization
	implementation( "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1" )

	// Ktor client
	transitiveInclude( implementation( "io.ktor:ktor-client-core:2.3.1" )!! )
	transitiveInclude( implementation( "io.ktor:ktor-client-cio:2.3.1" )!! )
	transitiveInclude( implementation( "io.ktor:ktor-client-json:2.3.1" )!! )
	transitiveInclude( implementation( "io.ktor:ktor-client-json-jvm:2.3.1" )!! )
	transitiveInclude( implementation( "io.ktor:ktor-client-serialization:2.3.1" )!! )
	transitiveInclude( implementation( "io.ktor:ktor-client-serialization-jvm:2.3.2" )!! )
	transitiveInclude( implementation( "io.ktor:ktor-client-content-negotiation:2.3.1" )!! )
	transitiveInclude( implementation( "io.ktor:ktor-serialization-kotlinx-json:2.3.1" )!! )

	transitiveInclude.resolvedConfiguration.resolvedArtifacts.forEach {
		include( it.moduleVersion.id.toString() )
	}
}

tasks {
	val javaVersion = JavaVersion.toVersion( ( project.extra[ "java_version" ] as String ).toInt() )

	withType<JavaCompile> {
		options.encoding = "UTF-8"
		sourceCompatibility = javaVersion.toString()
		targetCompatibility = javaVersion.toString()
		options.release.set( javaVersion.toString().toInt() )
	}

	withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
		kotlinOptions {
			jvmTarget = javaVersion.toString()
		}
	}

	jar {
		from( "LICENSE.txt" ) {
			rename { "${ it }_${ base.archivesName.get() }.txt" }
		}
	}

	processResources {

		// Metadata
		filesMatching( "fabric.mod.json" ) {
			expand( mutableMapOf(
				"version" to project.extra[ "mod_version" ] as String,
				"java" to project.extra[ "java_version" ] as String,
				"minecraft" to project.extra[ "minecraft_version" ] as String,
				"fabricloader" to project.extra[ "loader_version" ] as String,
				"fabric_api" to project.extra[ "fabric_version" ] as String,
				"fabric_language_kotlin" to project.extra[ "fabric_language_kotlin_version" ] as String
			) )
		}

		// Mixins
		filesMatching( "*.mixins.json" ) {
			expand( mutableMapOf(
				"java" to project.extra[ "java_version" ] as String
			) )
		}

	}

	java {
		toolchain {
			//languageVersion.set( JavaLanguageVersion.of( javaVersion.toString() ) )
		}

		sourceCompatibility = javaVersion
		targetCompatibility = javaVersion

		withSourcesJar()
	}
}
