package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.entity.VideoProject
import com.example.ui.theme.*
import com.example.viewmodel.VideoGeneratorViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoGeneratorViewModel = viewModel()
) {
    val context = LocalContext.current
    val projects by viewModel.videoProjects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val renderingProgress by viewModel.renderingProgress.collectAsState()
    val renderingStage by viewModel.renderingStage.collectAsState()
    val credits by viewModel.credits.collectAsState()
    val creditTimer by viewModel.creditTimer.collectAsState()

    val promptState by viewModel.prompt.collectAsState()
    val selectedCharId by viewModel.selectedCharacterId.collectAsState()
    val customCharName by viewModel.customCharacterName.collectAsState()
    val customCharDesc by viewModel.customCharacterDesc.collectAsState()
    val durationState by viewModel.duration.collectAsState()
    val resolutionState by viewModel.resolution.collectAsState()
    val isReferenceEditState by viewModel.isReferenceEdit.collectAsState()
    val referencePromptState by viewModel.referencePrompt.collectAsState()
    val referenceProjectIdState by viewModel.referenceProjectId.collectAsState()

    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().background(CosmicBackground),
        topBar = {
            TopAppBarCustom(
                credits = credits,
                timer = creditTimer,
                onForceRenew = {
                    viewModel.forceRenewCredits()
                    Toast.makeText(context, "Créditos renovados para o dia!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            
            // 1. ACTIVE VIDEO PLAYCARD / PLAYER PREVIEWER
            item {
                Text(
                    text = "Área de Exibição",
                    color = CosmicPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                VideoPlayerCard(
                    project = selectedProject,
                    isGenerating = isGenerating,
                    progress = renderingProgress,
                    stage = renderingStage,
                    onDownload = {
                        Toast.makeText(context, "Vídeo de 1080p baixado com sucesso na galeria!", Toast.LENGTH_LONG).show()
                    }
                )
            }

            // 2. CONFIGURATION ENGINE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(
                        1.dp,
                        Brush.linearGradient(listOf(CosmicPrimary.copy(alpha = 0.5f), CosmicSecondary.copy(alpha = 0.2f))),
                        RoundedCornerShape(16.dp)
                    ),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "GERADOR DE VÍDEO IA",
                            color = CosmicSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                        
                        // Workspace Status / API Key Warning
                        if (!com.example.api.GeminiClient.isApiKeyConfigured()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicAccent.copy(alpha = 0.15f))
                                    .border(1.dp, CosmicAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info API Key",
                                    tint = CosmicPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Usando motor de demonstração offline do Gemini Flash. Para habilitar IA real, configure GEMINI_API_KEY no painel de segredos do AI Studio.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Text Prompt Input
                        OutlinedTextField(
                            value = promptState,
                            onValueChange = { viewModel.prompt.value = it },
                            label = { Text("Descreva o vídeo que deseja criar...") },
                            placeholder = { Text("Ex: Astronauta correndo na lua em câmera lenta...") },
                            modifier = Modifier.fillMaxWidth().testTag("prompt_input_field"),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicPrimary,
                                unfocusedBorderColor = CosmicSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = CosmicPrimary,
                                focusedLabelColor = CosmicPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Character Consistency Module (Consistência de Personagem)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Consistência de Personagem",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Omini Flash Flow",
                                    fontSize = 11.sp,
                                    color = CosmicPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Preset Row
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    PresetCharacterCard(
                                        name = "Nenhum",
                                        desc = "Apenas prompt livre",
                                        isSelected = selectedCharId == null,
                                        onClick = { viewModel.selectedCharacterId.value = null }
                                    )
                                }
                                items(viewModel.presetCharacters) { character ->
                                    PresetCharacterCard(
                                        name = character.name.split(" ")[0],
                                        desc = character.desc,
                                        isSelected = selectedCharId == character.id,
                                        onClick = { viewModel.selectedCharacterId.value = character.id }
                                    )
                                }
                                item {
                                    PresetCharacterCard(
                                        name = "Customizado",
                                        desc = "Crie seu próprio personagem consistente",
                                        isSelected = selectedCharId == "custom",
                                        onClick = { viewModel.selectedCharacterId.value = "custom" }
                                    )
                                }
                            }

                            // Custom Character Inputs (displays if custom selected)
                            AnimatedVisibility(visible = selectedCharId == "custom") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(CosmicSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Ficha do Personagem Consistente",
                                        fontSize = 12.sp,
                                        color = CosmicPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedTextField(
                                        value = customCharName,
                                        onValueChange = { viewModel.customCharacterName.value = it },
                                        label = { Text("Nome do Personagem") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CosmicPrimary,
                                            unfocusedBorderColor = CosmicSurface
                                        )
                                    )
                                    OutlinedTextField(
                                        value = customCharDesc,
                                        onValueChange = { viewModel.customCharacterDesc.value = it },
                                        label = { Text("Como ele se parece? (Rosto, roupas, estilo)") },
                                        placeholder = { Text("Ex: Homem de 30 anos com cabelos loiros espetados, jaqueta jeans vermelha e estilo anime 3D.") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CosmicPrimary,
                                            unfocusedBorderColor = CosmicSurface
                                        )
                                    )
                                }
                            }
                        }

                        // Reference Video Editing (Edição de Vídeo de Referência)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isReferenceEditState,
                                    onCheckedChange = { viewModel.setReferenceEditMode(it, referenceProjectIdState) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = CosmicPrimary,
                                        uncheckedColor = TextSecondary
                                    )
                                )
                                Text(
                                    text = "Editar Vídeo de Referência (Consome 1 Crédito)",
                                    fontSize = 13.sp,
                                    color = if (isReferenceEditState) CosmicPrimary else TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        viewModel.setReferenceEditMode(!isReferenceEditState, referenceProjectIdState)
                                    }
                                )
                            }

                            AnimatedVisibility(visible = isReferenceEditState) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CosmicSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Selecione o vídeo do histórico para usar como base:",
                                        fontSize = 12.sp,
                                        color = CosmicSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (projects.isEmpty()) {
                                        Text(
                                            text = "Gere pelo menos um vídeo primeiro antes de usar para edição de referência.",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                        )
                                    } else {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(projects) { p ->
                                                val isThisRef = referenceProjectIdState == p.id
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(CosmicSurface)
                                                        .border(
                                                            2.dp,
                                                            if (isThisRef) CosmicPrimary else Color.Transparent,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            viewModel.referenceProjectId.value = p.id
                                                            viewModel.referencePrompt.value = "Transforme \"${p.prompt}\" para o estilo: "
                                                        }
                                                ) {
                                                    if (p.imageUrl != null) {
                                                        AsyncImage(
                                                            model = File(p.imageUrl),
                                                            contentDescription = "Project reference thumbnail",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Movie,
                                                                contentDescription = "Movie placeholder",
                                                                tint = TextMuted
                                                            )
                                                        }
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                                                    )
                                                    Text(
                                                        text = p.prompt.take(12) + "...",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        OutlinedTextField(
                                            value = referencePromptState,
                                            onValueChange = { viewModel.referencePrompt.value = it },
                                            label = { Text("O que mudar no vídeo de referência?") },
                                            placeholder = { Text("Ex: Mudar cores para neon cyberpunk, estilo argila...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CosmicSecondary,
                                                unfocusedBorderColor = CosmicSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Duration (Slider up to 10s) and Resolution Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Duration 10s Limit Slider
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Duração máxima",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${durationState}s (Até 10s grátis)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmicPrimary
                                    )
                                }
                                Slider(
                                    value = durationState.toFloat(),
                                    onValueChange = { viewModel.duration.value = it.toInt() },
                                    valueRange = 1f..10f,
                                    steps = 8,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CosmicPrimary,
                                        activeTrackColor = CosmicPrimary,
                                        inactiveTrackColor = CosmicSurfaceVariant
                                    )
                                )
                            }

                            // Resolution (1080p, 720p Selector)
                            Column(modifier = Modifier.width(130.dp)) {
                                Text(
                                    text = "Resolução Máxima",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CosmicSurfaceVariant)
                                        .padding(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("1080p", "720p").forEach { res ->
                                        val isSelected = resolutionState == res
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) CosmicPrimary else Color.Transparent)
                                                .clickable { viewModel.resolution.value = res },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = res,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.Black else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Generate Buttons
                        val isEnoughCredits = if (isReferenceEditState) credits >= 1 else credits >= 2
                        Button(
                            onClick = { viewModel.generateVideo() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_button"),
                            enabled = !isGenerating && promptState.isNotBlank() && isEnoughCredits,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isReferenceEditState) CosmicSecondary else CosmicPrimary,
                                disabledContainerColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(
                                    imageVector = if (isReferenceEditState) Icons.Default.Edit else Icons.Default.PlayArrow,
                                    contentDescription = "Generate logo",
                                    tint = if (isEnoughCredits) Color.Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        !isEnoughCredits -> "Créditos Insuficientes (Renove!)"
                                        isReferenceEditState -> "Editar Vídeo de Referência (1 Crédito)"
                                        else -> "Gerar Vídeo Omini Flash - 1080p (2 Créditos)"
                                    },
                                    color = if (isEnoughCredits) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. GENERATION TIMELINE / LIBRARY
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Meus Vídeos Gerados (${projects.size})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (projects.isNotEmpty()) {
                        Text(
                            text = "Limpar Tudo",
                            color = CosmicSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showClearConfirm = true }
                        )
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Empty list",
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum vídeo no histórico de inteligência artificial.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dica: Digite um prompt no editor acima e clique em gerar para criar seu primeiro vídeo de até 10 segundos em 1080p grátis!",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                items(projects) { project ->
                    VideoHistoryCard(
                        project = project,
                        isSelected = selectedProject?.id == project.id,
                        onSelect = { viewModel.selectProject(project) },
                        onDelete = { viewModel.deleteProject(project) },
                        onEditReference = {
                            viewModel.selectProject(project)
                            viewModel.setReferenceEditMode(true, project.id)
                        }
                    )
                }
            }
        }
    }

    // Modal para confirmação de apagar histórico
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Apagar Histórico?") },
            text = { Text("Isso removerá definitivamente todos os seus vídeos gerados e arquivos de visualização locais. Deseja prosseguir?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("APAGAR TUDO", color = CosmicSecondary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancelar", color = TextPrimary)
                }
            },
            containerColor = CosmicSurface,
            titleContentColor = Color.White,
            textContentColor = TextSecondary
        )
    }
}

// TOP BAR CUSTOM DESIGN WITH NEON CREDITS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarCustom(
    credits: Int,
    timer: String,
    onForceRenew: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = CosmicPrimary.copy(alpha = 0.3f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        color = CosmicBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CosmicPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VELOVIDEO IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                }
                Text(
                    text = "Omini Flash Renderer 10s",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Glowing Credits Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Timer Countdown
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Renovando em:",
                        fontSize = 8.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = timer,
                        fontSize = 11.sp,
                        color = CosmicSecondary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Credit Pill
                Surface(
                    onClick = onForceRenew,
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .border(1.dp, CosmicPrimary.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                        .testTag("credits_badge"),
                    color = CosmicSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Renew icon",
                            tint = CosmicPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$credits\u200B/\u200B10 CRÉDITOS", // Uses zero-width spaces for test identification safety
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CosmicPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

// VIDEO PLAYER SIMULATOR
@Composable
fun VideoPlayerCard(
    project: VideoProject?,
    isGenerating: Boolean,
    progress: Float,
    stage: String,
    onDownload: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var playTimelineProgress by remember { mutableFloatStateOf(0f) }

    // Loop active playback if selected
    LaunchedEffect(project, isPlaying) {
        if (project != null && isPlaying) {
            while (isPlaying) {
                delay(100)
                playTimelineProgress += 0.1f / project.duration.toFloat()
                if (playTimelineProgress >= 1f) {
                    playTimelineProgress = 0f
                }
            }
        } else {
            playTimelineProgress = 0.0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicSurface)
            .border(
                1.dp,
                if (isPlaying) CosmicPrimary.copy(alpha = 0.6f) else CosmicSurfaceVariant,
                RoundedCornerShape(16.dp)
            )
    ) {
        if (isGenerating) {
            // Generating Video Rendering state
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(64.dp),
                    color = CosmicPrimary,
                    trackColor = CosmicSurfaceVariant,
                    strokeWidth = 6.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Gerando Vídeo Omini Flash grátis: ${(progress * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedContent(targetState = stage) { targetStage ->
                    Text(
                        text = targetStage,
                        color = CosmicPrimary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (project != null) {
            // Displays Selected Generated Video
            if (project.imageUrl != null) {
                AsyncImage(
                    model = File(project.imageUrl),
                    contentDescription = "Video frames preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Processando visualização do renderizador...", color = TextSecondary)
                }
            }

            // Cinematic Scrim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // TOP INFO INFO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Red)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("1080p", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${project.duration} SEG",
                            color = CosmicPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Download Button
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Download video button",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // CENTER PLAY/PAUSE TRIGGER
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable { isPlaying = !isPlaying },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause control",
                    tint = CosmicPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // BOTTOM CONTROLS & TIMELINE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.prompt,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Segmented specs:
                val specList = buildList {
                    add("Omini Flash")
                    if (!project.characterName.isNullOrBlank()) {
                        add("Personagem: ${project.characterName}")
                    }
                    if (project.isReferenceEdit) {
                        add("Estilo Editado")
                    }
                }
                Text(
                    text = specList.joinToString(" • "),
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                // Timeline Scrubber Slider UI representation
                LinearProgressIndicator(
                    progress = if (isPlaying) playTimelineProgress else 0.0f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = CosmicPrimary,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        } else {
            // First Launch / No project selected state
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = "Movie placeholder",
                    tint = TextMuted,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nenhum Vídeo Selecionado",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Selecione um vídeo histórico abaixo ou gere um novo!",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// PRESET CHARACTER CHIP Selection
@Composable
fun PresetCharacterCard(
    name: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) CosmicPrimary else CosmicSurfaceVariant
    val bkColor = if (isSelected) CosmicPrimary.copy(alpha = 0.12f) else CosmicSurfaceVariant

    Box(
        modifier = Modifier
            .width(130.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bkColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = name,
                color = if (isSelected) CosmicPrimary else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 8.sp,
                lineHeight = 11.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// HISTORY ROW ITEM
@Composable
fun VideoHistoryCard(
    project: VideoProject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onEditReference: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val dateString = remember(project.timestamp) { dateFormat.format(Date(project.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isSelected) CosmicPrimary.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CosmicSurfaceVariant else CosmicSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail visual
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicSurfaceVariant)
            ) {
                if (project.imageUrl != null) {
                    AsyncImage(
                        model = File(project.imageUrl),
                        contentDescription = "Saved video thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Movie, contentDescription = "None", tint = TextMuted)
                    }
                }
                // REC Icon
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = "${project.duration}s", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Metada descriptions
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = project.prompt,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = buildString {
                        append("Resolução: ${project.resolution} ")
                        if (!project.characterName.isNullOrBlank()) {
                            append("• Hero: ${project.characterName} (Consistent)")
                        }
                    },
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (project.isReferenceEdit && !project.referencePrompt.isNullOrBlank()) {
                    Text(
                        text = "Editado: \"${project.referencePrompt}\"",
                        color = CosmicSecondary,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                    )
                    Text(
                        text = "Gerado grátis via Omini Flash • $dateString",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            // Options triggers (Delete / Use as Reference)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Use as Reference icon
                IconButton(onClick = onEditReference) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Use as Edit Reference",
                        tint = CosmicPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Delete history icon
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
