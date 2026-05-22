package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val repo = CardRepository(db.customCardDao())
    private val prefManager = PreferenceManager(application)

    // States
    private val _darkThemeEnabled = MutableStateFlow(prefManager.isDarkTheme)
    val darkThemeEnabled: StateFlow<Boolean> = _darkThemeEnabled.asStateFlow()

    private val _gridSize = MutableStateFlow(prefManager.gridSize)
    val gridSize: StateFlow<String> = _gridSize.asStateFlow()

    private val _activeCategory = MutableStateFlow("Necessidades")
    val activeCategory: StateFlow<String> = _activeCategory.asStateFlow()

    private val _currentSentence = MutableStateFlow<List<Card>>(emptyList())
    val currentSentence: StateFlow<List<Card>> = _currentSentence.asStateFlow()

    // Holds all custom cards from Room database
    val customCards: StateFlow<List<Card>> = repo.customCardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combines preloaded cards and custom cards
    val allCards: StateFlow<List<Card>> = customCards.map { custom ->
        Card.PreloadedCards + custom
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Card.PreloadedCards)

    // UI Toast or Fallback message state
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    // Text To Speech
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        // Load initial state
        _currentSentence.value = prefManager.loadLastSentence()
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Throwable) {
            Log.e("MinhaVozVM", "Falha crítica ao instanciar TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                val ptBrLocale = Locale.forLanguageTag("pt-BR")
                val result = tts?.setLanguage(ptBrLocale)
                if (result != null && result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true
                    
                    // Prioritize premium/high-quality native female voices with complete null-safety
                    val voices = tts?.voices
                    if (!voices.isNullOrEmpty()) {
                        val bestVoice = voices.asSequence()
                            .filterNotNull()
                            .filter {
                                val loc = it.locale
                                loc != null && loc.language == "pt" && (loc.country == "BR" || loc.country.isEmpty())
                            }
                            .sortedWith(compareByDescending<Voice> {
                                it.quality >= Voice.QUALITY_HIGH
                            }.thenByDescending {
                                !it.isNetworkConnectionRequired
                            }.thenByDescending {
                                val name = it.name?.lowercase(Locale.ROOT) ?: ""
                                name.contains("female") || name.contains("fem") || name.contains("-f-") || 
                                name.contains("sfg") || name.contains("gfb") || name.contains("pt-br-x")
                            })
                            .firstOrNull()

                        bestVoice?.let {
                            try {
                                tts?.voice = it
                                Log.d("MinhaVozVM", "Voz feminina de alta qualidade configurada: ${it.name}")
                            } catch (ttsEx: Throwable) {
                                Log.e("MinhaVozVM", "Erro ao definir voz específica do TTS", ttsEx)
                            }
                        }
                    }

                    // AAAAA rating parameters: Beautiful human cadence (Ortoépia & Prosódia)
                    tts?.setSpeechRate(0.92f) // Slightly relaxed speed for ultra-clear diction
                    tts?.setPitch(1.10f) // Feminine and friendly pitch customization sweet spot
                } else {
                    Log.e("MinhaVozVM", "TTS Language Pt-BR não é suportado ou faltam dados.")
                }
            } catch (e: Throwable) {
                Log.e("MinhaVozVM", "Erro ao selecionar voz ou idioma do TTS", e)
            }
        } else {
            Log.e("MinhaVozVM", "Inicialização do TTS falhou")
        }
    }

    fun toggleTheme() {
        val newValue = !_darkThemeEnabled.value
        _darkThemeEnabled.value = newValue
        prefManager.isDarkTheme = newValue
    }

    fun setGridSize(size: String) {
        _gridSize.value = size
        prefManager.gridSize = size
    }

    fun selectCategory(category: String) {
        _activeCategory.value = category
    }

    fun addCardToSentence(card: Card) {
        val updated = _currentSentence.value.toMutableList()
        updated.add(card)
        _currentSentence.value = updated
        prefManager.saveLastSentence(updated)
    }

    fun removeLastCard() {
        val updated = _currentSentence.value.toMutableList()
        if (updated.isNotEmpty()) {
            updated.removeAt(updated.size - 1)
            _currentSentence.value = updated
            prefManager.saveLastSentence(updated)
        }
    }

    fun clearSentence() {
        _currentSentence.value = emptyList()
        prefManager.saveLastSentence(emptyList())
    }

    fun addCustomCard(text: String, emoji: String) {
        viewModelScope.launch {
            if (text.isBlank() || emoji.isBlank()) {
                _uiMessage.emit("Por favor, preencha o texto e selecione um emoji.")
                return@launch
            }
            repo.addCustomCard(text, emoji)
            _uiMessage.emit("Cartão personalizado '$text' adicionado!")
        }
    }

    fun removeCustomCard(cardId: String) {
        viewModelScope.launch {
            repo.deleteCustomCard(cardId)
            // If the card was in the active sentence, remove it
            val updated = _currentSentence.value.filter { it.id != cardId }
            _currentSentence.value = updated
            prefManager.saveLastSentence(updated)
            _uiMessage.emit("Cartão excluído com sucesso.")
        }
    }

    private fun getPhoneticRepresentation(card: Card): String {
        return when (card.id) {
            "id_hello" -> "rélou"
            "id_thank_you" -> "ténkiu"
            "id_please" -> "plízi"
            "id_goodbye" -> "gudbái"
            "id_hola" -> "óla"
            "id_gracias" -> "grássias"
            "id_por_favor" -> "por favór"
            "id_adios" -> "adiós"
            "id_bonjour" -> "bonjúr"
            "id_merci" -> "mêrssí"
            "id_ciao" -> "tcháu"
            "id_grazie" -> "grátsie"
            "id_arigato" -> "arigatóu"
            "p_ele_ela" -> "ele ou ela"
            "p_eu" -> "Eu"
            "p_voce" -> "você"
            "p_nos" -> "nós"
            "p_mamae" -> "mamãe"
            "p_papai" -> "papai"
            "q_oi" -> "oi"
            "q_tchau" -> "tchau"
            "q_por_favor" -> "por favor"
            "q_obrigado" -> "obrigado"
            "q_desculpa" -> "desculpa"
            "s_dor_geral" -> "dor"
            "s_dor_cabeca" -> "dor de cabeça"
            "s_dor_barriga" -> "dor de barriga"
            "s_cansado_corpo" -> "cansado"
            "l_chacara" -> "sítio ou chácara"
            else -> {
                var cleanText = card.text
                if (cleanText.contains("(")) {
                    cleanText = cleanText.substringBefore("(").trim()
                }
                if (cleanText.contains("/")) {
                    cleanText = cleanText.replace("/", " ou ")
                }
                cleanText
            }
        }
    }

    private fun buildProsodicSentence(sentence: List<Card>): String {
        val sb = StringBuilder()
        sentence.forEachIndexed { index, card ->
            val word = getPhoneticRepresentation(card)
            sb.append(word)
            
            if (index < sentence.size - 1) {
                val nextCard = sentence[index + 1]
                // Insert comma pauses to emulate realistic human breathing rhythms (Prosódia)
                if (card.id in listOf("q_oi", "q_tchau", "n_sim", "n_nao", "q_por_favor", "q_obrigado", "q_desculpa") ||
                    card.category == "Perguntas" ||
                    card.category == "Apresentação" ||
                    nextCard.id in listOf("q_por_favor", "q_obrigado")
                ) {
                    sb.append(", ")
                } else {
                    sb.append(" ")
                }
            } else {
                if (card.category == "Perguntas" || card.id.startsWith("q_")) {
                    sb.append("?")
                } else {
                    sb.append(".")
                }
            }
        }
        return sb.toString().trim()
    }

    fun speakCurrentSentence() {
        val sentence = _currentSentence.value
        if (sentence.isEmpty()) {
            viewModelScope.launch {
                _uiMessage.emit("Sua barra de frases está vazia!")
            }
            return
        }
        val textToSpeak = buildProsodicSentence(sentence)
        try {
            if (isTtsReady && tts != null) {
                tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "MINHA_VOZ_SENTENCE")
            } else {
                viewModelScope.launch {
                    _uiMessage.emit("Síntese de voz offline indisponível. Texto: \"$textToSpeak\"")
                }
            }
        } catch (e: Throwable) {
            Log.e("MinhaVozVM", "Erro ao executar síntese de voz", e)
            viewModelScope.launch {
                _uiMessage.emit("Erro ao tentar falar: $textToSpeak")
            }
        }
    }

    // IA Smart Context Predictions
    val predictiveSuggestions: StateFlow<List<Card>> = combine(
        _currentSentence,
        allCards
    ) { sentence, cards ->
        val suggestions = mutableListOf<Card>()

        // 1. Time-of-Day Adaptive Suggestion (Sugerir necessidades baseado no horário)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDaysIds = when {
            // Manhã: Café, acordar, escovar dentes, etc.
            hour in 6..10 -> listOf("a_acordar", "al_leite", "al_pao", "a_escovar_dentes")
            // Almoço: Comer, banheiro, água
            hour in 11..14 -> listOf("a_comer", "al_arroz", "al_frango", "a_lavar_maos")
            // Tarde: Brincar, tablet, brinquedos
            hour in 15..18 -> listOf("a_brincar", "a_tablet", "al_bolacha", "l_parquinho")
            // Noite: Banho, dormir, dentes
            else -> listOf("a_tomar_banho", "a_dormir", "a_escovar_dentes", "al_sopa")
        }

        // Add matching time-of-day cards
        val timeBasedCards = cards.filter { it.id in timeOfDaysIds }
        suggestions.addAll(timeBasedCards)

        // 2. Active sentence ending context (Sugestões contextuais baseadas na frase atual)
        if (sentence.isNotEmpty()) {
            val lastCard = sentence.last()
            val contextualIds = when {
                lastCard.id == "n_quero" || lastCard.id == "a_comer" -> {
                    listOf("al_maca", "al_banana", "al_pao", "al_bolacha", "al_macarrao", "al_sorvete")
                }
                lastCard.id == "a_beber" || lastCard.id == "n_sede" -> {
                    listOf("al_agua", "al_leite", "al_suco", "al_iogurte")
                }
                lastCard.id == "q_oi" -> {
                    listOf("q_por_favor", "q_obrigado", "q_desculpa", "p_mamae", "p_papai")
                }
                lastCard.id == "a_brincar" -> {
                    listOf("a_tablet", "a_tv", "a_musica", "l_parquinho", "p_amigo")
                }
                lastCard.id in listOf("s_dor_geral", "s_dor_barriga", "s_dor_cabeca") -> {
                    listOf("n_ajuda", "p_mamae", "p_terapeuta", "p_medico")
                }
                else -> emptyList()
            }

            val contextualCards = cards.filter { it.id in contextualIds }
            // Deduplicate and prioritize contextual cards (add at the frontend)
            suggestions.removeAll(contextualCards)
            suggestions.addAll(0, contextualCards)
        } else {
            // Default first words suggestions
            val starterIds = listOf("n_quero", "n_ajuda", "q_oi", "n_sim", "n_nao", "n_banheiro")
            val starterCards = cards.filter { it.id in starterIds }
            suggestions.addAll(0, starterCards)
        }

        // Limit suggestions list to 6 elegant slots to preserve negative space
        suggestions.distinctBy { it.id }.take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("MinhaVozVM", "Erro ao encerrar TextToSpeech", e)
        }
    }
}
