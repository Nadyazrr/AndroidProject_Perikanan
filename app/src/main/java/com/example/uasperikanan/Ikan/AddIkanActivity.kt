package com.example.uasperikanan.Ikan

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.example.uasperikanan.MainActivity
import com.example.uasperikanan.databinding.ActivityAddIkanBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.Calendar

class AddIkanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddIkanBinding
    private val firestoreDatabase =  FirebaseFirestore.getInstance()
    private  val REQ_CAM = 101
    private lateinit var imgUri : Uri
    private var dataGambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddIkanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.TxtAddTglPerawatan.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    binding.TxtAddTglPerawatan.setText("" + year + "-" + monthOfYear + "-" + dayOfMonth)
                }, year, month, day)

            dpd.show()
        }

        binding.BtnAddIkan.setOnClickListener {
            addIkan()
        }

        binding.BtnImgIkan.setOnClickListener {
            openCamera()
        }

    }

    fun addIkan() {
        var nama_ikan : String = binding.TxtAddIkan.text.toString()
        var jenis_ikan : String = binding.TxtAddJnsIkan.text.toString()
        var jumlah_ikan : String = binding.TxtAddJmlIkan.text.toString()

        var tanggal_perawatan : String = binding.TxtAddTglPerawatan.text.toString()

        val ikan: MutableMap<String, Any> = HashMap()
        ikan["nama_ikan"] = nama_ikan
        ikan["jenis_ikan"] = jenis_ikan
        ikan["jumlah_ikan"] = jumlah_ikan
        ikan["tanggal_perawatan"] = tanggal_perawatan


        if (dataGambar != null) {
            uploadPictFirebase(dataGambar!!, "${nama_ikan} ${jenis_ikan}")

            firestoreDatabase.collection("ikan").add(ikan)
                .addOnSuccessListener {
                    val intentMain = Intent(this, MainActivity::class.java)
                    startActivity(intentMain)
                }
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == RESULT_OK) {
            dataGambar = data?.extras?.get("data") as Bitmap
            binding.BtnImgIkan.setImageBitmap(dataGambar)
        }
    }

    private fun uploadPictFirebase(img_bitmap: Bitmap, file_name: String) {
        val baos = ByteArrayOutputStream()
        val ref = FirebaseStorage.getInstance().reference.child("img_ikan/${file_name}.jpg")
        img_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val img = baos.toByteArray()
        ref.putBytes(img)
            .addOnCompleteListener {
                if(it.isSuccessful) {
                    ref.downloadUrl.addOnCompleteListener { Task ->
                        Task.result.let { Uri ->
                            imgUri = Uri
                            binding.BtnImgIkan.setImageBitmap(img_bitmap)
                        }
                    }
                }
            }
    }
}
