package com.urbanfactory.uf

// Datos únicamente para demostrar las interfaces. No existe backend en esta etapa.
data class PrendaUf(
    val id: Int,
    var nombre: String,
    var tipo: String,
    var precio: Int,
    var stock: Int,
    var tallas: String,
    var colores: String,
    var descripcion: String,
    var activa: Boolean = true
)

data class UsuarioUf(
    val id: Int,
    var nombre: String,
    var correo: String,
    var rol: String,
    var activo: Boolean = true
)

data class ItemUf(val prendaId: Int, val talla: String, val color: String, var cant: Int)
data class PedidoUf(val id: Int, val cliente: String, val detalle: String, val total: Int, val estado: String)

object DatosUf {
    val prendas = mutableListOf(
        PrendaUf(1, "Jean Baggy", "Jean", 120000, 14, "28,30,32,34", "Azul,Negro", "Jean de corte amplio para uso diario."),
        PrendaUf(2, "Camiseta Básica", "Camiseta", 48000, 22, "S,M,L,XL", "Negro,Blanco", "Camiseta urbana en algodón."),
        PrendaUf(3, "Buzo Urbano", "Buzo", 95000, 9, "S,M,L", "Azul,Gris", "Buzo cómodo con capota y bolsillo."),
        PrendaUf(4, "Jean Recto", "Jean", 110000, 7, "28,30,32", "Azul", "Jean de corte recto en tela denim.")
    )
    val usuarios = mutableListOf(
        UsuarioUf(1, "Ana López", "ana@ejemplo.com", "Comprador"),
        UsuarioUf(2, "Carlos Pérez", "carlos@ejemplo.com", "Vendedor"),
        UsuarioUf(3, "Equipo Urban Factory", "admin@ejemplo.com", "Administrador")
    )
    val carrito = mutableListOf<ItemUf>()
    val pedidos = mutableListOf(
        PedidoUf(1001, "Ana López", "1 Camiseta Básica · talla M", 48000, "Recibido"),
        PedidoUf(1002, "Ana López", "1 Jean Baggy · talla 30", 120000, "Entregado")
    )
    var nombrePerfil = "Ana López"
    var correoPerfil = "ana@ejemplo.com"
    var telefonoPerfil = "300 000 0000"

    fun prenda(id: Int): PrendaUf? = prendas.find { it.id == id }
    fun totalCarrito(): Int = carrito.sumOf { (prenda(it.prendaId)?.precio ?: 0) * it.cant }
    fun totalVentas(): Int = pedidos.sumOf { it.total }
    fun imagen(tipo: String): Int = when (tipo) {
        "Jean" -> R.drawable.img_jean_uf
        "Buzo" -> R.drawable.img_buzo_uf
        else -> R.drawable.img_cam_uf
    }
}
