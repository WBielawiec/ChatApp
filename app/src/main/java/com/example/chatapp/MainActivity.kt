package com.example.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.example.chatapp.databinding.ActivityMainBinding
import com.google.firebase.database.FirebaseDatabase

private lateinit var intent: Intent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if(pref.all["darkTheme"] == false) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        //addNumbersToDatabase()
    }

    private fun addNumbersToDatabase() {
        val numberRef = FirebaseDatabase.getInstance().getReference("/available-numbers/")

        var numbers = mutableListOf<Int>()
        //8889 users can register
        for (i in 111111 .. 119999){
            numbers.add(i)
        }
        numbers.shuffle()
        numberRef.setValue(numbers)
    }

}