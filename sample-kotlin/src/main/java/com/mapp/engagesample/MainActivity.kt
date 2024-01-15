package com.mapp.engagesample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import eu.brrm.shared_ui.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.name

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navigate(BaseTestFragment())
    }

    fun <T : Fragment> navigate(fragment: T) {
        supportFragmentManager.beginTransaction()
            .addToBackStack(fragment.javaClass.name)
            .add(fragment, fragment.javaClass.name)
            .commit()
    }
}