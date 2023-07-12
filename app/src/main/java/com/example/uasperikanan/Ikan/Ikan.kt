package com.example.uasperikanan.Ikan

import com.google.firebase.database.Exclude

data class Ikan(
    val id: String? = null,
    val nama_ikan: String? = null,
    var jenis_ikan: String? = null,
    val jml_ikan: Int? = null,
    val tgl_perawatan: String? = null,
) {

    @Exclude
    fun getMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "nama_ikan" to nama_ikan,
            "jenis_ikan" to jenis_ikan,
            "jumlah_ikan" to jml_ikan,
            "tgl_perawatan" to tgl_perawatan,
        )
    }
}
