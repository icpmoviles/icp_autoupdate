package es.icp.autoupdate

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

object AutoUpdate {

    fun BorrarPaquete(context: Context, paquete : LifecycleCoroutineScope){
        val intent : Intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$paquete")
        context.startActivity(intent)
    }

    fun Autoupdate(context: Context, url: String, apk: String) {
        val dManager: DownloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request: DownloadManager.Request = DownloadManager.Request(Uri.parse(url))

        request.setTitle("ACtualizando Orange RFID")
        request.setDescription("Descargando Orange RFID")
        request.setDestinationInExternalFilesDir(
            context,
            Environment.getExternalStorageDirectory().path + File.separator + "MyExternalStorageAppPath",
            apk
        )
//        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apk)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadApkId = dManager.enqueue(request)

        val query: DownloadManager.Query = DownloadManager.Query()
        query.setFilterById(downloadApkId)

        GlobalScope.launch {
            var lastMsg : String = ""
            var downloading = true

            while(downloading) {
                val cursor: Cursor = dManager.query(query)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val validado = cursor.getInt(columnIndex)
                        if (DownloadManager.STATUS_SUCCESSFUL == validado) {
                            downloading = false
                            val downloadFileURL: String =
                                cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            startInstall(context, downloadFileURL)

                        }
                    }
                    cursor.close()
                }
            }
        }

    }

    fun startInstall(context: Context, fichero: String): Boolean {
        val apk: File = File(fichero.replace("file:///", ""))
        if (!apk.exists()) {
            return false
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            apk
        )

        val intent: Intent = Intent()
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.action = Intent.ACTION_VIEW

        context.startActivity(intent)
        return true
    }
}