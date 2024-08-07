package io.github.jeadyx.sqlitebackuprestore

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import com.jeady.compose.util.software.DateTimeX
import io.github.jeadyx.gitversionmanager.GitManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object SqliteBackupRestore {
    private const val TAG = "[SqliteBackupRestore]"
    private lateinit var gitManager: GitManager
    private lateinit var downloadDir: String
    private lateinit var downloadDbPath: String
    private lateinit var originDbPath: String
    private lateinit var dbHelper: SqliteBackupHelper
    var errMsg = ""
    fun init(context: Context, dbInfo: DbInfo): SqliteBackupRestore{
        downloadDir = run{
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
            var saveDir = "$downloadDir/${context.packageName}"
            if(!File(saveDir).exists()){
                if(!File(saveDir).mkdirs()){
                    saveDir = downloadDir
                }
            }
            saveDir
        }
        dbHelper = SqliteBackupHelper(context, dbInfo.dbHelper, dbInfo.table, dbInfo.fields)
        originDbPath = context.dataDir.path + "/databases/${dbInfo.database}"
        downloadDbPath = "$downloadDir/${dbInfo.database}"
        return this
    }

    fun init(context: Context, dbInfo: DbInfo, repoInfo: RepoInfo): SqliteBackupRestore{
//        gitManager = GitManager("jeadyu", "healthcare-publisher", "af19696ba3697a0d2831598268441d79")
        gitManager = GitManager(repoInfo.repoOwner, repoInfo.repoName, repoInfo.accessToken)
        return init(context, dbInfo)
    }

    fun backupDatabase(context: Context, backupPath: String): String?{
        try {
            val dbName = File(backupPath).name
            val originalDbPath = "${context.applicationInfo.dataDir}/databases/$dbName"
            if(!File(originalDbPath).exists()){
                errMsg = "file $originalDbPath is not exists"
                return null
            }
            val dirFile = File(backupPath).parentFile
            if(dirFile?.isDirectory == false){
                if(!dirFile.mkdirs()){
                    return backupDatabase(context, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path)
                }
            }
            val inputStream = FileInputStream(originalDbPath)
            val outputStream = FileOutputStream(backupPath)
            val bufferSize = 8 * 1024
            val buffer = ByteArray(bufferSize)
            var lengthRead: Int
            while (inputStream.read(buffer).also { lengthRead = it } > 0) {
                outputStream.write(buffer, 0, lengthRead)
            }
            inputStream.close()
            outputStream.flush()
            outputStream.close()
            return backupPath
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }

    fun restoreFromLocal(restoreFilePath: String=downloadDbPath, overrideMode: OverrideMode=OverrideMode.UNSPECIFIED): Boolean{
        errMsg = ""
        if(File(restoreFilePath).isFile){
            val count = dbHelper.count()
            fun restore(){
                Log.d(TAG, "restore: from $originDbPath to $restoreFilePath")
                try {
                    File(restoreFilePath).copyTo(File(originDbPath), overwrite = true)
                    // showTip("恢复记录成功")
                }catch (e: Exception){
                    // showTip("复制失败 $e")
                }
            }
            if(count > 0){
                if(overrideMode == OverrideMode.UNSPECIFIED){
                    errMsg = "override mode not specified"
                    return false
                }else if(overrideMode == OverrideMode.Override){
                        restore()
                }else{
                    errMsg = dbHelper.merge(restoreFilePath)?:""
                    return errMsg == ""
                }
            }else{
                restore()
            }
            return true
        }else{
            errMsg = "本地记录文件不存在"
            return false
        }
    }

    /**
     * 从git仓库下载并恢复记录
     */
    fun downloadAndRestore(gitPath: String, reDownload: Boolean): Boolean{
        errMsg = ""
        if(!::gitManager.isInitialized){
            errMsg = "git not be initialized"
            return false
        }
        if(gitPath.isBlank()){
            errMsg = "git Path can not be blank"
            return false
        }
        fun downloadAndRestore(){
            gitManager.downloadFile(gitPath, downloadDbPath) { res ->
                Log.d(TAG, "TestVersionControl: restore from cloud res: $res")
                restoreFromLocal()
            }
        }
        if(File(downloadDbPath).exists() && !reDownload) {
            restoreFromLocal()
        }else{
            downloadAndRestore()
        }
        return errMsg == ""
    }
    fun uploadFile(gitPath: String, uploadCallback: (Boolean) -> Unit){
        if(!::gitManager.isInitialized){
            errMsg = "git not be initialized"
            uploadCallback(false)
            return
        }
        val backupDate = DateTimeX.formatToDateTime(System.currentTimeMillis(), "yyyy-MM-dd")
        gitManager.uploadFile(
            originDbPath, gitPath,
            "上传测试记录($backupDate)"
        ) {
            uploadCallback(true)
        }
    }
}

enum class OverrideMode{
    UNSPECIFIED,
    Override,
    Merge
}

data class RepoInfo(
    val repoOwner: String,
    val repoName: String,
    val accessToken: String
)

data class DbInfo(
    val dbHelper: SQLiteOpenHelper,
    val database: String,
    val table: String,
    val fields: Array<String>
)