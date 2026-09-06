package com.example.comicreader.adblock

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

/**
 * Structured rules extracted from filter lists (EasyList, EasyPrivacy, Hosts files, URLhaus).
 */
data class ParsedFilterRules(
    val blockedDomains: Set<String> = emptySet(),
    val exceptionDomains: Set<String> = emptySet(),
    val pathPatterns: List<String> = emptyList(),
    val exceptionPatterns: List<String> = emptyList(),
    val cosmeticSelectors: List<String> = emptyList(),
    val domainCosmeticSelectors: Map<String, List<String>> = emptyMap()
) {
    operator fun plus(other: ParsedFilterRules): ParsedFilterRules {
        val mergedBlocked = HashSet<String>(blockedDomains.size + other.blockedDomains.size).apply {
            addAll(blockedDomains)
            addAll(other.blockedDomains)
        }
        val mergedException = HashSet<String>(exceptionDomains.size + other.exceptionDomains.size).apply {
            addAll(exceptionDomains)
            addAll(other.exceptionDomains)
        }
        val mergedPaths = ArrayList<String>(pathPatterns.size + other.pathPatterns.size).apply {
            addAll(pathPatterns)
            addAll(other.pathPatterns)
        }
        val mergedExceptionPaths = ArrayList<String>(exceptionPatterns.size + other.exceptionPatterns.size).apply {
            addAll(exceptionPatterns)
            addAll(other.exceptionPatterns)
        }
        val mergedCosmetic = ArrayList<String>(cosmeticSelectors.size + other.cosmeticSelectors.size).apply {
            addAll(cosmeticSelectors)
            addAll(other.cosmeticSelectors)
        }
        val mergedDomainCosmetic = HashMap<String, MutableList<String>>()
        domainCosmeticSelectors.forEach { (domain, selectors) ->
            mergedDomainCosmetic.getOrPut(domain) { ArrayList() }.addAll(selectors)
        }
        other.domainCosmeticSelectors.forEach { (domain, selectors) ->
            mergedDomainCosmetic.getOrPut(domain) { ArrayList() }.addAll(selectors)
        }

        return ParsedFilterRules(
            blockedDomains = mergedBlocked,
            exceptionDomains = mergedException,
            pathPatterns = mergedPaths,
            exceptionPatterns = mergedExceptionPaths,
            cosmeticSelectors = mergedCosmetic,
            domainCosmeticSelectors = mergedDomainCosmetic
        )
    }

    val totalRuleCount: Int
        get() = blockedDomains.size + exceptionDomains.size + pathPatterns.size + cosmeticSelectors.size
}

/**
 * Multi-format parser for:
 * 1. Adblock Plus (ABP) format (EasyList, EasyPrivacy, URLhaus ABP format)
 * 2. Hosts file format (Peter Lowe's Ad & Tracking list: 127.0.0.1 domain / 0.0.0.0 domain)
 * 3. Plain domain lists
 */
object EasyListParser {

    fun parse(inputStream: InputStream): ParsedFilterRules {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        return parse(reader.lineSequence())
    }

    fun parse(lines: Sequence<String>): ParsedFilterRules {
        val blockedDomains = HashSet<String>()
        val exceptionDomains = HashSet<String>()
        val pathPatterns = ArrayList<String>()
        val exceptionPatterns = ArrayList<String>()
        val cosmeticSelectors = ArrayList<String>()
        val domainCosmeticSelectors = HashMap<String, ArrayList<String>>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            // Comments or empty lines (exclude ## cosmetic rules)
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) {
                continue
            }
            if (line.startsWith("#") && !line.startsWith("##") && !line.startsWith("#@#")) {
                continue
            }

            // Hosts format: "127.0.0.1 ad.example.com" or "0.0.0.0 tracker.org"
            if (line.startsWith("127.0.0.1") || line.startsWith("0.0.0.0")) {
                val tokens = line.split(Regex("\\s+"))
                if (tokens.size >= 2) {
                    val domain = tokens[1].trim().lowercase(Locale.ROOT)
                    if (domain.isNotEmpty() && domain != "localhost" && domain != "broadcasthost" && isValidDomain(domain)) {
                        blockedDomains.add(domain)
                    }
                }
                continue
            }

            // Exceptions: @@||domain.com^ or @@/path/
            if (line.startsWith("@@")) {
                val exceptionRule = line.substring(2)
                if (exceptionRule.startsWith("||")) {
                    val domain = extractDomainFromAnchor(exceptionRule.substring(2))
                    if (domain.isNotEmpty()) {
                        exceptionDomains.add(domain)
                    }
                } else {
                    val pattern = cleanPattern(exceptionRule)
                    if (pattern.length >= 3) {
                        exceptionPatterns.add(pattern)
                    }
                }
                continue
            }

            // Cosmetic element hiding rules: ##.banner or example.com##.sponsor
            val cosmeticIndex = line.indexOf("##")
            if (cosmeticIndex != -1) {
                val domainsPart = line.substring(0, cosmeticIndex)
                val selector = line.substring(cosmeticIndex + 2).trim()
                if (selector.isNotEmpty() && !selector.startsWith("+js") && !selector.startsWith("matches-css")) {
                    if (domainsPart.isEmpty()) {
                        cosmeticSelectors.add(selector)
                    } else {
                        val domains = domainsPart.split(",")
                        for (d in domains) {
                            val cleanD = d.trim().lowercase(Locale.ROOT)
                            if (cleanD.isNotEmpty() && !cleanD.startsWith("~")) {
                                domainCosmeticSelectors.getOrPut(cleanD) { ArrayList() }.add(selector)
                            }
                        }
                    }
                }
                continue
            }

            // Domain anchor: ||example.com^ or ||example.com^$third-party
            if (line.startsWith("||")) {
                val ruleContent = line.substring(2)
                val domain = extractDomainFromAnchor(ruleContent)
                if (domain.isNotEmpty() && isValidDomain(domain)) {
                    // Check if it's a domain-level block
                    if (!ruleContent.contains("/") || ruleContent.indexOf('/') > ruleContent.indexOf('^').let { if (it == -1) Int.MAX_VALUE else it }) {
                        blockedDomains.add(domain)
                    } else {
                        // It has an explicit path on this domain: e.g. ||example.com/ads/*
                        val pathPart = extractPathFromAnchor(ruleContent)
                        if (pathPart.length >= 4 && isSafeAdPattern(pathPart)) {
                            pathPatterns.add(pathPart)
                        } else {
                            blockedDomains.add(domain)
                        }
                    }
                }
                continue
            }

            // Bare domain (plain domain list): e.g. "ad.example.com"
            if (!line.contains("/") && !line.contains("?") && !line.contains(":") && !line.contains(" ") && isValidDomain(line)) {
                blockedDomains.add(line.lowercase(Locale.ROOT))
                continue
            }

            // Targeted path patterns (e.g. /pagead/, /ad_banner/, etc.)
            // Strictly require leading slash or ad keyword to prevent false positives on comic pages
            if (!line.contains("##") && !line.contains("#@#") && !line.contains("#?#")) {
                val pattern = cleanPattern(line)
                if (pattern.length in 5..80 && isSafeAdPattern(pattern)) {
                    pathPatterns.add(pattern)
                }
            }
        }

        return ParsedFilterRules(
            blockedDomains = blockedDomains,
            exceptionDomains = exceptionDomains,
            pathPatterns = pathPatterns.distinct(),
            exceptionPatterns = exceptionPatterns.distinct(),
            cosmeticSelectors = cosmeticSelectors.distinct(),
            domainCosmeticSelectors = domainCosmeticSelectors
        )
    }

    private fun extractDomainFromAnchor(rule: String): String {
        var domain = rule
        // Strip options after $
        val dollarIndex = domain.indexOf('$')
        if (dollarIndex != -1) {
            domain = domain.substring(0, dollarIndex)
        }
        // Strip separator ^
        val caretIndex = domain.indexOf('^')
        if (caretIndex != -1) {
            domain = domain.substring(0, caretIndex)
        }
        // Strip path if any
        val slashIndex = domain.indexOf('/')
        if (slashIndex != -1) {
            domain = domain.substring(0, slashIndex)
        }
        return domain.trim().lowercase(Locale.ROOT)
    }

    private fun extractPathFromAnchor(rule: String): String {
        val slashIndex = rule.indexOf('/')
        if (slashIndex == -1) return ""
        var path = rule.substring(slashIndex)
        val dollarIndex = path.indexOf('$')
        if (dollarIndex != -1) {
            path = path.substring(0, dollarIndex)
        }
        val caretIndex = path.indexOf('^')
        if (caretIndex != -1) {
            path = path.substring(0, caretIndex)
        }
        return path.replace("*", "").trim().lowercase(Locale.ROOT)
    }

    private fun cleanPattern(rule: String): String {
        var pattern = rule
        val dollarIndex = pattern.indexOf('$')
        if (dollarIndex != -1) {
            pattern = pattern.substring(0, dollarIndex)
        }
        return pattern.trim().replace("^", "").replace("*", "").lowercase(Locale.ROOT)
    }

    /**
     * Ensures path patterns target actual ad/tracker/malware endpoints and cannot
     * accidentally block legitimate comic chapter or image paths.
     */
    private fun isSafeAdPattern(pattern: String): Boolean {
        if (pattern.length < 4) return false
        if (pattern.startsWith("http://") || pattern.startsWith("https://")) return false

        val lower = pattern.lowercase(Locale.ROOT)
        // Disallow dangerous words that appear in comic reader URLs
        val forbiddenSubstrings = listOf(
            "chapter", "manga", "comic", "page", "image", "upload", "view", "read",
            "cdn", "content", "asset", "static", "media", "thumb", "cover"
        )
        if (forbiddenSubstrings.any { lower.contains(it) }) {
            return false
        }

        // Must explicitly contain an ad, popup, tracking, or malware marker
        val adKeywords = listOf(
            "/ad", "ad_", "_ad", "-ad", "ad-", "ad.", "/ads", "pagead",
            "adserver", "adsystem", "popunder", "popup", "pop_ad", "banner",
            "vignette", "telemetry", "analytics", "tracker", "pixel",
            "advert", "monetag", "adsterra", "clickadu", "exoclick",
            "propeller", "juicyads", "affiliate", "payload", "miner",
            "exploit", "malware", "trojan", ".exe", ".bin", ".apk", ".sh", ".elf"
        )
        return adKeywords.any { lower.contains(it) }
    }

    private fun isValidDomain(domain: String): Boolean {
        return domain.contains('.') && !domain.startsWith(".") && !domain.endsWith(".") && !domain.contains(" ")
    }
}
