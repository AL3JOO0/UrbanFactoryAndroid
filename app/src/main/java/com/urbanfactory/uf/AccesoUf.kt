package com.urbanfactory.uf

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast

class AccesoUf : Activity() {

    private val roles = listOf(
        "Comprador",
        "Vendedor",
        "Administrador"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            Toast.makeText(
                this,
                "Biometría no disponible en esta versión",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun rolElegido(): String {

        return when (
            findViewById<Spinner>(R.id.sp_rol).selectedItemPosition
        ) {
            1 -> "ven"
            2 -> "adm"
            else -> "com"
        }
    }

    private fun ingresar() {
        abrirRol()
    }

    private fun abrirRol() {

        val ventana = Intent(this, VistaUf::class.java).apply {
            putExtra("rol", rolElegido())
            putExtra("pag", "ini")
        }

        startActivity(ventana)
    }
}
