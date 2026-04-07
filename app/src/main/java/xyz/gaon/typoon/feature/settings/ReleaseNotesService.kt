package xyz.gaon.typoon.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class GitHubReleaseNote(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
)

class ReleaseNotesService
    @Inject
    constructor() {
        suspend fun fetchLatestRelease(preferredVersionCode: Int? = null): GitHubReleaseNote? =
            withContext(Dispatchers.IO) {
                val releases = fetchReleases().filterNotNull()
                if (releases.isEmpty()) {
                    return@withContext null
                }

                if (preferredVersionCode == null) {
                    return@withContext releases.first()
                }

                releases.firstOrNull { release ->
                    containsVersionCode(release, preferredVersionCode)
                } ?: releases.first()
            }

        private fun fetchReleases(): List<GitHubReleaseNote?> {
            val json =
                getJson("https://api.github.com/repos/gaon12/Typoon/releases?per_page=20")
            val releases = JSONArray(json)
            return buildList {
                for (index in 0 until releases.length()) {
                    val release = releases.getJSONObject(index)
                    if (release.optBoolean("draft", false) || release.optBoolean("prerelease", false)) {
                        continue
                    }
                    add(
                        GitHubReleaseNote(
                            tagName = release.optString("tag_name"),
                            name = release.optString("name"),
                            body = release.optString("body"),
                            htmlUrl = release.optString("html_url"),
                        ),
                    )
                }
            }
        }

        private fun containsVersionCode(
            release: GitHubReleaseNote,
            versionCode: Int,
        ): Boolean {
            val pattern = Regex("(^|\\D)$versionCode(\\D|$)")
            return pattern.containsMatchIn(release.tagName) || pattern.containsMatchIn(release.name)
        }

        @Throws(IOException::class)
        private fun getJson(url: String): String {
            val connection =
                (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    setRequestProperty("User-Agent", "Typoon-Android")
                }

            return try {
                val responseCode = connection.responseCode
                val stream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    throw IOException("GitHub API request failed ($responseCode)")
                }
                responseText
            } finally {
                connection.disconnect()
            }
        }
    }

object ReleaseNotesLanguageParser {
    fun parse(
        markdownBody: String,
        languageCode: String,
    ): String {
        val normalized =
            if (languageCode.startsWith("ko", ignoreCase = true)) {
                "ko"
            } else {
                "en"
            }

        return extractForLanguage(markdownBody, normalized)
            ?: extractForLanguage(markdownBody, "en")
            ?: markdownBody.trim()
    }

    private fun extractForLanguage(
        markdownBody: String,
        languageCode: String,
    ): String? {
        val regex =
            Regex(
                pattern = "<!--\\s*lang:$languageCode\\s*-->(.*?)<!--\\s*/lang:$languageCode\\s*-->",
                options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
            )
        return regex
            .find(markdownBody)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
