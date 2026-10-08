package com.opencallshield.auth

import com.opencallshield.net.Http
import com.opencallshield.net.UserFacingNetworkException

/**
 * Autenticacion con GitHub para una app movil sin backend.
 *
 * - Device Flow: oficial para apps/CLIs; solo usa el Client ID publico (sin secreto).
 * - PAT: el usuario pega un Personal Access Token con scope `public_repo`.
 */
class GitHubAuthManager {

    data class DeviceCode(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val interval: Int,
        val expiresIn: Int
    )

    sealed class PollResult {
        data class Success(val token: String) : PollResult()
        data object Pending : PollResult()
        data class SlowDown(val interval: Int) : PollResult()
        data class Error(val message: String) : PollResult()
    }

    /** Paso 1 del Device Flow: solicita el codigo de dispositivo. */
    fun startDeviceFlow(clientId: String, scope: String = SCOPE): DeviceCode {
        val res = Http.request(
            method = "POST",
            urlString = "https://github.com/login/device/code",
            headers = mapOf(
                "Accept" to "application/json",
                "Content-Type" to "application/x-www-form-urlencoded"
            ),
            body = Http.formBody(mapOf("client_id" to clientId, "scope" to scope))
        )
        if (!res.isSuccess) throw UserFacingNetworkException("Impossibile avviare l’accesso a GitHub (HTTP ${res.code}).")
        val j = res.json()
        if (j.has("error")) {
            throw UserFacingNetworkException(oauthError(j.getString("error")))
        }
        return DeviceCode(
            deviceCode = j.getString("device_code"),
            userCode = j.getString("user_code"),
            verificationUri = j.getString("verification_uri"),
            interval = j.optInt("interval", 5),
            expiresIn = j.optInt("expires_in", 900)
        )
    }

    /** Paso 2 del Device Flow: consulta si el usuario ya autorizo. */
    fun pollForToken(clientId: String, deviceCode: String): PollResult {
        val res = Http.request(
            method = "POST",
            urlString = "https://github.com/login/oauth/access_token",
            headers = mapOf(
                "Accept" to "application/json",
                "Content-Type" to "application/x-www-form-urlencoded"
            ),
            body = Http.formBody(
                mapOf(
                    "client_id" to clientId,
                    "device_code" to deviceCode,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code"
                )
            )
        )
        val j = res.json()
        if (j.has("access_token")) return PollResult.Success(j.getString("access_token"))
        return when (j.optString("error")) {
            "authorization_pending" -> PollResult.Pending
            "slow_down" -> PollResult.SlowDown(j.optInt("interval", 5))
            "expired_token" -> PollResult.Error("Il codice è scaduto. Ricomincia l’accesso.")
            "access_denied" -> PollResult.Error("Autorizzazione annullata.")
            "" -> PollResult.Error("Risposta inattesa da GitHub (HTTP ${res.code}).")
            else -> PollResult.Error(oauthError(j.getString("error")))
        }
    }

    /** Valida un token (PAT o device) y devuelve el login del usuario. */
    fun fetchLogin(token: String): String {
        val res = Http.request(
            method = "GET",
            urlString = "https://api.github.com/user",
            headers = mapOf(
                "Accept" to "application/vnd.github+json",
                "Authorization" to "Bearer $token"
            )
        )
        if (!res.isSuccess) throw UserFacingNetworkException("Token non valido o privo delle autorizzazioni necessarie (HTTP ${res.code}).")
        return res.json().getString("login")
    }

    private fun oauthError(error: String): String = when (error) {
        "incorrect_client_credentials" -> "Il Client ID dell’app OAuth non è valido. Controlla la configurazione."
        "device_flow_disabled" -> "L’accesso tramite codice dispositivo è disabilitato per questa app OAuth."
        "invalid_scope" -> "Le autorizzazioni richieste dall’app OAuth non sono valide."
        "unsupported_grant_type" -> "GitHub non supporta il metodo di autorizzazione richiesto."
        "incorrect_device_code" -> "Il codice dispositivo non è valido. Ricomincia l’accesso."
        "expired_token" -> "Il codice è scaduto. Ricomincia l’accesso."
        "access_denied" -> "Autorizzazione annullata."
        else -> "Autenticazione GitHub non riuscita (codice: $error)."
    }

    companion object {
        const val SCOPE = "public_repo"
    }
}

