package ufz.dev.eventosdeportivos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainFragment : Fragment() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnNotifications: ImageButton
    private lateinit var btnLogout: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        bottomNav = view.findViewById(R.id.bottom_navigation)
        btnNotifications = view.findViewById(R.id.btn_action_notifications)
        btnLogout = view.findViewById(R.id.btn_action_logout)

        setupUI()
        setupNavigation()

        // Load NewsFragment by default on start
        if (savedInstanceState == null) {
            loadFragment(NewsFragment())
        }

        return view
    }

    private fun setupUI() {
        btnNotifications.setOnClickListener {
            Toast.makeText(context, getString(R.string.desc_notifications), Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            // Clear navigation history and send back to Login
            parentFragmentManager.clearBackStackAndNavigateTo(LoginFragment())
        }
    }

    private fun setupNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_news -> NewsFragment()
                R.id.nav_teams -> TeamsFragment()
                R.id.nav_matches -> MatchesFragment()
                R.id.nav_favorites -> FavoritesFragment()
                else -> NewsFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.main_content_container, fragment)
            .commit()
    }
}
