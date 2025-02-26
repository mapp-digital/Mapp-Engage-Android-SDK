package com.appoxee.internal.migration

import com.appoxee.internal.migration.data.OldRegistration
import com.appoxee.shared.AppoxeeOptions
import java.io.File

internal interface MigrationHelper {
    suspend fun readTextFromFile(): String?

    suspend fun getRegistrationOptions(): AppoxeeOptions?

    suspend fun fetchRegistrationData(): OldRegistration?

    fun isEqual(o1: AppoxeeOptions?, o2: AppoxeeOptions?): Boolean

    fun getFilesDir(): File

    fun getFileName(): String

    fun deleteOldRegistration()
}