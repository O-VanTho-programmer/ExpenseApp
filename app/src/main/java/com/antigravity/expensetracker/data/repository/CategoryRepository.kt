package com.antigravity.expensetracker.data.repository

import com.antigravity.expensetracker.data.local.dao.CategoryDao
import com.antigravity.expensetracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    suspend fun addCategory(category: CategoryEntity) {
        categoryDao.insert(category)
    }

    suspend fun getCategoryByName(name: String): CategoryEntity? = categoryDao.getCategoryByName(name)
}
