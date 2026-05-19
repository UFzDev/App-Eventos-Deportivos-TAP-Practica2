package ufz.dev.eventosdeportivos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

/**
 * Fragmento de Bienvenida (Home) que muestra el estado de sesión activa del usuario
 * y valida la distinción de cuentas.
 */
class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        val tvUserEmail = view.findViewById<TextView>(R.id.tv_user_email)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btn_logout)

        // Obtener el usuario activo actual
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Protección de ruta: Si no hay sesión activa, redirige inmediatamente a Login
            parentFragmentManager.clearBackStackAndNavigateTo(LoginFragment())
            return
        }

        // Determinar correo o si ingresó como invitado (anónimo)
        val userEmail = currentUser.email ?: getString(R.string.btn_guest)
        
        // Asignar texto dinámico formateado usando strings.xml
        tvUserEmail?.text = getString(R.string.home_user_prefix, userEmail)

        // Listener para el cierre de sesión
        btnLogout?.setOnClickListener {
            // Cerrar sesión en el backend de Firebase
            auth.signOut()
            
            // Navegación limpia de retorno a Login limpiando toda la pila
            parentFragmentManager.clearBackStackAndNavigateTo(LoginFragment())
        }
    }
}
