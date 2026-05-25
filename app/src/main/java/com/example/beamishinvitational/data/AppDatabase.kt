package com.example.beamishinvitational.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Tournament::class, Player::class, Game::class, Score::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tournament_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
