package io.github.jeadyx.sqlitebackuprestore.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.system.Os.remove
import android.util.Log

class TestSqliteHelper(context: Context, private val dbName: String, private val tbName:String, private val fields: Array<String>): SQLiteOpenHelper(context, dbName, null, 1) {
    private val TAG = "[TestSqliteHelper] "

    override fun onCreate(db: SQLiteDatabase?) {
        Log.d(TAG, "onCreate: database create")
        val fieldsTypeString = fields.map {
            "$it TEXT"
        }.joinToString(",")
        val createTableQuery = "CREATE TABLE IF NOT EXISTS $tbName (id INTEGER PRIMARY KEY, $fieldsTypeString)"
        db?.execSQL(createTableQuery)
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let {
            upgradeByAlterFields(it, tbName, fields)
        }
    }
    private fun upgradeByAlterFields(db: SQLiteDatabase, tableName: String, newTableFields: Array<String>){
        val cursor = db.rawQuery("SELECT * FROM $tableName LIMIT 1", null)
        val columnNames = cursor.columnNames
        cursor.close()
        val willBeAdd: MutableList<String> = mutableListOf()
        val willBeDelete: MutableList<String> = mutableListOf()
        newTableFields.forEach {
            if(!columnNames.contains(it)){
                willBeAdd.add(it)
            }
        }
        columnNames.forEach {
            if(it!="id") {
                if (!newTableFields.contains(it)) {
                    willBeDelete.add(it)
                }
            }
        }
        Log.d(TAG, "upgradeByAlterFields: will be changed: $willBeAdd. $willBeDelete")
        if(willBeAdd.isNotEmpty()){
            val addFieldSql = "ALTER TABLE $tableName " + willBeAdd.map {
                "ADD COLUMN $it TEXT"
            }.joinToString(",") + ";"
            db.execSQL(addFieldSql)
        }
        if(willBeDelete.isNotEmpty()){
            val deleteFieldSql = "ALTER TABLE $tableName " + willBeDelete.map {
                "DROP COLUMN $it"
            }.joinToString(",") + ";"
            db.execSQL(deleteFieldSql)
        }
    }
    private fun upgradeByTmpTable(db: SQLiteDatabase, tableName: String, newTableFields: Set<String>){
        val cursor = db.rawQuery("SELECT * FROM $tableName LIMIT 1", null)
        val oldColumns = cursor.columnNames
        cursor.close()
        val willBeBackup = mutableListOf<String>()
        oldColumns.forEach {
            if(newTableFields.contains(it)){
                willBeBackup.add(it)
            }
        }
        // 备份旧表
        val alterTableSQL = "ALTER TABLE $tableName RENAME TO tmp_$tableName;"
        db.execSQL(alterTableSQL)
        // 创建新表
        val newTableType = newTableFields.map {
            "$it TEXT"
        }.joinToString(",")
        val createTableSQL =
            "CREATE TABLE IF NOT EXISTS $tableName ($newTableType);"
        db.execSQL(createTableSQL)
        //恢复数据
        val restoreFields = willBeBackup.joinToString(",")
        val restoreSql = "INSERT INTO $tableName ($restoreFields) SELECT $restoreFields FROM tmp_$tableName;"
        db.execSQL(restoreSql)
        // 删除临时表
        val dropTableSQL = "DROP TABLE IF EXISTS tmp_$tableName;"
        db.execSQL(dropTableSQL)
    }
    fun insertDataSet(data: ContentValues): Long{
        val db = this.writableDatabase
        db.insert(tbName, null, data)

        val cursor = db.rawQuery("select last_insert_rowid() from $tbName", null)
        cursor.moveToFirst()
        val id = cursor.getInt(0)

        cursor.close()
        db.close()
        return id.toLong()
    }
    fun queryAll(): List<Map<String, String>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("select * from $tbName;", null)
        val ret = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()){
            val tempMap = mutableMapOf<String, String>()
            cursor.columnNames.forEach {
                val idx = cursor.getColumnIndex(it)
                tempMap[it.toString()] = cursor.getString(idx)?:""
            }
            ret.add(tempMap)
        }
        cursor.close()
        db.close()
        return ret
    }
    fun count(): Int{
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $tbName", null)
        cursor.moveToFirst()
        val ret = cursor.getInt(0)
        cursor.close()
        return ret
    }
    fun query(where: String?=null, fieldFilter: Array<String>?=null): List<Map<String, String>>{
        val cursor =
            this.readableDatabase.query(tbName, fieldFilter, where, fields, null, null, null)
        Log.d(TAG, "query: count ${cursor.count}, ${cursor.columnNames.toList()}")
        val ret = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()){
            val tempMap = mutableMapOf<String, String>()
            cursor.columnNames.forEach {
                val idx = cursor.getColumnIndex(it)
                tempMap[it.toString()] = cursor.getString(idx)?:""
            }
            ret.add(tempMap)
        }
        cursor.close()
        return ret
    }
    fun query(fieldFilter: Array<String>){
        val cursor =
            this.readableDatabase.query(tbName, fieldFilter, null, null, null, null, null)
        Log.d(TAG, "query: count ${cursor.count}, ${cursor.columnNames.toList()}")
        val ret = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()){
            val tempMap = mutableMapOf<String, String>()
            cursor.columnNames.forEach {
                val idx = cursor.getColumnIndex(it)
                tempMap[it.toString()] = cursor.getString(idx)?:""
            }
            ret.add(tempMap)
        }
        cursor.close()
    }
    fun removeAll(){
        this.writableDatabase.delete(tbName, null, null)
    }
}