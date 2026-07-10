package com.example.escaperoomtimer.widget

import android.content.Context
import com.example.escaperoomtimer.R

object ManagerWidgetStyleRepository {
    private const val PREFS = "manager_widget_styles"

    enum class Palette(val label: String) {
        BLACK("검정"), CHARCOAL("진회색"), NAVY("남색"), GREEN("짙은 초록"), PURPLE("짙은 보라")
    }

    data class Style(val palette: Palette = Palette.BLACK, val opacity: Int = 100)

    fun load(context: Context, widgetId: Int): Style {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val palette = runCatching {
            Palette.valueOf(prefs.getString("palette_$widgetId", Palette.BLACK.name).orEmpty())
        }.getOrDefault(Palette.BLACK)
        return Style(palette, prefs.getInt("opacity_$widgetId", 100))
    }

    fun save(context: Context, widgetId: Int, style: Style) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("palette_$widgetId", style.palette.name)
            .putInt("opacity_$widgetId", style.opacity)
            .apply()
    }

    fun delete(context: Context, widgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove("palette_$widgetId")
            .remove("opacity_$widgetId")
            .apply()
    }

    fun backgroundRes(style: Style): Int {
        val suffix = when (style.opacity) { 70 -> "70"; 85 -> "85"; else -> "100" }
        val name = "widget_bg_${style.palette.name.lowercase()}_$suffix"
        return when (name) {
            "widget_bg_black_70" -> R.drawable.widget_bg_black_70
            "widget_bg_black_85" -> R.drawable.widget_bg_black_85
            "widget_bg_charcoal_70" -> R.drawable.widget_bg_charcoal_70
            "widget_bg_charcoal_85" -> R.drawable.widget_bg_charcoal_85
            "widget_bg_charcoal_100" -> R.drawable.widget_bg_charcoal_100
            "widget_bg_navy_70" -> R.drawable.widget_bg_navy_70
            "widget_bg_navy_85" -> R.drawable.widget_bg_navy_85
            "widget_bg_navy_100" -> R.drawable.widget_bg_navy_100
            "widget_bg_green_70" -> R.drawable.widget_bg_green_70
            "widget_bg_green_85" -> R.drawable.widget_bg_green_85
            "widget_bg_green_100" -> R.drawable.widget_bg_green_100
            "widget_bg_purple_70" -> R.drawable.widget_bg_purple_70
            "widget_bg_purple_85" -> R.drawable.widget_bg_purple_85
            "widget_bg_purple_100" -> R.drawable.widget_bg_purple_100
            else -> R.drawable.widget_bg_black_100
        }
    }
}
