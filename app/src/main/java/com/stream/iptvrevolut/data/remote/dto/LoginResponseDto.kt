package com.stream.iptvrevolut.data.remote.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class LoginResponseDto(
    @SerializedName("user_info") @SerialName("user_info") val userInfo: UserInfoDto? = null,
    @SerializedName("server_info") @SerialName("server_info") val serverInfo: ServerInfoDto? = null
)

@Serializable
data class UserInfoDto(
    val username: String? = null,
    val status: String? = null,
    @SerializedName("exp_date") @SerialName("exp_date") val expDate: String? = null,
    @SerializedName("is_trial") @SerialName("is_trial") val isTrial: String? = null,
    @SerializedName("active_cons") @SerialName("active_cons") val activeCons: String? = null,
    @SerializedName("max_connections") @SerialName("max_connections") val maxConnections: String? = null,
    @SerializedName("allowed_output_formats") @SerialName("allowed_output_formats") val allowedOutputFormats: List<String>? = null
)

@Serializable
data class ServerInfoDto(
    val url: String? = null,
    val port: String? = null,
    @SerializedName("https_port") @SerialName("https_port") val httpsPort: String? = null,
    @SerializedName("server_protocol") @SerialName("server_protocol") val serverProtocol: String? = null,
    @SerializedName("timezone") @SerialName("timezone") val timezone: String? = null
)
