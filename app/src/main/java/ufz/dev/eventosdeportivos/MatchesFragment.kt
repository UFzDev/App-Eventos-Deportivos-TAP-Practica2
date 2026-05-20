package ufz.dev.eventosdeportivos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import android.widget.Toast

class MatchesFragment : Fragment() {

    fun refresh() {
        Toast.makeText(context, "Actualizando partidos de fútbol...", Toast.LENGTH_SHORT).show()
        // Aquí se consumirá la API de partidos cuando esté lista
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_matches, container, false)
    }
}
