package com.example.cartescanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DTO pour l'affichage de l'historique dans la liste.
 */
data class ScanHistoryItem(
    val scanId: Long,
    val name: String?,
    val surname: String?,
    val organisationName: String?,
    val phone: String?,
    val linkedin: String?
)


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

    @Query("""
        SELECT 
            s.id AS scanId,
            p.name AS name,
            p.surname AS surname,
            o.organisation_name AS organisationName,
            c.tel AS phone,
            c.linkedin AS linkedin
        FROM tbl_scan s
        LEFT JOIN tbl_person p ON s.person_id = p.id
        LEFT JOIN tbl_organisation o ON s.organisation_id = o.id
        LEFT JOIN tbl_contact c ON p.contact_id = c.id OR o.contact_id = c.id
        GROUP BY s.id
        ORDER BY s.id DESC
    """)
    fun getAllScansFlow(): Flow<List<ScanHistoryItem>>

    @Query("SELECT * FROM tbl_image WHERE id = :id")
    fun observeImageById(id: Long): Flow<ImageEntity?>

    @Query("""
        SELECT 
            s.id AS scanId,
            p.name AS name,
            p.surname AS surname,
            o.organisation_name AS organisationName,
            c.tel AS phone,
            c.linkedin AS linkedin
        FROM tbl_scan s
        LEFT JOIN tbl_person p ON s.person_id = p.id
        LEFT JOIN tbl_organisation o ON s.organisation_id = o.id
        LEFT JOIN tbl_contact c ON p.contact_id = c.id OR o.contact_id = c.id
        INNER JOIN tbl_image i ON s.image_id = i.id
        WHERE i.process_completed = 0
        ORDER BY s.id DESC
    """)
    fun getPendingScansFlow(): Flow<List<ScanHistoryItem>>
    @Query("DELETE FROM tbl_image WHERE id = :imageId")
    suspend fun deleteImageById(imageId: Long)

    @Query("DELETE FROM tbl_contact WHERE id = :contactId")
    suspend fun deleteContactById(contactId: Long)
}