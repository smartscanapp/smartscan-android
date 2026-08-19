package com.fpf.smartscan.core.data.mappers

import com.fpf.smartscan.core.data.clusters.AutoCollectionData
import com.fpf.smartscan.core.data.tags.TagCollectionData
import com.fpf.smartscan.core.media.CollectionType
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.core.media.MediaStoreHelper

fun AutoCollectionData.toDomain(): MediaCollection = MediaCollection(
    id = clusterId,
    name = label?: MediaCollection.UNLABELLED_COLLECTION,
    size = prototypeSize,
    imageCount = imageCount,
    duplicateImageCount = duplicateImageCount,
    thumbNail = MediaStoreHelper.mediaIdToUri(thumbNailId, thumbNailType),
    type = CollectionType.CLUSTER
)

fun TagCollectionData.toDomain(): MediaCollection = MediaCollection(
    id = tagId,
    name = name,
    size = size,
    imageCount = imageCount,
    duplicateImageCount = duplicateImageCount,
    thumbNail = MediaStoreHelper.mediaIdToUri(thumbNailId, thumbNailType),
    type = CollectionType.TAG
)