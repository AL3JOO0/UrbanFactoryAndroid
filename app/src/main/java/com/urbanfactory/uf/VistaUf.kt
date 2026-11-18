package com.urbanfactory.uf

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

/** Controla las pantallas y las acciones de comprador, vendedor y administrador. */
class VistaUf : Activity() {

    private var rol = "com"
    private var pag = "ini"
    private var idActual = 0
    private var fotoSeleccionada: File? = null
    private var fotoPendiente: File? = null
    private var ubicacion = ""
    private val roles = listOf("Comprador", "Vendedor", "Administrador")

    /** Abre el XML correspondiente al rol y conecta sus botones. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DatosUf.iniciar(this)
        rol = intent.getStringExtra("rol") ?: "com"
        pag = intent.getStringExtra("pag") ?: "ini"
        idActual = intent.getIntExtra("id", 0)
        if (NubeUf.configurada(this) && NubeUf.usuario() == null) {
            startActivity(Intent(this, AccesoUf::class.java))
            finish()
            return
        }
        if (NubeUf.configurada(this)) rol = DatosUf.rolActivo
        val diseno = when ("${rol}_${pag}") {
            "com_ini" -> R.layout.com_ini_uf
            "com_cat" -> R.layout.com_cat_uf
            "com_det" -> R.layout.com_det_uf
            "com_car" -> R.layout.com_car_uf
            "com_pago" -> R.layout.com_pago_uf
            "com_ped" -> R.layout.com_ped_uf
            "com_per" -> R.layout.com_per_uf
            "ven_ini" -> R.layout.ven_ini_uf
            "ven_pro" -> R.layout.ven_pro_uf
            "ven_for" -> R.layout.ven_for_uf
            "ven_ped" -> R.layout.ven_ped_uf
            "ven_per" -> R.layout.ven_per_uf
            "adm_ini" -> R.layout.adm_ini_uf
            "adm_usu" -> R.layout.adm_usu_uf
            "adm_usu_for" -> R.layout.adm_usu_for_uf
            "adm_pro" -> R.layout.adm_pro_uf
            "adm_for" -> R.layout.adm_for_uf
            "adm_ven" -> R.layout.adm_ven_uf
            else -> {
                aviso("Pantalla no disponible")
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
        if (enNube() && (pag == "ini" || pag == "cat" || pag == "pro")) {
            NubeUf.cargarProductos(this) { correcto, _ ->
                if (correcto && (pag == "ini" || pag == "cat")) {
                    if (rol == "com") cargarPrendas(destacados = pag == "ini")
                }
                if (correcto && pag == "pro") listarProductos()
            }
        }
        if (enNube() && (pag == "ped" || pag == "ven")) {
            NubeUf.cargarPedidos { correcto, _ ->
                if (correcto && pag == "ped") listarPedidos()
                if (correcto && pag == "ven") reporteVentas()
            }
        }
        if (enNube() && rol == "adm" && pag == "usu") {
            NubeUf.cargarUsuarios { correcto, _ -> if (correcto) listarUsuarios() }
        }
    }

    private fun enNube(): Boolean = NubeUf.configurada(this) && NubeUf.usuario() != null

    /** Cambia de pantalla conservando el rol y el producto seleccionado. */
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

    /** Relaciona las opciones del menú con las pantallas disponibles. */
    private fun conectarMenu() {
        clic(R.id.ir_ini) { ir("ini") }
        when (rol) {
            "com" -> {
                clic(R.id.ir_cat) { ir("cat") }
                clic(R.id.ir_car) { ir("car") }
                clic(R.id.ir_ped) { ir("ped") }
                clic(R.id.ir_per) { ir("per") }
            }
            "ven" -> {
                clic(R.id.ir_pro) { ir("pro") }
                clic(R.id.ir_ped) { ir("ped") }
                clic(R.id.ir_per) { ir("per") }
            }
            "adm" -> {
                clic(R.id.ir_usu) { ir("usu") }
                clic(R.id.ir_pro) { ir("pro") }
                clic(R.id.ir_ven) { ir("ven") }
            }
        }
    }

    private fun texto(id: Int, valor: String) { findViewById<TextView>(id)?.text = valor }
    private fun escribir(id: Int, valor: String) { findViewById<EditText>(id)?.setText(valor) }
    private fun leer(id: Int): String = findViewById<EditText>(id)?.text?.toString()?.trim().orEmpty()
    private fun aviso(mensaje: String) { Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show() }
    private fun pesos(valor: Int): String = NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(valor)
    private fun listado(): LinearLayout? = findViewById(R.id.lista_uf)
    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private fun opciones(id: Int, datos: List<String>, seleccionado: Int = 0) {
        val lista = findViewById<Spinner>(id) ?: return
        lista.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, datos)
        if (seleccionado in datos.indices) lista.setSelection(seleccionado)
    }

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

    private fun imagenPrenda(vista: ImageView, p: PrendaUf) {
        vista.setImageResource(DatosUf.imagen(p.tipo))
        if (p.imagenLocal.isNotBlank()) {
            val archivo = File(p.imagenLocal)
            if (archivo.exists()) {
                BitmapFactory.decodeFile(archivo.absolutePath)?.let { vista.setImageBitmap(it) }
                return
            }
        }
        if (enNube() && p.imagenNube.isNotBlank()) NubeUf.mostrarFoto(p.imagenNube, vista)
    }

    /** Muestra las prendas y permite filtrarlas desde el catálogo. */
    private fun cargarPrendas(filtro: String = "", destacados: Boolean = false) {
        val lista = listado() ?: return
        lista.removeAllViews()
        val prendas = DatosUf.prendas.filter {
            it.activa && it.stock > 0 &&
                (filtro.isBlank() || it.nombre.contains(filtro, true) || it.tipo.contains(filtro, true))
        }.let { if (destacados) it.take(3) else it }
        if (prendas.isEmpty()) agregar(lista, TextView(this).apply { text = getString(R.string.sin_prendas_uf) })
        prendas.forEach { p ->
            val card = tarjeta(p.nombre, "${pesos(p.precio)} · Disponibles: ${p.stock}")
            card.addView(ImageView(this).apply {
                imagenPrenda(this, p)
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
            "pago" -> direccionYPago()
            "ped" -> listarPedidos()
            "per" -> perfil()
        }
    }

    /** Carga el producto seleccionado y permite agregarlo al carrito. */
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
        findViewById<ImageView>(R.id.img_prod)?.let { imagenPrenda(it, p) }
        opciones(R.id.sp_tal, p.tallas.split(",").map { it.trim() })
        opciones(R.id.sp_col, p.colores.split(",").map { it.trim() })
        escribir(R.id.edt_can, "1")
        clic(R.id.btn_vol) { ir("cat") }
        clic(R.id.btn_agr) {
            val cantidad = leer(R.id.edt_can).toIntOrNull() ?: 0
            val agregados = DatosUf.carrito.filter { it.prendaId == p.id }.sumOf { it.cant }
            if (cantidad < 1 || cantidad + agregados > p.stock || !p.activa) {
                aviso("Revisa la cantidad disponible")
                return@clic
            }
            val talla = findViewById<Spinner>(R.id.sp_tal).selectedItem.toString()
            val color = findViewById<Spinner>(R.id.sp_col).selectedItem.toString()
            DatosUf.carrito.add(ItemUf(p.id, talla, color, cantidad))
            persistirCarrito()
            ir("car")
        }
    }

    /** Permite aumentar, disminuir y quitar productos del carrito. */
    private fun carrito() {
        val lista = listado() ?: return
        lista.removeAllViews()
        DatosUf.carrito.toList().forEach { item ->
            val p = DatosUf.prenda(item.prendaId) ?: return@forEach
            val card = tarjeta(p.nombre, "Talla: ${item.talla} · Color: ${item.color} · Cant.: ${item.cant}\n${pesos(p.precio * item.cant)}")
            card.addView(boton("Aumentar cantidad") {
                val cantidadTotal = DatosUf.carrito.filter { it.prendaId == p.id }.sumOf { it.cant }
                if (cantidadTotal >= p.stock) aviso("No hay más unidades disponibles")
                else { item.cant++; persistirCarrito(); carrito() }
            })
            card.addView(boton("Disminuir cantidad") {
                if (item.cant > 1) item.cant-- else DatosUf.carrito.remove(item)
                persistirCarrito(); carrito()
            })
            card.addView(boton("Quitar del carrito") {
                DatosUf.carrito.remove(item); persistirCarrito(); carrito()
            })
            agregar(lista, card)
        }
        if (DatosUf.carrito.isEmpty()) agregar(lista, TextView(this).apply {
            text = getString(R.string.carrito_vacio_uf)
        })
        texto(R.id.txt_tot, pesos(DatosUf.totalCarrito()))
        clic(R.id.btn_pag) {
            if (DatosUf.carrito.isEmpty()) aviso("Agrega un producto al carrito")
            else ir("pago")
        }
    }

    /** Guarda los cambios del carrito localmente y en la nube cuando está disponible. */
    private fun persistirCarrito() {
        DatosUf.guardar()
        if (enNube()) NubeUf.guardarCarrito { correcto, mensaje -> if (!correcto) aviso(mensaje) }
    }

    /** Recibe la dirección y registra pedidos pendientes de pago. */
    private fun direccionYPago() {
        escribir(R.id.edt_dir, DatosUf.direccionPerfil)
        clic(R.id.btn_ubi) { obtenerUbicacion() }
        clic(R.id.btn_con) {
            val direccion = leer(R.id.edt_dir)
            if (direccion.isBlank()) { aviso("Escribe la dirección de entrega"); return@clic }
            if (DatosUf.carrito.isEmpty() || DatosUf.carrito.any { item ->
                    val p = DatosUf.prenda(item.prendaId)
                    p == null || !p.activa || item.cant < 1 || item.cant > p.stock
                }) {
                aviso("Revisa el carrito y la cantidad disponible")
                return@clic
            }
            val detalle = DatosUf.carrito.joinToString("; ") {
                "${it.cant} ${DatosUf.prenda(it.prendaId)?.nombre ?: "Prenda"} (${it.talla}, ${it.color})"
            }
            val pedido = PedidoUf(
                UUID.randomUUID().hashCode() and Int.MAX_VALUE,
                DatosUf.nombrePerfil, detalle, DatosUf.totalCarrito(), "Pendiente de pago",
                DatosUf.usuarioUid, direccion, System.currentTimeMillis(), ubicacion
            )
            if (enNube()) {
                NubeUf.guardarPedido(pedido) { correcto, mensaje ->
                    aviso(mensaje)
                    if (correcto) {
                        DatosUf.pedidos.add(0, pedido)
                        DatosUf.direccionPerfil = direccion
                        DatosUf.carrito.clear()
                        persistirCarrito()
                        ir("ped")
                    }
                }
            } else {
                DatosUf.pedidos.add(0, pedido)
                DatosUf.carrito.clear()
                DatosUf.direccionPerfil = direccion
                persistirCarrito()
                aviso("Pedido local guardado. No se realizó ningún pago")
                ir("ped")
            }
        }
        clic(R.id.btn_epayco) {
            if (!enNube()) aviso("Para un pago real debes configurar Firebase y el servidor de pagos")
            else PagoServidorUf.abrir(this, DatosUf.carrito, leer(R.id.edt_dir), ubicacion, ::aviso)
        }
    }

    /** Solicita permiso al celular antes de consultar su ubicación. */
    private fun obtenerUbicacion() {
        val permiso = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val aproximada = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permiso != PackageManager.PERMISSION_GRANTED && aproximada != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
            return
        }
        consultarGPS()
    }

    @SuppressLint("MissingPermission")
    private fun consultarGPS() {
        val gps = getSystemService(LOCATION_SERVICE) as LocationManager
        val proveedor = when {
            gps.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            gps.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> { aviso("Activa la ubicación del celular"); return }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            gps.getCurrentLocation(proveedor, null, mainExecutor) { lugar ->
                if (lugar == null) aviso("No se obtuvo la ubicación. Intenta de nuevo")
                else actualizarUbicacion(lugar.latitude, lugar.longitude)
            }
        } else {
            val lugar = gps.getLastKnownLocation(proveedor)
            if (lugar == null) aviso("Aún no hay ubicación disponible en el dispositivo")
            else actualizarUbicacion(lugar.latitude, lugar.longitude)
        }
    }

    private fun actualizarUbicacion(latitud: Double, longitud: Double) {
        ubicacion = "$latitud, $longitud"
        texto(R.id.txt_ubi, "Ubicación obtenida: $ubicacion. Escribe también tu dirección de entrega")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) consultarGPS()
            else aviso("No se autorizó el acceso a la ubicación")
        }
    }

    private fun perfil() {
        escribir(R.id.edt_nom, DatosUf.nombrePerfil)
        escribir(R.id.edt_cor, DatosUf.correoPerfil)
        escribir(R.id.edt_tel, DatosUf.telefonoPerfil)
        escribir(R.id.edt_dir, DatosUf.direccionPerfil)
        findViewById<EditText>(R.id.edt_cor)?.isEnabled = !enNube()
        clic(R.id.btn_sal) { salir() }
        clic(R.id.btn_gua) {
            val nombre = leer(R.id.edt_nom)
            if (nombre.isBlank()) { aviso("Escribe tu nombre"); return@clic }
            DatosUf.nombrePerfil = nombre
            DatosUf.telefonoPerfil = leer(R.id.edt_tel)
            DatosUf.direccionPerfil = leer(R.id.edt_dir)
            if (!enNube()) DatosUf.correoPerfil = leer(R.id.edt_cor)
            DatosUf.usuarioActual()?.let {
                it.nombre = nombre
                it.telefono = DatosUf.telefonoPerfil
                it.direccion = DatosUf.direccionPerfil
            }
            DatosUf.guardar()
            if (enNube()) NubeUf.guardarPerfil { _, texto -> aviso(texto) }
            else aviso("Perfil guardado en el dispositivo")
        }
    }

    /** Muestra el historial y las acciones disponibles según el rol. */
    private fun listarPedidos() {
        val lista = listado() ?: return
        lista.removeAllViews()
        val pedidos = when {
            rol != "com" -> DatosUf.pedidos
            DatosUf.usuarioUid.isNotBlank() -> DatosUf.pedidos.filter { it.uid == DatosUf.usuarioUid }
            else -> DatosUf.pedidos.filter { it.cliente == DatosUf.nombrePerfil }
        }
        if (pedidos.isEmpty()) agregar(lista, tarjeta("Sin pedidos", "Todavía no hay pedidos registrados"))
        pedidos.sortedByDescending { it.fecha }.forEach { p ->
            val card = tarjeta("Pedido #${p.id}", "${p.detalle}\n${pesos(p.total)} · ${p.estado}\nEntrega: ${p.direccion}")
            if (rol == "com" && p.estado == "Pendiente de pago") {
                card.addView(boton("Cancelar pedido") {
                    if (enNube()) {
                        NubeUf.cancelarPedido(p) { correcto, mensaje ->
                            aviso(mensaje)
                            if (correcto) { p.estado = "Cancelado"; DatosUf.guardar(); listarPedidos() }
                        }
                    } else { p.estado = "Cancelado"; DatosUf.guardar(); listarPedidos() }
                })
            }
            if ((rol == "ven" || rol == "adm") && p.estado == "Pagado") {
                listOf("En preparación", "Despachado", "Entregado").forEach { estado ->
                    card.addView(boton(estado) {
                        if (enNube()) {
                            NubeUf.cambiarEstadoPedido(p, estado) { correcto, mensaje ->
                                aviso(mensaje)
                                if (correcto) { p.estado = estado; DatosUf.guardar(); listarPedidos() }
                            }
                        } else { p.estado = estado; DatosUf.guardar(); listarPedidos() }
                    })
                }
            }
            agregar(lista, card)
        }
    }

    private fun vendedor() {
        when (pag) {
            "ini" -> {
                texto(R.id.txt_pro, DatosUf.prendas.size.toString())
                texto(R.id.txt_ped, DatosUf.pedidos.size.toString())
                clic(R.id.btn_nue) { ir("for") }
                clic(R.id.btn_pro) { ir("pro") }
                clic(R.id.btn_ped) { ir("ped") }
            }
            "pro" -> listarProductos()
            "for" -> formularioPrenda()
            "ped" -> listarPedidos()
            "per" -> perfil()
        }
    }

    private fun administrador() {
        when (pag) {
            "ini" -> {
                texto(R.id.txt_usu, DatosUf.usuarios.size.toString())
                texto(R.id.txt_pro, DatosUf.prendas.count { it.activa }.toString())
                texto(R.id.txt_ven, pesos(DatosUf.totalVentas()))
                clic(R.id.btn_usu) { ir("usu") }
                clic(R.id.btn_pro) { ir("pro") }
                clic(R.id.btn_ven) { ir("ven") }
                clic(R.id.btn_sal) { salir() }
            }
            "usu" -> listarUsuarios()
            "usu_for" -> formularioUsuario()
            "pro" -> listarProductos()
            "for" -> formularioPrenda()
            "ven" -> reporteVentas()
        }
    }

    private fun reporteVentas() {
        texto(R.id.txt_ped, DatosUf.pedidos.size.toString())
        texto(R.id.txt_tot, pesos(DatosUf.totalVentas()))
        val lista = listado() ?: return
        lista.removeAllViews()
        DatosUf.pedidos.forEach { p ->
            agregar(lista, tarjeta("Pedido #${p.id}", "${p.detalle}\n${pesos(p.total)} · ${p.estado}"))
        }
    }

    /** Muestra los usuarios para revisar y actualizar sus datos. */
    private fun listarUsuarios() {
        val lista = listado() ?: return
        lista.removeAllViews()
        clic(R.id.btn_nue) {
            if (enNube()) aviso("Las cuentas se crean desde Registro y los roles se asignan desde el servidor")
            else ir("usu_for")
        }
        DatosUf.usuarios.forEach { usuario ->
            val card = tarjeta(usuario.nombre, "${usuario.correo} · ${usuario.rol} · ${if (usuario.activo) "Activo" else "Inactivo"}")
            card.addView(boton("Editar datos") { ir("usu_for", usuario.id) })
            card.addView(boton(if (usuario.activo) "Desactivar" else "Activar") {
                usuario.activo = !usuario.activo
                DatosUf.guardar()
                if (enNube()) NubeUf.guardarUsuario(usuario) { _, mensaje -> aviso(mensaje) }
                listarUsuarios()
            })
            agregar(lista, card)
        }
    }

    private fun formularioUsuario() {
        val anterior = DatosUf.usuarios.find { it.id == idActual }
        opciones(R.id.sp_rol, roles, roles.indexOf(anterior?.rol).coerceAtLeast(0))
        if (anterior != null) {
            escribir(R.id.edt_nom, anterior.nombre)
            escribir(R.id.edt_cor, anterior.correo)
            if (enNube()) {
                findViewById<EditText>(R.id.edt_cor)?.isEnabled = false
                findViewById<Spinner>(R.id.sp_rol)?.isEnabled = false
            }
        }
        clic(R.id.btn_gua) {
            val nombre = leer(R.id.edt_nom)
            val correo = leer(R.id.edt_cor)
            if (nombre.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                aviso("Escribe un nombre y correo válidos")
                return@clic
            }
            if (DatosUf.usuarios.any { it.id != idActual && it.correo.equals(correo, true) }) {
                aviso("Ese correo ya está registrado")
                return@clic
            }
            if (anterior != null) {
                anterior.nombre = nombre
                if (!enNube()) {
                    anterior.correo = correo
                    anterior.rol = findViewById<Spinner>(R.id.sp_rol).selectedItem.toString()
                }
                DatosUf.guardar()
                if (enNube()) NubeUf.guardarUsuario(anterior) { _, mensaje -> aviso(mensaje) }
            } else {
                if (enNube()) { aviso("Registra la cuenta desde la pantalla de registro"); return@clic }
                DatosUf.usuarios.add(UsuarioUf((DatosUf.usuarios.maxOfOrNull { it.id } ?: 0) + 1,
                    nombre, correo, findViewById<Spinner>(R.id.sp_rol).selectedItem.toString()))
                DatosUf.guardar()
            }
            ir("usu")
        }
    }

    /** Permite consultar, editar y desactivar prendas registradas. */
    private fun listarProductos() {
        val lista = listado() ?: return
        lista.removeAllViews()
        clic(R.id.btn_nue) { ir("for") }
        DatosUf.prendas.forEach { prenda ->
            val card = tarjeta(prenda.nombre, "${pesos(prenda.precio)} · Stock: ${prenda.stock} · ${if (prenda.activa) "Activo" else "Inactivo"}")
            card.addView(boton("Editar") { ir("for", prenda.id) })
            card.addView(boton(if (prenda.activa) "Desactivar" else "Activar") {
                prenda.activa = !prenda.activa
                DatosUf.guardar()
                if (enNube()) NubeUf.guardarProducto(prenda) { _, mensaje -> aviso(mensaje) }
                listarProductos()
            })
            if (rol == "adm") {
                card.addView(boton("Eliminar producto") {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Eliminar producto")
                        .setMessage("¿Deseas eliminar esta prenda del catálogo?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar") { _, _ ->
                            if (enNube()) {
                                NubeUf.eliminarProducto(prenda) { correcto, mensaje ->
                                    aviso(mensaje)
                                    if (correcto) { DatosUf.prendas.remove(prenda); DatosUf.guardar(); listarProductos() }
                                }
                            } else { DatosUf.prendas.remove(prenda); DatosUf.guardar(); listarProductos() }
                        }.show()
                })
            }
            agregar(lista, card)
        }
    }

    /** Guarda los datos de la prenda; la fotografía es opcional y queda en este celular. */
    private fun formularioPrenda() {
        val tipos = listOf("Jean", "Camiseta", "Buzo")
        val anterior = DatosUf.prenda(idActual)
        opciones(R.id.sp_tip, tipos, tipos.indexOf(anterior?.tipo).coerceAtLeast(0))
        if (anterior != null) {
            escribir(R.id.edt_nom, anterior.nombre)
            escribir(R.id.edt_pre, anterior.precio.toString())
            escribir(R.id.edt_sto, anterior.stock.toString())
            escribir(R.id.edt_tal, anterior.tallas)
            escribir(R.id.edt_col, anterior.colores)
            escribir(R.id.edt_des, anterior.descripcion)
            findViewById<ImageView>(R.id.img_prod)?.let { imagenPrenda(it, anterior) }
        }
        clic(R.id.btn_fot) {
            android.app.AlertDialog.Builder(this).setTitle("Fotografía de la prenda")
                .setItems(arrayOf("Tomar foto", "Seleccionar de galería")) { _, opcion ->
                    if (opcion == 0) abrirCamara() else abrirGaleria()
                }.show()
        }
        clic(R.id.btn_gua) {
            val nombre = leer(R.id.edt_nom)
            val precio = leer(R.id.edt_pre).toIntOrNull() ?: 0
            val stock = leer(R.id.edt_sto).toIntOrNull() ?: -1
            val tallas = leer(R.id.edt_tal)
            val colores = leer(R.id.edt_col)
            if (nombre.isBlank() || precio <= 0 || stock < 0 || tallas.isBlank() || colores.isBlank()) {
                aviso("Completa nombre, precio, cantidad, tallas y colores")
                return@clic
            }
            val tipo = findViewById<Spinner>(R.id.sp_tip).selectedItem.toString()
            val foto = fotoSeleccionada?.absolutePath ?: anterior?.imagenLocal.orEmpty()
            val prenda = anterior ?: PrendaUf(UUID.randomUUID().hashCode() and Int.MAX_VALUE,
                nombre, tipo, precio, stock, tallas, colores, leer(R.id.edt_des))
            prenda.nombre = nombre
            prenda.tipo = tipo
            prenda.precio = precio
            prenda.stock = stock
            prenda.tallas = tallas
            prenda.colores = colores
            prenda.descripcion = leer(R.id.edt_des)
            prenda.imagenLocal = foto
            if (anterior == null) DatosUf.prendas.add(prenda)
            DatosUf.guardar()
            if (enNube()) {
                NubeUf.guardarProducto(prenda) { ok, mensaje ->
                    if (ok) {
                        aviso(if (foto.isBlank()) "Producto guardado en Firestore" else "Producto guardado. La foto queda en este celular")
                        ir("pro")
                    } else {
                        aviso("Quedó guardado en el celular, pero no en Firebase: $mensaje")
                    }
                }
            } else {
                aviso(if (foto.isBlank()) "Producto guardado en SQLite" else "Producto y foto guardados en el celular")
                ir("pro")
            }
        }
    }

    /** Abre la cámara para tomar una fotografía de la prenda. */
    private fun abrirCamara() {
        try {
            val carpeta = File(filesDir, "fotos").apply { mkdirs() }
            fotoPendiente = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.archivos", fotoPendiente!!)
            val camara = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                clipData = android.content.ClipData.newRawUri("foto", uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            @Suppress("DEPRECATION")
            startActivityForResult(camara, 51)
        } catch (ex: Exception) {
            aviso("No se pudo abrir la cámara: ${ex.localizedMessage}")
        }
    }

    private fun abrirGaleria() {
        try {
            val selector = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            @Suppress("DEPRECATION")
            startActivityForResult(selector, 52)
        } catch (ex: Exception) { aviso("No se pudo abrir la galería") }
    }

    @Deprecated("El resultado se utiliza para recibir una fotografía")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        if (requestCode == 51) {
            fotoSeleccionada = fotoPendiente?.takeIf { it.exists() && it.length() > 0 }
        } else if (requestCode == 52) {
            try {
                val origen = data?.data ?: return
                val carpeta = File(filesDir, "fotos").apply { mkdirs() }
                val destino = File(carpeta, "galeria_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(origen)?.use { entrada ->
                    destino.outputStream().use { salida -> entrada.copyTo(salida) }
                }
                fotoSeleccionada = destino.takeIf { it.exists() && it.length() > 0 }
            } catch (ex: Exception) { aviso("No se pudo leer la imagen") }
        }
        fotoSeleccionada?.let { archivo ->
            val imagen = BitmapFactory.decodeFile(archivo.absolutePath)
            findViewById<ImageView>(R.id.img_prod)?.setImageBitmap(imagen)
        } ?: aviso("No se recibió la fotografía")
    }

    /** Cierra la sesión y regresa a la pantalla de ingreso. */
    private fun salir() {
        if (enNube()) FirebaseAuth.getInstance().signOut()
        DatosUf.usuarioUid = ""
        DatosUf.rolActivo = "com"
        DatosUf.nombrePerfil = "Invitado"
        DatosUf.correoPerfil = ""
        DatosUf.telefonoPerfil = ""
        DatosUf.direccionPerfil = ""
        DatosUf.carrito.clear()
        DatosUf.guardar()
        startActivity(Intent(this, AccesoUf::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
