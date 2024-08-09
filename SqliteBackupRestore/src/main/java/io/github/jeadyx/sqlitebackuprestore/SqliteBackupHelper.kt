package io.github.jeadyx.sqlitebackuprestore

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class SqliteBackupHelper(private var sqliteHelper: SQLiteOpenHelper,
                         private var tbName: String,
                         private var primaryKey: String="id"
){
    private val TAG = "[SqliteBackupHelper]"

    fun insertDataSet(data: ContentValues): Long{
        data.remove(primaryKey)
        val db = sqliteHelper.writableDatabase
        db.insert(tbName, null, data)

        val cursor = db.rawQuery("select last_insert_rowid() from $tbName", null)
        cursor.moveToFirst()
        val id = cursor.getInt(0)

        cursor.close()
        db.close()
        return id.toLong()
    }

    fun count(): Int{
        val cursor =sqliteHelper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $tbName", null)
        cursor.moveToFirst()
        val ret = cursor.getInt(0)
        cursor.close()
        return ret
    }

    fun merge(otherDbFile: String): String?{
        val otherDb = SQLiteDatabase.openDatabase(otherDbFile, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val cursor = otherDb.query(tbName, null, null, null, null, null, null)
            while (cursor.moveToNext()){
                val values = ContentValues().also { content->
                    cursor.columnNames.forEach {
                        val idx = cursor.getColumnIndex(it)
                        content.put(it, cursor.getString(idx))
                    }
                }
                insertDataSet(values)
            }
            cursor.close()
            return null
        }catch (e: Exception){
            return e.toString()
        }
    }
}