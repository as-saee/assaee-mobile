package com.readflow.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.app.PendingIntent
import com.readflow.app.MainActivity
import android.widget.RemoteViews
import org.json.JSONArray
import android.graphics.BitmapFactory
import java.io.File

class ReadingWidgetProvider : AppWidgetProvider() {
    companion object { private const val TAG = "ReadflowWidget"; const val ACTION_PREVIOUS = "com.readflow.widget.PREVIOUS"; const val ACTION_NEXT = "com.readflow.widget.NEXT" }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        android.util.Log.d(TAG, "[WIDGET] Update requested ids=${ids.joinToString()}")
        val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        var title = prefs.getString("flutter.widget.title", null)
        var page = prefs.getInt("flutter.widget.page", 1)
        val imagePath = prefs.getString("flutter.widget.imagePath", null)
        val mode = prefs.getString("flutter.widget.mode", "text") ?: "text"
        val chunks = try { JSONArray(prefs.getString("flutter.widget.chunks", "[]")) } catch (_: Exception) { JSONArray() }
        val chunkIndex = prefs.getInt("flutter.widget.chunkIndex", 0).coerceIn(0, (chunks.length()-1).coerceAtLeast(0))
        val text = if (chunks.length() > 0) chunks.optString(chunkIndex) else prefs.getString("flutter.widget.text", "Open Readflow to select a page") ?: "Open Readflow to select a page"
        val books = prefs.getString("flutter.books", null)
        if (title.isNullOrBlank() && !books.isNullOrBlank()) try {
            val first = JSONArray(books).getJSONObject(0)
            title = first.optString("title", title)
            page = first.optInt("page", page)
        } catch (_: Exception) { }
        if (title.isNullOrBlank()) title = "No book selected"
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.reading_widget)
            views.setTextViewText(R.id.widget_title, "📖 ${title.take(42)}")
            views.setTextViewText(R.id.widget_preview, if (mode == "original") "Original page" else text.take(1200))
            views.setTextViewText(R.id.widget_page, if (chunks.length() > 0) "${chunkIndex + 1} / ${chunks.length()}" else "Page $page")
            val candidates = listOfNotNull(imagePath?.let { File(it) }, File(context.filesDir, "widget_page_$page.png"), File(File(context.filesDir, "app_flutter"), "widget_page_$page.png"))
            val image = candidates.firstOrNull { it.exists() }
            android.util.Log.d(TAG, "[WIDGET] Image path=${image?.path} exists=${image != null}")
            if (image != null) { val bitmap=BitmapFactory.decodeFile(image.path); android.util.Log.d(TAG, "[WIDGET] bitmap=${bitmap?.width}x${bitmap?.height} bytes=${image.length()}"); if(bitmap!=null) views.setImageViewBitmap(R.id.widget_page_image, bitmap) }
            views.setViewVisibility(R.id.widget_page_image, if (mode == "original") android.view.View.VISIBLE else android.view.View.GONE)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("widget_book_id", prefs.getString("flutter.widget.bookId", ""))
                putExtra("widget_page", page)
            }
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            views.setOnClickPendingIntent(R.id.widget_previous, action(context, ACTION_PREVIOUS, id))
            views.setOnClickPendingIntent(R.id.widget_next, action(context, ACTION_NEXT, id))
            android.util.Log.d(TAG, "[WIDGET] RemoteViews created; calling updateAppWidget")
            manager.updateAppWidget(id, views)
        }
    }
    private fun action(context: Context, action: String, id: Int): PendingIntent = PendingIntent.getBroadcast(context, id + action.hashCode(), Intent(context, ReadingWidgetProvider::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    override fun onReceive(context: Context, intent: Intent) { if (intent.action == ACTION_PREVIOUS || intent.action == ACTION_NEXT) { android.util.Log.d(TAG, "[WIDGET] ${intent.action} clicked"); val p=context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE); val chunks=try{JSONArray(p.getString("flutter.widget.chunks","[]"))}catch(_:Exception){JSONArray()}; val current=p.getInt("flutter.widget.chunkIndex",0); val next=if(intent.action==ACTION_NEXT)(current+1).coerceAtMost((chunks.length()-1).coerceAtLeast(0)) else (current-1).coerceAtLeast(0); p.edit().putInt("flutter.widget.chunkIndex",next).putString("flutter.widget.text",if(chunks.length()>0)chunks.optString(next) else "").apply(); onUpdate(context, AppWidgetManager.getInstance(context), AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, ReadingWidgetProvider::class.java))) } else super.onReceive(context,intent) }
}
