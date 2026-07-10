package com.example.escaperoomdisplay.widget

import android.content.Context
import com.example.escaperoomdisplay.R

object DisplayWidgetStyleRepository {
    private const val PREFS = "display_widget_styles"

    enum class Palette(val label: String) {
        BLACK("검정"), CHARCOAL("진회색"), NAVY("남색"), GREEN("짙은 초록"), PURPLE("짙은 보라")
    }

    data class Style(val palette: Palette = Palette.PURPLE, val opacity: Int = 100)

    fun load(context: Context, widgetId: Int): Style {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val palette = runCatching {
            Palette.valueOf(prefs.getString("palette_$widgetId", Palette.PURPLE.name).orEmpty())
        }.getOrDefault(Palette.PURPLE)
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
        return when (style.palette to style.opacity) {
            Palette.BLACK to 70 -> R.drawable.widget_bg_black_70
            Palette.BLACK to 85 -> R.drawable.widget_bg_black_85
            Palette.CHARCOAL to 70 -> R.drawable.widget_bg_charcoal_70
            Palette.CHARCOAL to 85 -> R.drawable.widget_bg_charcoal_85
            Palette.CHARCOAL to 100 -> R.drawable.widget_bg_charcoal_100
            Palette.NAVY to 70 -> R.drawable.widget_bg_navy_70
            Palette.NAVY to 85 -> R.drawable.widget_bg_navy_85
            Palette.NAVY to 100 -> R.drawable.widget_bg_navy_100
            Palette.GREEN to 70 -> R.drawable.widget_bg_green_70
            Palette.GREEN to 85 -> R.drawable.widget_bg_green_85
            Palette.GREEN to 100 -> R.drawable.widget_bg_green_100
            Palette.PURPLE to 70 -> R.drawable.widget_bg_purple_70
            Palette.PURPLE to 85 -> R.drawable.widget_bg_purple_85
            Palette.PURPLE to 100 -> R.drawable.widget_bg_purple_100
            else -> R.drawable.widget_bg_black_100
        }
    }
}
