package com.example.smartbudget_asistent_financiar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val category: String,
    val limitAmount: Double,
    val currency: String = "RON"
)
