package com.viral32111.askai

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

@Suppress( "UNUSED" )
object AskAI: ModInitializer {
	private const val MOD_ID = "askai"
	private val LOGGER = LoggerFactory.getLogger( "com.viral32111.askai" )

	override fun onInitialize() {
		LOGGER.info( "Ask AI has been initialized." )
	}
}
