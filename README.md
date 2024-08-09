Free Git Version Manager
---
A dependency for backup and restore your sqlite database.  

# Simple to use
0. add dependency  
```kotlin
implementation("io.github.jeadyx:SqliteBackupRestore:1.1")
```

## local backup  
1. init  
```kotlin
val localBackupHelper = SqliteBackupRestore.init(
    context,
    DbInfo(dbHelper, "dbName", "tbName") // dbHelper is a instance of SQLiteOpenHelper Extension
)
```

2. backup
```kotlin
val success = SqliteBackupRestore.backupFile(context, localBackupPath)
if(success){
    // backup success
}else{
    // backup failed error: [SqliteBackupRestore.errMsg]
}
```

3. restore
```kotlin
val success = SqliteBackupRestore.restoreFile(localBackupPath, OverrideMode.Merge)
if(success){
    // restore success 
}else{
    // restore failed error: [SqliteBackupRestore.errMsg]
}
```

## git server backup 默认gitee
1. init
```kotlin
val remoteBackupHelper = SqliteBackupRestore.init(
    context,
    DbInfo(dbHelper, "dbName", "tbName"), // dbHelper is a instance of SQLiteOpenHelper Extension
    RepoInfo("repoOwner", "repoName", "accessToken")
)
```

2. backup
```kotlin
SqliteBackupRestore.backupFile(gitBackupPath){success: Boolean->
    if(success) {
        // backup success
    }else{
        // backup failed, for error [SqliteBackupRestore.errMsg]
    }
}
```

3. restore
```kotlin
SqliteBackupRestore.restoreFile(gitBackupPath, overrideMode = OverrideMode.Merge){success: Boolean->
    if(success) {
        // restore success
    }else{
        // restore failed, for error [SqliteBackupRestore.errMsg]
    }
}
```

# Sample
[Module app](app)

# Donate
![donate.png](imgs/donate.png)  
