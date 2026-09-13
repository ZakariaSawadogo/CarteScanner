package com.example.cartescanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Classe de configuration principale de la base de données Room.
 * Implémente le pattern Singleton pour éviter les fuites de mémoire.
 */
@Database(
    entities = [
        ImageEntity::class,
        ContactEntity::class,
        OrganisationEntity::class,
        PersonEntity::class,
        ScanEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Récupère l'instance unique de la base de données ou la crée si elle n'existe pas.
         *
         * @param context Le contexte de l'application.
         * @return L'instance unique de AppDatabase.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scanner_carte_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}