package com.urbanfactory.uf

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.auth.GetTokenResult

class AccesoUf : Activity() {

    private val roles = listOf(
        "Comprador",
        "Vendedor",
        "Administrador"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DatosUf.iniciar(this)
        setContentView(R.layout.acceso_uf)

        val selector = findViewById<Spinner>(R.id.sp_rol)

        selector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )

        findViewById<Button>(R.id.btn_ing).setOnClickListener {
            ingresar()
        }

        findViewById<Button>(R.id.btn_reg).setOnClickListener {
            startActivity(Intent(this, RegistroUf::class.java))
        }

        findViewById<Button>(R.id.btn_cla).setOnClickListener {
            startActivity(Intent(this, ClaveUf::class.java))
        }

        findViewById<Button>(R.id.btn_bio).setOnClickListener {
            ingresarBio()
        }
    }

    private fun ingresar() {

        val correo = findViewById<EditText>(R.id.edt_cor)
            .text.toString().trim()

        val clave = findViewById<EditText>(R.id.edt_cla)
            .text.toString()

        if (NubeUf.configurada(this)) {

            if (correo.isBlank() || clave.isBlank()) {
                mensaje("Escribe tu correo y contraseña")
                return
            }

            NubeUf.ingresar(correo, clave) { correcto, detalle ->

                if (correcto) {

                    NubeUf.cargarProductos(this) { _, _ -> }

                    NubeUf.cargarPedidos()
                    NubeUf.cargarCarrito()

                    abrirRol(DatosUf.rolActivo)

                } else {
                    mensaje(detalle)
                }
            }

        } else {

            val elegido = when (
                findViewById<Spinner>(R.id.sp_rol).selectedItemPosition
            ) {
                1 -> "ven"
                2 -> "adm"
                else -> "com"
            }

            DatosUf.rolActivo = elegido
            DatosUf.usuarioUid = ""
            DatosUf.correoPerfil = correo

            DatosUf.nombrePerfil = DatosUf.usuarios
                .firstOrNull { it.correo.equals(correo, true) }
                ?.nombre ?: correo.ifBlank { "Usuario de prueba" }

            mensaje("Modo local: acceso de demostración")

            abrirRol(elegido)
        }
    }

    private fun abrirRol(rol: String) {

        val ventana = Intent(this, VistaUf::class.java)

        ventana.putExtra("rol", rol)
        ventana.putExtra("pag", "ini")

        startActivity(ventana)
        finish()
    }

    private fun ingresarBio() {

        if (!NubeUf.configurada(this)) {
            mensaje("Firebase no está configurado")
            return
        }

        val usuario = NubeUf.usuario()

        if (usuario == null) {
            mensaje("Primero debes ingresar con tu cuenta")
            return
        }

        val bloqueo = getSystemService(
            Context.KEYGUARD_SERVICE
        ) as KeyguardManager

        if (!bloqueo.isDeviceSecure) {
            mensaje("Configura huella o bloqueo seguro en el celular")
            return
        }

        try {

            val solicitud = BiometricPrompt.Builder(this)
                .setTitle("Ingreso con huella")
                .setSubtitle("Urban Factory")
                .setNegativeButton(
                    "Cancelar",
                    mainExecutor
                ) { _, _ -> }
                .build()

            solicitud.authenticate(
                CancellationSignal(),
                mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {

                        super.onAuthenticationSucceeded(result)

                        usuario.getIdToken(true)
                            .addOnSuccessListener { token: GetTokenResult ->

                                val rol = when (
                                    (token.claims["rol"] as? String)
                                        .orEmpty()
                                        .lowercase()
                                ) {
                                    "administrador" -> "adm"
                                    "vendedor" -> "ven"
                                    else -> "com"
                                }

                                DatosUf.usuarioUid = usuario.uid
                                DatosUf.rolActivo = rol
                                DatosUf.correoPerfil = usuario.email.orEmpty()

                                NubeUf.cargarProductos(this@AccesoUf) { _, _ -> }

                                NubeUf.cargarPedidos()
                                NubeUf.cargarCarrito()

                                abrirRol(rol)
                            }
                            .addOnFailureListener {
                                mensaje("No fue posible validar la sesión")
                            }
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        mensaje(errString.toString())
                    }
                }
            )

        } catch (ex: Exception) {
            mensaje("Biometría no disponible")
        }
    }

    private fun mensaje(texto: String) {
        Toast.makeText(
            this,
            texto,
            Toast.LENGTH_LONG
        ).show()
    }
}