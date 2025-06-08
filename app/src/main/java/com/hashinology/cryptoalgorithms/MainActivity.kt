package com.hashinology.cryptoalgorithms

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    lateinit var inputText: EditText
    lateinit var keyText: EditText
    lateinit var resultText: TextView
    lateinit var cipherSpinner: Spinner
    lateinit var encryptBtn: Button
    lateinit var decryptBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputText = findViewById(R.id.inputText)
        keyText = findViewById(R.id.keyText)
        resultText = findViewById(R.id.resultText)
        cipherSpinner = findViewById(R.id.cipherSpinner)
        encryptBtn = findViewById(R.id.encryptBtn)
        decryptBtn = findViewById(R.id.decryptBtn)
        resultText.setOnLongClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Encrypted/Decrypted Text", resultText.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Result copied to clipboard", Toast.LENGTH_SHORT).show()
            true
        }

        val ciphers = listOf(
            "Caesar Cipher",
            "Vigenère Cipher",
            "Playfair Cipher",
            "Rail Fence Cipher",

        )
        cipherSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ciphers)

        encryptBtn.setOnClickListener {
            val text = inputText.text.toString()
            val key = keyText.text.toString()
            val cipher = cipherSpinner.selectedItem.toString()
            resultText.text = when (cipher) {
                "Caesar Cipher" -> caesarEncrypt(text, key)
                "Vigenère Cipher" -> vigenereEncrypt(text, key)
                "Playfair Cipher" -> playfairEncrypt(text, key)
                "Rail Fence Cipher" -> {
                    val rails = key.toIntOrNull()
                    if (rails == null || rails < 2) "Key must be a number ≥ 2 and should not contain alphabets" else railFenceEncrypt(
                        text,
                        rails
                    )
                }

                else -> "Invalid cipher"
            }
        }

        decryptBtn.setOnClickListener {
            val text = inputText.text.toString()
            val key = keyText.text.toString()
            val cipher = cipherSpinner.selectedItem.toString()
            resultText.text = when (cipher) {
                "Caesar Cipher" -> caesarDecrypt(text, key)
                "Vigenère Cipher" -> vigenereDecrypt(text, key)
                "Playfair Cipher" -> playfairDecrypt(text, key)
                "Rail Fence Cipher" -> {
                    val rails = key.toIntOrNull()
                    if (rails == null || rails < 2) "Key must be a number ≥ 2 and should not contain alphabets" else railFenceDecrypt(
                        text,
                        rails
                    )
                }


                else -> "Invalid cipher"
            }
        }
        val copyBtn: Button = findViewById(R.id.copyBtn)
        copyBtn.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Result", resultText.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Result copied to clipboard", Toast.LENGTH_SHORT).show()
        }

    }

    // -------------------- Caesar Cipher --------------------
    fun caesarEncrypt(text: String, key: String): String {
        val shift = key.toIntOrNull()?.mod(26) ?: return "Invalid key"
        return caesarCipher(text, shift)
    }

    fun caesarDecrypt(text: String, key: String): String {
        val shift = key.toIntOrNull()?.mod(26) ?: return "Invalid key"
        return caesarCipher(text, (26 - shift) % 26)
    }

    private fun caesarCipher(text: String, shift: Int): String {
        return text.map { char ->
            when {
                char.isLetter() -> {
                    val base = if (char.isUpperCase()) 'A' else 'a'
                    (base + (char - base + shift).mod(26))
                }

                else -> char
            }
        }.joinToString("")
    }


    // -------------------- Vigenère Cipher --------------------
    fun vigenereEncrypt(text: String, key: String): String {
        if (key.isEmpty()) return "Key cannot be empty"
        val upperKey = key.uppercase().filter { it.isLetter() }
        if (upperKey.isEmpty()) return "Key must contain letters"

        var keyIndex = 0
        return text.map { c ->
            if (c.isLetter()) {
                val shift = upperKey[keyIndex % upperKey.length] - 'A'
                keyIndex++
                if (c.isUpperCase()) 'A' + (c - 'A' + shift) % 26
                else 'a' + (c - 'a' + shift) % 26
            } else c
        }.joinToString("")
    }

    fun vigenereDecrypt(text: String, key: String): String {
        if (key.isEmpty()) return "Key cannot be empty"
        val upperKey = key.uppercase().filter { it.isLetter() }
        if (upperKey.isEmpty()) return "Key must contain letters"

        var keyIndex = 0
        return text.map { c ->
            if (c.isLetter()) {
                val shift = upperKey[keyIndex % upperKey.length] - 'A'
                keyIndex++
                if (c.isUpperCase()) 'A' + (c - 'A' - shift + 26) % 26
                else 'a' + (c - 'a' - shift + 26) % 26
            } else c
        }.joinToString("")
    }

    // -------------------- Playfair Cipher --------------------
    fun playfairEncrypt(text: String, key: String): String {
        if (text.isEmpty()) return ""
        val cleanKey = key.lowercase().filter { it in 'a'..'z' }
        if (cleanKey.isEmpty()) return "Key must contain only letters"

        val matrix = generatePlayfairMatrix(cleanKey)
        val pairs = prepareTextPairs(text)
        return pairs.joinToString("") { encryptPlayfairPair(it, matrix) }.uppercase()
    }

    fun playfairDecrypt(text: String, key: String): String {
        if (text.isEmpty()) return ""
        val cleanKey = key.lowercase().filter { it in 'a'..'z' }
        if (cleanKey.isEmpty()) return "Key must contain only letters"

        val matrix = generatePlayfairMatrix(cleanKey)
        val pairs = text.lowercase().filter { it in 'a'..'z' }.chunked(2)
        return pairs.joinToString("") { decryptPlayfairPair(it, matrix) }
    }


    fun generatePlayfairMatrix(key: String): List<List<Char>> {
        val cleanKey = (key.lowercase() + "abcdefghiklmnopqrstuvwxyz")
            .replace("j", "i")
            .filter { it in 'a'..'z' }
        val unique = LinkedHashSet<Char>()
        cleanKey.forEach { if (it !in unique) unique.add(it) }
        return unique.chunked(5)
    }

    fun prepareTextPairs(text: String): List<String> {
        val clean = text.lowercase().replace("j", "i").filter { it in 'a'..'z' }
        val pairs = mutableListOf<String>()
        var i = 0
        while (i < clean.length) {
            val a = clean[i]
            val b = if (i + 1 < clean.length) clean[i + 1] else 'x'
            if (a == b) {
                pairs.add("$a" + "x")
                i++
            } else {
                pairs.add("$a$b")
                i += 2
            }
        }
        return pairs
    }

    fun findChar(matrix: List<List<Char>>, c: Char): Pair<Int, Int> {
        for (row in matrix.indices) {
            for (col in matrix[row].indices) {
                if (matrix[row][col] == c) return row to col
            }
        }
        return -1 to -1
    }

    fun encryptPlayfairPair(pair: String, matrix: List<List<Char>>): String {
        if (pair.length < 2) return pair
        val (r1, c1) = findChar(matrix, pair[0])
        val (r2, c2) = findChar(matrix, pair[1])

        if (r1 == -1 || c1 == -1 || r2 == -1 || c2 == -1) return pair

        return when {
            r1 == r2 -> "${matrix[r1][(c1 + 1) % 5]}${matrix[r2][(c2 + 1) % 5]}"
            c1 == c2 -> "${matrix[(r1 + 1) % 5][c1]}${matrix[(r2 + 1) % 5][c2]}"
            else -> "${matrix[r1][c2]}${matrix[r2][c1]}"
        }
    }

    fun decryptPlayfairPair(pair: String, matrix: List<List<Char>>): String {
        if (pair.length < 2) return pair
        val (r1, c1) = findChar(matrix, pair[0])
        val (r2, c2) = findChar(matrix, pair[1])

        if (r1 == -1 || c1 == -1 || r2 == -1 || c2 == -1) return pair

        return when {
            r1 == r2 -> "${matrix[r1][(c1 - 1 + 5) % 5]}${matrix[r2][(c2 - 1 + 5) % 5]}"
            c1 == c2 -> "${matrix[(r1 - 1 + 5) % 5][c1]}${matrix[(r2 - 1 + 5) % 5][c2]}"
            else -> "${matrix[r1][c2]}${matrix[r2][c1]}"
        }
    }


    // -------------------- Rail Fence Cipher --------------------
    fun railFenceEncrypt(text: String, rails: Int): String {
        if (rails < 2) return text
        val fence = Array(rails) { StringBuilder() }
        var rail = 0
        var direction = 1
        for (c in text) {
            fence[rail].append(c)
            rail += direction
            if (rail == 0 || rail == rails - 1) direction *= -1
        }
        return fence.joinToString("") { it.toString() }
    }

    fun railFenceDecrypt(cipher: String, rails: Int): String {
        if (rails < 2 || cipher.isEmpty()) return cipher

        val len = cipher.length
        val pattern = IntArray(len)
        var rail = 0
        var dir = 1

        // Step 1: Build the zigzag pattern
        for (i in 0 until len) {
            pattern[i] = rail
            rail += dir
            if (rail == 0 || rail == rails - 1) dir *= -1
        }

        // Step 2: Count occurrences for each rail
        val railCounts = IntArray(rails)
        for (r in pattern) railCounts[r]++

        // Step 3: Allocate and fill rails with cipher text
        val railsArray = Array(rails) { CharArray(railCounts[it]) }
        var index = 0
        for (r in 0 until rails) {
            for (j in 0 until railCounts[r]) {
                railsArray[r][j] = cipher[index++]
            }
        }

        // Step 4: Read characters in zigzag pattern
        val railPointers = IntArray(rails)
        val result = CharArray(len)
        for (i in 0 until len) {
            val r = pattern[i]
            result[i] = railsArray[r][railPointers[r]++]
        }

        return result.concatToString()
    }
}