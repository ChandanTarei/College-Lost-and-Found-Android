
package com.example.collegelostandfound

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

object UiFeedback {

    private const val LOADING_TAG = "lf_loading_overlay"
    private const val EMPTY_TAG = "lf_empty_message"

    fun showLoading(
        activity: Activity,
        message: String = "Loading items..."
    ) {
        val root = activity.findViewById<FrameLayout>(
            android.R.id.content
        )

        if (root.findViewWithTag<View>(LOADING_TAG) != null) {
            return
        }

        val overlay = FrameLayout(activity).apply {
            tag = LOADING_TAG
            setBackgroundColor(
                Color.argb(180, 245, 247, 252)
            )
            isClickable = true
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val progressBar = ProgressBar(activity)

        val textView = TextView(activity).apply {
            text = message
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(48, 72, 165))
            setPadding(0, 20, 0, 0)
        }

        container.addView(progressBar)
        container.addView(textView)

        overlay.addView(
            container,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    fun hideLoading(activity: Activity) {
        val root = activity.findViewById<FrameLayout>(
            android.R.id.content
        )

        val overlay = root.findViewWithTag<View>(
            LOADING_TAG
        )

        if (overlay != null) {
            root.removeView(overlay)
        }
    }

    fun showEmpty(
        activity: Activity,
        show: Boolean,
        message: String
    ) {
        val root = activity.findViewById<FrameLayout>(
            android.R.id.content
        )

        var emptyView = root.findViewWithTag<TextView>(
            EMPTY_TAG
        )

        if (emptyView == null) {
            emptyView = TextView(activity).apply {
                tag = EMPTY_TAG
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(102, 112, 133))
                setBackgroundResource(R.drawable.bg_lf_card)
                setPadding(28, 24, 28, 24)
                visibility = View.GONE
            }

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )

            val margin = (
                    24 * activity.resources.displayMetrics.density
                    ).toInt()

            params.leftMargin = margin
            params.rightMargin = margin

            root.addView(emptyView, params)
        }

        emptyView.text = message
        emptyView.visibility =
            if (show) View.VISIBLE else View.GONE
    }
}
