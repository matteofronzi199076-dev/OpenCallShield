package com.opencallshield.sync

import com.opencallshield.data.SpamNumber
import com.opencallshield.net.Http
import com.opencallshield.net.UserFacingNetworkException
import org.json.JSONArray
import org.json.JSONObject

/**
 * Envia aportes a la base publica abriendo un Issue en el repo destino con la
 * lista de numeros propuestos en formato JSON, listo para integrar en
 * `spam_numbers.json`. No requiere permiso de escritura sobre el repo: cualquier
 * usuario autenticado puede abrir un Issue en un repo publico.
 */
object GitHubContributor {

    /** Crea el Issue con la propuesta y devuelve su URL (html_url). */
    fun proposeNumbers(
        token: String,
        owner: String,
        repo: String,
        numbers: List<SpamNumber>
    ): String {
        require(numbers.isNotEmpty()) { "Non ci sono numeri da condividere." }

        val jsonArray = JSONArray()
        numbers.forEach { n ->
            jsonArray.put(
                JSONObject()
                    .put("number", n.number)
                    .put("reports", n.reports)
                    .put("tag", n.tag)
            )
        }

        val body = buildString {
            append("Proposta di numeri per `spam_numbers.json`, inviata dall’app OpenCallShield.\n\n")
            append("Totale: ${numbers.size}\n\n")
            append("```json\n")
            append(jsonArray.toString(2))
            append("\n```\n")
        }

        // Sin labels: asignar/crear etiquetas requiere permiso de escritura sobre el
        // repo. Un usuario externo (no colaborador) puede abrir un Issue, pero no
        // etiquetarlo; incluir labels provocaba un 403. El titulo ya identifica el aporte.
        val payload = JSONObject()
            .put("title", "Contributo alla banca dati SPAM: ${numbers.size} numeri")
            .put("body", body)

        val res = Http.request(
            method = "POST",
            urlString = "https://api.github.com/repos/$owner/$repo/issues",
            headers = mapOf(
                "Accept" to "application/vnd.github+json",
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = payload.toString()
        )
        if (!res.isSuccess) throw UserFacingNetworkException("Impossibile inviare il contributo a GitHub (HTTP ${res.code}).")
        return res.json().optString("html_url", "Issue creata")
    }
}

