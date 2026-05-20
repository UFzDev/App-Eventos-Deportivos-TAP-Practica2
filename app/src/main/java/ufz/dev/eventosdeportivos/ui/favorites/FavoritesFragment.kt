package ufz.dev.eventosdeportivos.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import ufz.dev.eventosdeportivos.R

class FavoritesFragment : Fragment() {

    fun refresh() {
        Toast.makeText(context, "Sincronizando favoritos...", Toast.LENGTH_SHORT).show()
        // Aquí se recargarán los favoritos desde Firestore o DB local cuando esté listo
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }
}
