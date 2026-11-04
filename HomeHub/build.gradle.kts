dependencies {
    // Uygulama zaten sağlıyor, pakete eklenmesin
    compileOnly("androidx.appcompat:appcompat:1.7.1")
}

// Her yeni sürümde bu sayıyı artır
version = 1

cloudstream {
    description = "Yüklü diğer eklentilerin ana sayfalarını tek yerde toplar"
    authors = listOf("Psrpis")

    // 0: Down, 1: Ok, 2: Slow, 3: Beta-only
    status = 1

    tvTypes = listOf("All")

    requiresResources = false
    // language bilerek belirtilmedi: dil filtresine takılmasın
}
