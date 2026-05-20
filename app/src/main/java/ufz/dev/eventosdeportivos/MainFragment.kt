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
    private lateinit var btnRefresh: ImageButton

    // Mantener las instancias de los fragmentos para preservar su estado y evitar recargas
    private val newsFragment = NewsFragment()
    private val teamsFragment = TeamsFragment()
    private val matchesFragment = MatchesFragment()
    private val favoritesFragment = FavoritesFragment()
    private var activeFragment: Fragment = newsFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        bottomNav = view.findViewById(R.id.bottom_navigation)
        btnNotifications = view.findViewById(R.id.btn_action_notifications)
        btnLogout = view.findViewById(R.id.btn_action_logout)
        btnRefresh = view.findViewById(R.id.btn_action_refresh)

        setupUI()
        setupNavigation()

        // Cargar NewsFragment por defecto solo en la primera creación
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .add(R.id.main_content_container, newsFragment, "NewsFragment")
                .commit()
            activeFragment = newsFragment
        } else {
            // Restaurar el fragmento activo si el sistema recreó la vista
            activeFragment = childFragmentManager.findFragmentByTag("NewsFragment") ?: newsFragment
        }

        return view
    }

    private fun setupUI() {
        btnNotifications.setOnClickListener {
            Toast.makeText(context, getString(R.string.desc_notifications), Toast.LENGTH_SHORT).show()
        }

        btnRefresh.setOnClickListener {
            // Invocar el método refresh de forma dinámica según el tipo del fragmento activo
            when (val current = activeFragment) {
                is NewsFragment -> current.refresh()
                is TeamsFragment -> current.refresh()
                is MatchesFragment -> current.refresh()
                is FavoritesFragment -> current.refresh()
            }
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            // Limpiar historial de navegación e ir al Login
            parentFragmentManager.clearBackStackAndNavigateTo(LoginFragment())
        }
    }

    private fun setupNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            val targetFragment = when (item.itemId) {
                R.id.nav_news -> newsFragment
                R.id.nav_teams -> teamsFragment
                R.id.nav_matches -> matchesFragment
                R.id.nav_favorites -> favoritesFragment
                else -> newsFragment
            }
            showFragment(targetFragment)
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        if (fragment == activeFragment) return

        val transaction = childFragmentManager.beginTransaction()
        
        // Si el fragmento destino no ha sido agregado al FragmentManager, lo añadimos
        if (!fragment.isAdded) {
            transaction.add(R.id.main_content_container, fragment, fragment::class.java.simpleName)
        }
        
        // Ocultamos el fragmento activo y mostramos el nuevo
        transaction.hide(activeFragment).show(fragment).commit()
        activeFragment = fragment
    }
}
