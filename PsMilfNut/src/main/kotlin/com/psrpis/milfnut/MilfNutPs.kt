// Orijinal: @Kraptor123 | Cs-GizliKeyif (MilfNut). Kişisel kullanım için düzenlendi.

package com.psrpis.milfnut

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class MilfNutPs : MainAPI() {
    override var mainUrl              = "https://milfnut.com"
    override var name                 = "MilfNut (Ps)"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    // Sadece iki bölüm: ana sayfa (en yeniler) ve Trending
    override val mainPage = mainPageOf(
        "${mainUrl}/"                   to "Latest",
        "${mainUrl}/category/trending/" to "Trending",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get("${request.data}").document
        } else {
            app.get("${request.data}page/$page/").document
        }
        val home = document.select("article.loop-video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("header span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = this.selectFirst("img")?.let { img ->
            val src = img.attr("data-lazy-src").ifBlank { img.attr("src") }
            fixUrlNull(src)
        }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf(
                "Referer" to "$mainUrl/",
                "User-Agent" to USER_AGENT
            )
        }
    }
    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1) {
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=${query}").document
        }

        val aramaCevap = document.select("article.loop-video").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.video-description")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.tags-list a").map { it.text() }
        val score          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("article.loop-video").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("div#video-actors a").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(score)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val video = fixUrlNull(document.selectFirst("meta[itemprop=contentURL]")?.attr("content")).toString()

        callback.invoke(newExtractorLink(
            this.name,
            this.name,
            video,
            INFER_TYPE,
            {
                this.referer = "${mainUrl}/"
            }
        ))

        return true
    }
}