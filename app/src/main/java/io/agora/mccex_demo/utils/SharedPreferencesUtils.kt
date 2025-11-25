package io.agora.mccex_demo.utils

import android.content.Context
import android.content.SharedPreferences
import io.agora.mccex_demo.constants.Constants

object SharedPreferencesUtils {
    private const val SHARED_PREFERENCES_NAME = Constants.TAG + "-sp"
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun writeString(context: Context, key: String?, value: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun readString(context: Context, key: String?, defaultValue: String?): String? {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(key, defaultValue)
    }

    fun writeInt(context: Context, key: String?, value: Int) {
        val editor = getSharedPreferences(context).edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun readInt(context: Context, key: String?, defaultValue: Int): Int {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun writeFloat(context: Context, key: String?, value: Float) {
        val editor = getSharedPreferences(context).edit()
        editor.putFloat(key, value)
        editor.apply()
    }

    fun readFloat(context: Context, key: String?, defaultValue: Float): Float {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun writeLong(context: Context, key: String?, value: Long) {
        val editor = getSharedPreferences(context).edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun readLong(context: Context, key: String?, defaultValue: Long): Long {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getLong(key, defaultValue)
    }

    fun writeBoolean(context: Context, key: String?, value: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun readBoolean(context: Context, key: String?, defaultValue: Boolean): Boolean {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun remove(context: Context, key: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.remove(key)
        editor.apply()
    }

    fun clear(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.clear()
        editor.apply()
    }
}
