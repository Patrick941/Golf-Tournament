package com.example.beamishinvitational.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Tournament::class, Player::class, Game::class, Score::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            Log.d("BeamishDebug", "Getting database instance")
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tournament_database"
                )
                .fallbackToDestructiveMigration() // Allows schema changes to just clear the DB
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
