package com.example.uasperikanan.Ikan

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.uasperikanan.R
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class IkanAdapter(private val ikanList: ArrayList<Ikan>) : RecyclerView.Adapter<IkanAdapter.IkanViewHolder>() {

    private lateinit var activity: AppCompatActivity
    class IkanViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val nama_ikan: TextView = itemView.findViewById(R.id.TVLNamaIkan)
        val jenis_ikan: TextView = itemView.findViewById(R.id.TVLJenisIkan)
        val jml_ikan: TextView = itemView.findViewById(R.id.TVLJumlahIkan)

        val img_ikan: ImageView = itemView.findViewById(R.id.ImageListUser)
        val btn_info: ImageView = itemView.findViewById(R.id.BTNInfoIkan)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IkanViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.ikan_list_layout, parent, false)
        return IkanViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: IkanViewHolder, position: Int) {
        val ikan = ikanList[position]
        holder.nama_ikan.text = ikan.nama_ikan.toString()
        holder.jenis_ikan.text = ikan.jenis_ikan.toString()
        holder.jml_ikan.text = ikan.jml_ikan.toString()

        holder.itemView.setOnClickListener {
            activity = it.context as AppCompatActivity
            activity.startActivity(Intent(activity, EditIkanActivity::class.java).apply{
                putExtra("nama_ikan", ikan.nama_ikan.toString())
                putExtra("jenis_ikan", ikan.jenis_ikan.toString())
                putExtra("jumlah_ikan",ikan.jml_ikan.toString())
                putExtra("tanggal_perawatan", ikan.tgl_perawatan.toString())
            })
        }
        holder.btn_info.setOnClickListener{
            activity = it.context as AppCompatActivity
            activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.include, ViewIkanFragment(ikan))
                .commit()
        }

        val storageRef = FirebaseStorage.getInstance().reference.child("img_ikan/${ikan.nama_ikan}_${ikan.jenis_ikan}.jpg")
        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.img_ikan.setImageBitmap(bitmap)
        }.addOnFailureListener{
            Log.e("foto ?", "gagal")
        }
    }


    override fun getItemCount(): Int {
        return ikanList.size
    }
}