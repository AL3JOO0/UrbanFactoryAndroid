package com.urbanfactory.uf

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale

class VistaUf : Activity() {

    private var rol = "com"
    private var pag = "ini"
    private var idActual = 0
    private val roles = listOf("Comprador", "Vendedor", "Administrador")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rol = intent.getStringExtra("rol") ?: "com"
        pag = intent.getStringExtra("pag") ?: "ini"
        idActual = intent.getIntExtra("id", 0)

        val diseno = when ("${rol}_${pag}") {
            "com_ini" -> R.layout.com_ini_uf
            "com_cat" -> R.layout.com_cat_uf
            "com_det" -> R.layout.com_det_uf
            "com_car" -> R.layout.com_car_uf
            "ven_ini" -> R.layout.ven_ini_uf
            "ven_for" -> R.layout.ven_for_uf
            "adm_ini" -> R.layout.adm_ini_uf
            "adm_for" -> R.layout.adm_for_uf
            "adm_usu_for" -> R.layout.adm_usu_for_uf
            "adm_ven" -> R.layout.adm_ven_uf
            else -> {
                Toast.makeText(this, "Pantalla no incluida en esta versión", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }

        setContentView(diseno)
        conectarMenu()

        when (rol) {
            "com" -> comprador()
            "ven" -> vendedor()
            "adm" -> administrador()
        }
    }

    private fun ir(pagina: String, id: Int = 0) {
        startActivity(Intent(this, VistaUf::class.java).apply {
            putExtra("rol", rol)
            putExtra("pag", pagina)
            putExtra("id", id)
        })
        finish()
    }

    private fun clic(id: Int, accion: () -> Unit) {
        findViewById<View>(id)?.setOnClickListener { accion() }
    }

    private fun conectarMenu() {
        clic(R.id.ir_ini) { ir("ini") }
        when (rol) {
            "com" -> {
                clic(R.id.ir_cat) { ir("cat") }
                clic(R.id.ir_car) { ir("car") }
            }
            "ven" -> {
                clic(R.id.ir_pro) { ir("for") }
            }
            "adm" -> {
                clic(R.id.ir_usu) { ir("usu_for") }
                clic(R.id.ir_pro) { ir("for") }
                clic(R.id.ir_ven) { ir("ven") }
            }
        }
    }

    private fun texto(id: Int, valor: String) {
        findViewById<TextView>(id)?.text = valor
    }

    private fun escribir(id: Int, valor: String) {
        findViewById<EditText>(id)?.setText(valor)
    }

    private fun leer(id: Int): String =
        findViewById<EditText>(id)?.text?.toString()?.trim().orEmpty()

    private fun aviso(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun pesos(valor: Int): String =
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(valor)

    private fun listado(): LinearLayout? = findViewById(R.id.lista_uf)

    private fun opciones(id: Int, datos: List<String>, seleccionado: Int = 0) {
        val lista = findViewById<Spinner>(id) ?: return
        lista.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, datos)
        if (seleccionado in datos.indices) lista.setSelection(seleccionado)
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private fun tarjeta(titulo: String, detalle: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(12), dp(15), dp(12))
        background = getDrawable(R.drawable.tarjeta_uf)
        addView(TextView(this@VistaUf).apply {
            text = titulo
            textSize = 17f
            setTextColor(getColor(R.color.texto_uf))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        addView(TextView(this@VistaUf).apply {
            text = detalle
            textSize = 14f
            setTextColor(getColor(R.color.gris_uf))
        })
    }

    private fun agregar(lista: LinearLayout, vista: View) {
        lista.addView(vista, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
    }

    private fun boton(titulo: String, accion: () -> Unit): Button = Button(this).apply {
        text = titulo
        isAllCaps = false
        setOnClickListener { accion() }
    }

    private fun cargarPrendas(filtro: String = "", destacados: Boolean = false) {
        val lista = listado() ?: return
        lista.removeAllViews()
        val prendas = DatosUf.prendas.filter {
            it.activa && it.stock > 0 &&
                    (filtro.isBlank() || it.nombre.contains(filtro, true) || it.tipo.contains(filtro, true))
        }.let { if (destacados) it.take(3) else it }

        if (prendas.isEmpty()) {
            agregar(lista, TextView(this).apply { text = getString(R.string.sin_prendas_uf) })
        }
        prendas.forEach { p ->
            val card = tarjeta(p.nombre, "${pesos(p.precio)} · Disponibles: ${p.stock}")
            card.addView(ImageView(this).apply {
                setImageResource(DatosUf.imagen(p.tipo))
                contentDescription = p.nombre
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(130)))
            card.addView(boton("Ver producto") { ir("det", p.id) })
            agregar(lista, card)
        }
    }

    private fun comprador() {
        when (pag) {
            "ini" -> {
                cargarPrendas(destacados = true)
                clic(R.id.btn_cat) { ir("cat") }
            }
            "cat" -> {
                cargarPrendas()
                findViewById<EditText>(R.id.edt_bus)?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        cargarPrendas(s.toString())
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
            }
            "det" -> detallePrenda()
            "car" -> carrito()
        }
    }

    private fun detallePrenda() {
        val p = DatosUf.prenda(idActual) ?: run {
            aviso("Producto no encontrado")
            ir("cat")
            return
        }
        texto(R.id.txt_nom, p.nombre)
        texto(R.id.txt_pre, pesos(p.precio))
        texto(R.id.txt_des, p.descripcion)
        texto(R.id.txt_sto, "Unidades disponibles: ${p.stock}")
        findViewById<ImageView>(R.id.img_prod)?.setImageResource(DatosUf.imagen(p.tipo))
        opciones(R.id.sp_tal, p.tallas.split(",").map { it.trim() })
        opciones(R.id.sp_col, p.colores.split(",").map { it.trim() })
        escribir(R.id.edt_can, "1")
        clic(R.id.btn_vol) { ir("cat") }
        clic(R.id.btn_agr) {
            val cantidad = leer(R.id.edt_can).toIntOrNull() ?: 0
            val agregados = DatosUf.carrito.filter { it.prendaId == p.id }.sumOf { it.cant }
            if (cantidad < 1 || cantidad + agregados > p.stock) {
                aviso("Revisa la cantidad disponible")
                return@clic
            }
            val talla = findViewById<Spinner>(R.id.sp_tal).selectedItem.toString()
            val color = findViewById<Spinner>(R.id.sp_col).selectedItem.toString()
            DatosUf.carrito.add(ItemUf(p.id, talla, color, cantidad))
            ir("car")
        }
    }

    private fun carrito() {
        val lista = listado() ?: return
        lista.removeAllViews()
        DatosUf.carrito.toList().forEach { item ->
            val p = DatosUf.prenda(item.prendaId) ?: return@forEach
            val card = tarjeta(p.nombre, "Talla: ${item.talla} · Color: ${item.color} · Cant.: ${item.cant}\n${pesos(p.precio * item.cant)}")
            card.addView(boton("Quitar del carrito") {
                DatosUf.carrito.remove(item)
                carrito()
            })
            agregar(lista, card)
        }
        if (DatosUf.carrito.isEmpty()) {
            agregar(lista, TextView(this).apply { text = getString(R.string.carrito_vacio_uf) })
        }
        texto(R.id.txt_tot, pesos(DatosUf.totalCarrito()))
        clic(R.id.btn_pag) {
            if (DatosUf.carrito.isEmpty()) {
                aviso("Agrega un producto al carrito")
                return@clic
            }
            if (DatosUf.carrito.any { item ->
                    val p = DatosUf.prenda(item.prendaId)
                    p == null || !p.activa || item.cant > p.stock
                }) {
                aviso("Revisa el inventario antes de comprar")
                return@clic
            }
            val detalle = DatosUf.carrito.joinToString("; ") {
                "${it.cant} ${DatosUf.prenda(it.prendaId)?.nombre ?: "Prenda"}"
            }
            val total = DatosUf.totalCarrito()
            DatosUf.carrito.forEach { item ->
                DatosUf.prenda(item.prendaId)?.let { it.stock -= item.cant }
            }
            DatosUf.pedidos.add(0, PedidoUf(
                (DatosUf.pedidos.maxOfOrNull { it.id } ?: 1000) + 1,
                DatosUf.nombrePerfil, detalle, total, "Recibido"
            ))
            DatosUf.carrito.clear()
            aviso("Compra simulada registrada; no se realizó ningún cobro")
            ir("ini")
        }
    }

    private fun vendedor() {
        when (pag) {
            "ini" -> {
                texto(R.id.txt_pro, DatosUf.prendas.size.toString())
                texto(R.id.txt_ped, DatosUf.pedidos.size.toString())
                clic(R.id.btn_nue) { ir("for") }
            }
            "for" -> formularioPrenda()
        }
    }

    private fun administrador() {
        when (pag) {
            "ini" -> {
                texto(R.id.txt_usu, DatosUf.usuarios.size.toString())
                texto(R.id.txt_pro, DatosUf.prendas.count { it.activa }.toString())
                texto(R.id.txt_ven, pesos(DatosUf.totalVentas()))
                clic(R.id.btn_usu) { ir("usu_for") }
                clic(R.id.btn_pro) { ir("for") }
                clic(R.id.btn_ven) { ir("ven") }
                clic(R.id.btn_sal) { salir() }
            }
            "usu_for" -> formularioUsuario()
            "for" -> formularioPrenda()
            "ven" -> {
                texto(R.id.txt_ped, DatosUf.pedidos.size.toString())
                texto(R.id.txt_tot, pesos(DatosUf.totalVentas()))
                val lista = listado() ?: return
                DatosUf.pedidos.forEach { pedido ->
                    agregar(lista, tarjeta("Pedido #${pedido.id}", "${pedido.detalle}\n${pesos(pedido.total)}"))
                }
            }
        }
    }

    private fun formularioUsuario() {
        opciones(R.id.sp_rol, roles)
        clic(R.id.btn_gua) {
            val nombre = leer(R.id.edt_nom)
            val correo = leer(R.id.edt_cor)
            if (nombre.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                aviso("Escribe un nombre y correo válidos")
                return@clic
            }
            if (DatosUf.usuarios.any { it.correo.equals(correo, ignoreCase = true) }) {
                aviso("Ese correo ya está registrado")
                return@clic
            }
            val rolNuevo = findViewById<Spinner>(R.id.sp_rol).selectedItem.toString()
            DatosUf.usuarios.add(UsuarioUf((DatosUf.usuarios.maxOfOrNull { it.id } ?: 0) + 1, nombre, correo, rolNuevo))
            aviso("Usuario de prueba registrado")
            ir("ini")
        }
    }

    private fun formularioPrenda() {
        val tipos = listOf("Jean", "Camiseta", "Buzo")
        opciones(R.id.sp_tip, tipos)
        clic(R.id.btn_fot) {
            aviso("La cámara no está disponible en esta versión")
        }
        clic(R.id.btn_gua) {
            val nombre = leer(R.id.edt_nom)
            val precio = leer(R.id.edt_pre).toIntOrNull() ?: 0
            val stock = leer(R.id.edt_sto).toIntOrNull() ?: -1
            val tallas = leer(R.id.edt_tal)
            val colores = leer(R.id.edt_col)
            if (nombre.isBlank() || precio <= 0 || stock < 0 || tallas.isBlank() || colores.isBlank()) {
                aviso("Completa el nombre, precio, cantidad, tallas y colores")
                return@clic
            }
            val tipo = findViewById<Spinner>(R.id.sp_tip).selectedItem.toString()
            DatosUf.prendas.add(PrendaUf(
                (DatosUf.prendas.maxOfOrNull { it.id } ?: 0) + 1,
                nombre, tipo, precio, stock, tallas, colores, leer(R.id.edt_des)
            ))
            aviso("Prenda de prueba registrada")
            ir("ini")
        }
    }

    private fun salir() {
        startActivity(Intent(this, AccesoUf::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
}
