package com.viral32111.askai

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.viral32111.askai.config.Configuration
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Suppress( "UNUSED" )
class AskAI: DedicatedServerModInitializer {
	companion object {
		private const val MOD_ID = "askai"
		private val LOGGER: Logger = LoggerFactory.getLogger( "com.viral32111.$MOD_ID" )

		@OptIn( ExperimentalSerializationApi::class )
		private val JSON = Json {
			prettyPrint = true
			prettyPrintIndent = "\t"
			ignoreUnknownKeys = true
		}

		private val HTTP_CLIENT_JSON = Json {
			prettyPrint = false
			ignoreUnknownKeys = true
		}

		// https://ktor.io/docs/serialization-client.html#register_json
		private val HTTP_CLIENT = HttpClient( CIO ) {
			install( ContentNegotiation ) {
				json( HTTP_CLIENT_JSON )
			}
		}

		private const val CONFIGURATION_DIRECTORY_NAME = "viral32111"
		private const val CONFIGURATION_FILE_NAME = "$MOD_ID.json"

		private var configuration = Configuration()
	}

	override fun onInitializeServer() {
		LOGGER.info( "Ask AI initialized on the server." )

		configuration = loadConfigurationFile()

		registerCommands()
	}

	private fun loadConfigurationFile(): Configuration {
		val serverConfigurationDirectory = FabricLoader.getInstance().configDir
		val configurationDirectory = serverConfigurationDirectory.resolve( CONFIGURATION_DIRECTORY_NAME )
		val configurationFile = configurationDirectory.resolve( CONFIGURATION_FILE_NAME )

		if ( configurationDirectory.notExists() ) {
			configurationDirectory.createDirectory()
			LOGGER.info( "Created directory '${ configurationDirectory }' for configuration files." )
		}

		if ( configurationFile.notExists() ) {
			val configAsJSON = JSON.encodeToString( Configuration() )

			configurationFile.writeText( configAsJSON, options = arrayOf(
				StandardOpenOption.CREATE_NEW,
				StandardOpenOption.WRITE
			) )

			LOGGER.info( "Created configuration file '${ configurationFile }'." )
		}

		val configAsJSON = configurationFile.readText()
		val config = JSON.decodeFromString<Configuration>( configAsJSON )
		LOGGER.info( "Loaded configuration from file '${ configurationFile }'" )

		return config
	}

	private fun registerCommands() {
		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			dispatcher.register( literal( "ai" ).then( CommandManager.argument( "question", StringArgumentType.greedyString() ).executes { context ->
				val player = context.source.player

				// This probably happens if the server console executes the command
				if ( player == null ) {
					LOGGER.error( "Player is null within command execution!?" )
					throw SimpleCommandExceptionType( Text.literal( "Something went horribly wrong while attempting to execute that command." ) ).create() // TODO: Should really be Text.translatable()
				}

				val question = StringArgumentType.getString( context, "question" )
				LOGGER.info( "Player '${ player.displayName.string }' (${ player.uuidAsString }) asked AI '${ question }'" )

				if ( configuration.openAIAPIKey.isEmpty() ) {
					context.source.sendError( Text.literal("No OpenAI API key has been configured!" ) )
					return@executes 0
				}

				CoroutineScope( Dispatchers.IO ).launch {
					val answer = queryOpenAIChatCompletionAPI( question, player )
					if ( answer.isNullOrEmpty() ) {
						LOGGER.error( "OpenAI API chat completion API response message content is null/empty!" )
						context.source.sendError( Text.literal("An error occurred while querying the OpenAI API. See the server console for more details." ) )
					} else {
						player.sendMessage( Text.literal( answer ) )
					}
				}

				player.sendMessage( Text.literal( "Generating a response to your question, please wait..." ) )

				return@executes Command.SINGLE_SUCCESS
			} ) )
		}

		LOGGER.info( "Registered Ask AI chat command." )
	}

	// https://platform.openai.com/docs/api-reference/chat/create
	private suspend fun queryOpenAIChatCompletionAPI( question: String, player: ServerPlayerEntity ): String? {
		try {
			val requestBody = HTTP_CLIENT_JSON.encodeToString( OpenAIChatCompletionRequest(
				messages = arrayOf(
					OpenAIChatCompletionMessage( "system", "You answer questions about Minecraft." ),
					OpenAIChatCompletionMessage( "user", question, player.displayName.string )
				),
				uuid = player.uuidAsString
			) )

			// https://ktor.io/docs/request.html#upload_file
			LOGGER.info( "Sending HTTP request to OpenAI API... (${ requestBody })" )
			val response: HttpResponse = HTTP_CLIENT.post( "https://api.openai.com/v1/chat/completions" ) {
				contentType( ContentType.Application.Json )
				bearerAuth( configuration.openAIAPIKey )
				setBody( requestBody )
			}
			LOGGER.info( "Received response from OpenAI API: '${ response.status }'" )

			return if ( response.status == HttpStatusCode.OK ) {
				val data = response.body<JsonObject>()
				data[ "choices" ]?.jsonArray?.get( 0 )?.jsonObject?.get( "message" )?.jsonObject?.get( "content" )?.jsonPrimitive?.contentOrNull?.trim()
			} else {
				LOGGER.error( "Unsuccessful HTTP status code '${ response.status }' from querying OpenAI API (${ response.bodyAsText() })" )
				null
			}
		} catch ( exception: Exception ) {
			LOGGER.error( "Error while querying OpenAI API: '${ exception.message }'" )
			return null
		}
	}
}
