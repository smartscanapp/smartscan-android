package com.fpf.smartscan.cloud.index

import com.fpf.smartscan.core.index.BaseIndexListener

object CloudImageIndexListener : BaseIndexListener(
    tag = "CloudImageIndexListener"
) {
    override val itemName: String = "Image"
}
