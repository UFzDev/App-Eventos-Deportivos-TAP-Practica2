package ufz.dev.eventosdeportivos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el diseño para este fragmento
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Capturar el enlace para ir a iniciar sesión
        val tvLinkToLogin = view.findViewById<TextView>(R.id.tv_link_to_login)

        tvLinkToLogin?.setOnClickListener {
            // Regresar de manera segura al LoginFragment usando la extension de utilidad
            parentFragmentManager.navigateBackOr(LoginFragment())
        }
    }
}
