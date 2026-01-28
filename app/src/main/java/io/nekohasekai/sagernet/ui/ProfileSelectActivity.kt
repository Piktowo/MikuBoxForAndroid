package io.nekohasekai.sagernet.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.ProxyEntity

class ProfileSelectActivity : ThemedActivity(R.layout.layout_empty),
    ConfigurationFragment.SelectCallback {

    companion object {
        const val EXTRA_SELECTED = "selected"
        const val EXTRA_PROFILE_ID = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selected: ProxyEntity? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SELECTED, ProxyEntity::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_SELECTED) as? ProxyEntity
        }

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_holder,
                ConfigurationFragment(true, selected, R.string.select_profile)
            )
            .commitAllowingStateLoss()
    }

    override fun returnProfile(profileId: Long) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_PROFILE_ID, profileId)
        })
        finish()
    }
}
