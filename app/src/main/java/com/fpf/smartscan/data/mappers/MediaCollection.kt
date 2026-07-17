package com.fpf.smartscan.data.mappers

import com.fpf.smartscan.data.clusters.AutoCollectionData
import com.fpf.smartscan.data.tags.TagCollectionData
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaStoreHelper

fun AutoCollectionData.toDomain(): MediaCollection = MediaCollection(
    id = clusterId,
    name = label?: MediaCollection.UNLABELLED_COLLECTION,
    size = prototypeSize,
    thumbNail = MediaStoreHelper.mediaIdToUri(thumbNailId, thumbNailType),
    type = CollectionType.CLUSTER
)

fun TagCollectionData.toDomain(): MediaCollection = MediaCollection(
    id = tagId,
    name = name,
    size = size,
    thumbNail = MediaStoreHelper.mediaIdToUri(thumbNailId, thumbNailType),
    type = CollectionType.TAG
)