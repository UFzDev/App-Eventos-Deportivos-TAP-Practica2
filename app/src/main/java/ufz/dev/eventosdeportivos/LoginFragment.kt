package ufz.dev.eventosdeportivos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

/**
 * Fragmento encargado de gestionar el Inicio de Sesión de los usuarios.
 * Integra validaciones locales y la API de Firebase Authentication.
 */
class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Si ya existe una sesión activa, redirige dinámicamente a Home para mejorar la UX
        if (auth.currentUser != null) {
            parentFragmentManager.clearBackStackAndNavigateTo(MainFragment())
            return
        }

        // Vincular componentes de la vista
        val editEmail = view.findViewById<TextInputEditText>(R.id.edit_email)
        val editPassword = view.findViewById<TextInputEditText>(R.id.edit_password)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btn_login)
        val btnGuest = view.findViewById<MaterialButton>(R.id.btn_guest)
        val tvLinkToRegister = view.findViewById<TextView>(R.id.tv_link_to_register)

        // Botón de Inicio de Sesión
        btnLogin?.setOnClickListener {
            val email = editEmail?.text?.toString()?.trim() ?: ""
            val password = editPassword?.text?.toString()?.trim() ?: ""

            // Validación de campos vacíos en cliente
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, getString(R.string.error_empty_fields), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Deshabilitar botones para evitar clicks múltiples concurrentes (loading state)
            setLoadingState(true, btnLogin, btnGuest)

            // Consumo de servicio de Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Navegación limpia al panel principal en caso de éxito
                        parentFragmentManager.clearBackStackAndNavigateTo(MainFragment())
                    } else {
                        // Reestablecer controles y notificar al usuario mediante Toasts dinámicos
                        setLoadingState(false, btnLogin, btnGuest)
                        Toast.makeText(
                            context,
                            task.exception?.localizedMessage ?: getString(R.string.error_auth_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Botón de Continuar como Invitado (Autenticación Anónima de Firebase)
        btnGuest?.setOnClickListener {
            setLoadingState(true, btnLogin, btnGuest)

            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        parentFragmentManager.clearBackStackAndNavigateTo(MainFragment())
                    } else {
                        setLoadingState(false, btnLogin, btnGuest)
                        Toast.makeText(
                            context,
                            task.exception?.localizedMessage ?: getString(R.string.error_auth_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Navegación hacia la pantalla de registro
        tvLinkToRegister?.setOnClickListener {
            parentFragmentManager.navigateTo(RegisterFragment())
        }
    }

    /**
     * Alterna la interactividad del formulario para evitar dobles peticiones de red.
     */
    private fun setLoadingState(isLoading: Boolean, loginBtn: MaterialButton?, guestBtn: MaterialButton?) {
        loginBtn?.isEnabled = !isLoading
        guestBtn?.isEnabled = !isLoading
    }
}
