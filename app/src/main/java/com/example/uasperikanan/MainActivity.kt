package com.example.uasperikanan

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.uasperikanan.Ikan.AddIkanActivity
import com.example.uasperikanan.Ikan.Ikan
import com.example.uasperikanan.Ikan.IkanAdapter
import com.example.uasperikanan.auth.SettingsActivity
import com.example.uasperikanan.databinding.ActivityMainBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


private val ViewBinding.ikanListView: RecyclerView
    get() {
        TODO("Not yet implemented")
    }

class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var firebaseDataBase: FirebaseDatabase? = null
    private var databaseReference: DatabaseReference? = null

    private lateinit var ikanRecyclerView: RecyclerView
    private lateinit var ikanArrayList: ArrayList<Ikan>
    private lateinit var IkanAdapter: IkanAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ikanRecyclerView = binding.ikanListView
        ikanRecyclerView.layoutManager = LinearLayoutManager(this)
        ikanRecyclerView.setHasFixedSize(true)

        ikanArrayList = arrayListOf()
        IkanAdapter = IkanAdapter(ikanArrayList)

        firebaseDataBase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDataBase?.getReference("data_user")

        load_data()

        binding.btnAddIkan.setOnClickListener {
            val intentMain = Intent(this, AddIkanActivity::class.java)
            startActivity(intentMain)
        }

        swipeDelete()
        binding.txtSearchIkan.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val keyword = binding.txtSearchIkan.text.toString()
                if(keyword.isNotEmpty()) {
                    search_data(keyword)
                }
                else{
                    load_data()
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })


        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.nav_bottom_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_setting -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }



    }
    private fun load_data() {
        ikanArrayList.clear()
        db = FirebaseFirestore.getInstance()
        db.collection("ikan").
        addSnapshotListener(object : EventListener<QuerySnapshot> {
            override fun onEvent(
                value: QuerySnapshot?,
                error: FirebaseFirestoreException?
            ) {
                if (error != null){
                    Log.e("Firestore Error", error.message.toString())
                    return
                }
                for (dc:DocumentChange in value?.documentChanges!!){
                    if(dc.type == DocumentChange.Type.ADDED)
                        ikanArrayList.add(dc.document.toObject(Ikan::class.java))
                }
                IkanAdapter.notifyDataSetChanged()
            }
        })
    }
    private fun search_data(keyword :String) {
        ikanArrayList.clear()

        db = FirebaseFirestore.getInstance()

        val query = db.collection("ikan")
            .orderBy("nama")
            .startAt(keyword)
            .get()
        query.addOnSuccessListener {
            ikanArrayList.clear()
            for (document in it) {
                ikanArrayList.add(document.toObject(Ikan::class.java))
            }
        }
    }

    private fun deleteIkan(Ikan: Ikan, doc_id: String) {

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Apakah ${Ikan.nama_ikan} ingin dihapus ?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                lifecycleScope.launch {
                    db.collection("ikan")
                        .document(doc_id).delete()
                    deleteFoto("img_ikan/${Ikan.nama_ikan}.jpg")
                    Toast.makeText(
                        applicationContext,
                        Ikan.nama_ikan.toString() + " is deleted",
                        Toast.LENGTH_LONG
                    ).show()
                    load_data()
                }
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
                load_data()
            }
        val alert = builder.create()
        alert.show()

    }

    private fun swipeDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.RIGHT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                lifecycleScope.launch {
                    val Ikan = ikanArrayList[position]
                    val personQuery = db.collection("ikan")
                        .whereEqualTo("nama_ikan", Ikan.nama_ikan)
                        .whereEqualTo("jenis_ikan", Ikan.jenis_ikan)
                        .whereEqualTo("jumlah_ikan", Ikan.jml_ikan)
                        .whereEqualTo("tgl_perawatan", Ikan.tgl_perawatan)
                        .get()
                        .await()

                    if (personQuery.documents.isNotEmpty()) {
                        for (document in personQuery) {
                            try {
                                deleteIkan(Ikan, document.id)
                                load_data()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        applicationContext,
                                        e.message.toString(),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                applicationContext,
                                "User yang ingin di hapus tidak ditemukan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }).attachToRecyclerView(ikanRecyclerView)
    }

    private fun deleteFoto(file_name: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val deleteFileRef = storageRef.child(file_name)
        if (deleteFileRef != null) {
            deleteFileRef.delete().addOnSuccessListener {
                Log.e("deleted", "success")
            }.addOnFailureListener {
                Log.e("deleted", "failed")
            }
        }
    }


}



