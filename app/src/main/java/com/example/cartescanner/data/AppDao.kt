package com.example.cartescanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Interface d'acces aux donnees (DAO) pour manipuler les entites de la base de donnees locale.
 */
@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity): Long

    @Query("SELECT * FROM tbl_image WHERE id = :id")
    suspend fun getImageById(id: Long): ImageEntity?

    @Update
    suspend fun updateImage(image: ImageEntity)

    @Query("SELECT * FROM tbl_image WHERE processed = 1 AND process_completed = 0")
    suspend fun getPendingImages(): List<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Query("SELECT * FROM tbl_contact WHERE id = :id")
    suspend fun getContactById(id: Long): ContactEntity?

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganisation(organisation: OrganisationEntity): Long

    @Query("SELECT * FROM tbl_organisation WHERE id = :id")
    suspend fun getOrganisationById(id: Long): OrganisationEntity?

    @Update
    suspend fun updateOrganisation(organisation: OrganisationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Query("SELECT * FROM tbl_person WHERE id = :id")
    suspend fun getPersonById(id: Long): PersonEntity?

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Query("SELECT * FROM tbl_scan WHERE image_id = :imageId")
    suspend fun getScanByImageId(imageId: Long): ScanEntity?
}