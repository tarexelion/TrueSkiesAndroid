package com.trueskies.android.util

/**
 * ICAO ↔ IATA airline code registry.
 * Ported from iOS AirlineCodeRegistry.swift — used to generate search variants
 * so that searching "WMT5YV" (ICAO) also tries "W45YV" (IATA) and vice versa.
 */
object AirlineCodeRegistry {

    /** IATA (2-char) → ICAO (3-char) mapping */
    private val iataToIcao: Map<String, String>

    /** ICAO (3-char) → IATA (2-char) mapping */
    private val icaoToIata: Map<String, String>

    init {
        // Pairs: IATA to ICAO
        val pairs = listOf(
            // North America
            "B6" to "JBU", "UA" to "UAL", "AA" to "AAL", "DL" to "DAL",
            "WN" to "SWA", "AS" to "ASA", "F9" to "FFT", "NK" to "NKS",
            "G4" to "AAY", "AC" to "ACA", "HA" to "HAL", "WS" to "WJA",
            "YX" to "RPA", "MX" to "MXX", "SY" to "SCX", "PD" to "POE",
            "F8" to "FLE",
            // Europe
            "BA" to "BAW", "LH" to "DLH", "AF" to "AFR", "KL" to "KLM",
            "IB" to "IBE", "I2" to "IBS", "AY" to "FIN", "SK" to "SAS",
            "U2" to "EZY", "FR" to "RYR", "SN" to "BEL", "TK" to "THY",
            "VY" to "VLG", "PC" to "PGT", "DY" to "NAX", "BY" to "TOM",
            "RO" to "ROT", "A3" to "AEE", "EI" to "EIN", "BT" to "BTI",
            "XK" to "CCM", "EN" to "DLA", "UX" to "AEA", "OS" to "AUA",
            "WX" to "BCY", "SS" to "CRL", "WK" to "EDW", "EW" to "EWG",
            "BF" to "FBU", "FI" to "ICE", "LM" to "LOG", "LO" to "LOT",
            "LG" to "LGL", "N0" to "NBT", "OA" to "OAL", "OG" to "FPY",
            "XQ" to "SXS", "LX" to "SWR", "TP" to "TAP", "V7" to "VOE",
            "WF" to "WIF", "W6" to "WZZ", "W4" to "WMT", "FB" to "LZB",
            "BE" to "BEE", "DE" to "CFG", "LS" to "EXS",
            "HV" to "TRA", "TO" to "TVF",
            // Middle East
            "EK" to "UAE", "EY" to "ETD", "QR" to "QTR", "SV" to "SVA",
            "FZ" to "FDB", "GF" to "GFA", "WY" to "OMA", "ME" to "MEA",
            "RJ" to "RJA", "KU" to "KAC", "LY" to "ELY", "XY" to "KNE",
            "J9" to "JZR", "OV" to "OMS", "AH" to "DAH", "G9" to "ABY",
            // Asia
            "SQ" to "SIA", "CX" to "CPA", "NH" to "ANA", "JL" to "JAL",
            "KE" to "KAL", "OZ" to "AAR", "TG" to "THA", "MH" to "MAS",
            "GA" to "GIA", "BR" to "EVA", "CI" to "CAL", "CA" to "CCA",
            "MU" to "CES", "CZ" to "CSN", "HU" to "CHH", "MF" to "CXA",
            "AI" to "AIC", "6E" to "IGO", "SG" to "SEJ", "G8" to "GOW",
            "VN" to "HVN", "VJ" to "VJC", "5J" to "CEB", "PR" to "PAL",
            "AK" to "AXM", "JQ" to "JST", "TR" to "TGW", "UL" to "ALK",
            "PK" to "PIA", "BG" to "BBC", "QH" to "BAV", "D7" to "XAX",
            "RS" to "ASV", "QP" to "AKJ", "ID" to "BTK", "QG" to "CTV",
            "7C" to "JJA", "LJ" to "JNA", "HO" to "DKH", "8L" to "LKE",
            "MM" to "APJ", "ZH" to "CSZ", "3U" to "CSC", "6J" to "SNJ",
            "9C" to "CQH", "JX" to "SJX", "GS" to "GCR", "IT" to "TTW",
            "TW" to "TWB", "UK" to "VTI",
            // Oceania
            "QF" to "QFA", "NZ" to "ANZ", "VA" to "VOZ", "SB" to "ACI",
            "ZL" to "RXA",
            // Africa
            "ET" to "ETH", "MS" to "MSR", "KQ" to "KQA", "AT" to "RAM",
            "WB" to "RWD", "SA" to "SAA", "8U" to "AAW", "BP" to "BOT",
            "2J" to "VBW", "HF" to "VRE", "MD" to "MDG", "SW" to "NMB",
            "HC" to "SZN", "HM" to "SEY", "TC" to "ATC", "4Z" to "LNK",
            "W3" to "ARA", "KP" to "SKK", "FN" to "FTZ", "TM" to "LAM",
            "LN" to "LAA", "FA" to "SFR", "DT" to "DTA", "TU" to "TAR",
            "UR" to "UGA", "P4" to "APK",
            // Latin America
            "LA" to "LAN", "AV" to "AVA", "CM" to "CMP", "G3" to "GLO",
            "AD" to "AZU", "AM" to "AMX", "VB" to "VIV", "Y4" to "VOI",
            "AR" to "ARG", "TX" to "FWI", "TS" to "TSC", "Z8" to "AZN",
            "UP" to "BHS", "OB" to "BOV", "BW" to "BWA", "KX" to "CAY",
            "FO" to "FBZ", "JA" to "JAT", "LI" to "LIA", "H2" to "SKU",
            // Russia & CIS
            "SU" to "AFL", "B2" to "BRU", "9U" to "MLD", "PS" to "AUI",
            // Cargo
            "CV" to "CLX", "CK" to "CKK", "D0" to "DHK", "FX" to "FDX",
            "K4" to "CKS", "KZ" to "NCA", "PO" to "PAC", "5Y" to "GTI",
            // Other
            "VS" to "VIR", "AZ" to "ITY", "JU" to "ASL", "OU" to "CTN",
            "HY" to "UZB", "KC" to "KZR", "MT" to "AMC",
            "JT" to "LNI", "TN" to "THT", "BI" to "RBA", "BJ" to "LBT",
            "KF" to "ABB", "XP" to "VXP", "FJ" to "FJI", "MK" to "MAU",
            "GQ" to "SEH", "BX" to "ABL", "BC" to "SKY", "YW" to "ANE",
            "SC" to "CDG", "RA" to "RNA", "QL" to "LER",
            "4O" to "AIJ", "9W" to "JAI", "MN" to "CAW", "JE" to "MNO",
        )

        iataToIcao = pairs.toMap()
        icaoToIata = pairs.associate { (iata, icao) -> icao to iata }
    }

    /** Convert ICAO airline code to IATA. E.g. "WMT" → "W4", "THY" → "TK" */
    fun iataFromIcao(icao: String): String? = icaoToIata[icao.uppercase()]

    /** Convert IATA airline code to ICAO. E.g. "W4" → "WMT", "TK" → "THY" */
    fun icaoFromIata(iata: String): String? = iataToIcao[iata.uppercase()]

    /**
     * Extract airline prefix and flight number from a flight ident.
     * Uses iOS regex pattern: airline prefix is 2-3 letter code or letter+digit / digit+letter.
     * E.g. "WMT5YV" → ("WMT", "5YV"), "W46203" → ("W4", "6203"), "TK123" → ("TK", "123")
     */
    fun parseFlightIdent(ident: String): Pair<String, String>? {
        val upper = ident.uppercase().trim().replace("\\s+".toRegex(), "")
        // iOS regex: ^([A-Z]{2,3}|[A-Z][0-9]|[0-9][A-Z])\s*(\d{1,5})([A-Z]{0,3})$
        val match = Regex("""^([A-Z]{2,3}|[A-Z][0-9]|[0-9][A-Z])(\d{1,5}[A-Z]{0,3})$""")
            .matchEntire(upper)
        if (match != null) {
            return match.groupValues[1] to match.groupValues[2]
        }
        // Fallback: try known 3-letter ICAO prefix even if number part has letters
        if (upper.length > 3) {
            val prefix3 = upper.substring(0, 3)
            val rest3 = upper.substring(3)
            if (prefix3.all { it.isLetter() } && rest3.isNotEmpty() &&
                (icaoToIata.containsKey(prefix3) || iataToIcao.containsKey(prefix3))) {
                return prefix3 to rest3
            }
        }
        // Fallback: try known 2-char prefix
        if (upper.length > 2) {
            val prefix2 = upper.substring(0, 2)
            val rest2 = upper.substring(2)
            if (rest2.isNotEmpty() &&
                (iataToIcao.containsKey(prefix2) || icaoToIata.containsKey(prefix2))) {
                return prefix2 to rest2
            }
        }
        return null
    }

    /**
     * Strip trailing ATC suffix letters from a flight number.
     * E.g. "5YV" → "5", "40SP" → "40", "123" → "123", "76Q" → "76"
     * Mirrors iOS PersonalFlightManager.flightIdentCandidates() baseNumber logic.
     */
    private fun stripAtcSuffix(number: String): String {
        var base = number
        while (base.length > 1 && base.last().isLetter()) {
            base = base.dropLast(1)
        }
        return base
    }

    /**
     * Generate search candidate variants for a flight ident.
     * Mirrors iOS PersonalFlightManager.flightIdentCandidates() logic:
     *
     * Priority 1: Original ident with all code variants (ICAO/IATA)
     * Priority 2: ATC suffix-stripped variants with code variants
     * Priority 3: Leading-zero-stripped variants
     *
     * Example: "WMT5YV" → ["WMT5YV", "W45YV", "WMT5", "W45"]
     */
    fun searchCandidates(query: String): List<String> {
        val upper = query.uppercase().trim().replace("\\s+".toRegex(), "")
        val parsed = parseFlightIdent(upper) ?: return listOf(upper)
        val (prefix, number) = parsed

        val candidates = mutableListOf<String>()

        // Collect all airline code variants (original + ICAO/IATA conversion)
        val codeVariants = mutableListOf(prefix)
        iataFromIcao(prefix)?.let { codeVariants.add(it) }
        icaoFromIata(prefix)?.let { codeVariants.add(it) }

        // Priority 1: Full ident with all code variants
        for (code in codeVariants) {
            candidates.add("$code$number")
        }

        // Priority 2: Suffix-stripped variants (if number contains ATC suffix letters)
        val baseNumber = stripAtcSuffix(number)
        if (baseNumber != number && baseNumber.isNotEmpty()) {
            for (code in codeVariants) {
                candidates.add("$code$baseNumber")
            }
        }

        // Priority 3: Leading-zero-stripped (e.g. IBE0132 → IBE132)
        if (number.startsWith("0") && number.length > 1) {
            val stripped = number.trimStart('0')
            if (stripped.isNotEmpty() && stripped != number) {
                for (code in codeVariants) {
                    candidates.add("$code$stripped")
                }
            }
        }

        return candidates.distinct()
    }
}
