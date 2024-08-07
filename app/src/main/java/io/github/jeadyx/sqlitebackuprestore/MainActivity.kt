package io.github.jeadyx.sqlitebackuprestore

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var dbHelper: TestSqliteHelper? by remember {
        mutableStateOf(null)
    }
    var count by remember {
        mutableIntStateOf(-1)
    }
    LazyColumn(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        item{
            Text("本地备份与恢复 $count")
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ButtonText("重新打开") {
                    dbHelper = TestSqliteHelper(context, "testDb", "testTb", arrayOf("name", "age"))
                }
                ButtonText("查询个数") {
                    count = dbHelper?.count() ?: 0
                }
                ButtonText("清空") {
                    dbHelper?.removeAll()
                }
                ButtonText("新增数据") {
                    dbHelper?.insertDataSet(ContentValues().apply {
                        put("name", "jeadyx")
                        put("age", Random.nextInt(30))
                    })
                }
                ButtonText("备份") {
                    Log.d(
                        TAG,
                        "${
                            SqliteBackupRestore.backupDatabase(
                                context,
                                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}/testDb"
                            )
                        }"
                    )
                }
                ButtonText("恢复") {
                    Log.d(
                        TAG,
                        "Greeting: ${
                            SqliteBackupRestore.restoreFromLocal(
                                "${
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                                }/testDb", OverrideMode.Override
                            )
                        }"
                    )
                }
                ButtonText("云端上传") {
                    Log.d(
                        TAG,
                        "Greeting: ${
                            SqliteBackupRestore.uploadFile(
                                "publisher/test/testdb.db"
                            ){
                                Log.d(TAG, "uploadFile: $it")
                            }
                        }"
                    )
                }
                ButtonText("云端恢复") {
                    Log.d(
                        TAG,
                        "Greeting: ${
                            SqliteBackupRestore.downloadAndRestore(
                                "publisher/test/testdb.db", true
                            )
                        }"
                    )
                }
                LaunchedEffect(Unit) {
                    dbHelper = TestSqliteHelper(context, "testDb", "testTb", arrayOf("name", "age"))
                    SqliteBackupRestore.init(
                        context,
                        DbInfo(dbHelper!!, "testDb", "testTb", arrayOf("name", "age")),
                        RepoInfo("jeadyu", "healthcare-publisher", "af19696ba3697a0d2831598268441d79")
                    )
                }
            }
        }
    }
}