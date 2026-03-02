package com.example.al_aalim

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.al_aalim.adapter.ReciterAdapter
import com.example.al_aalim.model.Reciter

class ReciterSelectionActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var recitersList: RecyclerView
    private lateinit var btnBack: ImageView
    private lateinit var reciterAdapter: ReciterAdapter
    private lateinit var sharedPreferences: android.content.SharedPreferences

    private val reciters = listOf(
        Reciter(
            id = "alafasy",
            nameEnglish = "Mishary Rashid Alafasy",
            country = "Kuwait",
            photoUrl = "https://static.qurancdn.com/images/reciters/6/mishary-rashid-alafasy-profile.jpeg"
        ),
        Reciter(
            id = "sudais",
            nameEnglish = "Abdul Rahman Al-Sudais",
            country = "Saudi Arabia",
            photoUrl = "https://static.qurancdn.com/images/reciters/2/abdul-rahman-al-sudais-profile.jpeg"
        ),
        Reciter(
            id = "shatri",
            nameEnglish = "Abu Bakr Al-Shatri",
            country = "Saudi Arabia",
            photoUrl = "https://static.qurancdn.com/images/reciters/3/abu-bakr-al-shatri-pofile.jpeg"
        ),
        Reciter(
            id = "shuraym",
            nameEnglish = "Saood Ash-Shuraym",
            country = "Saudi Arabia",
            photoUrl = "https://static.qurancdn.com/images/reciters/8/saoud-shuraim-profile.jpeg"
        ),
        Reciter(
            id = "husary",
            nameEnglish = "Mahmoud Khalil Al-Husary",
            country = "Egypt",
            photoUrl = "https://static.qurancdn.com/images/reciters/5/mahmoud-khalil-al-hussary-profile.png"
        ),
        Reciter(
            id = "muaiqly",
            nameEnglish = "Maher Al-Muaiqly",
            country = "Saudi Arabia",
            photoUrl = "https://static.qurancdn.com/images/reciters/25/Maher-al-Muaiqly-profile.png"
        ),
        Reciter(
            id = "ghamdi",
            nameEnglish = "Saad Al-Ghamdi",
            country = "Saudi Arabia",
            photoUrl = "https://static.qurancdn.com/images/reciters/16/saad-al-ghamdi-profile.png"
        ),
        Reciter(
            id = "abdulbasit",
            nameEnglish = "Abdul Basit Abdul Samad",
            country = "Egypt",
            photoUrl = "https://static.qurancdn.com/images/reciters/1/abdelbasset-profile.jpeg"
        ),
        Reciter(
            id = "minshawi",
            nameEnglish = "Muhammad Siddiq Al-Minshawi",
            country = "Egypt",
            photoUrl = "https://static.qurancdn.com/images/reciters/7/mohamed-siddiq-el-minshawi-profile.jpeg"
        ),
        Reciter(
            id = "hanirifai",
            nameEnglish = "Hani Rifai",
            country = "Saudi Arabia",
            photoUrl = "https://static.qurancdn.com/images/reciters/4/hani-ar-rifai-profile.jpeg"
        ),
        Reciter(
            id = "ajmi",
            nameEnglish = "Ahmed Al-Ajmi",
            country = "Saudi Arabia",
            photoUrl = "https://static.qurancdn.com/images/reciters/22/Ahmed-ibn-Ali-al-Ajmy-profile.png"
        ),
        Reciter(
            id = "dosari",
            nameEnglish = "Yasser Al-Dosari",
            country = "Saudi Arabia",
            photoUrl = "https://static.qurancdn.com/images/reciters/20/yasser-profile.png"
        )
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_reciter_selection)
        
        // Handle window insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, 0)
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupBackButton()
    }

    private fun initViews() {
        searchInput = findViewById(R.id.search_input)
        recitersList = findViewById(R.id.reciters_list)
        btnBack = findViewById(R.id.btn_back)
    }

    private fun setupRecyclerView() {
        // Load saved reciter
        val savedReciterId = sharedPreferences.getString("selected_reciter", "alafasy")

        reciterAdapter = ReciterAdapter(
            reciters = reciters,
            selectedReciterId = savedReciterId,
            onReciterSelected = { reciter ->
                // Save selection
                sharedPreferences.edit()
                    .putString("selected_reciter", reciter.id)
                    .putString("selected_reciter_name", reciter.nameEnglish)
                    .apply()

                Toast.makeText(
                    this,
                    "Selected: ${reciter.nameEnglish}",
                    Toast.LENGTH_SHORT
                ).show()

                // Return to SurahReaderActivity so the name label & audio update
                setResult(RESULT_OK)
                finish()
            }
        )

        recitersList.layoutManager = GridLayoutManager(this, 2)
        recitersList.adapter = reciterAdapter
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                reciterAdapter.filter(s.toString())
            }
        })
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            finish()
        }
    }
}
