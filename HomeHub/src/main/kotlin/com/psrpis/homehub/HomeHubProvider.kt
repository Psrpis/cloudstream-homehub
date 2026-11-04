package com.psrpis.homehub

import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType

/**
 * HomeHub: yüklü diğer eklentilerin ana sayfa bölümlerini tek bir ana sayfada toplar.
 * Tıklanan içerik, kendi eklentisi (apiName) üzerinden açılır.
 */
class HomeHubProvider : MainAPI() {
    override var name = "HomeHub"
    override var mainUrl = "https://homehub.invalid"
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val supportedTypes = setOf(
        TvType.Movie, TvType.TvSeries, TvType.Anime, TvType.AnimeMovie,
        TvType.OVA, TvType.Cartoon, TvType.AsianDrama, TvType.Others
    )

    companion object {
        private const val SEP = "|||"

        // Sadece bu eklentiler olsun istiyorsan adlarını yaz. Boşsa hepsi dahil olur.
        // Örnek: setOf("Kisskh", "Goojara")
        private val ONLY: Set<String> = emptySet()

        // Ana sayfada görmek istemediğin eklentiler
        private val EXCLUDE: Set<String> = emptySet()
    }

    private fun sources(): List<MainAPI> =
        APIHolder.allProviders.filter { p ->
            p.name != name &&
                p.hasMainPage &&
                p.mainPage.isNotEmpty() &&
                (ONLY.isEmpty() || p.name in ONLY) &&
                p.name !in EXCLUDE
        }

    // Bölümler sabit değil: her açılışta, o an yüklü eklentilerden oluşturulur
    override var mainPage: List<MainPageData>
        get() = sources().flatMap { p ->
            p.mainPage.map { d ->
                val title = if (d.name.isBlank()) p.name else "${p.name} • ${d.name}"
                MainPageData(title, "${p.name}$SEP${d.data}", d.horizontalImages)
            }
        }.ifEmpty { listOf(MainPageData("Eklenti yok", "", false)) }
        set(_) {}

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val parts = request.data.split(SEP, limit = 2)
        if (parts.size < 2) return null
        val providerName = parts[0]
        val origData = parts[1]

        val provider = APIHolder.allProviders.firstOrNull { it.name == providerName } ?: return null
        val orig = provider.mainPage.firstOrNull { it.data == origData }
        val origRequest = MainPageRequest(
            orig?.name ?: "",
            origData,
            orig?.horizontalImages ?: false
        )

        val res = try {
            provider.getMainPage(page, origRequest)
        } catch (e: Exception) {
            null
        } ?: return null

        val items = res.items.map {
            HomePageList("${provider.name} • ${it.name}", it.list, it.isHorizontalImages)
        }
        return HomePageResponse(items, res.hasNext)
    }

    override suspend fun search(query: String): List<SearchResponse> = emptyList()
}
