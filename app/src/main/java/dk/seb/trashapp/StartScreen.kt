package dk.seb.trashapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner

class StartScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_screen)
        var selectedItem = ""
        val spinner: Spinner = findViewById(R.id.spinner)
        ArrayAdapter.createFromResource(this,
        R.array.kommuner,
        android.R.layout.simple_spinner_item
        ).also {
            adapter -> adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedItem = spinner.getItemAtPosition(p2).toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            val sp = getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
            var editor = sp.edit()
            editor.putString("kommune", selectedItem)
            editor.commit()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}