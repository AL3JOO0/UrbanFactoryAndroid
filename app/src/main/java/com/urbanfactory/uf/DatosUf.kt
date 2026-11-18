package com.urbanfactory.uf

import android.content.Context

data class PrendaUf(
    val id: Int,
    var nombre: String,
    var tipo: String,
    var precio: Int,
    var stock: Int,
    var tallas: String,
    var colores: String,
    var descripcion: String,
    var activa: Boolean = true,
    var imagenLocal: String = "",
    var imagenNube: String = ""
)

data class UsuarioUf(
    val id: Int,
    var nombre: String,
    var correo: String,
    var rol: String,
    var activo: Boolean = true,
    var telefono: String = "",
    var direccion: String = "",
    var uid: String = ""
)

data class ItemUf(val prendaId: Int, val talla: String, val color: String, var cant: Int)

data class PedidoUf(
    val id: Int,
    val cliente: String,
    val detalle: String,
    val total: Int,
    var estado: String,
    val uid: String = "",
    val direccion: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val ubicacion: String = ""
)

/** Aquí organizamos los datos que utilizan las pantallas de Urban Factory. */
object DatosUf {
    val prendas = mutableListOf<PrendaUf>()
    val usuarios = mutableListOf<UsuarioUf>()
    val carrito = mutableListOf<ItemUf>()
    val pedidos = mutableListOf<PedidoUf>()
    var nombrePerfil = "Invitado"
    var correoPerfil = ""
    var telefonoPerfil = ""
    var direccionPerfil = ""
    var usuarioUid = ""
    var rolActivo = "com"
    private var base: BaseLocalUf? = null

    /** Abre SQLite y carga la información cuando inicia el aplicativo. */
    fun iniciar(context: Context) {
        if (base != null) return
        base = BaseLocalUf(context.applicationContext)
        base?.cargar()
        if (prendas.isEmpty()) {
            prendas.addAll(listOf(
                PrendaUf(1, "Jean Baggy", "Jean", 120000, 14, "28,30,32,34", "Azul,Negro", "Jean de corte amplio para uso diario."),
                PrendaUf(2, "Camiseta Básica", "Camiseta", 48000, 22, "S,M,L,XL", "Negro,Blanco", "Camiseta urbana en algodón."),
                PrendaUf(3, "Buzo Urbano", "Buzo", 95000, 9, "S,M,L", "Azul,Gris", "Buzo con capota y bolsillo."),
                PrendaUf(4, "Jean Recto", "Jean", 110000, 7, "28,30,32", "Azul", "Jean recto en tela denim.")
            ))
        }
        if (usuarios.isEmpty()) {
            usuarios.addAll(listOf(
                UsuarioUf(1, "Ana López", "ana@ejemplo.com", "Comprador"),
                UsuarioUf(2, "Carlos Pérez", "carlos@ejemplo.com", "Vendedor"),
                UsuarioUf(3, "Equipo Urban Factory", "admin@ejemplo.com", "Administrador")
            ))
        }
        guardar()
    }

    fun guardar() = base?.guardar() ?: Unit
    fun prenda(id: Int): PrendaUf? = prendas.find { it.id == id }
    fun totalCarrito(): Int = carrito.sumOf { (prenda(it.prendaId)?.precio ?: 0) * it.cant }
    fun totalVentas(): Int = pedidos.filter { it.estado == "Pagado" }.sumOf { it.total }
    /** Selecciona la imagen que corresponde a cada tipo de prenda. */
    fun imagen(tipo: String): Int = when (tipo) {
        "Jean" -> R.drawable.img_jean_uf
        "Buzo" -> R.drawable.img_buzo_uf
        else -> R.drawable.img_cam_uf
    }
    fun usuarioActual(): UsuarioUf? = usuarios.firstOrNull { it.uid.isNotBlank() && it.uid == usuarioUid }
        ?: usuarios.firstOrNull { it.correo.equals(correoPerfil, true) }
}
