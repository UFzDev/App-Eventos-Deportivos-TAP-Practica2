package ufz.dev.eventosdeportivos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Capturar el enlace para ir a registrarse
        val tvLinkToRegister = view.findViewById<TextView>(R.id.tv_link_to_register)

        tvLinkToRegister?.setOnClickListener {
            // Reemplazar este fragmento por el RegisterFragment usando la extension de utilidad
            parentFragmentManager.navigateTo(RegisterFragment())
        }
    }
}
