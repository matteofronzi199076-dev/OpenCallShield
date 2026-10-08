package com.opencallshield.data

/** Un pais con su bandera (emoji) y su prefijo telefonico internacional. */
data class Country(val name: String, val flag: String, val dialCode: String)

/**
 * Catalogo curado de paises frecuentemente asociados a llamadas SPAM internacionales,
 * mas los principales, para que el usuario bloquee por pais sin escribir prefijos.
 * El `dialCode` (con +) es lo que se guarda en la lista negra de prefijos.
 */
object Countries {
    val ALL: List<Country> = listOf(
        Country("Nigeria", "🇳🇬", "+234"),
        Country("India", "🇮🇳", "+91"),
        Country("Indonesia", "🇮🇩", "+62"),
        Country("Costa d’Avorio", "🇨🇮", "+225"),
        Country("Pakistan", "🇵🇰", "+92"),
        Country("Filippine", "🇵🇭", "+63"),
        Country("Bangladesh", "🇧🇩", "+880"),
        Country("Kenya", "🇰🇪", "+254"),
        Country("Marocco", "🇲🇦", "+212"),
        Country("Egitto", "🇪🇬", "+20"),
        Country("Sudafrica", "🇿🇦", "+27"),
        Country("Russia", "🇷🇺", "+7"),
        Country("Cina", "🇨🇳", "+86"),
        Country("Vietnam", "🇻🇳", "+84"),
        Country("Turchia", "🇹🇷", "+90"),
        Country("Romania", "🇷🇴", "+40"),
        Country("Ucraina", "🇺🇦", "+380"),
        Country("Regno Unito", "🇬🇧", "+44"),
        Country("Stati Uniti/Canada", "🇺🇸", "+1"),
        Country("Messico", "🇲🇽", "+52"),
        Country("Colombia", "🇨🇴", "+57"),
        Country("Venezuela", "🇻🇪", "+58"),
        Country("Perù", "🇵🇪", "+51"),
        Country("Ecuador", "🇪🇨", "+593"),
        Country("Cile", "🇨🇱", "+56"),
        Country("Argentina", "🇦🇷", "+54"),
        Country("Brasile", "🇧🇷", "+55"),
        Country("Bolivia", "🇧🇴", "+591"),
        Country("Panama", "🇵🇦", "+507"),
        Country("Guatemala", "🇬🇹", "+502"),
        Country("Repubblica Dominicana", "🇩🇴", "+1809"),
        Country("Spagna", "🇪🇸", "+34"),
        Country("Francia", "🇫🇷", "+33"),
        Country("Germania", "🇩🇪", "+49"),
        Country("Italia", "🇮🇹", "+39"),
        Country("Portogallo", "🇵🇹", "+351"),
        Country("Albania", "🇦🇱", "+355"),
        Country("Kosovo", "🇽🇰", "+383"),
        Country("Emirati Arabi Uniti", "🇦🇪", "+971"),
        Country("Arabia Saudita", "🇸🇦", "+966"),
        Country("Thailandia", "🇹🇭", "+66"),
        Country("Malesia", "🇲🇾", "+60"),
        Country("Nepal", "🇳🇵", "+977"),
        Country("Ghana", "🇬🇭", "+233"),
        Country("Camerun", "🇨🇲", "+237"),
        Country("Numeri a tariffa maggiorata (1900)", "⭐", "+1900")
    )

    /** Busca el pais cuyo dialCode coincide con el prefijo dado. */
    fun byDialCode(code: String): Country? = ALL.firstOrNull { it.dialCode == code }
}

