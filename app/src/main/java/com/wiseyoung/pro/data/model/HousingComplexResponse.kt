package com.wiseyoung.pro.data.model

import com.google.gson.annotations.SerializedName

data class HousingComplexResponse(
    @SerializedName("complexId")
    val complexId: String? = null,

    // 배포 서버 응답 필드 (HousingComplexListResponse)
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("organization")
    val organization: String? = null,

    @SerializedName("region")
    val region: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("completionDate")
    val completionDate: String? = null,

    @SerializedName("housingType")
    val housingType: String? = null,

    @SerializedName("heatingType")
    val heatingType: String? = null,

    @SerializedName("hasElevator")
    val hasElevator: Boolean? = null,

    // LH API 원본 필드 (공고 matchedComplex 등)
    @SerializedName("hsmpNm")
    val hsmpNm: String? = null,

    @SerializedName("insttNm")
    val insttNm: String? = null,

    @SerializedName("brtcNm")
    val brtcNm: String? = null,

    @SerializedName("signguNm")
    val signguNm: String? = null,

    @SerializedName("rnAdres")
    val rnAdres: String? = null,

    @SerializedName("completeDate")
    val completeDate: String? = null,

    @SerializedName("suplyTyNm")
    val suplyTyNm: String? = null,

    @SerializedName("styleNm")
    val styleNm: String? = null,

    @SerializedName("supplyArea")
    val supplyArea: Double? = null,

    @SerializedName("houseTyNm")
    val houseTyNm: String? = null,

    @SerializedName("heatMthdDetailNm")
    val heatMthdDetailNm: String? = null,

    @SerializedName("buldStleNm")
    val buldStleNm: String? = null,

    @SerializedName("elevator")
    val elevator: Boolean? = null,

    @SerializedName("parkingSpaces")
    val parkingSpaces: Int? = null,

    @SerializedName("deposit")
    val deposit: Int? = null,

    @SerializedName("monthlyRent")
    val monthlyRent: Int? = null,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("isBookmarked")
    val isBookmarked: Boolean? = null,

    @SerializedName("distanceFromUser")
    val distanceFromUser: Double? = null,

    @SerializedName("totalUnits")
    val totalUnits: Int? = null
) {
    fun displayName(): String = name ?: hsmpNm ?: ""
    fun displayAddress(): String = address ?: rnAdres ?: ""
    fun displayOrganization(): String = organization ?: insttNm ?: ""
    fun displayRegion(): String = region ?: signguNm ?: brtcNm ?: ""
    fun displayCompletionDate(): String = completionDate ?: completeDate ?: ""
    fun displayHousingType(): String = housingType ?: houseTyNm ?: suplyTyNm ?: ""
    fun displayHeatingType(): String = heatingType ?: heatMthdDetailNm ?: ""
    fun displayHasElevator(): Boolean = hasElevator ?: elevator ?: false
}
