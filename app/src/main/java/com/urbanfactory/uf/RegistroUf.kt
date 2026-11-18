package com.urbanfactory.uf

import android.app.Activity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

/** Recibe los datos del formulario para crear una cuenta. */
class RegistroUf : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DatosUf.iniciar(this)
        setContentView(R.layout.registro_uf)
        val nombre = findViewById<EditText>(R.id.edt_nom)
        val correo = findViewById<EditText>(R.id.edt_cor)
        val telefono = findViewById<EditText>(R.id.edt_tel)
        val clave = findViewById<EditText>(R.id.edt_cla)
        findViewById<Button>(R.id.btn_vol).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_gua).setOnClickListener {
            val nom = nombre.text.toString().trim()
            val cor = correo.text.toString().trim()
            val tel = telefono.text.toString().trim()
            val pass = clave.text.toString()
            if (nom.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cor).matches() || pass.length < 6) {
                aviso("Ingresa nombre, correo válido y una contraseña de al menos 6 caracteres")
                return@setOnClickListener
            }
            if (NubeUf.configurada(this)) {
                NubeUf.registrar(nom, cor, pass, tel) { correcto, texto ->
                    aviso(texto)
                    if (correcto) finish()
                }
            } else {
                if (DatosUf.usuarios.any { it.correo.equals(cor, true) }) {
                    aviso("Ese correo ya existe en los datos de prueba")
                    return@setOnClickListener
                }
                DatosUf.usuarios.add(UsuarioUf((DatosUf.usuarios.maxOfOrNull { it.id } ?: 0) + 1,
                    nom, cor, "Comprador", telefono = tel))
                DatosUf.guardar()
                aviso("Usuario de prueba guardado en SQLite. La contraseña no se almacena")
                finish()
            }
        }
    }

    private fun aviso(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
}
