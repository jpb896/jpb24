package com.jpb.jpb24x.helpers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocInfo(
    @SerialName("soc_id") val socId: String,
    @SerialName("marketing_name") val marketingName: String,
    @SerialName("process_node") val processNode: String,
    @SerialName("vendor") val vendor: String,
    @SerialName("foundry") val foundry: String,
    @SerialName("fab") val fab: String
)