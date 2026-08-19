package com.fpf.smartscan.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

object TestDatabase {
    fun create(): MediaDatabase {
        return Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MediaDatabase::class.java
        ).allowMainThreadQueries().build()
    }
}