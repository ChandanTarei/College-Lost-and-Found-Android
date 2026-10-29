package com.example.collegelostandfound
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

object UiPolish {

    private val primary = Color.rgb(48, 72, 165)
    private val teal = Color.rgb(0, 133, 119)
    private val danger = Color.rgb(198, 40, 40)
    private val titleColor = Color.rgb(32, 42, 68)

    fun apply(activity: Activity) {

        // Change the system status bar color.
        activity.window.statusBarColor = primary

        if (Build.VERSION.SDK_INT >= 26) {
            activity.window.navigationBarColor =
                Color.rgb(245, 247, 252)
        }

        val root = activity.findViewById<ViewGroup>(
            android.R.id.content
        )

        if (root.childCount == 0) return

        // Apply the soft background to the screen.
        val content = root.getChildAt(0)

        content?.setBackgroundResource(
            R.drawable.bg_lf_screen
        )

        // Style existing controls recursively.
        styleChildren(root)
    }

    private fun styleChildren(root: View) {

        when (root) {

            is Button -> {
                styleButton(root)
            }

            is EditText -> {
                styleInput(root)
            }

            is TextView -> {
                styleText(root)
            }

            is RecyclerView -> {
                root.clipToPadding = false
            }
        }

        if (root is ViewGroup) {

            for (index in 0 until root.childCount) {
                styleChildren(root.getChildAt(index))
            }
        }
    }

    private fun styleButton(button: Button) {

        val buttonText =
            button.text.toString().lowercase()

        val buttonColor = when {

            buttonText.contains("delete") ||
                    buttonText.contains("reject") ||
                    buttonText.contains("remove") -> danger

            buttonText.contains("found") ||
                    buttonText.contains("approve") ||
                    buttonText.contains("request contact") ||
                    buttonText.contains("submit") -> teal

            else -> primary
        }

        button.backgroundTintList =
            ColorStateList.valueOf(buttonColor)

        button.setTextColor(Color.WHITE)

        button.isAllCaps = false

        button.minHeight = dp(button.context, 48)

        button.elevation =
            dp(button.context, 2).toFloat()
    }

    private fun styleInput(input: EditText) {

        val left = input.paddingLeft
        val top = input.paddingTop
        val right = input.paddingRight
        val bottom = input.paddingBottom

        input.setBackgroundResource(
            R.drawable.bg_lf_input
        )

        input.setPadding(
            maxOf(left, dp(input.context, 14)),
            maxOf(top, dp(input.context, 10)),
            maxOf(right, dp(input.context, 14)),
            maxOf(bottom, dp(input.context, 10))
        )

        input.setTextColor(titleColor)
        input.setHintTextColor(
            Color.rgb(102, 112, 133)
        )
    }

    private fun styleText(textView: TextView) {

        // Only large headings are recolored.
        // Normal text and status colors are preserved.
        if (textView.textSize /
            textView.resources.displayMetrics.scaledDensity
            >= 22f
        ) {
            textView.setTextColor(titleColor)
        }
    }

    fun styleReportCard(itemView: View) {

        itemView.setBackgroundResource(
            R.drawable.bg_lf_card
        )

        itemView.elevation =
            dp(itemView.context, 3).toFloat()

        // Keep the card's existing layout dimensions
        // and click listeners unchanged.
    }

    private fun dp(
        context: android.content.Context,
        value: Int
    ): Int {

        return (
                value * context.resources
                    .displayMetrics.density
                ).toInt()
    }
}
