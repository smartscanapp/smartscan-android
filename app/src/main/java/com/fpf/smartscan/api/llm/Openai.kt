package com.fpf.smartscan.api.llm

import com.fpf.smartscan.api.HttpMethod
import com.fpf.smartscan.api.RequestHandler
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody


class OpenaiClient(
    private val apiKey: String,
    private val url: String = "https://api.openai.com/v1/responses",
    private val config: LLMProviderConfig
) {

    @Serializable
    private data class Content(
        val type: String,
        val text: String = "",
        val image_url: String? = null
    )

    @Serializable
    private data class Input(
        val role: String,
        val content: List<Content>
    )

    @Serializable
    private data class TextFormat(
        val type: String,
        val name: String,
        val schema: JsonElement,
        val strict: Boolean = true
    )

    @Serializable
    private data class TextConfig(
        val format: TextFormat
    )

    @Serializable
    private data class ImageInputRequest(
        val model: String,
        val input: List<Input>,
        val temperature: Float = 0.1f,
        val max_output_tokens: Int? = null,
    )
    @Serializable
    private data class StructuredImageInputRequest(
        val model: String,
        val input: List<Input>,
        val text: TextConfig,
        val temperature: Float = 0.1f,
        val max_output_tokens: Int? = null,
    )

    object ContentType {
        const val TEXT = "input_text"
        const val IMAGE = "input_image"
    }

    object Role {
        const val USER = "user"
        const val SYSTEM = "system"
        const val ASSISTANT = "assistant"
    }

    private val requestHandler = RequestHandler()


    suspend fun generateTextFromImage(prompt: String, inputImage: String): String {
        val input = mutableListOf<Input>()
        if(!config.systemPrompt.isNullOrBlank()){
            input.add (
                Input(
                    role = Role.SYSTEM,
                    content = listOf(Content(type = ContentType.TEXT, text = config.systemPrompt))
                )
            )
        }
        input.add(
            Input(
                role = Role.USER,
                content = listOf(
                    Content(type = ContentType.TEXT, text = prompt),
                    Content(type = ContentType.IMAGE, image_url = inputImage)
                )
            )
        )
        val request = ImageInputRequest(model = config.model, input = input, temperature = config.temperature, max_output_tokens = config.maxTokens)
        val requestBody = Json.encodeToString(request).toRequestBody("application/json".toMediaType())
        val headers = mapOf("Authorization" to "Bearer $apiKey")
        val result = requestHandler.makeRequest(url, HttpMethod.POST, headers, requestBody)
        val response = result.getOrThrow()
        return parseResponse(response.body)
    }

    suspend fun <T> generateJsonFromImage(prompt: String, inputImage: String, schemaParser: KSerializer<T>): T {
        val input = mutableListOf<Input>()
        if(!config.systemPrompt.isNullOrBlank()){
            input.add (
                Input(
                    role = Role.SYSTEM,
                    content = listOf(Content(type = ContentType.TEXT, text = config.systemPrompt))
                )
            )
        }
        input.add(
            Input(
                role = Role.USER,
                content = listOf(
                    Content(type = ContentType.TEXT, text = prompt),
                    Content(type = ContentType.IMAGE, image_url = inputImage)
                )
            )
        )
        val request = StructuredImageInputRequest(
            model = config.model,
            temperature = config.temperature,
            max_output_tokens = config.maxTokens,
            input = input,
            text = TextConfig(
                format = TextFormat(
                    type = "json_schema",
                    name = schemaParser.descriptor.serialName.substringAfterLast("."),
                    schema = schemaParser.toJsonSchema()
                )
            )
        )

        val requestBody = Json.encodeToString(request).toRequestBody("application/json".toMediaType())
        val headers = mapOf("Authorization" to "Bearer $apiKey")
        val result = requestHandler.makeRequest(url, HttpMethod.POST, headers, requestBody)
        val response = result.getOrThrow()
        return parseStructuredResponse(response.body, schemaParser)
    }

    private fun parseResponse(jsonString: String): String {
        val json = Json.parseToJsonElement(jsonString)
        val outputText = json.jsonObject["output"]!!.jsonArray
            .first().jsonObject["content"]!!.jsonArray
            .first().jsonObject["text"]!!.jsonPrimitive.content
        return outputText
    }

    private fun <T> parseStructuredResponse(jsonString: String, serializer: KSerializer<T>): T {
        val json = Json.parseToJsonElement(jsonString)
        val outputText = json.jsonObject["output"]!!
            .jsonArray.first()
            .jsonObject["content"]!!
            .jsonArray.first()
            .jsonObject["text"]!!
            .jsonPrimitive
            .content
        return Json.decodeFromString(serializer, outputText)
    }
}

//  JSON Schema helpers
fun <T> KSerializer<T>.toJsonSchema(): JsonElement =
    descriptor.toSchema()

private fun SerialDescriptor.toSchema(): JsonElement =
    buildJsonObject {
        put("type", "object")

        put(
            "properties",
            buildJsonObject {
                for (i in 0 until elementsCount) {
                    put(
                        getElementName(i),
                        getElementDescriptor(i).toSchemaProperty()
                    )
                }
            }
        )

        putJsonArray("required") {
            for (i in 0 until elementsCount) {
                val descriptor = getElementDescriptor(i)
                if (!descriptor.isNullable && !isElementOptional(i)) {
                    add(getElementName(i))
                }
            }
        }

        put("additionalProperties", false)
    }

private fun SerialDescriptor.toSchemaProperty(): JsonElement {
    val schema = when (kind) {
        PrimitiveKind.STRING ->
            buildJsonObject { put("type", "string") }

        PrimitiveKind.BOOLEAN ->
            buildJsonObject { put("type", "boolean") }

        PrimitiveKind.INT,
        PrimitiveKind.LONG ->
            buildJsonObject { put("type", "integer") }

        PrimitiveKind.FLOAT,
        PrimitiveKind.DOUBLE ->
            buildJsonObject { put("type", "number") }

        StructureKind.LIST ->
            buildJsonObject {
                put("type", "array")
                put("items", getElementDescriptor(0).toSchemaProperty())
            }

        StructureKind.CLASS,
        StructureKind.OBJECT ->
            toSchema()

        else ->
            buildJsonObject { put("type", "string") }
    }

    if (!isNullable) {
        return schema
    }

    val obj = schema.jsonObject

    return buildJsonObject {
        for ((key, value) in obj) {
            if (key != "type") {
                put(key, value)
            }
        }

        putJsonArray("type") {
            add(obj["type"]!!)
            add(JsonPrimitive("null"))
        }
    }
}