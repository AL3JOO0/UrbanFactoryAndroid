package com.urbanfactory.uf

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Solicita la página de pago al servidor sin guardar llaves privadas en Android. */
object PagoServidorUf {

    /** Envía el carrito y abre el enlace validado que devuelve el servidor. */
    fun abrir(pantalla: Activity, carrito: List<ItemUf>, direccion: String, ubicacion: String, aviso: (String) -> Unit) {
        val endpoint = BuildConfig.URL_SERVIDOR_PAGO
        if (!endpoint.startsWith("https://")) {
            aviso("Debes configurar la dirección HTTPS de tu servidor de pagos")
            return
        }
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            aviso("Debes iniciar sesión antes de pagar")
            return
        }
        if (carrito.isEmpty()) {
            aviso("El carrito está vacío")
            return
        }
        if (direccion.isBlank()) {
            aviso("Primero escribe la dirección de entrega")
            return
        }
        usuario.getIdToken(false).addOnSuccessListener { token ->
            val jwt = token.token.orEmpty()
            if (jwt.isBlank()) {
                aviso("No se pudo validar la sesión")
                return@addOnSuccessListener
            }
            Thread {
                var conexion: HttpsURLConnection? = null
                try {
                    val cuerpo = JSONObject().apply {
                        put("direccion", direccion)
                        put("ubicacion", ubicacion)
                        put("items", JSONArray().apply {
                            carrito.forEach { item ->
                                put(JSONObject().apply {
                                    put("productoId", item.prendaId)
                                    put("cantidad", item.cant)
                                    put("talla", item.talla)
                                    put("color", item.color)
                                })
                            }
                        })
                    }
                    conexion = URL(endpoint).openConnection() as HttpsURLConnection
                    conexion.requestMethod = "POST"
                    conexion.connectTimeout = 15000
                    conexion.readTimeout = 15000
                    conexion.setRequestProperty("Authorization", "Bearer $jwt")
                    conexion.setRequestProperty("Content-Type", "application/json")
                    conexion.doOutput = true
                    conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
                    val codigo = conexion.responseCode
                    val respuesta = if (codigo in 200..299) conexion.inputStream.bufferedReader().use { it.readText() }
                    else conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (codigo !in 200..299) throw IllegalStateException("El servidor rechazó el pago ($codigo): ${respuesta.take(140)}")
                    val enlace = JSONObject(respuesta).optString("checkoutUrl")
                    val url = Uri.parse(enlace)
                    val host = url.host.orEmpty().lowercase()
                    val servidor = Uri.parse(endpoint).host.orEmpty().lowercase()
                    if (url.scheme != "https" || !(host == servidor || host == "epayco.co" || host.endsWith(".epayco.co"))) {
                        throw IllegalStateException("El servidor no devolvió una dirección de pago confiable")
                    }
                    pantalla.runOnUiThread {
                        pantalla.startActivity(Intent(Intent.ACTION_VIEW, url))
                        aviso("El pedido solo se marcará pagado cuando el servidor confirme el cobro")
                    }
                } catch (ex: Exception) {
                    pantalla.runOnUiThread { aviso(ex.localizedMessage ?: "No fue posible iniciar el pago") }
                } finally {
                    conexion?.disconnect()
                }
            }.start()
        }.addOnFailureListener { aviso("No fue posible obtener una sesión válida") }
    }
}
