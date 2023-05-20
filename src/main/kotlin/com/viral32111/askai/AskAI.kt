package com.viral32111.askai

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

@Suppress( "UNUSED" )
object AskAI: ModInitializer {
	private const val MOD_ID = "ask-ai"
	private val LOGGER = LoggerFactory.getLogger( "ask-ai" )

	override fun onInitialize() {
		LOGGER.info( "Ask AI has been initialized." )
	}
}
