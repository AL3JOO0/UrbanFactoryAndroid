package com.urbanfactory.uf

import android.app.Activity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class RegistroUf : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.registro_uf)

        val botonVolver = findViewById<Button>(R.id.btn_vol)
        val botonGuardar = findViewById<Button>(R.id.btn_gua)

        val campoNombre = findViewById<EditText>(R.id.edt_nom)
        val campoCorreo = findViewById<EditText>(R.id.edt_cor)
        val campoClave = findViewById<EditText>(R.id.edt_cla)

        botonVolver.setOnClickListener {
            finish()
        }

        botonGuardar.setOnClickListener {

            val nombre = campoNombre.text.toString().trim()
            val correo = campoCorreo.text.toString().trim()
            val clave = campoClave.text.toString()

            val correoValido = Patterns.EMAIL_ADDRESS
                .matcher(correo)
                .matches()

            if (nombre.isBlank() || !correoValido || clave.length < 4) {

                Toast.makeText(
                    this,
                    "Escribe nombre, correo válido y contraseña de al menos 4 caracteres",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            val correoExiste = DatosUf.usuarios.any {
                it.correo.equals(correo, ignoreCase = true)
            }

            if (correoExiste) {

                Toast.makeText(
                    this,
                    "Ya existe un usuario de prueba con ese correo",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            val nuevoId = (DatosUf.usuarios.maxOfOrNull {
                it.id
            } ?: 0) + 1

            val nuevoUsuario = UsuarioUf(
                nuevoId,
                nombre,
                correo,
                "Comprador"
            )

            DatosUf.usuarios.add(nuevoUsuario)

            Toast.makeText(
                this,
                "Registro de prueba guardado en memoria",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }
}
