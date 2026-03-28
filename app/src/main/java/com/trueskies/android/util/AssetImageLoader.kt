package com.trueskies.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Utility for loading images from the Android assets folder.
 * Assets are organized in subdirectories mirroring the iOS Assets.xcassets structure:
 *
 * - `aircraft/`         — Airline-specific aircraft images (e.g., `aal_b738.png`)
 * - `aircraft_generic/` — Generic aircraft type images (e.g., `a350.png`)
 * - `airline_logos/`    — Airline logo images (e.g., `turkish_airlines_logo.png`)
 * - `passport_emblems/` — Country emblems for passport/log (e.g., `turkey_emblem.png`)
 * - `seat_maps/`        — Aircraft seat map images
 */
object AssetImageLoader {

    /** IATA/ICAO code → asset name mapping (ported from iOS AirlineLogoView.logoMap) */
    private val airlineCodeToLogo = mapOf(
        // North American Airlines
        "B6" to "jetblue_logo", "JBU" to "jetblue_logo",
        "UA" to "united_logo", "UAL" to "united_logo",
        "AA" to "american_logo", "AAL" to "american_logo",
        "DL" to "delta_logo", "DAL" to "delta_logo",
        "WN" to "southwest_logo", "SWA" to "southwest_logo",
        "AS" to "alaska_logo", "ASA" to "alaska_logo",
        "F9" to "frontier_logo", "FFT" to "frontier_logo",
        "NK" to "spirit_logo", "NKS" to "spirit_logo",
        "G4" to "allegiant_logo", "AAY" to "allegiant_logo",
        "AC" to "air_canada_logo", "ACA" to "air_canada_logo",
        "HA" to "hawaiian_airlines_logo", "HAL" to "hawaiian_airlines_logo",
        "WS" to "westjet_logo", "WJA" to "westjet_logo",
        "YX" to "republic_airways_logo", "RPA" to "republic_airways_logo",
        "MX" to "breeze_airways_logo", "MXX" to "breeze_airways_logo",
        "SY" to "sun_country_airlines_logo", "SCX" to "sun_country_airlines_logo",
        "PD" to "porter_airlines_logo", "POE" to "porter_airlines_logo",
        "F8" to "flair_airlines_logo", "FLE" to "flair_airlines_logo",
        // European Airlines
        "BA" to "british_airways_logo", "BAW" to "british_airways_logo",
        "LH" to "lufthansa_logo", "DLH" to "lufthansa_logo",
        "AF" to "air_france_logo", "AFR" to "air_france_logo",
        "KL" to "klm_logo", "KLM" to "klm_logo",
        "IB" to "iberia_logo", "IBE" to "iberia_logo",
        "I2" to "iberia_express_logo", "IBS" to "iberia_express_logo",
        "AY" to "finnair_logo", "FIN" to "finnair_logo",
        "SK" to "sas_logo", "SAS" to "sas_logo",
        "U2" to "easyjet_logo", "EZY" to "easyjet_logo",
        "FR" to "ryanair_logo", "RYR" to "ryanair_logo",
        "SN" to "brussels_airlines_logo", "BEL" to "brussels_airlines_logo",
        "TK" to "turkish_airlines_logo", "THY" to "turkish_airlines_logo",
        "VY" to "vueling_logo", "VLG" to "vueling_logo",
        "PC" to "pegasus_logo", "PGT" to "pegasus_logo",
        "DY" to "norwegian_air_logo", "NAX" to "norwegian_air_logo",
        "BY" to "tui_airways_logo", "TOM" to "tui_airways_logo",
        "RO" to "tarom_logo", "ROT" to "tarom_logo",
        "A3" to "aegean_airlines_logo", "AEE" to "aegean_airlines_logo",
        "EI" to "aer_lingus_logo", "EIN" to "aer_lingus_logo",
        "BT" to "air_baltic_logo", "BTI" to "air_baltic_logo",
        "XK" to "air_corsica_logo", "CCM" to "air_corsica_logo",
        "EN" to "air_dolomiti_logo", "DLA" to "air_dolomiti_logo",
        "UX" to "air_europa_logo", "AEA" to "air_europa_logo",
        "OS" to "austrian_airlines_logo", "AUA" to "austrian_airlines_logo",
        "WX" to "cityjet_logo", "BCY" to "cityjet_logo",
        "SS" to "corsair_international_logo", "CRL" to "corsair_international_logo",
        "WK" to "edelweiss_air_logo", "EDW" to "edelweiss_air_logo",
        "EW" to "eurowings_logo", "EWG" to "eurowings_logo",
        "BF" to "french_bee_logo", "FBU" to "french_bee_logo",
        "FI" to "icelandair_logo", "ICE" to "icelandair_logo",
        "LM" to "loganair_logo", "LOG" to "loganair_logo",
        "LO" to "lot_polish_airlines_logo", "LOT" to "lot_polish_airlines_logo",
        "LG" to "luxair_logo", "LGL" to "luxair_logo",
        "N0" to "norse_atlantic_airways_logo", "NBT" to "norse_atlantic_airways_logo",
        "OA" to "olympic_air_logo", "OAL" to "olympic_air_logo",
        "OG" to "play_logo", "FPY" to "play_logo",
        "XQ" to "sunexpress_logo", "SXS" to "sunexpress_logo",
        "LX" to "swiss_international_air_lines_logo", "SWR" to "swiss_international_air_lines_logo",
        "TP" to "tap_air_portugal_logo", "TAP" to "tap_air_portugal_logo",
        "V7" to "volotea_logo", "VOE" to "volotea_logo",
        "WF" to "wideroe_logo", "WIF" to "wideroe_logo",
        // Middle Eastern Airlines
        "EK" to "emirates_logo", "UAE" to "emirates_logo",
        "EY" to "etihad_airways_logo", "ETD" to "etihad_airways_logo",
        "QR" to "qatar_airways_logo", "QTR" to "qatar_airways_logo",
        "SV" to "saudia_logo", "SVA" to "saudia_logo",
        "FZ" to "flydubai_logo", "FDB" to "flydubai_logo",
        "GF" to "gulf_air_logo", "GFA" to "gulf_air_logo",
        "WY" to "oman_air_logo", "OMA" to "oman_air_logo",
        "ME" to "middle_east_airlines_logo", "MEA" to "middle_east_airlines_logo",
        "RJ" to "royal_jordanian_logo", "RJA" to "royal_jordanian_logo",
        "KU" to "kuwait_airways_logo", "KAC" to "kuwait_airways_logo",
        "LY" to "el_al_israel_airlines_logo", "ELY" to "el_al_israel_airlines_logo",
        "XY" to "flynas_logo", "KNE" to "flynas_logo",
        "J9" to "jazeera_airways_logo", "JZR" to "jazeera_airways_logo",
        "OV" to "salamair_logo", "OMS" to "salamair_logo",
        "AH" to "air_algerie_logo", "DAH" to "air_algerie_logo",
        // Asian Airlines
        "SQ" to "singapore_airlines_logo", "SIA" to "singapore_airlines_logo",
        "CX" to "cathay_pacific_logo", "CPA" to "cathay_pacific_logo",
        "NH" to "all_nippon_logo", "ANA" to "all_nippon_logo",
        "JL" to "japan_airlines_logo", "JAL" to "japan_airlines_logo",
        "KE" to "korean_air_logo", "KAL" to "korean_air_logo",
        "OZ" to "asiana_airlines_logo", "AAR" to "asiana_airlines_logo",
        "TG" to "thai_airways_logo", "THA" to "thai_airways_logo",
        "MH" to "malaysia_airlines_logo", "MAS" to "malaysia_airlines_logo",
        "GA" to "garuda_indonesia_logo", "GIA" to "garuda_indonesia_logo",
        "BR" to "eva_air_logo", "EVA" to "eva_air_logo",
        "CI" to "china_airlines_logo", "CAL" to "china_airlines_logo",
        "CA" to "air_china_logo", "CCA" to "air_china_logo",
        "MU" to "china_eastern_logo", "CES" to "china_eastern_logo",
        "CZ" to "china_southern_logo", "CSN" to "china_southern_logo",
        "HU" to "hainan_airlines_logo", "CHH" to "hainan_airlines_logo",
        "MF" to "xiamen_airlines_logo", "CXA" to "xiamen_airlines_logo",
        "AI" to "air_india_logo", "AIC" to "air_india_logo",
        "6E" to "indigo_logo", "IGO" to "indigo_logo",
        "SG" to "spicejet_logo", "SEJ" to "spicejet_logo",
        "G8" to "go_first_logo", "GOW" to "go_first_logo",
        "VN" to "vietnam_airlines_logo", "HVN" to "vietnam_airlines_logo",
        "VJ" to "vietjet_air_logo", "VJC" to "vietjet_air_logo",
        "5J" to "cebu_pacific_logo", "CEB" to "cebu_pacific_logo",
        "PR" to "philippine_airlines_logo", "PAL" to "philippine_airlines_logo",
        "AK" to "airasia_logo", "AXM" to "airasia_logo",
        "JQ" to "jetstar_airways_logo", "JST" to "jetstar_airways_logo",
        "TR" to "scoot_logo", "TGW" to "scoot_logo",
        "UL" to "srilankan_airlines_logo", "ALK" to "srilankan_airlines_logo",
        "PK" to "pakistan_international_airlines_logo", "PIA" to "pakistan_international_airlines_logo",
        "BG" to "biman_bangladesh_airlines_logo", "BBC" to "biman_bangladesh_airlines_logo",
        "QH" to "bamboo_airways_logo", "BAV" to "bamboo_airways_logo",
        "D7" to "airasia_x_logo", "XAX" to "airasia_x_logo",
        "RS" to "air_seoul_logo", "ASV" to "air_seoul_logo",
        "QP" to "akasa_air_logo", "AKJ" to "akasa_air_logo",
        "ID" to "batik_air_logo", "BTK" to "batik_air_logo",
        "QG" to "citilink_logo", "CTV" to "citilink_logo",
        "7C" to "jeju_air_logo", "JJA" to "jeju_air_logo",
        "LJ" to "jin_air_logo", "JNA" to "jin_air_logo",
        "HO" to "juneyao_airlines_logo", "DKH" to "juneyao_airlines_logo",
        "8L" to "lucky_air_logo", "LKE" to "lucky_air_logo",
        "MM" to "peach_aviation_logo", "APJ" to "peach_aviation_logo",
        "ZH" to "shenzhen_airlines_logo", "CSZ" to "shenzhen_airlines_logo",
        "3U" to "sichuan_airlines_logo", "CSC" to "sichuan_airlines_logo",
        "6J" to "solaseed_air_logo", "SNJ" to "solaseed_air_logo",
        "9C" to "spring_airlines_logo", "CQH" to "spring_airlines_logo",
        "JX" to "starlux_airlines_logo", "SJX" to "starlux_airlines_logo",
        "GS" to "tianjin_airlines_logo", "GCR" to "tianjin_airlines_logo",
        "IT" to "tigerair_taiwan_logo", "TTW" to "tigerair_taiwan_logo",
        "TW" to "t_way_air_logo", "TWB" to "t_way_air_logo",
        "UK" to "vistara_logo", "VTI" to "vistara_logo",
        // Oceania Airlines
        "QF" to "qantas_logo", "QFA" to "qantas_logo",
        "NZ" to "air_new_zealand_logo", "ANZ" to "air_new_zealand_logo",
        "VA" to "virgin_australia_logo", "VOZ" to "virgin_australia_logo",
        "SB" to "aircalin_logo", "ACI" to "aircalin_logo",
        "ZL" to "rex_airlines_logo", "RXA" to "rex_airlines_logo",
        // African Airlines
        "ET" to "ethiopian_airlines_logo", "ETH" to "ethiopian_airlines_logo",
        "MS" to "egyptair_logo", "MSR" to "egyptair_logo",
        "KQ" to "kenya_airways_logo", "KQA" to "kenya_airways_logo",
        "AT" to "royal_air_maroc_logo", "RAM" to "royal_air_maroc_logo",
        "WB" to "rwandair_logo", "RWD" to "rwandair_logo",
        "SA" to "south_african_airways_logo", "SAA" to "south_african_airways_logo",
        "8U" to "afriqiyah_airways_logo", "AAW" to "afriqiyah_airways_logo",
        "BP" to "air_botswana_logo", "BOT" to "air_botswana_logo",
        "2J" to "air_burkina_logo", "VBW" to "air_burkina_logo",
        "HF" to "air_cote_d_ivoire_logo", "VRE" to "air_cote_d_ivoire_logo",
        "MD" to "air_madagascar_logo", "MDG" to "air_madagascar_logo",
        "SW" to "air_namibia_logo", "NMB" to "air_namibia_logo",
        "HC" to "air_senegal_logo", "SZN" to "air_senegal_logo",
        "HM" to "air_seychelles_logo", "SEY" to "air_seychelles_logo",
        "TC" to "air_tanzania_logo", "ATC" to "air_tanzania_logo",
        "4Z" to "airlink_logo", "LNK" to "airlink_logo",
        "W3" to "arik_air_logo", "ARA" to "arik_air_logo",
        "KP" to "asky_airlines_logo", "SKK" to "asky_airlines_logo",
        "FN" to "fastjet_logo", "FTZ" to "fastjet_logo",
        "TM" to "lam_mozambique_airlines_logo", "LAM" to "lam_mozambique_airlines_logo",
        "LN" to "libyan_airlines_logo", "LAA" to "libyan_airlines_logo",
        "FA" to "safair_logo", "SFR" to "safair_logo",
        "DT" to "taag_angola_airlines_logo", "DTA" to "taag_angola_airlines_logo",
        "TU" to "tunisair_logo", "TAR" to "tunisair_logo",
        "UR" to "uganda_airlines_logo", "UGA" to "uganda_airlines_logo",
        // Latin American Airlines
        "LA" to "latam_logo", "LAN" to "latam_logo",
        "AV" to "avianca_logo", "AVA" to "avianca_logo",
        "CM" to "copa_airlines_logo", "CMP" to "copa_airlines_logo",
        "G3" to "gol_logo", "GLO" to "gol_logo",
        "AD" to "azul_logo", "AZU" to "azul_logo",
        "AM" to "aeromexico_logo", "AMX" to "aeromexico_logo",
        "VB" to "viva_aerobus_logo", "VIV" to "viva_aerobus_logo",
        "Y4" to "volaris_logo", "VOI" to "volaris_logo",
        "AR" to "aerolineas_argentinas_logo", "ARG" to "aerolineas_argentinas_logo",
        "TX" to "air_caraibes_logo", "FWI" to "air_caraibes_logo",
        "TS" to "air_transat_logo", "TSC" to "air_transat_logo",
        "Z8" to "amaszonas_logo", "AZN" to "amaszonas_logo",
        "UP" to "bahamasair_logo", "BHS" to "bahamasair_logo",
        "OB" to "boliviana_de_aviacion_logo", "BOV" to "boliviana_de_aviacion_logo",
        "BW" to "caribbean_airlines_logo", "BWA" to "caribbean_airlines_logo",
        "KX" to "cayman_airways_logo", "CAY" to "cayman_airways_logo",
        "FO" to "flybondi_logo", "FBZ" to "flybondi_logo",
        "JA" to "jetsmart_logo", "JAT" to "jetsmart_logo",
        "LI" to "liat_logo", "LIA" to "liat_logo",
        "H2" to "sky_airline_logo", "SKU" to "sky_airline_logo",
        "FB" to "bulgaria_air_logo", "LZB" to "bulgaria_air_logo",
        // Russian & CIS Airlines
        "SU" to "aeroflot_logo", "AFL" to "aeroflot_logo",
        "B2" to "belavia_logo", "BRU" to "belavia_logo",
        "9U" to "air_moldova_logo", "MLD" to "air_moldova_logo",
        "PS" to "ukraine_international_airlines_logo", "AUI" to "ukraine_international_airlines_logo",
        // Cargo Airlines
        "CV" to "cargolux_logo", "CLX" to "cargolux_logo",
        "CK" to "china_cargo_airlines_logo", "CKK" to "china_cargo_airlines_logo",
        "D0" to "dhl_air_logo", "DHK" to "dhl_air_logo",
        "FX" to "fedex_express_logo", "FDX" to "fedex_express_logo",
        "K4" to "kalitta_air_logo", "CKS" to "kalitta_air_logo",
        "KZ" to "nippon_cargo_airlines_logo", "NCA" to "nippon_cargo_airlines_logo",
        "PO" to "polar_air_cargo_logo", "PAC" to "polar_air_cargo_logo",
        "5Y" to "atlas_air_logo", "GTI" to "atlas_air_logo",
        // Other International
        "VS" to "virgin_atlantic_logo", "VIR" to "virgin_atlantic_logo",
        "AZ" to "ita_airways_logo", "ITY" to "ita_airways_logo", "AZA" to "ita_airways_logo",
        "JU" to "air_serbia_logo", "ASL" to "air_serbia_logo",
        "OU" to "croatia_airlines_logo", "CTN" to "croatia_airlines_logo",
        "HY" to "uzbekistan_airways_logo", "UZB" to "uzbekistan_airways_logo",
        "KC" to "air_astana_logo", "KZR" to "air_astana_logo",
        "LS" to "jet2_logo", "EXS" to "jet2_logo",
        "MT" to "air_malta_logo", "AMC" to "air_malta_logo", "KM" to "air_malta_logo",
        "0B" to "blue_air_logo", "BLA" to "blue_air_logo",
        "G9" to "air_arabia_logo", "ABY" to "air_arabia_logo",
        "HV" to "transavia_logo", "TRA" to "transavia_logo",
        "TO" to "transavia_logo", "TVF" to "transavia_logo",
        "JT" to "lion_air_logo", "LNI" to "lion_air_logo",
        "TN" to "air_tahiti_nui_logo", "THT" to "air_tahiti_nui_logo",
        "BI" to "royal_brunei_airlines_logo", "RBA" to "royal_brunei_airlines_logo",
        "BJ" to "nouvelair_logo", "LBT" to "nouvelair_logo",
        "KF" to "air_belgium_logo", "ABB" to "air_belgium_logo",
        "XP" to "avelo_airlines_logo", "VXP" to "avelo_airlines_logo",
        "DE" to "condor_logo", "CFG" to "condor_logo",
        "FJ" to "fiji_airways_logo", "FJI" to "fiji_airways_logo",
        "MK" to "air_mauritius_logo", "MAU" to "air_mauritius_logo",
        "GQ" to "sky_express_logo", "SEH" to "sky_express_logo",
        "BX" to "air_busan_logo", "ABL" to "air_busan_logo",
        "BC" to "skymark_airlines_logo", "SKY" to "skymark_airlines_logo",
        "YW" to "air_nostrum_logo", "ANE" to "air_nostrum_logo",
        "P4" to "air_peace_logo", "APK" to "air_peace_logo",
        "TKJ" to "ajet_logo",
        "SC" to "shandong_airlines_logo", "CDG" to "shandong_airlines_logo",
        "RA" to "nepal_airlines_logo", "RNA" to "nepal_airlines_logo",
        "W6" to "wizz_air_logo", "WZZ" to "wizz_air_logo", "W4" to "wizz_air_logo", "WMT" to "wizz_air_logo",
        "BE" to "flybe_logo", "BEE" to "flybe_logo",
        "4O" to "interjet_logo", "AIJ" to "interjet_logo",
        "9W" to "jet_airways_logo", "JAI" to "jet_airways_logo",
        "MN" to "kulula_logo", "CAW" to "kulula_logo",
        "JE" to "mango_airlines_logo", "MNO" to "mango_airlines_logo",
        "X3" to "tui_airways_logo",
        "QL" to "laser_airlines_logo", "LER" to "laser_airlines_logo"
    )

    /**
     * Load an airline logo by IATA or ICAO code.
     * Uses a comprehensive code→asset mapping (synced with iOS).
     */
    fun loadAirlineLogoByCode(context: Context, code: String?): Bitmap? {
        if (code == null) return null
        val logoName = airlineCodeToLogo[code.uppercase()] ?: return null
        return loadBitmap(context, "airline_logos/${logoName}.png")
    }

    /**
     * Load a Bitmap from assets, returning null if not found.
     */
    fun loadBitmap(context: Context, path: String): Bitmap? {
        return try {
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load an aircraft image by airline ICAO code and aircraft type ICAO code.
     * Falls back to generic aircraft image if airline-specific one is not found.
     *
     * @param airlineIcao e.g., "AAL" (American Airlines), "THY" (Turkish Airlines)
     * @param aircraftIcao e.g., "B738", "A321", "B789"
     */
    fun loadAircraftBitmap(context: Context, airlineIcao: String?, aircraftIcao: String?): Bitmap? {
        if (aircraftIcao == null) return null
        val typeCode = aircraftIcao.lowercase()

        // Try airline-specific image first
        if (airlineIcao != null) {
            val airlineCode = airlineIcao.lowercase()
            val specific = loadBitmap(context, "aircraft/${airlineCode}_${typeCode}.png")
            if (specific != null) return specific
        }

        // Fall back to generic aircraft type
        return loadBitmap(context, "aircraft_generic/${typeCode}.png")
    }

    /**
     * Load an airline logo by airline name.
     * Tries multiple name variations to find a match:
     * - Full name (e.g., "Turkish Airlines" → "turkish_airlines_logo.png")
     * - Without common suffixes like "Airlines", "Airways", "Air Lines"
     * - First word only (e.g., "Pegasus Airlines" → "pegasus_logo.png")
     */
    fun loadAirlineLogoBitmap(context: Context, airlineName: String?): Bitmap? {
        if (airlineName == null) return null

        val candidates = buildList {
            // Full name as-is
            add(airlineName)
            // Remove common suffixes
            val stripped = airlineName
                .replace(Regex("\\s+(Airlines?|Airways?|Air Lines?)\\s*$", RegexOption.IGNORE_CASE), "")
                .trim()
            if (stripped != airlineName) add(stripped)
            // First word only (e.g., "Pegasus" from "Pegasus Airlines")
            val firstWord = airlineName.split(" ").firstOrNull()
            if (firstWord != null && firstWord != stripped) add(firstWord)
        }

        for (candidate in candidates) {
            val sanitized = candidate.lowercase()
                .replace(Regex("[^a-z0-9]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
            val bitmap = loadBitmap(context, "airline_logos/${sanitized}_logo.png")
            if (bitmap != null) return bitmap
        }
        return null
    }

    /**
     * Load a country emblem by country name.
     * The emblem filenames follow the pattern: `{country_name}_emblem.png`
     */
    fun loadPassportEmblemBitmap(context: Context, countryName: String?): Bitmap? {
        if (countryName == null) return null
        val sanitized = countryName.lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return loadBitmap(context, "passport_emblems/${sanitized}_emblem.png")
    }

    /**
     * List all available files in an asset subdirectory.
     */
    fun listAssets(context: Context, directory: String): List<String> {
        return try {
            context.assets.list(directory)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Composable helper to load an asset image as ImageBitmap.
 * Returns null if the asset is not found.
 */
@Composable
fun rememberAssetImage(path: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(path) {
        AssetImageLoader.loadBitmap(context, path)?.asImageBitmap()
    }
}

/**
 * Composable helper to load an aircraft image.
 */
@Composable
fun rememberAircraftImage(airlineIcao: String?, aircraftIcao: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(airlineIcao, aircraftIcao) {
        AssetImageLoader.loadAircraftBitmap(context, airlineIcao, aircraftIcao)?.asImageBitmap()
    }
}

/**
 * Composable helper to load an airline logo.
 * Tries code-based lookup first (IATA/ICAO), then falls back to name-based lookup.
 */
@Composable
fun rememberAirlineLogo(airlineName: String?, airlineCode: String? = null): ImageBitmap? {
    val context = LocalContext.current
    return remember(airlineName, airlineCode) {
        AssetImageLoader.loadAirlineLogoByCode(context, airlineCode)?.asImageBitmap()
            ?: AssetImageLoader.loadAirlineLogoBitmap(context, airlineName)?.asImageBitmap()
    }
}
