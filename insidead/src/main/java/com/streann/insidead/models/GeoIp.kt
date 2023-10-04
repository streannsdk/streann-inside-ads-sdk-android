package com.streann.insidead.models

data class GeoIp(
    var asName: String? = null,
    var asNumber: Int? = null,
    var areaCode: Int? = null,
    var city: String? = null,
    var connSpeed: String? = null,
    var connType: String? = null,
    var continentCode: String? = null,
    var countryCode: String? = null,
    var countryCode3: String? = null,
    var country: String? = null,
    var latitude: String? = null,
    var longitude: String? = null,
    var metroCode: Int? = null,
    var postalCode: String? = null,
    var proxyDescription: String? = null,
    var proxyType: String? = null,
    var region: String? = null,
    var ip: String? = null,
    var utcOffset: Int? = null
)