package ufz.dev.eventosdeportivos.utils

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import ufz.dev.eventosdeportivos.R

/**
 * Extensiones de utilidad para simplificar la navegación entre Fragmentos en Android.
 */

/**
 * @param fragment Instancia del Fragment de destino al que se desea navegar.
 * @param containerId ID del contenedor XML donde se hospedará el fragmento. Por defecto usa [R.id.fragment_container].
 * @param addToBackStack Determina si la transacción debe añadirse a la pila de retroceso. Por defecto es true.
 * @param backStackName Nombre identificador en la pila de retroceso. Por defecto es null.
 * @param animate Si es true aplica transiciones de fundido suaves (fade in/out).
 */
fun FragmentManager.navigateTo(
    fragment: Fragment,
    @IdRes containerId: Int = R.id.fragment_container,
    addToBackStack: Boolean = true,
    backStackName: String? = null,
    animate: Boolean = true
) {
    beginTransaction().apply {
        if (animate) {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
        replace(containerId, fragment)
        if (addToBackStack) {
            addToBackStack(backStackName)
        }
        commit()
    }
}

/**
 * Retorna al fragmento anterior en el historial si es posible (BackStack).
 * De lo contrario, realiza una navegación limpia de seguridad hacia el fragmento alternativo especificado.
 *
 * @param fallbackFragment Fragmento de respaldo al que se navegará si la pila está vacía.
 * @param containerId ID del contenedor XML donde se hospedará el fragmento. Por defecto usa [R.id.fragment_container].
 */
fun FragmentManager.navigateBackOr(
    fallbackFragment: Fragment,
    @IdRes containerId: Int = R.id.fragment_container
) {
    if (backStackEntryCount > 0) {
        popBackStack()
    } else {
        navigateTo(
            fragment = fallbackFragment,
            containerId = containerId,
            addToBackStack = false,
            animate = true
        )
    }
}

/**
 * Limpia todas las entradas de la pila de retroceso (BackStack) y realiza una navegación limpia
 * hacia el fragmento indicado sin registrar historial previo. Útil para Login exitoso y Logout.
 *
 * @param fragment Instancia del Fragment de destino al que se desea navegar.
 * @param containerId ID del contenedor XML. Por defecto usa [R.id.fragment_container].
 */
fun FragmentManager.clearBackStackAndNavigateTo(
    fragment: Fragment,
    @IdRes containerId: Int = R.id.fragment_container
) {
    if (backStackEntryCount > 0) {
        popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
    beginTransaction().apply {
        setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        replace(containerId, fragment)
        commit()
    }
}
