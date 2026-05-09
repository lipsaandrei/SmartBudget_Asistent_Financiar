package com.example.smartbudget_asistent_financiar.di

import android.content.Context
import androidx.room.Room
import com.example.smartbudget_asistent_financiar.data.local.SmartBudgetDatabase
import com.example.smartbudget_asistent_financiar.data.local.dao.ReceiptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartBudgetDatabase =
        Room.databaseBuilder(
            context,
            SmartBudgetDatabase::class.java,
            "smartbudget.db"
        ).build()

    @Provides
    fun provideReceiptDao(db: SmartBudgetDatabase): ReceiptDao = db.receiptDao()
}
