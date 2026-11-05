package com.urbanfactory.uf

import android.app.Activity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class ClaveUf : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.clave_uf)

        val botonVolver = findViewById<Button>(R.id.btn_vol)

        val botonEnviar = findViewById<Button>(R.id.btn_env)

        val campoCorreo = findViewById<EditText>(R.id.edt_cor)


        botonVolver.setOnClickListener {
            finish()
        }


        botonEnviar.setOnClickListener {

            val correo = campoCorreo.text.toString().trim()

            val correoValido = Patterns.EMAIL_ADDRESS
                .matcher(correo)
                .matches()

            if (!correoValido) {

                Toast.makeText(
                    this,
                    "Ingresa un correo válido",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Solicitud simulada. No se envió ningún correo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
