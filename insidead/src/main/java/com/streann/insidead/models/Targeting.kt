package com.streann.insidead.models

data class Targeting(
    var id: String? = null,
    var version: Int? = null,
    var createdOn: String? = null,
    var modifiedOn: String? = null,
    var name: String? = null,
    var resellerId: String? = null,
    var targets: Targets? = null
)

data class Targets(
    var type: String? = null,
    var ids: ArrayList<String>? = null
)
