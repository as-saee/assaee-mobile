package com.readflow.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private var pending: Map<String, Any>? = null
    override fun onCreate(state: android.os.Bundle?) { super.onCreate(state); capture(intent) }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); setIntent(intent); capture(intent) }
    private fun capture(intent: android.content.Intent?) { if(intent?.hasExtra("widget_book_id") == true) pending=mapOf("bookId" to (intent.getStringExtra("widget_book_id") ?: ""), "page" to intent.getIntExtra("widget_page",1)) }
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.readflow/widget").setMethodCallHandler { call, result ->
            if (call.method == "pending") { result.success(pending); pending=null; return@setMethodCallHandler }
            if (call.method == "refresh") {
                android.util.Log.d("ReadflowWidget", "[BRIDGE] refresh args=${call.arguments}")
                (call.arguments as? Map<*, *>)?.let { args ->
                    getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE).edit()
                        .putString("flutter.widget.bookId", args["bookId"] as? String)
                        .putString("flutter.widget.title", args["title"] as? String)
                        .putInt("flutter.widget.page", (args["page"] as? Number)?.toInt() ?: 1)
                        .putInt("flutter.widget.total", (args["total"] as? Number)?.toInt() ?: Int.MAX_VALUE)
                        .putString("flutter.widget.imagePath", args["imagePath"] as? String)
                        .putString("flutter.widget.mode", args["mode"] as? String ?: "text")
                        .putString("flutter.widget.text", args["text"] as? String ?: "")
                        .putString("flutter.widget.chunks", org.json.JSONArray((args["chunks"] as? List<*>)?.map { it.toString() } ?: emptyList<String>()).toString())
                        .putInt("flutter.widget.chunkIndex", 0)
                        .apply()
                }
                val manager = AppWidgetManager.getInstance(this)
                val component = ComponentName(this, ReadingWidgetProvider::class.java)
                ReadingWidgetProvider().onUpdate(this, manager, manager.getAppWidgetIds(component))
                result.success(null)
            } else result.notImplemented()
        }
    }
}
