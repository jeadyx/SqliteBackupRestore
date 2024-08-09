package io.github.jeadyx.sqlitebackuprestore

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jeadyx.sqlitebackuprestore.database.TestSqliteHelper
import io.github.jeadyx.sqlitebackuprestore.ui.ButtonText
import io.github.jeadyx.sqlitebackuprestore.ui.Dialog1
import io.github.jeadyx.sqlitebackuprestore.ui.showDialog1
import io.github.jeadyx.sqlitebackuprestore.ui.theme.SqliteBackupRestoreTheme
import java.util.concurrent.Flow
import kotlin.random.Random

private val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SqliteBackupRestoreTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                    Dialog1()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var count by remember {
        mutableIntStateOf(-1)
    }
    var dbHelper: TestSqliteHelper? by remember {
        mutableStateOf(null)
    }
    fun countLocal(){
        count = dbHelper?.count() ?: 0
    }
    fun showError()=showDialog1(SqliteBackupRestore.errMsg.ifEmpty { "未知错误" })
    LazyColumn(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        item{
            Text("Sqlite备份与恢复 数据量：$count")
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var localBackupHelper: SqliteBackupRestore? by remember {
                    mutableStateOf(null)
                }
                var dbName by remember {
                    mutableStateOf("testDb")
                }
                var tbName by remember {
                    mutableStateOf("testTb")
                }
                val fields = remember {
                    mutableStateListOf("name", "score", "uuid")
                }
                TextField(modifier = Modifier.fillMaxWidth(), value = dbName, onValueChange = {
                    dbName = it
                })
                TextField(modifier = Modifier.fillMaxWidth(), value = tbName, onValueChange = {
                    tbName = it
                })
                TextField(modifier = Modifier.fillMaxWidth(), value = fields.joinToString(","), onValueChange = {
                    fields.clear()
                    fields.addAll(it.replace(" ", "").split(","))
                })
                var repoOwner by remember {
                    mutableStateOf("jeadyu")
                }
                var repoName by remember {
                    mutableStateOf("healthcare-publisher")
                }
                var accessToken by remember {
                    mutableStateOf("af19696ba3697a0d2831598268441d79")
                }
                //        gitManager = GitManager("jeadyu", "healthcare-publisher", "af19696ba3697a0d2831598268441d79")
                ButtonText("清空", enabled = dbHelper != null) {
                    dbHelper?.removeAll()
                    countLocal()
                    showtoast(context, "已清空")
                }
                ButtonText("新增数据", enabled = dbHelper != null) {
                    dbHelper?.insertDataSet(ContentValues().apply {
                        put("name", "jeadyx")
                        put("score", Random.nextInt(150))
                    })
                    countLocal()
                    showtoast(context, "已新增数据")
                }
                ButtonText("备份", enabled = dbHelper!=null && dbName.isNotBlank() && tbName.isNotBlank()) {
                    localBackupHelper = SqliteBackupRestore.init(
                        context,
                        DbInfo(dbHelper!!, dbName, tbName)
                    )
                    val localBackupPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}/$dbName"
                    val res = SqliteBackupRestore.backupFile(context, localBackupPath)
                    if(res){
                        showDialog1("已备份至$localBackupPath")
                    }else{
                        showtoast(context, "备份失败")
                        showError()
                    }
                    countLocal()
                }
                ButtonText("恢复", enabled = dbHelper!=null && dbName.isNotBlank() && tbName.isNotBlank()) {
                    localBackupHelper = SqliteBackupRestore.init(
                        context,
                        DbInfo(dbHelper!!, dbName, tbName)
                    )
                    val res = SqliteBackupRestore.restoreFile(
                        "${
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                        }/$dbName", OverrideMode.Merge
                    )
                    if(res){
                        showtoast(context, "已恢复")
                    }else{
                        showtoast(context, "恢复失败")
                        showError()
                    }
                    dbHelper = TestSqliteHelper(context, dbName , tbName, fields.toTypedArray())
                    countLocal()
                }
                TextField(modifier = Modifier.fillMaxWidth(), value = repoOwner, onValueChange = {
                    repoOwner = it
                })
                TextField(modifier = Modifier.fillMaxWidth(), value = repoName, onValueChange = {
                    repoName = it
                })
                TextField(modifier = Modifier.fillMaxWidth(), value = accessToken, onValueChange = {
                    accessToken = it
                })
                var gitBackupPath by remember {
                    mutableStateOf("test/bk${Random.nextInt(1000, 9999)}-$dbName")
                }
                TextField(modifier = Modifier.fillMaxWidth(), value = gitBackupPath, onValueChange = {
                    gitBackupPath = it
                })
                var remoteBackupHelper by remember {
                    mutableStateOf<SqliteBackupRestore?>(null)
                }
                ButtonText("随机备份路径") {
                    gitBackupPath = "test/bk${Random.nextInt(1000, 9999)}-$dbName"
                }
                ButtonText("云端上传", enabled = repoOwner.isNotEmpty() && repoName.isNotEmpty() && accessToken.isNotEmpty()) {
                    val repoInfo = RepoInfo(repoOwner, repoName, accessToken)
                    remoteBackupHelper = SqliteBackupRestore.init(
                        context,
                        DbInfo(dbHelper!!, dbName, tbName),
                        repoInfo
                    )
                    gitBackupPath = "test/$dbName-${Random.nextInt(1000, 9999)}"
                    SqliteBackupRestore.backupFile(gitBackupPath){
                        if(it) {
                            showDialog1("已上传至$repoOwner/$repoName/$gitBackupPath")
                        }else{
                            showtoast(context, "上传失败")
                            showError()
                        }
                    }
                }
                ButtonText("云端恢复", enabled = repoOwner.isNotEmpty() && repoName.isNotEmpty() && accessToken.isNotEmpty()) {
                    val repoInfo = RepoInfo(repoOwner, repoName, accessToken)
                    remoteBackupHelper = SqliteBackupRestore.init(
                        context,
                        DbInfo(dbHelper!!, dbName, tbName),
                        repoInfo
                    )
                    if(gitBackupPath.isEmpty()){
                        return@ButtonText showDialog1("还未上传")
                    }
                    SqliteBackupRestore.restoreFile(gitBackupPath, overrideMode = OverrideMode.Merge){
                        if(it) {
                            showtoast(context, "已恢复")
                        }else{
                            showtoast(context, "恢复失败")
                            showError()
                        }
                        dbHelper = TestSqliteHelper(context, dbName , tbName, fields.toTypedArray())
                        countLocal()
                    }
                }
                LaunchedEffect(Unit) {
                    dbHelper = TestSqliteHelper(context, dbName , tbName, fields.toTypedArray())
                    countLocal()
                }
            }
        }
    }
}

fun showtoast(context: Context, s: String) {
    Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
}
