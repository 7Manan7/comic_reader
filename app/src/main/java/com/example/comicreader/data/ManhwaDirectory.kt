package com.example.comicreader.data

/**
 * Data model for curated manhwa and manga bookmark items.
 */
data class ManhwaSite(
    val id: String,
    val name: String,
    val url: String,
    val tag: String,
    val description: String,
    val iconEmoji: String,
    val accentColorHex: Long = 0xFF6366F1
)

/**
 * Curated directory of top manhwa, webtoon, and manga reading websites
 * available as 1-tap bookmarks on the Kuro Reader Home tab.
 */
object ManhwaDirectory {

    val SITES: List<ManhwaSite> = listOf(
        ManhwaSite(
            id = "asura",
            name = "Asura Scans",
            url = "https://asuracomic.net/",
            tag = "Top Manhwa",
            description = "Leading scanlator for solo leveling, regression & action manhwa",
            iconEmoji = "⚔️",
            accentColorHex = 0xFFFF4655
        ),
        ManhwaSite(
            id = "demonic",
            name = "Demonic Scans",
            url = "https://demonicscans.org/",
            tag = "Action & Martial Arts",
            description = "Fast updates on martial arts, action, and fantasy manhwa series",
            iconEmoji = "😈",
            accentColorHex = 0xFFDC2626
        ),
        ManhwaSite(
            id = "flame",
            name = "Flame Comics",
            url = "https://flamecomics.xyz/",
            tag = "High Quality Webtoons",
            description = "Daily chapters of trending Korean action and fantasy webtoons",
            iconEmoji = "🔥",
            accentColorHex = 0xFFF97316
        ),
        ManhwaSite(
            id = "comix",
            name = "Comix",
            url = "https://comix.to/",
            tag = "Fast & Minimal",
            description = "Clean, high-speed reader with responsive full-width webtoon view",
            iconEmoji = "📚",
            accentColorHex = 0xFF3B82F6
        ),
        ManhwaSite(
            id = "mangadex",
            name = "MangaDex",
            url = "https://mangadex.org/",
            tag = "Open Community Hub",
            description = "Non-profit ad-free reader with thousands of scanlation teams",
            iconEmoji = "🐱",
            accentColorHex = 0xFFFF6740
        ),
        ManhwaSite(
            id = "mangakatana",
            name = "MangaKatana",
            url = "https://mangakatana.com/",
            tag = "Vast Archive",
            description = "Massive manga and manhwa catalog with instant page loads",
            iconEmoji = "🗡️",
            accentColorHex = 0xFF10B981
        ),
        ManhwaSite(
            id = "webtoons",
            name = "Webtoons",
            url = "https://www.webtoons.com/",
            tag = "Official Publisher",
            description = "Official Korean webcomics platform featuring global top creators",
            iconEmoji = "🟢",
            accentColorHex = 0xFF00DC64
        ),
        ManhwaSite(
            id = "mangafreak",
            name = "MangaFreak",
            url = "https://ww3.mangafreak.me/",
            tag = "Daily Releases",
            description = "Speedy chapter releases across hundreds of trending series",
            iconEmoji = "⚡",
            accentColorHex = 0xFFEAB308
        ),
        ManhwaSite(
            id = "comick",
            name = "ComicK",
            url = "https://comick.io/",
            tag = "Modern UI",
            description = "Next-gen webtoon reader with bookmark tracking and dark mode",
            iconEmoji = "🚀",
            accentColorHex = 0xFF6366F1
        ),
        ManhwaSite(
            id = "mangapark",
            name = "MangaPark",
            url = "https://mangapark.net/",
            tag = "Multi-Source",
            description = "Comprehensive multi-source aggregator with high-res chapters",
            iconEmoji = "📖",
            accentColorHex = 0xFFA855F7
        )
    )
}
