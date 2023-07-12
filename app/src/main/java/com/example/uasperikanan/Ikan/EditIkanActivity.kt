package com.example.uasperikanan.Ikan

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Peternakan.Ikan.Ikan
import com.example.uasperikanan.MainActivity
import com.example.uasperikanan.databinding.ActivityEditIkanBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class EditIkanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditIkanBinding
    private val db = FirebaseFirestore.getInstance()
    private val  REQ_CAM = 101
    private lateinit var imgUri : Uri
    private var dataGambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditIkanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val (year, month, day, curr_Ikan) = setDefaultValue()

        binding.TxtEditTglRawat.setOnClickListener {
            val dpd = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    binding.TxtEditTglRawat.setText("" + year + "-" + monthOfYear + "-" + dayOfMonth)
                }, year.toString().toInt(), month.toString().toInt(), day.toString().toInt()
            )

            dpd.show()
        }
        binding.BtnEditIkan.setOnClickListener {
            val new_data_pasien = newIkan()
            updateIkan(curr_Ikan as Ikan, new_data_pasien)

            val intentMain = Intent(this, MainActivity::class.java)
            startActivity(intentMain)
            finish()
        }

        showFoto()

        binding.BtnImgIkan.setOnClickListener {
            openCamera()
        }

    }

    fun setDefaultValue(): Array<Any> {
        val intent = intent
        val nama_ikan = intent.getStringExtra("nama_ikan").toString()
        val jenis_ikan = intent.getStringExtra("jenis_ikan").toString()
        val jumlah_ikan = intent.getStringExtra("jumlah_ikan").toString()
        val tgl_rawat = intent.getStringExtra("tanggal_perawatan").toString()

        binding.TxtEditNama.setText(nama_ikan)
        binding.TxtEditJenis.setText(jenis_ikan)
        binding.TxtEditJumlah.setText(jumlah_ikan)
        binding.TxtEditTglRawat.setText(tgl_rawat)

        val tgl_split = intent.getStringExtra("tgl_perawatan")
            .toString().split("-").toTypedArray()
        val year = tgl_split[0].toInt()
        val month = tgl_split[1].toInt() - 1
        val day = tgl_split[2].toInt()

        val curr_Ikan = Ikan(nama_ikan, jenis_ikan, jumlah_ikan)
        return arrayOf(year, month, day, curr_Ikan)

    }

    fun newIkan(): Map<String, Any> {
        var nama_ikan: String = binding.TxtEditNama.text.toString()
        var jenis_ikan: String = binding.TxtEditJenis.text.toString()
        var jml_ikan: String = binding.TxtEditJumlah.text.toString()
        var tgl_perawatan: String = binding.TxtEditTglRawat.text.toString()

        if (dataGambar != null) {
            uploadPictFirebase(dataGambar!!, "${nama_ikan}_${jenis_ikan}")
        }

        val ikan = mutableMapOf<String, Any>()
        ikan["nama_ikan"] = nama_ikan
        ikan["jenis_ikan"] = jenis_ikan
        ikan["jumlah_ikan"] = jml_ikan
        ikan["tgl_perawatan"] = tgl_perawatan

        return ikan
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.packageManager?.let {
                intent?.resolveActivity(it).also {
                    startActivityForResult(intent, REQ_CAM)
                }
            }
        }
    }


    private fun updateIkan(ikan: Ikan, newIkanMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personQuery = db.collection("pasien")
                .whereEqualTo("nama_ikan", ikan.nama_ikan)
                .whereEqualTo("jenis_ikan", ikan.jenis_ikan)
                .whereEqualTo("jumlah_ikan", ikan.jumlah_ikan)
                .whereEqualTo("tgl_perawatan", ikan.tgl_perawatan)
                .get()
                .await()

            if(personQuery.documents.isNotEmpty()) {
                for(document in personQuery) {
                    try {
                        db.collection("pasien").document(document.id).set(
                            newIkanMap,
                            SetOptions.merge()
                        )
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@EditIkanActivity,
                                e.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditIkanActivity,
                        "No persons matched the query.", Toast.LENGTH_LONG).show()
                }
            }
        }

    fun showFoto() {
        val intent = intent
        val nik = intent.getStringExtra("nik").toString()
        val nama = intent.getStringExtra("nama").toString()

        val storageRef = FirebaseStorage.getInstance().reference.child("img_pasien/${nik}_${nama}.jpg")
        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.BtnImgIkan.setImageBitmap(bitmap)
        }.addOnFailureListener{
            Log.e("foto ?", "gagal")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == RESULT_OK) {
            dataGambar = data?.extras?.get("data") as Bitmap
            binding.BtnImgIkan.setImageBitmap(dataGambar)
        }
    }

    private fun uploadPictFirebase(img_bitmap: Bitmap, file_name: String) {
        val baos = ByteArrayOutputStream()
        val ref = FirebaseStorage.getInstance().reference.child("img_pasien/${file_name}.jpg")
        img_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val img = baos.toByteArray()
        ref.putBytes(img)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    ref.downloadUrl.addOnCompleteListener { Task ->
                        Task.result.let { Uri ->
                            imgUri = Uri
                            binding.BtnImgIkan.setImageBitmap(img_bitmap)
                        }
                    }
                }
            }
    }}



