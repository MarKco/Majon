package com.ilsecondodasinistra.majon.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.db.NoteDao
import com.ilsecondodasinistra.majon.data.db.PartDao
import com.ilsecondodasinistra.majon.data.db.ProjectDao
import com.ilsecondodasinistra.majon.data.repository.MajonRepositoryImpl
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MajonDatabase =
        Room.databaseBuilder(context, MajonDatabase::class.java, "majon.db").build()

    @Provides
    fun provideProjectDao(db: MajonDatabase): ProjectDao = db.projectDao()

    @Provides
    fun providePartDao(db: MajonDatabase): PartDao = db.partDao()

    @Provides
    fun provideNoteDao(db: MajonDatabase): NoteDao = db.noteDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    fun provideClock(): () -> Long = { System.currentTimeMillis() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMajonRepository(impl: MajonRepositoryImpl): MajonRepository
}
