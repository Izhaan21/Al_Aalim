package com.example.al_aalim

import android.widget.Filter
import android.widget.Filterable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class Surah(
    val number: Int,
    val name: String,
    val meaning: String,
    val arabicName: String
)

class SurahAdapter(
    private val allSurahs: List<Surah>,
    private val onSurahClick: ((Surah) -> Unit)? = null
) : RecyclerView.Adapter<SurahAdapter.SurahViewHolder>(), Filterable {

    // Tracks which surah numbers are favourited (shared state)
    private val favourites = mutableSetOf<Int>()

    // Currently displayed list (search-filtered + optionally favourites-only)
    private var displayList: List<Surah> = allSurahs.toList()
    private var showFavouritesOnly = false
    private var lastQuery: CharSequence = ""

    inner class SurahViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNumber: TextView   = itemView.findViewById(R.id.tv_surah_number)
        val tvName: TextView     = itemView.findViewById(R.id.tv_surah_name)
        val tvMeaning: TextView  = itemView.findViewById(R.id.tv_surah_meaning)
        val tvArabic: TextView   = itemView.findViewById(R.id.tv_surah_arabic)
        val ivHeart: ImageView   = itemView.findViewById(R.id.iv_favourite)
        val rowContainer: android.widget.RelativeLayout = itemView.findViewById(R.id.row_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurahViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_surah, parent, false)
        return SurahViewHolder(view)
    }

    override fun onBindViewHolder(holder: SurahViewHolder, position: Int) {
        val surah = displayList[position]
        holder.tvNumber.text  = surah.number.toString()
        holder.tvName.text    = surah.name
        holder.tvMeaning.text = surah.meaning
        holder.tvArabic.text  = surah.arabicName

        // Heart state
        updateHeartIcon(holder.ivHeart, surah.number)

        // Heart click — toggle favourite
        holder.ivHeart.setOnClickListener {
            if (favourites.contains(surah.number)) {
                favourites.remove(surah.number)
            } else {
                favourites.add(surah.number)
            }
            updateHeartIcon(holder.ivHeart, surah.number)

            // If we're in Favourites tab, refresh list immediately
            if (showFavouritesOnly) applyFilters()
        }

        // Row click → open SurahReaderActivity
        holder.rowContainer.setOnClickListener {
            val ctx = holder.itemView.context
            val intent = android.content.Intent(ctx, SurahReaderActivity::class.java).apply {
                putExtra(SurahReaderActivity.EXTRA_SURAH_NUMBER,  surah.number)
                putExtra(SurahReaderActivity.EXTRA_SURAH_NAME,    surah.name)
                putExtra(SurahReaderActivity.EXTRA_SURAH_MEANING, surah.meaning)
            }
            ctx.startActivity(intent)
        }
    }

    private fun updateHeartIcon(iv: ImageView, surahNumber: Int) {
        if (favourites.contains(surahNumber)) {
            iv.setImageResource(R.drawable.ic_heart)       // filled gold
            iv.setColorFilter(
                ContextCompat.getColor(iv.context, R.color.gold)
            )
        } else {
            iv.setImageResource(R.drawable.ic_heart_outline) // outline white
            iv.clearColorFilter()
        }
    }

    override fun getItemCount(): Int = displayList.size

    // ----- Filtering -----

    /** Call this when the Favourites tab is selected/deselected. */
    fun setFavouritesOnly(onlyFavs: Boolean) {
        showFavouritesOnly = onlyFavs
        applyFilters()
    }

    fun getFavouritesCount(): Int = favourites.size

    private fun applyFilters() {
        val base = if (showFavouritesOnly) {
            allSurahs.filter { favourites.contains(it.number) }
        } else {
            allSurahs.toList()
        }
        val q = lastQuery.toString().trim()
        displayList = if (q.isEmpty()) {
            base
        } else {
            base.filter { s ->
                s.number.toString() == q ||
                    s.name.contains(q, ignoreCase = true) ||
                    s.meaning.contains(q, ignoreCase = true) ||
                    s.arabicName.contains(q, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            lastQuery = constraint ?: ""
            val base = if (showFavouritesOnly) {
                allSurahs.filter { favourites.contains(it.number) }
            } else {
                allSurahs.toList()
            }
            val q = lastQuery.toString().trim()
            val results = FilterResults()
            results.values = if (q.isEmpty()) base else base.filter { s ->
                s.number.toString() == q ||
                    s.name.contains(q, ignoreCase = true) ||
                    s.meaning.contains(q, ignoreCase = true) ||
                    s.arabicName.contains(q, ignoreCase = true)
            }
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            displayList = (results?.values as? List<Surah>) ?: emptyList()
            notifyDataSetChanged()
        }
    }

    companion object {
        fun getAllSurahs(): List<Surah> = listOf(
            Surah(1, "Surah Al-Fatihah", "The Opening", "الفاتحة"),
            Surah(2, "Surah Al-Baqarah", "The Cow", "البقرة"),
            Surah(3, "Surah Ali 'Imran", "Family of Imran", "آل عمران"),
            Surah(4, "Surah An-Nisa'", "The Women", "النساء"),
            Surah(5, "Surah Al-Ma'idah", "The Table Spread", "المائدة"),
            Surah(6, "Surah Al-An'am", "The Cattle", "الأنعام"),
            Surah(7, "Surah Al-A'raf", "The Heights", "الأعراف"),
            Surah(8, "Surah Al-Anfal", "The Spoils of War", "الأنفال"),
            Surah(9, "Surah At-Tawbah", "The Repentance", "التوبة"),
            Surah(10, "Surah Yunus", "Jonah", "يونس"),
            Surah(11, "Surah Hud", "Hud", "هود"),
            Surah(12, "Surah Yusuf", "Joseph", "يوسف"),
            Surah(13, "Surah Ar-Ra'd", "The Thunder", "الرعد"),
            Surah(14, "Surah Ibrahim", "Abraham", "إبراهيم"),
            Surah(15, "Surah Al-Hijr", "The Rocky Tract", "الحجر"),
            Surah(16, "Surah An-Nahl", "The Bee", "النحل"),
            Surah(17, "Surah Al-Isra'", "The Night Journey", "الإسراء"),
            Surah(18, "Surah Al-Kahf", "The Cave", "الكهف"),
            Surah(19, "Surah Maryam", "Mary", "مريم"),
            Surah(20, "Surah Ta-Ha", "Ta-Ha", "طه"),
            Surah(21, "Surah Al-Anbiya'", "The Prophets", "الأنبياء"),
            Surah(22, "Surah Al-Hajj", "The Pilgrimage", "الحج"),
            Surah(23, "Surah Al-Mu'minun", "The Believers", "المؤمنون"),
            Surah(24, "Surah An-Nur", "The Light", "النور"),
            Surah(25, "Surah Al-Furqan", "The Criterion", "الفرقان"),
            Surah(26, "Surah Ash-Shu'ara'", "The Poets", "الشعراء"),
            Surah(27, "Surah An-Naml", "The Ant", "النمل"),
            Surah(28, "Surah Al-Qasas", "The Stories", "القصص"),
            Surah(29, "Surah Al-'Ankabut", "The Spider", "العنكبوت"),
            Surah(30, "Surah Ar-Rum", "The Romans", "الروم"),
            Surah(31, "Surah Luqman", "Luqman", "لقمان"),
            Surah(32, "Surah As-Sajdah", "The Prostration", "السجدة"),
            Surah(33, "Surah Al-Ahzab", "The Combined Forces", "الأحزاب"),
            Surah(34, "Surah Saba'", "Sheba", "سبأ"),
            Surah(35, "Surah Fatir", "Originator", "فاطر"),
            Surah(36, "Surah Ya-Sin", "Ya-Sin", "يس"),
            Surah(37, "Surah As-Saffat", "Those Ranged in Ranks", "الصافات"),
            Surah(38, "Surah Sad", "Sad", "ص"),
            Surah(39, "Surah Az-Zumar", "The Groups", "الزمر"),
            Surah(40, "Surah Ghafir", "The Forgiver", "غافر"),
            Surah(41, "Surah Fussilat", "Explained in Detail", "فصلت"),
            Surah(42, "Surah Ash-Shura", "The Consultation", "الشورى"),
            Surah(43, "Surah Az-Zukhruf", "The Ornaments of Gold", "الزخرف"),
            Surah(44, "Surah Ad-Dukhan", "The Smoke", "الدخان"),
            Surah(45, "Surah Al-Jathiyah", "The Crouching", "الجاثية"),
            Surah(46, "Surah Al-Ahqaf", "The Wind-Curved Sandhills", "الأحقاف"),
            Surah(47, "Surah Muhammad", "Muhammad", "محمد"),
            Surah(48, "Surah Al-Fath", "The Victory", "الفتح"),
            Surah(49, "Surah Al-Hujurat", "The Rooms", "الحجرات"),
            Surah(50, "Surah Qaf", "Qaf", "ق"),
            Surah(51, "Surah Adh-Dhariyat", "The Winnowing Winds", "الذاريات"),
            Surah(52, "Surah At-Tur", "The Mount", "الطور"),
            Surah(53, "Surah An-Najm", "The Star", "النجم"),
            Surah(54, "Surah Al-Qamar", "The Moon", "القمر"),
            Surah(55, "Surah Ar-Rahman", "The Beneficent", "الرحمن"),
            Surah(56, "Surah Al-Waqi'ah", "The Inevitable", "الواقعة"),
            Surah(57, "Surah Al-Hadid", "The Iron", "الحديد"),
            Surah(58, "Surah Al-Mujadila", "The Pleading Woman", "المجادلة"),
            Surah(59, "Surah Al-Hashr", "The Exile", "الحشر"),
            Surah(60, "Surah Al-Mumtahanah", "She That Is to Be Examined", "الممتحنة"),
            Surah(61, "Surah As-Saf", "The Ranks", "الصف"),
            Surah(62, "Surah Al-Jumu'ah", "The Congregation", "الجمعة"),
            Surah(63, "Surah Al-Munafiqun", "The Hypocrites", "المنافقون"),
            Surah(64, "Surah At-Taghabun", "The Mutual Disillusion", "التغابن"),
            Surah(65, "Surah At-Talaq", "The Divorce", "الطلاق"),
            Surah(66, "Surah At-Tahrim", "The Prohibition", "التحريم"),
            Surah(67, "Surah Al-Mulk", "The Sovereignty", "الملك"),
            Surah(68, "Surah Al-Qalam", "The Pen", "القلم"),
            Surah(69, "Surah Al-Haqqah", "The Reality", "الحاقة"),
            Surah(70, "Surah Al-Ma'arij", "The Ascending Stairways", "المعارج"),
            Surah(71, "Surah Nuh", "Noah", "نوح"),
            Surah(72, "Surah Al-Jinn", "The Jinn", "الجن"),
            Surah(73, "Surah Al-Muzzammil", "The Enshrouded One", "المزمل"),
            Surah(74, "Surah Al-Muddaththir", "The Cloaked One", "المدثر"),
            Surah(75, "Surah Al-Qiyamah", "The Resurrection", "القيامة"),
            Surah(76, "Surah Al-Insan", "The Human", "الإنسان"),
            Surah(77, "Surah Al-Mursalat", "The Emissaries", "المرسلات"),
            Surah(78, "Surah An-Naba'", "The Tidings", "النبأ"),
            Surah(79, "Surah An-Nazi'at", "Those Who Drag Forth", "النازعات"),
            Surah(80, "Surah 'Abasa", "He Frowned", "عبس"),
            Surah(81, "Surah At-Takwir", "The Overthrowing", "التكوير"),
            Surah(82, "Surah Al-Infitar", "The Cleaving", "الانفطار"),
            Surah(83, "Surah Al-Mutaffifin", "The Defrauding", "المطففين"),
            Surah(84, "Surah Al-Inshiqaq", "The Sundering", "الانشقاق"),
            Surah(85, "Surah Al-Buruj", "The Mansions of the Stars", "البروج"),
            Surah(86, "Surah At-Tariq", "The Morning Star", "الطارق"),
            Surah(87, "Surah Al-A'la", "The Most High", "الأعلى"),
            Surah(88, "Surah Al-Ghashiyah", "The Overwhelming", "الغاشية"),
            Surah(89, "Surah Al-Fajr", "The Dawn", "الفجر"),
            Surah(90, "Surah Al-Balad", "The City", "البلد"),
            Surah(91, "Surah Ash-Shams", "The Sun", "الشمس"),
            Surah(92, "Surah Al-Layl", "The Night", "الليل"),
            Surah(93, "Surah Ad-Duha", "The Morning Hours", "الضحى"),
            Surah(94, "Surah Ash-Sharh", "The Relief", "الشرح"),
            Surah(95, "Surah At-Tin", "The Fig", "التين"),
            Surah(96, "Surah Al-'Alaq", "The Clot", "العلق"),
            Surah(97, "Surah Al-Qadr", "The Power", "القدر"),
            Surah(98, "Surah Al-Bayyinah", "The Clear Proof", "البينة"),
            Surah(99, "Surah Az-Zalzalah", "The Earthquake", "الزلزلة"),
            Surah(100, "Surah Al-'Adiyat", "The Courser", "العاديات"),
            Surah(101, "Surah Al-Qari'ah", "The Calamity", "القارعة"),
            Surah(102, "Surah At-Takathur", "The Rivalry in World Increase", "التكاثر"),
            Surah(103, "Surah Al-'Asr", "The Declining Day", "العصر"),
            Surah(104, "Surah Al-Humazah", "The Traducer", "الهمزة"),
            Surah(105, "Surah Al-Fil", "The Elephant", "الفيل"),
            Surah(106, "Surah Quraysh", "Quraysh", "قريش"),
            Surah(107, "Surah Al-Ma'un", "The Small Kindnesses", "الماعون"),
            Surah(108, "Surah Al-Kawthar", "The Abundance", "الكوثر"),
            Surah(109, "Surah Al-Kafirun", "The Disbelievers", "الكافرون"),
            Surah(110, "Surah An-Nasr", "The Divine Support", "النصر"),
            Surah(111, "Surah Al-Masad", "The Palm Fiber", "المسد"),
            Surah(112, "Surah Al-Ikhlas", "The Sincerity", "الإخلاص"),
            Surah(113, "Surah Al-Falaq", "The Daybreak", "الفلق"),
            Surah(114, "Surah An-Nas", "Mankind", "الناس")
        )
    }
}
