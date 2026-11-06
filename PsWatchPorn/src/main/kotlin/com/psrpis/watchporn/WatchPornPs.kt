// Orijinal: @Kraptor123 | Cs-GizliKeyif (WatchPorn). Kişisel kullanım için düzenlendi.

package com.psrpis.watchporn

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class WatchPornPs : MainAPI() {
    override var mainUrl = "https://watchporn.to"
    override var name = "WatchPorn (Ps)"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)


    // 4 bölüm. Adresleri değiştirmek istersen sadece buradaki satırları düzenle.
    override val mainPage = mainPageOf(
        "${mainUrl}/latest-updates/"          to "New",
        "${mainUrl}/top-rated/"               to "Top Rated",
        "${mainUrl}/most-popular/"            to "Most Viewed",
        "${mainUrl}/tags/exclusive/"          to "Exclusive",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}$page/"
        val document = app.get(url).document
        val home = document.select("div.wp-card").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = listOf(
                HomePageList(
                    name = request.name,
                    list = home,
                    isHorizontalImages = true
                )
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a.wp-card__link")?.attr("title")?.trim()?.ifEmpty { null } ?: return null
        val href = fixUrlNull(this.selectFirst("a.wp-card__link")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.thumb")?.attr("data-webp")?.ifEmpty { null } ?: this.selectFirst("img.thumb")?.attr("src"))
        val rating = this.selectFirst("span.wp-r")?.text()?.replace("%", "")?.trim()

        return newMovieSearchResponse(title, "$href|$posterUrl", TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(rating)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = "${mainUrl}/search/?q=$query&from_videos=$page"
        val document = app.get(url).document

        val aramaCevap = document.select("div.wp-card").mapNotNull { it.toMainPageResult() }
        val totalPages = document.selectFirst("form.wp-pag__go")?.attr("data-total")?.toIntOrNull()
        val hasNext = totalPages != null && page < totalPages

        return newSearchResponseList(aramaCevap, hasNext)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query, 1).items

    override suspend fun load(data: String): LoadResponse? {
        val (url, storedPoster) = data.split("|").let {
            it[0] to it.getOrNull(1)
        }

        val response = app.get(url)
        val document = response.document
        val cookies = response.cookies.toString()

        val title = document.selectFirst("h1.wp-vi__title")?.text()?.trim()?.ifEmpty { null } ?: return null
        val poster = storedPoster ?: document.selectFirst("meta[property=og:image]")?.attr("content")

        val tags = document.select("div.wp-chips a, div.wp-tags a").map { it.text().trim() }
        val actors = document.select("div.wp-castrow a.wp-cast span.wp-cast__n").map { Actor(it.text().trim()) }

        val durationText = document.select("div.wp-vmeta span").map { it.text().trim() }.firstOrNull { it.matches(Regex("\\d+:\\d+")) }
        val parts = durationText?.split(":")?.mapNotNull { it.toIntOrNull() }
        val totalMinutes = when (parts?.size) {
            3 -> (parts[0] * 60) + parts[1]
            2 -> parts[0]
            else -> null
        }

        val recommendations = document.select("div.related-videos div.wp-card").mapNotNull {
            it.toMainPageResult()
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.posterHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                "Referer" to "$mainUrl/",
                "Cookie" to cookies
            )
            this.tags = tags
            this.duration = totalMinutes
            this.recommendations = recommendations
            addActors(actors)

            Log.d(name, "Loaded Video: $title")
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val pageHtml = app.get(data).text
        return KtPlayerExtractor.getLinks(name, mainUrl, data, pageHtml, callback = callback)
    }
}
