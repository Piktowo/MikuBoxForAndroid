package io.nekohasekai.sagernet.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.widget.ListListener
import io.nekohasekai.sagernet.widget.StatsBar

class ThemeSettingsFragment : ToolbarFragment(R.layout.uwu_collapse_layout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view, ListListener)

        val collapsingToolbar =
            view.findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        collapsingToolbar.title = getString(R.string.uwu_miku_ui)

        val fragment = ThemeSettingsPreferenceFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .commitAllowingStateLoss()

        view.post {
            val prefFragment = childFragmentManager.findFragmentById(R.id.settings)
                    as? ThemeSettingsPreferenceFragment ?: fragment

            val prefRecycler = prefFragment.listView
            val bottomAppBar = requireActivity().findViewById<StatsBar>(R.id.stats)
                ?: return@post

            fun updateBottomBarVisibility() {
                val isConnected = DataStore.serviceState == BaseService.State.Connected
                val showController = DataStore.showBottomBar

                if (!isConnected) {
                    bottomAppBar.performHide()
                } else {
                    if (showController) bottomAppBar.performShow()
                    else bottomAppBar.performHide()
                }
            }

            updateBottomBarVisibility()

            if (prefRecycler != null) {
                ViewCompat.setNestedScrollingEnabled(prefRecycler, true)

                prefRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        val isConnected = DataStore.serviceState == BaseService.State.Connected
                        val showController = DataStore.showBottomBar

                        if (isConnected && showController) {
                            if (dy > 6) bottomAppBar.performHide()
                            else if (dy < -6) bottomAppBar.performShow()
                        } else {
                            bottomAppBar.performHide()
                        }
                    }
                })
            }
        }
    }
}