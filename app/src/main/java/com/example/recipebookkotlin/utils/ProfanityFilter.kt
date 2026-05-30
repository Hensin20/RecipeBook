package com.example.recipebookkotlin.utils

object ProfanityFilter {
    private val badWords = listOf(
        "дурень", "дебіл", "ідіот", "лох", "падло", "сволота", "мразь", "бовдур", "придурок",
        "мудак", "мудило", "чмо", "гнида", "скотина", "тварюка",
        "гівно", "говно", "срака", "засранець", "хер", "хрін",
        "бля", "блядь", "блять", "сука", "сучка", "курва", "шлюха", "шалава",
        "хуй", "ніхуя", "дохуя", "охуєнно", "пізда", "пизда", "піздюк", "пиздюк", "піздець", "пиздец",
        "єбать", "ебать", "заїбав", "заебав", "уйобок", "уебок", "в'їбати",
        "підарас", "пидарас", "підор", "пидор", "педик", "залупа"
    )

    private val maskedPatterns = listOf(
        Regex("х[^а-яіїєґa-z]й"),
        Regex("п[^а-яіїєґa-z]зд"),
        Regex("б[^а-яіїєґa-z]ядь"),
        Regex("с[^а-яіїєґa-z]ка")
    )

    /**
     * Шукає нецензурну лексику.
     * @return Знайдене погане слово (String) або null, якщо текст чистий.
     */
    fun findProfanity(text: String): String? {
        if (text.isBlank()) return null

        val lowerCaseText = text.lowercase()

        // 1. Шукаємо замасковані слова (х?й)
        for (pattern in maskedPatterns) {
            val match = pattern.find(lowerCaseText)
            if (match != null) {
                return match.value // Повертаємо те, що знайшли
            }
        }

        // 2. Шукаємо звичайні слова (розбиваємо текст по пробілах та спецсимволах)
        // Це вирішує проблему з кирилицею
        val cleanText = lowerCaseText.replace(Regex("[^а-яіїєґa-z0-9]"), " ")
        val words = cleanText.split("\\s+".toRegex())

        for (word in words) {
            if (word in badWords) {
                return word // Повертаємо знайдене погане слово
            }
        }

        return null // Текст чистий
    }
}