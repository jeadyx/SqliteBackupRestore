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

/**
 * Simple to backup and restore your sqlite database from local or git server.
 */
object SqliteBackupRestore {
    private const val TAG = "[SqliteBackupRestore]"
    lateinit var gitManager: GitManager
    private lateinit var downloadDir: String
    private lateinit var downloadDbPath: String
    private lateinit var originDbPath: String
    private lateinit var dbHelper: SqliteBackupHelper

    /**
     * error message when running
     */
    var errMsg = ""

    /**
     * init with local backup sys
     * @param context: context
     * @param dbInfo: sqlite database info [DbInfo]
     */
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
        dbHelper = SqliteBackupHelper(dbInfo.dbHelper, dbInfo.table)
        originDbPath = context.dataDir.path + "/databases/${dbInfo.database}"
        downloadDbPath = "$downloadDir/${dbInfo.database}"
        return this
    }

    /**
     * init with git server backup sys
     * @param context: context
     * @param dbInfo: sqlite database info [DbInfo]
     * @param repoInfo: git server info [RepoInfo]
     */
    fun init(context: Context, dbInfo: DbInfo, repoInfo: RepoInfo?): SqliteBackupRestore{
        repoInfo?.let {
            gitManager = GitManager(repoInfo.repoOwner, repoInfo.repoName, repoInfo.accessToken)
        }
        return init(context, dbInfo)
    }

    /**
     * backup file to local
     * @param context: context
     * @param backupPath: backup file path; u should make the path is writable for your app
     * @return: true: backup success; false: backup failed, and u can find error msg at [errMsg]
     */
    fun backupFile(context: Context, backupPath: String): Boolean{
        errMsg = ""
        try {
            val dbName = File(backupPath).name
            val originalDbPath = "${context.applicationInfo.dataDir}/databases/$dbName"
            if(!File(originalDbPath).exists()){
                errMsg = "file $originalDbPath is not exists"
                return false
            }
            val dirFile = File(backupPath).parentFile
            if(dirFile?.isDirectory == false){
                if(!dirFile.mkdirs()){
                    return backupFile(context, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path)
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
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            errMsg = e.localizedMessage.toString()
            return false
        }
    }

    /**
     * restore file from local
     * @param restoreFilePath: backup file path
     * @param overrideMode: override mode if local database is not empty; default is [OverrideMode.UNSPECIFIED]
     * @return: true: restore success; false: restore failed, and u can find error msg at [errMsg]
     */
    fun restoreFile(restoreFilePath: String, overrideMode: OverrideMode=OverrideMode.UNSPECIFIED): Boolean{
        errMsg = ""
        if(File(restoreFilePath).isFile){
            val count = dbHelper.count()
            fun restore(): Boolean{
                Log.d(TAG, "restore: from $originDbPath to $restoreFilePath")
                try {
                    File(restoreFilePath).copyTo(File(originDbPath), overwrite = true)
                    return true
                }catch (e: Exception){
                    errMsg = e.localizedMessage.toString()
                    return false
                }
            }
            if(count > 0){
                if(overrideMode == OverrideMode.UNSPECIFIED){
                    errMsg = "override mode not specified"
                    return false
                }else if(overrideMode == OverrideMode.Override){
                    return restore()
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
     * restore file from git server
     * @param gitPath: git server file path; the path is based on git server and owner/repo name. e.g. if your git path is "https://github.com/jeady/testRepo/file", the param gitPath just is "file"
     * @param localPath: local file path; the path is that the file be downloaded
     * @param reDownload: whether to download file from git server if the file was exists
     * @param overrideMode: override mode if local database is not empty; default is [OverrideMode.UNSPECIFIED]
     * @param restoreCallback: callback when restore finished；return true: restore success; false: restore failed, and u can find error msg at [errMsg]
     *
     *
     */
    fun restoreFile(gitPath: String, localPath: String= downloadDbPath, reDownload: Boolean=true, overrideMode: OverrideMode=OverrideMode.UNSPECIFIED, restoreCallback: (Boolean) -> Unit){
        errMsg = ""
        if(!::gitManager.isInitialized){
            errMsg = "git not be initialized"
            restoreCallback(false)
        }
        if(gitPath.isBlank()){
            errMsg = "git Path can not be blank"
            restoreCallback(false)
        }
        fun downloadAndRestore(){
            gitManager.downloadFile(gitPath, localPath) { res ->
                Log.d(TAG, "TestVersionControl: restore from cloud res: $res")
                res.errMsg?.let{
                    errMsg = it
                    restoreCallback(false)
                }?:run{
                    restoreCallback(restoreFile(localPath, overrideMode=overrideMode))
                }
            }
        }
        if(File(localPath).exists() && !reDownload) {
            restoreCallback(restoreFile(localPath, overrideMode=overrideMode))
        }else{
            downloadAndRestore()
        }
    }

    /**
     * backup file to git server
     * @param gitPath: git server file path; the path is based on git server and owner/repo name. e.g. if your git path is "https://github.com/jeady/testRepo/file", the param gitPath just is "file"
     * @param uploadCallback: callback when upload finished；return true: upload success; false: upload failed, and u can find error msg at [errMsg]
     */
    fun backupFile(gitPath: String, uploadCallback: (Boolean) -> Unit){
        errMsg = ""
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
            Log.d(TAG, "uploadFile: it $it")
            errMsg = it?:""
            uploadCallback(it?.contains("download_url")?:false)
        }
    }
}

/**
 * override mode
 * @param UNSPECIFIED: not specified; if local database is not empty, will error
 * @param Override: override local database if local database is not empty
 * @param Merge: merge the database if local database is not empty
 */
enum class OverrideMode{
    UNSPECIFIED,
    Override,
    Merge
}

/**
 * git info
 * @param repoOwner: repo owner
 * @param repoName: repo name
 * @param accessToken: access token
 */
data class RepoInfo(
    val repoOwner: String,
    val repoName: String,
    val accessToken: String
)

/**
 * local sqlite database info
 * @param dbHelper: sqlite helper that extends [SQLiteOpenHelper]
 * @param database: database name
 * @param table: table name
 */
data class DbInfo(
    val dbHelper: SQLiteOpenHelper,
    val database: String,
    val table: String
)