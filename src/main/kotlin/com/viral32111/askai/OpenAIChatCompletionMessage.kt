package com.viral32111.askai

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIChatCompletionMessage(
	@Required @SerialName( "role" ) val role: String = "",
	@Required @SerialName( "content" ) val content: String = "",
	@SerialName( "name" ) val name: String = ""
)
