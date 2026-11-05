package com.urbanfactory.uf

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class InicioUf : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.ini_uf)

        val tiempoEspera = 3000L

        Handler(Looper.getMainLooper()).postDelayed({

            if (!isFinishing && !isDestroyed) {

                val pantallaAcceso = Intent(
                    this,
                    AccesoUf::class.java
                )

                startActivity(pantallaAcceso)

                finish()
            }

        }, tiempoEspera)
    }
}
