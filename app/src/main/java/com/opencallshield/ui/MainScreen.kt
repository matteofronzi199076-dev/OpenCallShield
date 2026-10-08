package com.opencallshield.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencallshield.data.BlockedCall
import com.opencallshield.data.Countries
import com.opencallshield.data.SpamNumber
import com.opencallshield.data.SpamRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val REPO_URL = "https://github.com/jhonsu01/OpenCallShield"
private const val KOFI_URL = "https://ko-fi.com/V7V81LV7GX"
private const val GUIDE_URL =
    "https://github.com/jhonsu01/OpenCallShield/blob/main/docs/CREAR_BASE_COLABORATIVA.md"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestRole: () -> Unit
) {
    val tabs = listOf("Protezione", "Lista SPAM", "Cronologia")
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAccount by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val spamNumbers by viewModel.spamNumbers.collectAsStateWithLifecycle()
    val blockedCalls by viewModel.blockedCalls.collectAsStateWithLifecycle()

    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(authState.message) {
        authState.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumeAuthMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (showAccount) "Collabora" else "OpenCallShield")
                    }
                },
                actions = {
                    IconButton(onClick = { showAccount = !showAccount }) {
                        Icon(
                            if (showAccount) Icons.Filled.Close else Icons.Filled.MoreVert,
                            contentDescription = if (showAccount) "Chiudi" else "Altre opzioni"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (showAccount) {
                AccountTab(authState, viewModel)
            } else {
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                when (selectedTab) {
                    0 -> ProtectionTab(state, viewModel, onRequestRole, spamNumbers.size, blockedCalls.size)
                    1 -> SpamListTab(spamNumbers, authState, state.syncing, viewModel) { showAccount = true }
                    else -> HistoryTab(blockedCalls, spamNumbers, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProtectionTab(
    state: SettingsUiState,
    viewModel: MainViewModel,
    onRequestRole: () -> Unit,
    spamCount: Int,
    blockedCount: Int
) {
    val uriHandler = LocalUriHandler.current
    var advancedOpen by remember { mutableStateOf(false) }
    var countriesOpen by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Protezione attiva",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "$spamCount numeri nella lista nera  -  $blockedCount chiamate filtrate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Button(onClick = onRequestRole, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Shield, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Attiva come app per filtrare le chiamate")
        }
        Text(
            "Android chiederà il permesso per consentire a OpenCallShield di filtrare le tue chiamate. " +
                "È necessario per bloccare lo SPAM.",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        SettingRow(
            title = "Blocca i numeri sconosciuti",
            subtitle = "Rifiuta le chiamate da numeri non presenti nei tuoi contatti",
            checked = state.blockUnknown,
            onCheckedChange = viewModel::setBlockUnknown
        )
        SettingRow(
            title = "Blocca i prefissi sospetti",
            subtitle = "Usa la lista nera dei prefissi internazionali",
            checked = state.blockPrefixes,
            onCheckedChange = viewModel::setBlockPrefixes
        )
        SettingRow(
            title = "Silenzia anziché rifiutare",
            subtitle = "La chiamata non squilla, ma risulta persa",
            checked = state.silence,
            onCheckedChange = viewModel::setSilence
        )

        // --- Selector de paises por bandera (colapsable) ---
        val activePrefixes = remember(state.prefixes) {
            state.prefixes.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
        val countriesCount = Countries.ALL.count { it.dialCode in activePrefixes }
        TextButton(
            onClick = { countriesOpen = !countriesOpen },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (countriesOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (countriesCount > 0) "Blocca le chiamate per Paese  ($countriesCount)"
                else "Blocca le chiamate per Paese"
            )
        }
        if (countriesOpen) {
            Text(
                "Tocca le bandiere dei Paesi da cui NON vuoi ricevere chiamate. " +
                    "Non è necessario scrivere i prefissi.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Countries.ALL.forEach { c ->
                    FilterChip(
                        selected = c.dialCode in activePrefixes,
                        onClick = { viewModel.toggleCountryPrefix(c.dialCode) },
                        label = { Text("${c.flag} ${c.name}") }
                    )
                }
            }
        }

        HorizontalDivider()

        OutlinedButton(
            onClick = { viewModel.syncNow() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.syncing
        ) {
            if (state.syncing) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Sincronizzazione in corso...")
            } else {
                Text("Sincronizza ora")
            }
        }

        // --- Ajustes avanzados (colapsable): URL de la base y prefijos manuales ---
        TextButton(
            onClick = { advancedOpen = !advancedOpen },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (advancedOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text("Impostazioni avanzate")
        }
        if (advancedOpen) {
            OutlinedTextField(
                value = state.syncUrl,
                onValueChange = viewModel::setSyncUrl,
                label = { Text("URL del database collaborativo (JSON)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.prefixes,
                onValueChange = viewModel::setPrefixes,
                label = { Text("Prefissi manuali (avanzato, separati da virgole)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            TextButton(
                onClick = { uriHandler.openUri(GUIDE_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Come creare il tuo database collaborativo? (guida)")
            }
        }

        HorizontalDivider()

        // Donaciones (Ko-fi)
        Button(
            onClick = { uriHandler.openUri(KOFI_URL) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29ABE0))
        ) {
            Text("☕  Sostienimi su Ko-fi")
        }
        Text(
            "OpenCallShield è gratuito e a codice aperto. Se ti è utile, considera di sostenerlo.",
            style = MaterialTheme.typography.bodySmall
        )

        // Pie: enlace al repositorio
        TextButton(
            onClick = { uriHandler.openUri(REPO_URL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("OpenCallShield  -  vedi il progetto su GitHub")
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SpamListTab(
    numbers: List<SpamNumber>,
    authState: AuthUiState,
    syncing: Boolean,
    viewModel: MainViewModel,
    onOpenCollaborate: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val localCount = numbers.count { it.source == "local" }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Numero da segnalare") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                viewModel.report(input)
                input = ""
            }) {
                Icon(Icons.Filled.Block, contentDescription = "Segnala")
            }
        }

        Spacer(Modifier.size(8.dp))
        OutlinedButton(
            onClick = { if (authState.loggedIn) viewModel.contribute() else onOpenCollaborate() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !authState.busy
        ) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (authState.loggedIn) "Invia $localCount numeri al database pubblico"
                else "Contribuisci al database pubblico (facoltativo)"
            )
        }

        Spacer(Modifier.size(8.dp))
        OutlinedButton(
            onClick = { viewModel.syncNow() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !syncing
        ) {
            if (syncing) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Sincronizzazione in corso...")
            } else {
                Icon(Icons.Filled.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sincronizza il database pubblico")
            }
        }
        Text(
            "Sincronizza prima il database pubblico: i numeri già presenti non vengono " +
                "segnalati di nuovo. Così contribuisci solo con numeri nuovi ed eviti i duplicati.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.size(12.dp))
        if (numbers.isEmpty()) {
            EmptyState("Non ci sono ancora numeri segnalati.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(numbers, key = { it.number }) { item ->
                    val displayTag = when (item.tag.lowercase(Locale.ROOT)) {
                        "spam" -> "SPAM"
                        "example" -> "Esempio"
                        "scam" -> "Truffa"
                        else -> item.tag
                    }
                    val displaySource = when (item.source) {
                        "local" -> "locale"
                        "github" -> "GitHub"
                        else -> item.source
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.number, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "$displayTag - ${item.reports} segnalazioni - $displaySource",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { viewModel.remove(item) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Elimina")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    calls: List<BlockedCall>,
    spamNumbers: List<SpamNumber>,
    viewModel: MainViewModel
) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN) }
    val spamSet = remember(spamNumbers) { spamNumbers.map { it.number }.toHashSet() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chiamate filtrate", style = MaterialTheme.typography.titleMedium)
            if (calls.isNotEmpty()) {
                OutlinedButton(onClick = { viewModel.clearHistory() }) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Svuota")
                }
            }
        }
        Text(
            "Tocca + per aggiungere il numero alla lista SPAM o il segno di spunta per rimuoverlo.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.size(12.dp))
        if (calls.isEmpty()) {
            EmptyState("Nessuna chiamata è stata ancora filtrata.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(calls, key = { it.id }) { call ->
                    val normalized = SpamRepository.normalize(call.number)
                    val inList = normalized in spamSet
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    call.number,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${if (call.silenced) "Silenziata" else "Rifiutata"} - ${call.reason}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    formatter.format(Date(call.timestamp)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (inList) {
                                IconButton(onClick = { viewModel.removeNumber(call.number) }) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Rimuovi dalla lista SPAM",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                IconButton(onClick = { viewModel.addToSpam(call.number) }) {
                                    Icon(
                                        Icons.Filled.AddCircle,
                                        contentDescription = "Aggiungi alla lista SPAM"
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

@Composable
private fun AccountTab(
    authState: AuthUiState,
    viewModel: MainViewModel
) {
    val uriHandler = LocalUriHandler.current
    val displayMethod = when (authState.method) {
        "device" -> "codice dispositivo"
        "pat" -> "token personale"
        null -> "-"
        else -> authState.method
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (authState.loggedIn) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Connesso come @${authState.login}", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Metodo: $displayMethod. I tuoi contributi vengono inviati come segnalazioni (Issue) al repository pubblico.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            OutlinedButton(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Esci")
            }
            return@Column
        }

        // Estado del Device Flow en curso
        val device = authState.deviceCode
        if (device != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Passaggio 1 - Inserisci questo codice su GitHub:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        device.userCode,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.size(12.dp))
                    Button(
                        onClick = { uriHandler.openUri(device.verificationUri) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Apri ${device.verificationUri}")
                    }
                    Spacer(Modifier.size(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("In attesa di autorizzazione...", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.size(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.cancelDeviceLogin() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Annulla") }
                }
            }
            return@Column
        }

        Text(
            "Collaborare è FACOLTATIVO. L'app funziona completamente anche senza. Collega un " +
                "account solo se vuoi contribuire con numeri al database pubblico collaborativo.",
            style = MaterialTheme.typography.bodyMedium
        )

        // --- Device Flow ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Opzione A - Codice del dispositivo", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = authState.clientId,
                    onValueChange = viewModel::setClientId,
                    label = { Text("ID client (pubblico)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.startDeviceLogin() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !authState.busy
                ) {
                    Text("Connetti")
                }
                Text(
                    "Si aprirà github.com/login/device per l'autorizzazione. L'ID client è pubblico.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        HorizontalDivider()

        // --- PAT ---
        var pat by remember { mutableStateOf("") }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Opzione B - Token personale (PAT)", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = pat,
                    onValueChange = { pat = it },
                    label = { Text("Token (ambito public_repo)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedButton(
                    onClick = { viewModel.loginWithPat(pat) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !authState.busy
                ) {
                    Text("Connetti con token")
                }
                Text(
                    "Crea il token su github.com/settings/tokens con il permesso public_repo.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (authState.busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Elaborazione in corso...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Shield,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

