package ufz.dev.eventosdeportivos.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import ufz.dev.eventosdeportivos.R
import ufz.dev.eventosdeportivos.ui.main.MainFragment
import ufz.dev.eventosdeportivos.utils.clearBackStackAndNavigateTo
import ufz.dev.eventosdeportivos.utils.navigateBackOr

/**
 * Fragmento encargado de gestionar el Registro de nuevos usuarios en la plataforma.
 * Contiene validaciones asíncronas de contraseñas, registro de perfil y persistencia
 * en Cloud Firestore.
 */
class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Vincular componentes de la vista
        val editName = view.findViewById<TextInputEditText>(R.id.edit_name)
        val editEmail = view.findViewById<TextInputEditText>(R.id.edit_email)
        val editPassword = view.findViewById<TextInputEditText>(R.id.edit_password)
        val editConfirmPassword = view.findViewById<TextInputEditText>(R.id.edit_confirm_password)
        val btnRegister = view.findViewById<MaterialButton>(R.id.btn_register)
        val tvLinkToLogin = view.findViewById<TextView>(R.id.tv_link_to_login)

        // Botón de Registro de cuenta
        btnRegister?.setOnClickListener {
            val name = editName?.text?.toString()?.trim() ?: ""
            val email = editEmail?.text?.toString()?.trim() ?: ""
            val password = editPassword?.text?.toString()?.trim() ?: ""
            val confirmPassword = editConfirmPassword?.text?.toString()?.trim() ?: ""

            // Validar que no haya campos vacíos
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(context, getString(R.string.error_empty_fields), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Validar que las contraseñas coincidan
            if (password != confirmPassword) {
                Toast.makeText(context, getString(R.string.error_password_mismatch), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Validar longitud mínima de la contraseña (exigencia del backend de Firebase Auth >= 6)
            if (password.length < 6) {
                Toast.makeText(context, getString(R.string.error_password_short), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Activar estado de carga para el botón
            btnRegister.isEnabled = false

            // Crear el usuario en Firebase Auth (Módulo de autenticación)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            // Instanciar Cloud Firestore y preparar el documento de usuario (Excluyendo Contraseña)
                            val db = FirebaseFirestore.getInstance()
                            val userData = hashMapOf(
                                "uid" to user.uid,
                                "name" to name,
                                "email" to email,
                                "createdAt" to Timestamp.now()
                            )

                            // Crear el documento del usuario en la colección "users" usando su UID único como ID
                            db.collection("users").document(user.uid)
                                .set(userData)
                                .addOnCompleteListener { firestoreTask ->
                                    if (!firestoreTask.isSuccessful) {
                                        // Notificación preventiva no-bloqueante por si fallan las Reglas de Seguridad en la consola
                                        Toast.makeText(context, "Advertencia: Perfil creado en Auth, pero falló el registro en base de datos.", Toast.LENGTH_LONG).show()
                                    }
                                    
                                    // Actualizar el perfil del usuario con su Nombre Completo en Firebase Auth
                                    val profileUpdates = userProfileChangeRequest {
                                        displayName = name
                                    }

                                    user.updateProfile(profileUpdates)
                                        .addOnCompleteListener { profileTask ->
                                            Toast.makeText(context, getString(R.string.success_register), Toast.LENGTH_SHORT).show()
                                            parentFragmentManager.clearBackStackAndNavigateTo(MainFragment())
                                        }
                                }
                        }
                    } else {
                        // Habilitar botón y mostrar el error devuelto por Firebase Auth
                        btnRegister.isEnabled = true
                        Toast.makeText(
                            context,
                            task.exception?.localizedMessage ?: getString(R.string.error_register_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Navegación hacia el inicio de sesión
        tvLinkToLogin?.setOnClickListener {
            parentFragmentManager.navigateBackOr(LoginFragment())
        }
    }
}
