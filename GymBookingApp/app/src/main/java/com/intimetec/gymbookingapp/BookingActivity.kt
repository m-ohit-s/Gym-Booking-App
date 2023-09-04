package com.intimetec.gymbookingapp

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.gms.tasks.Task

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.actionCodeSettings
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.intimetec.gymbookingapp.databinding.ActivityBookingBinding
import kotlinx.coroutines.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.typeOf


class BookingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookingBinding
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore
    private lateinit var dateOfBooking: Map<String, Any>
    val details = mutableMapOf<String, String>()
    var slots = mutableMapOf<String?, Any?>()
    private lateinit var database: Task<QuerySnapshot>
    private lateinit var slotDetails: MutableList<QueryDocumentSnapshot>

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        slotDetails = mutableListOf()
        dateOfBooking = mutableMapOf()
        binding.button.setOnClickListener {
            auth = FirebaseAuth.getInstance()
            auth.signOut()
            Intent(this, LoginActivity::class.java).also {
                startActivity(it)
                finish()
            }
        }

        val username = auth.currentUser?.email.toString()

        database = readDatabaseUsers(auth.currentUser?.email.toString())

        CoroutineScope(Dispatchers.IO).launch {
            async {
                database.addOnCompleteListener { task->
                    task.addOnSuccessListener { result->
                        for(doc in result){
                            details.put("name", doc.data["name"].toString())
                            details.put("email", doc.data["email"].toString())
                            details.put("location", doc.data["location"].toString())
                            details.put("slot", doc.data["slotvalue"].toString())
                            details.put("bookingMonth", doc.data["DateOfBooking"]
                                .toString())

//                            dateOfBooking = doc.data["DateOfBooking"] as Map<String, Any>

                            updateUI(details)
                        }
                        Log.d("DatabaseBookingActivity", details.toString())
                    }
                    task.addOnFailureListener{
                        Log.d("DatabaseBookingActivity", it.toString())
                        Toast.makeText(this@BookingActivity, "Something Wrong In Fetching Database", Toast.LENGTH_LONG).show()
                    }
                }
            }.await().addOnCompleteListener {
                readDatabaseSlots()
                    .addOnCompleteListener {
                    it.addOnSuccessListener {
                        for(doc in it){
                            slots = doc.data
                            Log.d("DatabaseBookingActivity", doc.data.toString())
                            Log.d("DatabaseBookingActivity", details["location"].toString())
                            Log.d("DatabaseBookingActivity", slots[details["location"].toString()].toString())
                            var slotsAtLocation = slots[details["location"].toString()] as Map<String, Int>
                            Log.d("DatabaseBookingActivity", slotsAtLocation.toString())
                            slotsAtLocation = slotsAtLocation.filter { (key, value)-> value<20 }
                            val slotsSpin = slotsAtLocation.keys
                            Log.d("DatabaseBookingActivity", slotsSpin.toString())
                            val spinner = binding.spinnerTimeSlots
                            slotsSpin.toTypedArray()
                            val adapter = ArrayAdapter(this@BookingActivity, android.R.layout.simple_spinner_dropdown_item, slotsSpin.toTypedArray())
                            spinner.adapter = adapter
                        }
                    }
                    it.addOnFailureListener {
                        Log.d("DatabaseBookingActivity", it.toString())
                    }
                }
            }
            Log.d("DatabaseBookingActivity", slots.keys.toString())

            Log.d("DatabaseBookingActivity", slotDetails.toString())

        }

        Log.d("DatabaseBookingActivity", dateOfBooking.toString())

        updateUI(details)

        var availableSlots = mutableMapOf<String, Int>()

//        details["location"]?.let {
//            readDatabaseSlots(it)
//                .addOnCompleteListener { task->
//                    task.addOnSuccessListener { result->
//                        Log.d("DatabaseBookingActivity", "ABC")
//                    }
//                    task.addOnFailureListener{
//                        Log.d("DatabaseBookingActivity", it.toString())
//                    }
//                }
//        }


        Log.d("DatabaseBookingActivity", availableSlots.toString())

        binding.bookBtn.setOnClickListener {
            try {
                addSlotToFirebase(username)
                val bookingDateAndTime = getCurrentDateAndTime()
                addEditDateToFirebase()
                sendEmail(username)
                Toast.makeText(this, "Slot has been booked check mail", Toast.LENGTH_LONG).show()
//                updateSlotDB(details["location"].toString(), details["slot"])
            }catch (e: Exception){
                Log.d("DatabaseBookingActivity", e.toString())
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }
            updateUI(details)
        }
    }

    override fun onStart() {
        super.onStart()
        updateUI(details)
        Log.d("DatabaseBookingActivity", slotDetails.toString())

//        readDatabaseSlots()
//            .addOnCompleteListener {
//                it.addOnSuccessListener {
//                    for(doc in it){
//                        slots = doc.data
//                        Log.d("DatabaseBookingActivity", doc.data.toString())
//                        Log.d("DatabaseBookingActivity", details["location"].toString())
//                        Log.d("DatabaseBookingActivity", slots[details["location"].toString()].toString())
//                        var slotsAtLocation = slots[details["location"].toString()] as Map<String, Int>
//                        Log.d("DatabaseBookingActivity", slotsAtLocation.toString())
//                        slotsAtLocation = slotsAtLocation.filter { (key, value)-> value <20 }
//                        val slotsSpin = slotsAtLocation.keys
//                        Log.d("DatabaseBookingActivity", slotsSpin.toString())
//                        val spinner = binding.spinnerTimeSlots
//                        slotsSpin.toTypedArray()
//                        val adapter = ArrayAdapter(this@BookingActivity, android.R.layout.simple_spinner_dropdown_item, slotsSpin.toTypedArray())
//                        spinner.adapter = adapter
//                    }
//                }
//                it.addOnFailureListener {
//                    Log.d("DatabaseBookingActivity", it.toString())
//                }
//            }
    }

    private fun readDatabaseUsers(email: String?): Task<QuerySnapshot> {
        return db.collection("ITTGym")
            .whereEqualTo("email", email)
            .get()
    }

    private fun readDatabaseSlots(): Task<QuerySnapshot> =
        db.collection("LocationSlots").get()

    private fun readDatabaseSlots2(location: String): Task<DocumentSnapshot> =
        db.collection("SlotsInLocation").document(location).get()

    private fun updateUI(map: MutableMap<String, String>){
        binding.tvNameDB.text = map["name"]

        if(map["slot"] == null){
           binding.tvCurrentSlotDB.text = getString(R.string.current_slot_detail)
        }
        else{
            binding.tvCurrentSlotDB.text = map["slot"]
        }

        binding.tvLocationDB.text = map["location"]
        binding.tvEmailDB.text = map["email"]

        Log.d("DatabaseBookingActivity", map["bookingMonth"].toString())
    }

    private fun addSlotToFirebase(email: String?){
        val slotValue = binding.spinnerTimeSlots.selectedItem.toString()
        details["slot"] = slotValue

        val data = hashMapOf(
            "slotvalue" to slotValue
        )

        database.addOnCompleteListener { task->
                task.addOnSuccessListener { result->
                    for(doc in result) {
                        db.collection("ITTGym").document(doc.id).set(
                            data,
                            SetOptions.merge()
                        )
                    }
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentDateAndTime(): LocalDateTime{
        val current = LocalDateTime.now()
        Log.d("DatabaseBookingActivity", current.toString())
        return current
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addEditDateToFirebase(){
        val dateOfBooking = hashMapOf(
            "DateOfBooking" to getCurrentDateAndTime()
        )
        database.addOnCompleteListener { task->
            task.addOnSuccessListener { result->
                for(doc in result){
                    db.collection("ITTGym").document(doc.id).set(
                        dateOfBooking,
                        SetOptions.merge()
                    )
                }
            }
        }
    }

    private fun sendEmail(email: String){
        val actionCodeSettings = actionCodeSettings {
            url = "https://gymbookingitt-6a49d.firebaseapp.com"
            handleCodeInApp = true
            setAndroidPackageName(
                "com.intimetec.android",
                false,
                "12"
            )
        }
        auth = FirebaseAuth.getInstance()
        auth.sendPasswordResetEmail(email, actionCodeSettings)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    Log.d("DatabaseBookingActivity", "Email Sent")
                }
            }
            .addOnFailureListener {
                Log.d("DatabaseBookingActivity", it.toString())
            }
    }

    private fun updateSlotDB(location: String, slot: String?){
        val readSlot = db.collection("LocationSlots").get()
            readSlot.addOnCompleteListener { task->
                task.addOnSuccessListener { result->
                    for(doc in result){
                        var slotMap = doc.data as Map<String, Any>

                        var slotData = (slotMap[location] as Map<String, Int>).toMutableMap()
                        slotData[slot]?.let {
                            slotData["$slot"] = slotData[slot]!! + 1
                        }

                        Log.d("SlotMap", slotData.toString())
                    }
                }
            }
    }
}