package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Card
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Debounced clickable modifier to block accidental double taps
fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    delayMillis: Long = 700L,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableStateOf(0L) }
    clickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = LocalIndication.current
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > delayMillis) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

// Gentle Scale Animation on press
@Composable
fun scaleAnimation(pressed: Boolean): Float {
    val scale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1.0f, label = "scale")
    return scale
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State bindings
    val darkTheme by viewModel.darkThemeEnabled.collectAsStateWithLifecycle()
    val gridSize by viewModel.gridSize.collectAsStateWithLifecycle()
    val activeCategory by viewModel.activeCategory.collectAsStateWithLifecycle()
    val currentSentence by viewModel.currentSentence.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val predictions by viewModel.predictiveSuggestions.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }

    // Filter cards for active category
    val filteredCards = remember(activeCategory, allCards) {
        if (activeCategory == "Meus") {
            allCards.filter { it.category == "Meus" }
        } else {
            allCards.filter { it.category == activeCategory }
        }
    }

    // Connect flow notifications
    LaunchedEffect(key1 = true) {
        viewModel.uiMessage.collectLatest { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Ícone de Notificação de som",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Minha Voz",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .semantics { contentDescription = "Configurações" }
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // FIXED PHRASE CONSTRUCTION BAR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .navigationBarsPadding(),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Constructed sequence row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp)
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(18.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (currentSentence.isEmpty()) {
                            Text(
                                text = "Toque nos cartões acima para criar sua mensagem...",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(currentSentence) { card ->
                                    CardChip(card = card)
                                }
                            }
                        }
                    }

                    // Bottom Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // SPEAK SENTENCE (Primary prominent blue action)
                        Button(
                            onClick = { viewModel.speakCurrentSentence() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(56.dp)
                                .testTag("speak_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Ouvir frase completa",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OUVIR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // ERASE LAST (Calm slate button)
                        Button(
                            onClick = { viewModel.removeLastCard() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("delete_last_button"),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Apagar último",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // CLEAR ALL (Soft warning red container button)
                        Button(
                            onClick = { viewModel.clearSentence() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("clear_button"),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Limpar tudo",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // CATEGORIES ROW Selector (Arredondados, scroll suave)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(Card.Categories) { (catName, catEmoji) ->
                    val isSelected = activeCategory == catName
                    Surface(
                        modifier = Modifier
                            .testTag("category_$catName")
                            .semantics { contentDescription = "Categoria $catName" },
                        onClick = { viewModel.selectCategory(catName) },
                        shape = RoundedCornerShape(24.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = catEmoji, fontSize = 16.sp)
                            Text(
                                text = catName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // SMART PREDICTIONS VIEW (💡 Sugestões de IA)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Sugestões Rápidas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    items(predictions) { card ->
                        SuggestionBubble(card = card, onClick = { viewModel.addCardToSentence(card) })
                    }
                }
            }

            // MAIN PICTOGRAMS RESPONSIVE GRID
            val cols = when (gridSize) {
                "Confortável" -> 2
                "Compacto" -> 4
                else -> 3
            }

            if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (activeCategory == "Meus") "⭐" else "🧩",
                            fontSize = 44.sp,
                            modifier = Modifier.scale(1.2f)
                        )
                        Text(
                            text = if (activeCategory == "Meus") {
                                "Nenhum cartão personalizado criado.\nToque no ícone ⚙️ no topo para criar cartões personalizados com seus próprios emojis e expressões!"
                            } else {
                                "Não há cartões carregados nesta categoria."
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        if (activeCategory == "Meus") {
                            Button(
                                onClick = {
                                    showSettings = true
                                    showAddCardDialog = true
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Criar cartão agora")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredCards, key = { it.id }) { card ->
                        CommunicationCard(
                            card = card,
                            onClick = { viewModel.addCardToSentence(card) }
                        )
                    }
                }
            }
        }
    }

    // --- CONFIGURATIONS / SETTINGS MODAL ---
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configurações Clínicas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showSettings = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. Theme Configuration
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tema Escuro / Noturno",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Melhora o conforto visual noturno",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = darkTheme,
                                onCheckedChange = { viewModel.toggleTheme() },
                                modifier = Modifier.testTag("theme_switch")
                            )
                        }

                        // 2. Grid Format Density
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Tamanho da Grade",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ajusta o tamanho dos pictogramas na tela",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("Confortável", "Normal", "Compacto").forEach { size ->
                                    val isSelected = gridSize == size
                                    Button(
                                        onClick = { viewModel.setGridSize(size) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            contentColor = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("grid_size_$size")
                                    ) {
                                        Text(size, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 3. Manage Custom Cards Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cartões Personalizados",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = { showAddCardDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("add_custom_card_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Criar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // List existing custom cards
                            val listCustoms = allCards.filter { it.isCustom }
                            if (listCustoms.isEmpty()) {
                                Text(
                                    text = "Nenhum cartão personalizado criado até o momento.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listCustoms.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(item.emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                item.text,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { viewModel.removeCustomCard(item.id) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Deletar cartão",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD CUSTOM CARD EDITOR DIALOG ---
    if (showAddCardDialog) {
        var cardText by remember { mutableStateOf("") }
        var selectedCardEmoji by remember { mutableStateOf("⭐") }

        // Child communication emoji set
        val appEmojis = listOf(
            "⭐", "🧸", "🪁", "🎈", "🚗", "🧩", "🎨", "🎲", "📱", "💻", "🎒", "⚽",
            "🍎", "🍌", "🥖", "🥛", "🍪", "🍦", "🍚", "🍗", "🍕", "🍔", "🥤", "🍿",
            "🍉", "🍓", "😊", "😢", "😠", "😴", "🥺", "🤒", "👋", "👍", "👎", "💖",
            "🫂", "🚽", "🚿", "🧼", "🪥", "👕", "👟", "🧹", "🛌", "🚶‍♂️", "🏃", "🏊",
            "🏠", "🏫", "🌳", "🛝", "🏖️", "🛒", "🏥", "💡", "❓", "💬", "💖", "🤝"
        )

        Dialog(onDismissRequest = { showAddCardDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Novo Cartão Personalizado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showAddCardDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Emoji Preview
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                )
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = selectedCardEmoji, fontSize = 44.sp)
                        }

                        // Text Field
                        OutlinedTextField(
                            value = cardText,
                            onValueChange = { cardText = it },
                            label = { Text("Texto do Cartão (ex: Quero Colo)") },
                            placeholder = { Text("O que o cartão irá falar...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_text_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Emoji Selector Header
                        Text(
                            text = "Selecione um Símbolo/Emoji:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Selector Emojis Grid Flow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            appEmojis.forEach { emoji ->
                                val isActive = selectedCardEmoji == emoji
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.background
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isActive) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.1f
                                            ),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedCardEmoji = emoji }
                                        .testTag("emoji_picker_$emoji"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddCardDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                if (cardText.isNotBlank()) {
                                    viewModel.addCustomCard(cardText, selectedCardEmoji)
                                    showAddCardDialog = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_custom_card"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Salvar Cartão")
                        }
                    }
                }
            }
        }
    }
}

// --- MICROCOMPONENTS ---

@Composable
fun CommunicationCard(
    card: Card,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale = scaleAnimation(pressed = isPressed)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp, max = 130.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .semantics { contentDescription = "Cartão de comunicação: ${card.text}" }
            .testTag("communication_card_${card.id}"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isPressed) 0.dp else 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (isPressed) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.emoji,
                fontSize = 38.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = card.text.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CardChip(card: Card) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .semantics { contentDescription = "Item na frase: ${card.text}" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = card.emoji, fontSize = 20.sp)
            Text(
                text = card.text,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SuggestionBubble(
    card: Card,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
        modifier = Modifier
            .semantics { contentDescription = "Sugestão ${card.text}" }
            .testTag("suggestion_${card.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = card.emoji, fontSize = 16.sp)
            Text(
                text = card.text,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
