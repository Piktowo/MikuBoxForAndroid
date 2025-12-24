package io.nekohasekai.sagernet.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.utils.DPIController
import io.nekohasekai.sagernet.utils.Theme

abstract class ThemedActivity : AppCompatActivity {
    constructor() : super()
    constructor(contentLayoutId: Int) : super(contentLayoutId)

    override fun attachBaseContext(newBase: Context) {
        val dpi = DataStore.dpiValue
        val wrapped = if (dpi > 0) DPIController.wrapWithDpi(newBase, dpi) else newBase
        super.attachBaseContext(wrapped)
    }

    var themeResId = 0
    var uiMode = 0
    open val isDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isDialog) {
            Theme.apply(this)
        } else {
            Theme.applyDialog(this)
            
            Theme.applyWindowBlur(window)
        }
        Theme.applyNightTheme()

        super.onCreate(savedInstanceState)
        
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                if (f is DialogFragment) {
                    Theme.applyWindowBlur(f.dialog?.window)
                }
            }
        }, true)

        uiMode = resources.configuration.uiMode

        val dpi = DataStore.dpiValue
        if (dpi > 0) {
            DPIController.applyDpi(this, dpi)
        }

        if (Build.VERSION.SDK_INT >= 35) {
             findViewById<android.view.View>(android.R.id.content)?.let { rootView ->
                ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
                    val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                    findViewById<AppBarLayout>(R.id.appbar)?.apply {
                        updatePadding(top = top)
                    }
                    insets
                }
            }
        }
    }

    override fun setTheme(resId: Int) {
        super.setTheme(resId)
        themeResId = resId
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.uiMode != uiMode) {
            uiMode = newConfig.uiMode
            ActivityCompat.recreate(this)
        }
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        super.startActivity(intent, options)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    fun snackbar(@StringRes resId: Int): Snackbar = snackbar("").setText(resId)
    fun snackbar(text: CharSequence): Snackbar = snackbarInternal(text).apply {
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).apply {
            maxLines = 10
        }
    }

    internal open fun snackbarInternal(text: CharSequence): Snackbar = throw NotImplementedError()
}
