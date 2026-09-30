package com.fpf.smartscan.core.collections

interface CollectionManager<Item, Collection> {
    suspend fun move(items: Set<Item>, newCollectionId: Long,  currentCollectionId: Long )
    suspend fun merge(primaryCollectionId: Long,  secondaryCollectionIds: List<Long>)
    suspend fun rename(collectionId: Long, name: String)
    suspend fun delete(collectionIds: List<Long>){
        throw NotImplementedError()
    }
    suspend fun remove(items: Set<Item>, collectionId: Long){
        throw NotImplementedError()
    }
    suspend fun get(): List<Collection>
}