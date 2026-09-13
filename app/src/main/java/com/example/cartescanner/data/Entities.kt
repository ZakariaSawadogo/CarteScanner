package com.example.cartescanner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Représente l'image scannée stockée localement et son état de traitement.
 */
@Entity(tableName = "tbl_image")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "image_path") val imagePath: String,
    val processed: Boolean = false,
    @ColumnInfo(name = "process_completed") val processCompleted: Boolean = false
)

/**
 * Contient les informations de contact extraites d'une carte.
 */
@Entity(tableName = "tbl_contact")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tel: String?,
    val linkedin: String?,
    val location: String?,
    val twitter: String?,
    val whatsapp: String?,
    val facebook: String?,
    val others: String?,
    val qrCodeText: String?
)

/**
 * Représente l'organisation ou l'entreprise figurant sur le scan.
 */
@Entity(
    tableName = "tbl_organisation",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class OrganisationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "organisation_type") val organisationType: String?,
    @ColumnInfo(name = "organisation_name") val organisationName: String?,
    @ColumnInfo(name = "contact_id") val contactId: Long?
)

/**
 * Représente l'individu identifié sur la carte de visite.
 */
@Entity(
    tableName = "tbl_person",
    foreignKeys = [
        ForeignKey(
            entity = OrganisationEntity::class,
            parentColumns = ["id"],
            childColumns = ["organisation_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val surname: String?,
    val position: String?,
    @ColumnInfo(name = "organisation_id") val organisationId: Long?,
    @ColumnInfo(name = "contact_id") val contactId: Long?
)

/**
 * Entité de liaison centrale regroupant toutes les données d'un scan spécifique.
 */
@Entity(
    tableName = "tbl_scan",
    foreignKeys = [
        ForeignKey(entity = ImageEntity::class, parentColumns = ["id"], childColumns = ["image_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["person_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = OrganisationEntity::class, parentColumns = ["id"], childColumns = ["organisation_id"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "image_id") val imageId: Long,
    @ColumnInfo(name = "person_id") val personId: Long?,
    @ColumnInfo(name = "organisation_id") val organisationId: Long?
)