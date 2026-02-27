package com.shunsukehayashi.miyabidash.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import com.shunsukehayashi.miyabidash.data.models.OpenClawStatus
import com.shunsukehayashi.miyabidash.data.models.AgentStatus
import com.shunsukehayashi.miyabidash.data.models.SessionStatus
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface OpenClawApiService {
    @Headers("Content-Type: application/json")
    @GET("status")
    suspend fun getStatus(
        @Header("Authorization") token: String? = null
    ): OpenClawStatus

    companion object {
        fun create(baseUrl: String, token: String?): OpenClawApiService {
            val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val clientBuilder = OkHttpClient.Builder()

            clientBuilder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })

            if (!token.isNullOrBlank()) {
                clientBuilder.addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Authorization", token)
                        .build()
                    chain.proceed(request)
                }
            }

            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create(createFlexibleGson()))
                .build()

            return retrofit.create(OpenClawApiService::class.java)
        }

        private fun createFlexibleGson(): Gson {
            return GsonBuilder()
                .registerTypeAdapter(OpenClawStatus::class.java, OpenClawStatusDeserializer())
                .create()
        }
    }
}

private class OpenClawStatusDeserializer : JsonDeserializer<OpenClawStatus> {
    private val agentListType = object : TypeToken<List<AgentStatus>>() {}.type
    private val sessionListType = object : TypeToken<List<SessionStatus>>() {}.type

    override fun deserialize(
        json: JsonElement,
        typeOfT: java.lang.reflect.Type?,
        context: JsonDeserializationContext
    ): OpenClawStatus {
        val obj = json.asJsonObject

        val parsedAgents = parseListOrCount<AgentStatus>(obj.get("agents"), context, agentListType)
        val parsedSessions = parseListOrCount<SessionStatus>(obj.get("sessions"), context, sessionListType)
        val agents = parsedAgents.first
        val agentCountFromLegacy = parsedAgents.second
        val sessions = parsedSessions.first
        val sessionCountFromLegacy = parsedSessions.second

        return OpenClawStatus(
            status = obj.getOrNull("status")?.asStringOrNull(),
            healthy = obj.getOrNull("healthy")?.asBooleanOrNull(),
            agents = agents,
            sessions = sessions,
            memory = obj.getOrNull("memory")?.let { context.deserialize(it, com.shunsukehayashi.miyabidash.data.models.MemoryStatus::class.java) },
            error = obj.getOrNull("error")?.asStringOrNull(),
            summary = obj.getOrNull("summary")?.asStringOrNull(),
            gateway = obj.getOrNull("gateway")?.asStringOrNull(),
            gatewayStatus = obj.getOrNull("gatewayStatus")?.asStringOrNull(),
            gatewayLatencyMs = obj.getOrNull("gatewayLatencyMs")?.asIntOrNull(),
            telegram = obj.getOrNull("telegram")?.asStringOrNull(),
            telegramStatus = obj.getOrNull("telegramStatus")?.asStringOrNull(),
            heartbeatTasks = obj.getOrNull("heartbeatTasks")?.asIntOrNull(),
            memoryChunks = obj.getOrNull("memoryChunks")?.asIntOrNull(),
            updateAvailable = obj.getOrNull("updateAvailable")?.asBooleanOrNull(),
            agentList = obj.getOrNull("agentList")?.let { context.deserialize(it, agentListType) } ?: emptyList(),
            agentCount = obj.getOrNull("agentCount")?.asIntOrNull() ?: agentCountFromLegacy,
            sessionCount = obj.getOrNull("sessionCount")?.asIntOrNull() ?: obj.getOrNull("sessionsCount")?.asIntOrNull() ?: sessionCountFromLegacy
        )
    }

    private fun <T> parseListOrCount(
        element: JsonElement?,
        context: JsonDeserializationContext,
        listType: Type
    ): Pair<List<T>, Int?> {
        if (element == null || element.isJsonNull) {
            return Pair(emptyList(), null)
        }
        if (element.isJsonArray) {
            @Suppress("UNCHECKED_CAST")
            return Pair(context.deserialize(element, listType) as? List<T> ?: emptyList(), null)
        }
        if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) {
            return Pair(emptyList(), element.asIntOrNull())
        }
        return Pair(emptyList(), null)
    }

    private fun JsonElement.asStringOrNull(): String? = when {
        isJsonNull || !isJsonPrimitive -> null
        else -> asString
    }

    private fun JsonElement.asBooleanOrNull(): Boolean? = when {
        isJsonNull || !isJsonPrimitive -> null
        else -> try {
            asBoolean
        } catch (_: IllegalStateException) {
            null
        }
    }

    private fun JsonElement.asIntOrNull(): Int? = when {
        isJsonNull || !isJsonPrimitive -> null
        else -> try {
            asInt
        } catch (_: Exception) {
            null
        }
    }
}

private fun JsonElement.getOrNull(key: String): JsonElement? = if (asJsonObject.has(key) && !asJsonObject[key].isJsonNull) {
    asJsonObject[key]
} else {
    null
}
