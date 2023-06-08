package com.viral32111.askai.config

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
	@Required @SerialName( "openai-api-key" ) val openAIAPIKey: String = "",
)
