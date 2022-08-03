package es.icp.autoupdate

import android.app.Activity
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

object AutoUpdate {

    private lateinit var progressDialog : ProgressDialog

    fun BorrarPaquete(context: Context, paquete : LifecycleCoroutineScope){
        val intent : Intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$paquete")
        context.startActivity(intent)
    }

    fun Autoupdate(context: Context, url: String, apk: String, activity : Activity) {

        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Actualizando ...")
        progressDialog.setMessage("Descargando una versión mejorada de la app ${apk.replace(".apk", "")} ...")
        progressDialog.setCancelable(false)
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
//        progressDialog.max = 100


        activity.runOnUiThread {
            progressDialog.show()
        }


        val dManager: DownloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request: DownloadManager.Request = DownloadManager.Request(Uri.parse(url))

        request.setTitle("Actualizando ${apk.replace(".apk", "")}")
        request.setDescription("Descargando ${apk.replace(".apk", "")}")
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
//                        val sizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
//                        val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
//
//                        val size = cursor.getInt(sizeIndex)
//                        val downloaded = cursor.getInt(downloadedIndex)
//                        var progress : Double = 0.0
//                        if (size != -1) {
//                            progress = downloaded * 100.0 / size
//                            activity.runOnUiThread {
//                                progressDialog.progress = progress.toInt()
//                            }
//
//                            Log.d("DESCARGANDO....", "Desargado el $progress %")
//                        }

                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val validado = cursor.getInt(columnIndex)
                        if (DownloadManager.STATUS_SUCCESSFUL == validado) {
                            downloading = false
                            val downloadFileURL: String =
                                cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            startInstall(context, downloadFileURL)
                            activity.runOnUiThread {
                                progressDialog.hide()
                                progressDialog.dismiss()
                            }
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