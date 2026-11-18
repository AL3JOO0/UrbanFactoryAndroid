package com.urbanfactory.uf

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID

/** Conecta el aplicativo con Firebase cuando el proyecto está configurado. */
object NubeUf {
    /** Comprueba si Firebase está disponible antes de utilizarlo. */
    fun configurada(context: Context): Boolean =
        FirebaseApp.getApps(context.applicationContext).isNotEmpty() || FirebaseApp.initializeApp(context.applicationContext) != null

    fun usuario() = FirebaseAuth.getInstance().currentUser

    /** Valida las credenciales y obtiene el rol asignado a la cuenta. */
    fun ingresar(correo: String, clave: String, resultado: (Boolean, String) -> Unit) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, clave)
            .addOnSuccessListener { auth ->
                val actual = auth.user ?: run { resultado(false, "No se encontró la cuenta"); return@addOnSuccessListener }
                actual.getIdToken(true).addOnSuccessListener { token ->
                    val rol = token.claims["rol"] as? String ?: "Comprador"
                    DatosUf.usuarioUid = actual.uid
                    DatosUf.correoPerfil = actual.email.orEmpty()
                    DatosUf.nombrePerfil = actual.displayName ?: actual.email.orEmpty()
                    DatosUf.rolActivo = when (rol.lowercase()) {
                        "administrador" -> "adm"
                        "vendedor" -> "ven"
                        else -> "com"
                    }
                    FirebaseFirestore.getInstance().collection("usuarios").document(actual.uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists() && doc.getBoolean("activo") == false) {
                                FirebaseAuth.getInstance().signOut()
                                resultado(false, "Esta cuenta está desactivada")
                                return@addOnSuccessListener
                            }
                            DatosUf.nombrePerfil = doc.getString("nombre") ?: DatosUf.nombrePerfil
                            DatosUf.telefonoPerfil = doc.getString("telefono") ?: ""
                            DatosUf.direccionPerfil = doc.getString("direccion") ?: ""
                            resultado(true, "")
                        }.addOnFailureListener { resultado(false, "No se pudo consultar el perfil: ${it.localizedMessage}") }
                }.addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible comprobar los permisos") }
            }.addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible iniciar sesión") }
    }

    /** Crea una cuenta de comprador y guarda su perfil en Firestore. */
    fun registrar(nombre: String, correo: String, clave: String, telefono: String, resultado: (Boolean, String) -> Unit) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(correo, clave)
            .addOnSuccessListener { auth ->
                val uid = auth.user?.uid ?: run { resultado(false, "Registro incompleto"); return@addOnSuccessListener }
                val datos = mapOf("nombre" to nombre, "correo" to correo, "telefono" to telefono,
                    "direccion" to "", "activo" to true, "rol" to "Comprador", "uid" to uid)
                FirebaseFirestore.getInstance().collection("usuarios").document(uid).set(datos)
                    .addOnSuccessListener {
                        FirebaseAuth.getInstance().signOut()
                        resultado(true, "Cuenta creada. Ingresa con tu correo y contraseña")
                    }.addOnFailureListener { resultado(false, "La cuenta se creó, pero no se guardó el perfil: ${it.localizedMessage}") }
            }.addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible registrar la cuenta") }
    }

    /** Solicita a Firebase el correo de recuperación de contraseña. */
    fun recuperarClave(correo: String, resultado: (Boolean, String) -> Unit) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(correo)
            .addOnSuccessListener { resultado(true, "Revisa tu correo para recuperar la contraseña") }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible enviar el correo") }
    }

    fun guardarPerfil(resultado: (Boolean, String) -> Unit) {
        val uid = usuario()?.uid ?: run { resultado(false, "Debes iniciar sesión"); return }
        val datos = mapOf("nombre" to DatosUf.nombrePerfil, "telefono" to DatosUf.telefonoPerfil,
            "direccion" to DatosUf.direccionPerfil, "correo" to DatosUf.correoPerfil)
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).update(datos)
            .addOnSuccessListener { resultado(true, "Perfil actualizado") }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible guardar el perfil") }
    }

    /** Consulta las prendas almacenadas en Firestore. */
    fun cargarProductos(context: Context, resultado: (Boolean, String) -> Unit = { _, _ -> }) {
        FirebaseFirestore.getInstance().collection("prendas").get().addOnSuccessListener { consulta ->
            val fotosLocales = DatosUf.prendas.associate { it.id to it.imagenLocal }
            DatosUf.prendas.clear()
            consulta.documents.forEach { d ->
                val numero = d.getLong("id")?.toInt() ?: return@forEach
                DatosUf.prendas.add(PrendaUf(
                    numero, d.getString("nombre").orEmpty(), d.getString("tipo").orEmpty(),
                    d.getLong("precio")?.toInt() ?: 0, d.getLong("stock")?.toInt() ?: 0,
                    d.getString("tallas").orEmpty(), d.getString("colores").orEmpty(),
                    d.getString("descripcion").orEmpty(), d.getBoolean("activa") != false,
                    fotosLocales[numero].orEmpty(), d.getString("imagenNube").orEmpty()
                ))
            }
            DatosUf.guardar()
            resultado(true, "Productos sincronizados")
        }.addOnFailureListener { resultado(false, it.localizedMessage ?: "No se pudieron cargar los productos") }
    }

    /** Registra o actualiza la información de una prenda. */
    fun guardarProducto(p: PrendaUf, resultado: (Boolean, String) -> Unit) {
        val datos = mapOf(
            "id" to p.id, "nombre" to p.nombre, "tipo" to p.tipo, "precio" to p.precio,
            "stock" to p.stock, "tallas" to p.tallas, "colores" to p.colores,
            "descripcion" to p.descripcion, "activa" to p.activa, "imagenNube" to p.imagenNube
        )
        FirebaseFirestore.getInstance().collection("prendas").document(p.id.toString()).set(datos)
            .addOnSuccessListener { resultado(true, "Producto guardado en Firestore") }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No se pudo guardar en Firestore") }
    }

    /** Guarda el carrito en Firestore para conservarlo al cambiar de dispositivo. */
    fun guardarCarrito(resultado: (Boolean, String) -> Unit = { _, _ -> }) {
        val uid = usuario()?.uid ?: return
        val items = DatosUf.carrito.map { item ->
            mapOf("productoId" to item.prendaId, "cantidad" to item.cant,
                "talla" to item.talla, "color" to item.color)
        }
        FirebaseFirestore.getInstance().collection("carritos").document(uid)
            .set(mapOf("uid" to uid, "items" to items, "actualizado" to System.currentTimeMillis()))
            .addOnSuccessListener { resultado(true, "Carrito sincronizado") }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No se pudo guardar el carrito") }
    }

    /** Recupera los productos que el comprador tenía en su carrito. */
    fun cargarCarrito(resultado: (Boolean, String) -> Unit = { _, _ -> }) {
        val uid = usuario()?.uid ?: return
        FirebaseFirestore.getInstance().collection("carritos").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    DatosUf.carrito.clear()
                    val items = doc.get("items") as? List<*> ?: emptyList<Any>()
                    items.forEach { raw ->
                        val item = raw as? Map<*, *> ?: return@forEach
                        val id = (item["productoId"] as? Number)?.toInt() ?: return@forEach
                        val cant = (item["cantidad"] as? Number)?.toInt() ?: return@forEach
                        if (cant > 0) DatosUf.carrito.add(ItemUf(id,
                            item["talla"]?.toString().orEmpty(), item["color"]?.toString().orEmpty(), cant))
                    }
                    DatosUf.guardar()
                }
                resultado(true, "Carrito consultado")
            }.addOnFailureListener { resultado(false, it.localizedMessage ?: "No se pudo cargar el carrito") }
    }

    /** Elimina un producto del catálogo, dejando la operación registrada en Firestore. */
    fun eliminarProducto(p: PrendaUf, resultado: (Boolean, String) -> Unit) {
        FirebaseFirestore.getInstance().collection("prendas").document(p.id.toString()).delete()
            .addOnSuccessListener { resultado(true, "Producto eliminado") }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible eliminar el producto") }
    }

    /** Guarda el pedido como pendiente mientras se confirma el pago. */
    fun guardarPedido(p: PedidoUf, resultado: (Boolean, String) -> Unit) {
        val datos = mapOf("id" to p.id, "cliente" to p.cliente, "detalle" to p.detalle,
            "total" to p.total, "estado" to "Pendiente de pago", "uid" to p.uid,
            "direccion" to p.direccion, "fecha" to p.fecha, "ubicacion" to p.ubicacion)
        FirebaseFirestore.getInstance().collection("pedidos").document(p.id.toString()).set(datos)
            .addOnSuccessListener { resultado(true, "Pedido registrado. El pago sigue pendiente") }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No se pudo guardar el pedido") }
    }

    /** Consulta los pedidos permitidos para el usuario actual. */
    fun cargarPedidos(resultado: (Boolean, String) -> Unit = { _, _ -> }) {
        val uid = usuario()?.uid ?: return
        val consulta = FirebaseFirestore.getInstance().collection("pedidos")
        val query = if (DatosUf.rolActivo == "com") consulta.whereEqualTo("uid", uid) else consulta
        query.get().addOnSuccessListener { datos ->
            DatosUf.pedidos.clear()
            datos.documents.forEach { d ->
                DatosUf.pedidos.add(PedidoUf(d.getLong("id")?.toInt() ?: return@forEach,
                    d.getString("cliente").orEmpty(), d.getString("detalle").orEmpty(),
                    d.getLong("total")?.toInt() ?: 0, d.getString("estado") ?: "Pendiente de pago",
                    d.getString("uid").orEmpty(), d.getString("direccion").orEmpty(),
                    d.getLong("fecha") ?: 0L, d.getString("ubicacion").orEmpty()))
            }
            DatosUf.guardar()
            resultado(true, "Pedidos actualizados")
        }.addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible consultar pedidos") }
    }

    /** Carga los perfiles para la pantalla del administrador. */
    fun cargarUsuarios(resultado: (Boolean, String) -> Unit = { _, _ -> }) {
        FirebaseFirestore.getInstance().collection("usuarios").get().addOnSuccessListener { datos ->
            DatosUf.usuarios.clear()
            datos.documents.forEach { d ->
                DatosUf.usuarios.add(UsuarioUf(
                    d.getLong("id")?.toInt() ?: (d.id.hashCode() and Int.MAX_VALUE),
                    d.getString("nombre").orEmpty(), d.getString("correo").orEmpty(),
                    d.getString("rol") ?: "Comprador", d.getBoolean("activo") != false,
                    d.getString("telefono").orEmpty(), d.getString("direccion").orEmpty(), d.id
                ))
            }
            DatosUf.guardar()
            resultado(true, "Usuarios actualizados")
        }.addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible consultar usuarios") }
    }

    /** Actualiza los datos del perfil sin cambiar permisos de acceso. */
    fun guardarUsuario(u: UsuarioUf, resultado: (Boolean, String) -> Unit) {
        if (u.uid.isBlank()) {
            resultado(false, "La cuenta debe registrarse primero en Firebase Authentication")
            return
        }
        val datos = mapOf("nombre" to u.nombre, "correo" to u.correo,
            "telefono" to u.telefono, "direccion" to u.direccion,
            "activo" to u.activo)
        FirebaseFirestore.getInstance().collection("usuarios").document(u.uid).update(datos)
            .addOnSuccessListener { resultado(true, "Perfil actualizado. Los roles reales se administran desde el servidor") }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible actualizar") }
    }

    fun cancelarPedido(p: PedidoUf, resultado: (Boolean, String) -> Unit) {
        FirebaseFirestore.getInstance().collection("pedidos").document(p.id.toString())
            .update("estado", "Cancelado")
            .addOnSuccessListener { resultado(true, "Pedido cancelado") }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible cancelar el pedido") }
    }

    fun cambiarEstadoPedido(p: PedidoUf, estado: String, resultado: (Boolean, String) -> Unit) {
        FirebaseFirestore.getInstance().collection("pedidos").document(p.id.toString())
            .update("estado", estado)
            .addOnSuccessListener { resultado(true, "Estado actualizado") }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No se pudo cambiar el pedido") }
    }

    /** Envía una imagen a Firebase Storage y devuelve su ruta. */
    fun subirFoto(archivo: File, resultado: (Boolean, String) -> Unit) {
        val uid = usuario()?.uid ?: run { resultado(false, "No hay una sesión activa"); return }
        val ruta = "fotos/$uid/${UUID.randomUUID()}.jpg"
        FirebaseStorage.getInstance().reference.child(ruta).putFile(Uri.fromFile(archivo))
            .addOnSuccessListener { resultado(true, ruta) }
            .addOnFailureListener { resultado(false, it.localizedMessage ?: "No fue posible subir la foto") }
    }

    fun mostrarFoto(ruta: String, vista: ImageView) {
        if (ruta.isBlank()) return
        FirebaseStorage.getInstance().reference.child(ruta).getBytes(5L * 1024 * 1024)
            .addOnSuccessListener { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { vista.setImageBitmap(it) }
            }
    }
}
