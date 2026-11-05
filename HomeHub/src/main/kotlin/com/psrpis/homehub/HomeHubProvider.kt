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
// Başına "!" koyduk: harf sıralamasında en başta çıkar. İstersen " " (boşluk) veya "#" yapabilirsin.
internal const val SELF_NAME = "! HomeHub"

/** Yüklü tüm eklentilerin güvenli bir kopyası (kilit altında alınır). */
internal fun allApis(): List<MainAPI> =
    APIHolder.allProviders.withLock { APIHolder.allProviders.toList() }

/** Başka bir eklentinin ana sayfasındaki tek bir bölüm. */
internal data class LiveSection(
    val provider: String,
    val name: String,
    val data: String,
    val horizontal: Boolean
) {
    val key: String get() = "$provider$SEP$data"
    val title: String get() = if (name.isBlank()) provider else "$provider • $name"
    val shortName: String get() = name.ifBlank { provider }
}

internal fun liveSections(): List<LiveSection> =
    allApis()
        .filter { api -> api.name != SELF_NAME && api.hasMainPage && api.mainPage.isNotEmpty() }
        .flatMap { api ->
            api.mainPage.map { d -> LiveSection(api.name, d.name, d.data, d.horizontalImages) }
        }

/**
 * HomeHub: yüklü diğer eklentilerin ana sayfa bölümlerini, ayarlarda seçilen
 * sırayla tek bir ana sayfada toplar. Tıklanan içerik kendi eklentisiyle açılır.
 */
class HomeHubProvider : MainAPI() {
    override var name = SELF_NAME
    override var mainUrl = "https://homehub.invalid"
    override val hasMainPage = true
    override val supportedTypes = setOf(
        TvType.Movie, TvType.TvSeries, TvType.Anime, TvType.AnimeMovie,
        TvType.OVA, TvType.Cartoon, TvType.AsianDrama, TvType.Others
    )

    // Her açılışta, o an yüklü eklentiler ve kayıtlı ayarlardan oluşturulur
    override val mainPage: List<MainPageData>
        get() {
            val live = liveSections()
            val chosen: List<LiveSection> =
                if (!HomeHubPrefs.configured) {
                    live
                } else {
                    val byKey = live.associateBy { s -> s.key }
                    HomeHubPrefs.sections().entries
                        .sortedByDescending { e -> e.value }
                        .mapNotNull { e -> byKey[e.key] }
                }

            val showName = HomeHubPrefs.showExtensionName
            val dupes = chosen.groupingBy { s -> s.shortName }.eachCount()
            val list = chosen.map { s ->
                val title =
                    if (showName || (dupes[s.shortName] ?: 0) > 1) s.title else s.shortName
                MainPageData(title, s.key, s.horizontal)
            }
            return list.ifEmpty {
                listOf(MainPageData("HomeHub: ayarlardan bölüm seç", "", false))
            }
        }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val parts = request.data.split(SEP, limit = 2)
        if (parts.size < 2) return null
        val providerName = parts[0]
        val origData = parts[1]

        val provider = allApis().firstOrNull { api -> api.name == providerName } ?: return null
        val orig = provider.mainPage.firstOrNull { d -> d.data == origData }
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

        val items = res.items.map { l ->
            HomePageList("${provider.name} • ${l.name}", l.list, l.isHorizontalImages)
        }
        return newHomePageResponse(items, res.hasNext)
    }

    override suspend fun search(query: String): List<SearchResponse> = emptyList()
}
