package com.trueskies.android.domain.models

/**
 * Visa requirement for a destination country based on passport country.
 * Matches iOS VisaRequirementCategory from PassportVisaModels.swift.
 */
data class VisaRequirement(
    val category: VisaCategory,
    val requirement: String,
    val passportCountry: String,
    val destinationCountry: String
) {
    companion object {
        fun fromResponse(
            requirement: String,
            visaRequired: Boolean?,
            visaFree: Boolean?,
            passportCountry: String,
            destinationCountry: String
        ): VisaRequirement {
            val category = when {
                visaFree == true -> VisaCategory.VISA_FREE
                visaRequired == true -> VisaCategory.VISA_REQUIRED
                else -> categorize(requirement)
            }
            return VisaRequirement(
                category = category,
                requirement = requirement,
                passportCountry = passportCountry,
                destinationCountry = destinationCountry
            )
        }

        private fun categorize(requirement: String): VisaCategory {
            val lower = requirement.lowercase()
            return when {
                lower.contains("visa not required") || lower.contains("visa free")
                        || lower.contains("freedom of movement") -> VisaCategory.VISA_FREE
                lower.contains("visa on arrival") || lower.contains("on arrival") -> VisaCategory.VISA_ON_ARRIVAL
                lower.contains("evisa") || lower.contains("e-visa")
                        || lower.contains("electronic") -> VisaCategory.E_VISA
                lower.contains("visa required") || lower.contains("required") -> VisaCategory.VISA_REQUIRED
                else -> VisaCategory.UNKNOWN
            }
        }
    }
}

enum class VisaCategory {
    VISA_FREE,
    VISA_ON_ARRIVAL,
    E_VISA,
    VISA_REQUIRED,
    UNKNOWN
}
