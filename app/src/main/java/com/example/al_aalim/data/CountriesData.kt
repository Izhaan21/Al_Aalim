package com.example.al_aalim.data

/**
 * Data class representing a country with its flag, name, and capital coordinates
 */
data class CountryLocation(
    val flag: String,
    val name: String,
    val capital: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Complete list of countries with their flags and capital coordinates
 */
object CountriesData {
    val allCountries = listOf(
        // A
        CountryLocation("🇦🇫", "Afghanistan", "Kabul", 34.5553, 69.2075),
        CountryLocation("🇦🇱", "Albania", "Tirana", 41.3275, 19.8187),
        CountryLocation("🇩🇿", "Algeria", "Algiers", 36.7538, 3.0588),
        CountryLocation("🇦🇩", "Andorra", "Andorra la Vella", 42.5063, 1.5218),
        CountryLocation("🇦🇴", "Angola", "Luanda", -8.8390, 13.2894),
        CountryLocation("🇦🇷", "Argentina", "Buenos Aires", -34.6037, -58.3816),
        CountryLocation("🇦🇲", "Armenia", "Yerevan", 40.1792, 44.4991),
        CountryLocation("🇦🇺", "Australia", "Canberra", -35.2809, 149.1300),
        CountryLocation("🇦🇹", "Austria", "Vienna", 48.2082, 16.3738),
        CountryLocation("🇦🇿", "Azerbaijan", "Baku", 40.4093, 49.8671),
        
        // B
        CountryLocation("🇧🇭", "Bahrain", "Manama", 26.2285, 50.5860),
        CountryLocation("🇧🇩", "Bangladesh", "Dhaka", 23.8103, 90.4125),
        CountryLocation("🇧🇾", "Belarus", "Minsk", 53.9006, 27.5590),
        CountryLocation("🇧🇪", "Belgium", "Brussels", 50.8503, 4.3517),
        CountryLocation("🇧🇯", "Benin", "Porto-Novo", 6.4969, 2.6289),
        CountryLocation("🇧🇹", "Bhutan", "Thimphu", 27.4728, 89.6390),
        CountryLocation("🇧🇴", "Bolivia", "La Paz", -16.4897, -68.1193),
        CountryLocation("🇧🇦", "Bosnia and Herzegovina", "Sarajevo", 43.8563, 18.4131),
        CountryLocation("🇧🇼", "Botswana", "Gaborone", -24.6282, 25.9231),
        CountryLocation("🇧🇷", "Brazil", "Brasília", -15.8267, -47.9218),
        CountryLocation("🇧🇳", "Brunei", "Bandar Seri Begawan", 4.9031, 114.9398),
        CountryLocation("🇧🇬", "Bulgaria", "Sofia", 42.6977, 23.3219),
        CountryLocation("🇧🇫", "Burkina Faso", "Ouagadougou", 12.3714, -1.5197),
        
        // C
        CountryLocation("🇰🇭", "Cambodia", "Phnom Penh", 11.5564, 104.9282),
        CountryLocation("🇨🇲", "Cameroon", "Yaoundé", 3.8480, 11.5021),
        CountryLocation("🇨🇦", "Canada", "Ottawa", 45.4215, -75.6972),
        CountryLocation("🇨🇫", "Central African Republic", "Bangui", 4.3947, 18.5582),
        CountryLocation("🇹🇩", "Chad", "N'Djamena", 12.1348, 15.0557),
        CountryLocation("🇨🇱", "Chile", "Santiago", -33.4489, -70.6693),
        CountryLocation("🇨🇳", "China", "Beijing", 39.9042, 116.4074),
        CountryLocation("🇨🇴", "Colombia", "Bogotá", 4.7110, -74.0721),
        CountryLocation("🇨🇬", "Congo", "Brazzaville", -4.2634, 15.2429),
        CountryLocation("🇨🇷", "Costa Rica", "San José", 9.9281, -84.0907),
        CountryLocation("🇭🇷", "Croatia", "Zagreb", 45.8150, 15.9819),
        CountryLocation("🇨🇺", "Cuba", "Havana", 23.1136, -82.3666),
        CountryLocation("🇨🇾", "Cyprus", "Nicosia", 35.1856, 33.3823),
        CountryLocation("🇨🇿", "Czech Republic", "Prague", 50.0755, 14.4378),
        
        // D
        CountryLocation("🇩🇰", "Denmark", "Copenhagen", 55.6761, 12.5683),
        CountryLocation("🇩🇯", "Djibouti", "Djibouti", 11.5886, 43.1456),
        CountryLocation("🇩🇴", "Dominican Republic", "Santo Domingo", 18.4861, -69.9312),
        
        // E
        CountryLocation("🇪🇨", "Ecuador", "Quito", -0.1807, -78.4678),
        CountryLocation("🇪🇬", "Egypt", "Cairo", 30.0444, 31.2357),
        CountryLocation("🇸🇻", "El Salvador", "San Salvador", 13.6929, -89.2182),
        CountryLocation("🇬🇶", "Equatorial Guinea", "Malabo", 3.7504, 8.7371),
        CountryLocation("🇪🇷", "Eritrea", "Asmara", 15.3229, 38.9251),
        CountryLocation("🇪🇪", "Estonia", "Tallinn", 59.4370, 24.7536),
        CountryLocation("🇪🇹", "Ethiopia", "Addis Ababa", 9.0320, 38.7469),
        
        // F
        CountryLocation("🇫🇯", "Fiji", "Suva", -18.1416, 178.4419),
        CountryLocation("🇫🇮", "Finland", "Helsinki", 60.1699, 24.9384),
        CountryLocation("🇫🇷", "France", "Paris", 48.8566, 2.3522),
        
        // G
        CountryLocation("🇬🇦", "Gabon", "Libreville", 0.4162, 9.4673),
        CountryLocation("🇬🇲", "Gambia", "Banjul", 13.4549, -16.5790),
        CountryLocation("🇬🇪", "Georgia", "Tbilisi", 41.7151, 44.8271),
        CountryLocation("🇩🇪", "Germany", "Berlin", 52.5200, 13.4050),
        CountryLocation("🇬🇭", "Ghana", "Accra", 5.6037, -0.1870),
        CountryLocation("🇬🇷", "Greece", "Athens", 37.9838, 23.7275),
        CountryLocation("🇬🇹", "Guatemala", "Guatemala City", 14.6349, -90.5069),
        CountryLocation("🇬🇳", "Guinea", "Conakry", 9.6412, -13.5784),
        CountryLocation("🇬🇾", "Guyana", "Georgetown", 6.8013, -58.1551),
        
        // H
        CountryLocation("🇭🇹", "Haiti", "Port-au-Prince", 18.5944, -72.3074),
        CountryLocation("🇭🇳", "Honduras", "Tegucigalpa", 14.0723, -87.1921),
        CountryLocation("🇭🇺", "Hungary", "Budapest", 47.4979, 19.0402),
        
        // I
        CountryLocation("🇮🇸", "Iceland", "Reykjavik", 64.1466, -21.9426),
        CountryLocation("🇮🇳", "India", "New Delhi", 28.6139, 77.2090),
        CountryLocation("🇮🇩", "Indonesia", "Jakarta", -6.2088, 106.8456),
        CountryLocation("🇮🇷", "Iran", "Tehran", 35.6892, 51.3890),
        CountryLocation("🇮🇶", "Iraq", "Baghdad", 33.3152, 44.3661),
        CountryLocation("🇮🇪", "Ireland", "Dublin", 53.3498, -6.2603),
        CountryLocation("🇮🇹", "Italy", "Rome", 41.9028, 12.4964),
        CountryLocation("🇨🇮", "Ivory Coast", "Yamoussoukro", 6.8276, -5.2893),
        
        // J
        CountryLocation("🇯🇲", "Jamaica", "Kingston", 17.9714, -76.7936),
        CountryLocation("🇯🇵", "Japan", "Tokyo", 35.6762, 139.6503),
        CountryLocation("🇯🇴", "Jordan", "Amman", 31.9454, 35.9284),
        
        // K
        CountryLocation("🇰🇿", "Kazakhstan", "Astana", 51.1605, 71.4704),
        CountryLocation("🇰🇪", "Kenya", "Nairobi", -1.2921, 36.8219),
        CountryLocation("🇰🇼", "Kuwait", "Kuwait City", 29.3759, 47.9774),
        CountryLocation("🇰🇬", "Kyrgyzstan", "Bishkek", 42.8746, 74.5698),
        
        // L
        CountryLocation("🇱🇦", "Laos", "Vientiane", 17.9757, 102.6331),
        CountryLocation("🇱🇻", "Latvia", "Riga", 56.9496, 24.1052),
        CountryLocation("🇱🇧", "Lebanon", "Beirut", 33.8938, 35.5018),
        CountryLocation("🇱🇸", "Lesotho", "Maseru", -29.3151, 27.4869),
        CountryLocation("🇱🇷", "Liberia", "Monrovia", 6.3156, -10.8074),
        CountryLocation("🇱🇾", "Libya", "Tripoli", 32.8872, 13.1913),
        CountryLocation("🇱🇮", "Liechtenstein", "Vaduz", 47.1410, 9.5209),
        CountryLocation("🇱🇹", "Lithuania", "Vilnius", 54.6872, 25.2797),
        CountryLocation("🇱🇺", "Luxembourg", "Luxembourg", 49.6116, 6.1319),
        
        // M
        CountryLocation("🇲🇰", "North Macedonia", "Skopje", 41.9981, 21.4254),
        CountryLocation("🇲🇬", "Madagascar", "Antananarivo", -18.8792, 47.5079),
        CountryLocation("🇲🇼", "Malawi", "Lilongwe", -13.9626, 33.7741),
        CountryLocation("🇲🇾", "Malaysia", "Kuala Lumpur", 3.1390, 101.6869),
        CountryLocation("🇲🇻", "Maldives", "Malé", 4.1755, 73.5093),
        CountryLocation("🇲🇱", "Mali", "Bamako", 12.6392, -8.0029),
        CountryLocation("🇲🇹", "Malta", "Valletta", 35.8989, 14.5146),
        CountryLocation("🇲🇷", "Mauritania", "Nouakchott", 18.0735, -15.9582),
        CountryLocation("🇲🇺", "Mauritius", "Port Louis", -20.1609, 57.5012),
        CountryLocation("🇲🇽", "Mexico", "Mexico City", 19.4326, -99.1332),
        CountryLocation("🇲🇩", "Moldova", "Chișinău", 47.0105, 28.8638),
        CountryLocation("🇲🇨", "Monaco", "Monaco", 43.7384, 7.4246),
        CountryLocation("🇲🇳", "Mongolia", "Ulaanbaatar", 47.8864, 106.9057),
        CountryLocation("🇲🇪", "Montenegro", "Podgorica", 42.4304, 19.2594),
        CountryLocation("🇲🇦", "Morocco", "Rabat", 34.0209, -6.8416),
        CountryLocation("🇲🇿", "Mozambique", "Maputo", -25.9692, 32.5732),
        CountryLocation("🇲🇲", "Myanmar", "Naypyidaw", 19.7633, 96.0785),
        
        // N
        CountryLocation("🇳🇦", "Namibia", "Windhoek", -22.5609, 17.0658),
        CountryLocation("🇳🇵", "Nepal", "Kathmandu", 27.7172, 85.3240),
        CountryLocation("🇳🇱", "Netherlands", "Amsterdam", 52.3676, 4.9041),
        CountryLocation("🇳🇿", "New Zealand", "Wellington", -41.2866, 174.7756),
        CountryLocation("🇳🇮", "Nicaragua", "Managua", 12.1149, -86.2362),
        CountryLocation("🇳🇪", "Niger", "Niamey", 13.5116, 2.1254),
        CountryLocation("🇳🇬", "Nigeria", "Abuja", 9.0579, 7.4951),
        CountryLocation("🇰🇵", "North Korea", "Pyongyang", 39.0392, 125.7625),
        CountryLocation("🇳🇴", "Norway", "Oslo", 59.9139, 10.7522),
        
        // O
        CountryLocation("🇴🇲", "Oman", "Muscat", 23.5880, 58.3829),
        
        // P
        CountryLocation("🇵🇰", "Pakistan", "Islamabad", 33.6844, 73.0479),
        CountryLocation("🇵🇸", "Palestine", "Ramallah", 31.9038, 35.2034),
        CountryLocation("🇵🇦", "Panama", "Panama City", 9.1012, -79.4025),
        CountryLocation("🇵🇬", "Papua New Guinea", "Port Moresby", -9.4438, 147.1803),
        CountryLocation("🇵🇾", "Paraguay", "Asunción", -25.2637, -57.5759),
        CountryLocation("🇵🇪", "Peru", "Lima", -12.0464, -77.0428),
        CountryLocation("🇵🇭", "Philippines", "Manila", 14.5995, 120.9842),
        CountryLocation("🇵🇱", "Poland", "Warsaw", 52.2297, 21.0122),
        CountryLocation("🇵🇹", "Portugal", "Lisbon", 38.7223, -9.1393),
        
        // Q
        CountryLocation("🇶🇦", "Qatar", "Doha", 25.2854, 51.5310),
        
        // R
        CountryLocation("🇷🇴", "Romania", "Bucharest", 44.4268, 26.1025),
        CountryLocation("🇷🇺", "Russia", "Moscow", 55.7558, 37.6173),
        CountryLocation("🇷🇼", "Rwanda", "Kigali", -1.9403, 29.8739),
        
        // S
        CountryLocation("🇸🇦", "Saudi Arabia", "Riyadh", 24.7136, 46.6753),
        CountryLocation("🇸🇳", "Senegal", "Dakar", 14.7167, -17.4677),
        CountryLocation("🇷🇸", "Serbia", "Belgrade", 44.7866, 20.4489),
        CountryLocation("🇸🇬", "Singapore", "Singapore", 1.3521, 103.8198),
        CountryLocation("🇸🇰", "Slovakia", "Bratislava", 48.1486, 17.1077),
        CountryLocation("🇸🇮", "Slovenia", "Ljubljana", 46.0569, 14.5058),
        CountryLocation("🇸🇴", "Somalia", "Mogadishu", 2.0469, 45.3182),
        CountryLocation("🇿🇦", "South Africa", "Pretoria", -25.7479, 28.2293),
        CountryLocation("🇰🇷", "South Korea", "Seoul", 37.5665, 126.9780),
        CountryLocation("🇸🇸", "South Sudan", "Juba", 4.8594, 31.5713),
        CountryLocation("🇪🇸", "Spain", "Madrid", 40.4168, -3.7038),
        CountryLocation("🇱🇰", "Sri Lanka", "Colombo", 6.9271, 79.8612),
        CountryLocation("🇸🇩", "Sudan", "Khartoum", 15.5007, 32.5599),
        CountryLocation("🇸🇷", "Suriname", "Paramaribo", 5.8520, -55.2038),
        CountryLocation("🇸🇪", "Sweden", "Stockholm", 59.3293, 18.0686),
        CountryLocation("🇨🇭", "Switzerland", "Bern", 46.9480, 7.4474),
        CountryLocation("🇸🇾", "Syria", "Damascus", 33.5138, 36.2765),
        
        // T
        CountryLocation("🇹🇼", "Taiwan", "Taipei", 25.0330, 121.5654),
        CountryLocation("🇹🇯", "Tajikistan", "Dushanbe", 38.5598, 68.7740),
        CountryLocation("🇹🇿", "Tanzania", "Dodoma", -6.1630, 35.7516),
        CountryLocation("🇹🇭", "Thailand", "Bangkok", 13.7563, 100.5018),
        CountryLocation("🇹🇬", "Togo", "Lomé", 6.1256, 1.2254),
        CountryLocation("🇹🇳", "Tunisia", "Tunis", 36.8065, 10.1815),
        CountryLocation("🇹🇷", "Turkey", "Ankara", 39.9334, 32.8597),
        CountryLocation("🇹🇲", "Turkmenistan", "Ashgabat", 37.9601, 58.3261),
        
        // U
        CountryLocation("🇺🇬", "Uganda", "Kampala", 0.3476, 32.5825),
        CountryLocation("🇺🇦", "Ukraine", "Kyiv", 50.4501, 30.5234),
        CountryLocation("🇦🇪", "United Arab Emirates", "Abu Dhabi", 24.4539, 54.3773),
        CountryLocation("🇬🇧", "United Kingdom", "London", 51.5074, -0.1278),
        CountryLocation("🇺🇸", "United States", "Washington D.C.", 38.9072, -77.0369),
        CountryLocation("🇺🇾", "Uruguay", "Montevideo", -34.9011, -56.1645),
        CountryLocation("🇺🇿", "Uzbekistan", "Tashkent", 41.2995, 69.2401),
        
        // V
        CountryLocation("🇻🇦", "Vatican City", "Vatican City", 41.9029, 12.4534),
        CountryLocation("🇻🇪", "Venezuela", "Caracas", 10.4806, -66.9036),
        CountryLocation("🇻🇳", "Vietnam", "Hanoi", 21.0285, 105.8542),
        
        // Y
        CountryLocation("🇾🇪", "Yemen", "Sana'a", 15.3694, 44.1910),
        
        // Z
        CountryLocation("🇿🇲", "Zambia", "Lusaka", -15.3875, 28.3228),
        CountryLocation("🇿🇼", "Zimbabwe", "Harare", -17.8252, 31.0335)
    )
    
    fun searchCountries(query: String): List<CountryLocation> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase().trim()
        return allCountries.filter { 
            it.name.lowercase().contains(lowerQuery) || 
            it.capital.lowercase().contains(lowerQuery)
        }
    }
}
