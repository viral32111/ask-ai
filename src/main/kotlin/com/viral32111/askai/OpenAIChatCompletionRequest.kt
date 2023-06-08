package com.viral32111.askai

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIChatCompletionRequest(
	@Required @SerialName( "model" ) val model: String = "gpt-3.5-turbo",
	@Required @SerialName( "messages" ) val messages: Array<OpenAIChatCompletionMessage> = emptyArray(),
	@SerialName( "max_tokens" ) val maximumTokens: Int = 150,
	@Required @SerialName( "user" ) val uuid: String,
)
