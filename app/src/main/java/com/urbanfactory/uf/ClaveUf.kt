package com.urbanfactory.uf

import android.app.Activity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

/** Permite solicitar la recuperación de acceso por correo. */
class ClaveUf : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clave_uf)
        val correo = findViewById<EditText>(R.id.edt_cor)
        findViewById<Button>(R.id.btn_vol).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_env).setOnClickListener {
            val direccion = correo.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(direccion).matches()) {
                aviso("Ingresa un correo válido")
                return@setOnClickListener
            }
            if (NubeUf.configurada(this)) {
                NubeUf.recuperarClave(direccion) { correcto, texto ->
                    aviso(texto)
                    if (correcto) finish()
                }
            } else {
                aviso("Sin Firebase configurado no es posible enviar el correo")
            }
        }
    }

    private fun aviso(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
}
