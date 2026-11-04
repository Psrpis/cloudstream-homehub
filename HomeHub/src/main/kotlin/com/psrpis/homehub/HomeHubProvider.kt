package com.psrpis.homehub

import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newHomePageResponse

internal const val SEP = "|||"
internal const val HUB_NAME = "HomeHub"

/** Ana sayfada gösterilebilecek tek bir bölüm (bir eklentinin bir ana sayfa satırı). */
internal data class HubSection(
    val key: String,          // "EklentiAdı|||orijinalData"
    val title: String,        // "EklentiAdı • Bölüm"
    val horizontal: Boolean,
    val providerName: String
)

/** Yüklü tüm eklentilerin güvenli bir kopyası (kilit altında alınır). */
internal fun allApis(): List<MainAPI> =
    APIHolder.allProviders.withLock { APIHolder.allProviders.toList() }

/** Yüklü eklentilerdeki tüm bölümler, doğal sırayla. */
internal fun allSections(): List<HubSection> =
    allApis()
        .filter { p -> p.name != HUB_NAME && p.hasMainPage && p.mainPage.isNotEmpty() }
        .flatMap { p ->
            p.mainPage.map { d ->
                HubSection(
                    key = "${p.name}$SEP${d.data}",
                    title = if (d.name.isBlank()) p.name else "${p.name} • ${d.name}",
                    horizontal = d.horizontalImages,
                    providerName = p.name
                )
            }
        }
        .distinctBy { it.key }

/** Kullanıcının kaydettiği sıraya göre dizilmiş bölümler (kapalı olanlar dahil). */
internal fun orderedSections(): List<HubSection> {
    val idx = HomeHubPrefs.order().withIndex().associate { it.value to it.index }
    return allSections().sortedBy { idx[it.key] ?: Int.MAX_VALUE }
}

/**
 * HomeHub: yüklü diğer eklentilerin ana sayfa bölümlerini tek bir ana sayfada toplar.
 * Bölümler ayarlardan tek tek açılıp kapatılabilir ve sıralanabilir.
 * Tıklanan içerik, kendi eklentisi (apiName) üzerinden açılır.
 */
class HomeHubProvider : MainAPI() {
    override var name = HUB_NAME
    override var mainUrl = "https://homehub.invalid"
    override val hasMainPage = true
    override val supportedTypes = setOf(
        TvType.Movie, TvType.TvSeries, TvType.Anime, TvType.AnimeMovie,
        TvType.OVA, TvType.Cartoon, TvType.AsianDrama, TvType.Others
    )

    // Bölümler sabit değil: her açılışta, kayıtlı ayarlara göre oluşturulur
    override val mainPage: List<MainPageData>
        get() {
            val off = HomeHubPrefs.disabledSections()
            return orderedSections()
                .filter { it.key !in off }
                .map { MainPageData(it.title, it.key, it.horizontal) }
                .ifEmpty { listOf(MainPageData("Bölüm yok", "", false)) }
        }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val parts = request.data.split(SEP, limit = 2)
        if (parts.size < 2) return null
        val providerName = parts[0]
        val origData = parts[1]

        val provider = allApis().firstOrNull { it.name == providerName } ?: return null
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
        return newHomePageResponse(items, res.hasNext)
    }

    override suspend fun search(query: String): List<SearchResponse> = emptyList()
}
