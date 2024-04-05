package com.example.navindoor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BlockDetails : AppCompatActivity() {

    private lateinit var blockDetails: Array<String> // Initialize block details array
    private lateinit var viewAllButton: Button // Declare viewAllButton variable
    private lateinit var searchButton: Button // Declare searchButton variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_details)

        // Initialize block details array from resources
        blockDetails = resources.getStringArray(R.array.block_names)

        // Find the search button
        searchButton = findViewById(R.id.searchButton)


        // Set OnClickListener for the search button
        searchButton.setOnClickListener {
            showSearchDialog()
        }

        viewAllButton = findViewById(R.id.viewAllButton)
        viewAllButton.setOnClickListener {
            performSearch("")
        }

    }


    private fun showSearchDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.block_search_dialog, null)

        // Find views in the custom layout
        val editText = dialogView.findViewById<EditText>(R.id.searchEditText)
        val searchButton = dialogView.findViewById<Button>(R.id.searchButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        // Create AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set OnClickListener for search button
        searchButton.setOnClickListener {
            val searchText = editText.text.toString().trim()
            if (searchText.isNotEmpty()) {
                performSearch(searchText)
            }
            dialog.dismiss()
        }

        // Set OnClickListener for cancel button
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }


    private fun performSearch(searchText: String) {
        val filteredBlocks = blockDetails.filter {
            // Normalize both the block name and search query by converting them to lowercase
            val normalizedBlockName = it.toLowerCase().trim()
            val normalizedSearchQuery = searchText.toLowerCase().trim()
            // Remove extra spaces and then check if the normalized block name contains the normalized search query
            normalizedBlockName.replace("\\s+".toRegex(), " ").contains(normalizedSearchQuery)
        }
        // Update UI with filtered blocks
        updateUI(filteredBlocks)
    }

    private fun updateUI(filteredBlocks: List<String>) {
        // Update TextViews with filtered block details
        val blockTextViews = arrayOf(
            R.id.block1TextView, R.id.block2TextView, R.id.block3TextView,
            R.id.block4TextView, R.id.block5TextView, R.id.block6TextView,
            R.id.block7TextView, R.id.block8TextView, R.id.block9TextView,
            R.id.block10TextView, R.id.block11TextView, R.id.block12TextView,
            R.id.block13TextView, R.id.block14TextView, R.id.block15TextView,
            R.id.block16TextView, R.id.block17TextView, R.id.block18TextView,
            R.id.block19TextView, R.id.block20TextView, R.id.block21TextView,
            R.id.block22TextView, R.id.block23TextView, R.id.block24TextView,
            R.id.block25TextView, R.id.block26TextView, R.id.block27TextView,
            R.id.block28TextView, R.id.block29TextView, R.id.block30TextView,
            R.id.block31TextView, R.id.block32TextView, R.id.block33TextView,
            R.id.block34TextView, R.id.block35TextView, R.id.block36TextView,
            R.id.block37TextView, R.id.block38TextView, R.id.block39TextView,
            R.id.block40TextView, R.id.block41TextView, R.id.block42TextView,
            R.id.block43TextView, R.id.block45TextView,
            R.id.block46TextView, R.id.block47TextView,
            R.id.block51TextView,
            R.id.block52TextView, R.id.block53TextView, R.id.block54TextView,
            R.id.block55TextView
        )

        for ((index, textViewId) in blockTextViews.withIndex()) {
            val textView = findViewById<TextView>(textViewId)
            if (index < filteredBlocks.size) {
                textView.text = filteredBlocks[index]
                textView.visibility = View.VISIBLE
            } else {
                textView.visibility = View.GONE
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Navigate to MainActivity
        val intent = Intent(this@BlockDetails, MainActivity::class.java)
        startActivity(intent)
        finish() // Optional: finish current activity
    }

}
