package com.wiseyoung.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 북마크 데이터를 SharedPreferences에 저장하고 불러오는 유틸리티
 */
object BookmarkPreferences {
    const val PREFS_NAME = "bookmark_prefs"
    private const val KEY_BOOKMARKS = "bookmarks"
    private val gson = Gson()
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 북마크 목록 저장
     */
    fun saveBookmarks(context: Context, bookmarks: List<BookmarkItem>) {
        val prefs = getSharedPreferences(context)
        val json = gson.toJson(bookmarks)
        prefs.edit().putString(KEY_BOOKMARKS, json).apply()
    }
    
    /**
     * 북마크 목록 불러오기
     */
    fun getBookmarks(context: Context): List<BookmarkItem> {
        val prefs = getSharedPreferences(context)
        val json = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BookmarkItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 북마크 추가
     */
    fun addBookmark(context: Context, bookmark: BookmarkItem) {
        val bookmarks = getBookmarks(context).toMutableList()
        // 중복 체크 (제목과 타입으로)
        if (!bookmarks.any { it.title == bookmark.title && it.type == bookmark.type }) {
            bookmarks.add(bookmark)
            saveBookmarks(context, bookmarks)
        }
    }
    
    /**
     * 북마크 제거
     */
    fun removeBookmark(context: Context, title: String, type: BookmarkType) {
        val bookmarks = getBookmarks(context).toMutableList()
        bookmarks.removeAll { it.title == title && it.type == type }
        saveBookmarks(context, bookmarks)
    }
    
    /**
     * 북마크 확인
     */
    fun isBookmarked(context: Context, title: String, type: BookmarkType): Boolean {
        val bookmarks = getBookmarks(context)
        return bookmarks.any { it.title == title && it.type == type }
    }
    
    /**
     * 모든 북마크 삭제
     */
    fun clearBookmarks(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit().remove(KEY_BOOKMARKS).apply()
    }
}

