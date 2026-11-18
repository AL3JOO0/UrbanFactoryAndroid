package com.urbanfactory.uf

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Base de datos local para conservar la información al cerrar la aplicación. */
class BaseLocalUf(context: Context) : SQLiteOpenHelper(context, "urban_factory.db", null, 2) {

    /** Crea las tablas que se necesitan en el celular. */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE prendas (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, tipo TEXT NOT NULL, precio INTEGER NOT NULL, stock INTEGER NOT NULL, tallas TEXT NOT NULL, colores TEXT NOT NULL, descripcion TEXT NOT NULL, activa INTEGER NOT NULL, imagen_local TEXT NOT NULL, imagen_nube TEXT NOT NULL)")
        db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, correo TEXT NOT NULL, rol TEXT NOT NULL, activo INTEGER NOT NULL, telefono TEXT NOT NULL, direccion TEXT NOT NULL, uid TEXT NOT NULL)")
        db.execSQL("CREATE TABLE carrito (id INTEGER PRIMARY KEY AUTOINCREMENT, prenda_id INTEGER NOT NULL, talla TEXT NOT NULL, color TEXT NOT NULL, cantidad INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE pedidos (id INTEGER PRIMARY KEY, cliente TEXT NOT NULL, detalle TEXT NOT NULL, total INTEGER NOT NULL, estado TEXT NOT NULL, uid TEXT NOT NULL, direccion TEXT NOT NULL, fecha INTEGER NOT NULL, ubicacion TEXT NOT NULL DEFAULT '')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE pedidos ADD COLUMN ubicacion TEXT NOT NULL DEFAULT ''")
    }

    /** Recupera los productos, usuarios y pedidos que ya están guardados. */
    fun cargar() {
        val db = readableDatabase
        DatosUf.prendas.clear()
        db.rawQuery("SELECT id,nombre,tipo,precio,stock,tallas,colores,descripcion,activa,imagen_local,imagen_nube FROM prendas", null).use { c ->
            while (c.moveToNext()) DatosUf.prendas.add(PrendaUf(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getString(5), c.getString(6), c.getString(7), c.getInt(8) == 1, c.getString(9), c.getString(10)))
        }
        DatosUf.usuarios.clear()
        db.rawQuery("SELECT id,nombre,correo,rol,activo,telefono,direccion,uid FROM usuarios", null).use { c ->
            while (c.moveToNext()) DatosUf.usuarios.add(UsuarioUf(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4) == 1, c.getString(5), c.getString(6), c.getString(7)))
        }
        DatosUf.carrito.clear()
        db.rawQuery("SELECT prenda_id,talla,color,cantidad FROM carrito", null).use { c ->
            while (c.moveToNext()) DatosUf.carrito.add(ItemUf(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3)))
        }
        DatosUf.pedidos.clear()
        db.rawQuery("SELECT id,cliente,detalle,total,estado,uid,direccion,fecha,ubicacion FROM pedidos", null).use { c ->
            while (c.moveToNext()) DatosUf.pedidos.add(PedidoUf(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3), c.getString(4), c.getString(5), c.getString(6), c.getLong(7), c.getString(8)))
        }
    }

    /** Guarda los cambios en una sola transacción para evitar datos a medias. */
    fun guardar() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("prendas", null, null)
            DatosUf.prendas.forEach { p ->
                db.insertOrThrow("prendas", null, ContentValues().apply {
                    put("id", p.id); put("nombre", p.nombre); put("tipo", p.tipo)
                    put("precio", p.precio); put("stock", p.stock); put("tallas", p.tallas)
                    put("colores", p.colores); put("descripcion", p.descripcion)
                    put("activa", if (p.activa) 1 else 0)
                    put("imagen_local", p.imagenLocal); put("imagen_nube", p.imagenNube)
                })
            }
            db.delete("usuarios", null, null)
            DatosUf.usuarios.forEach { u ->
                db.insertOrThrow("usuarios", null, ContentValues().apply {
                    put("id", u.id); put("nombre", u.nombre); put("correo", u.correo)
                    put("rol", u.rol); put("activo", if (u.activo) 1 else 0)
                    put("telefono", u.telefono); put("direccion", u.direccion); put("uid", u.uid)
                })
            }
            db.delete("carrito", null, null)
            DatosUf.carrito.forEach { i ->
                db.insertOrThrow("carrito", null, ContentValues().apply {
                    put("prenda_id", i.prendaId); put("talla", i.talla)
                    put("color", i.color); put("cantidad", i.cant)
                })
            }
            db.delete("pedidos", null, null)
            DatosUf.pedidos.forEach { p ->
                db.insertOrThrow("pedidos", null, ContentValues().apply {
                    put("id", p.id); put("cliente", p.cliente); put("detalle", p.detalle)
                    put("total", p.total); put("estado", p.estado); put("uid", p.uid)
                    put("direccion", p.direccion); put("fecha", p.fecha); put("ubicacion", p.ubicacion)
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
