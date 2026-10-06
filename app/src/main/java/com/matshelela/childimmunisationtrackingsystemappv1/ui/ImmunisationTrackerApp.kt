package com.matshelela.childimmunisationtrackingsystemappv1.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matshelela.childimmunisationtrackingsystemappv1.data.*
import com.matshelela.childimmunisationtrackingsystemappv1.ui.theme.*
import com.matshelela.childimmunisationtrackingsystemappv1.viewmodel.AppViewModel
import com.matshelela.childimmunisationtrackingsystemappv1.viewmodel.Screen
import com.matshelela.childimmunisationtrackingsystemappv1.viewmodel.UserRole
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmunisationTrackerApp(viewModel: AppViewModel) {
    val childList by viewModel.childProfiles.collectAsStateWithLifecycle()
    val recordList by viewModel.vaccinationRecords.collectAsStateWithLifecycle()
    val appointmentList by viewModel.appointments.collectAsStateWithLifecycle()
    val messageList by viewModel.messages.collectAsStateWithLifecycle()
    val clinicList by viewModel.clinics.collectAsStateWithLifecycle()
    val doctorNurseList by viewModel.doctorsNurses.collectAsStateWithLifecycle()

    var showAddChildDialog by remember { mutableStateOf(false) }
    var showReportPdfModal by remember { mutableStateOf(false) }
    var activeAdministerDoseAppointment by remember { mutableStateOf<Appointment?>(null) }
    
    // Notifications system trigger banner
    val systemNotifications = messageList.filter { 
        it.senderName.contains("System Alert") && 
        it.recipientRole == "Parent" &&
        it.timestamp > System.currentTimeMillis() - 120000 // Just within last 2 minutes
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFEAB308), Color(0xFF0060A9))
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ZW",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ZimVax Tracker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Ministry of Health & Child Care",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // AI Voice & Child Immunization Advisor
                        IconButton(
                            onClick = { viewModel.currentScreen = Screen.AiAdvisorChat }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "AI Voice & Chat Advisor",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Encryption visual indicator (HIPAA Demo Toggle)
                        IconButton(
                            onClick = { viewModel.viewDecryptedMode = !viewModel.viewDecryptedMode }
                        ) {
                            Icon(
                                imageVector = if (viewModel.viewDecryptedMode) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = "HIPAA Security Status",
                                tint = if (viewModel.viewDecryptedMode) MaterialTheme.colorScheme.primary else Color(0xFFEAB308)
                            )
                        }
                        Text(
                            text = if (viewModel.viewDecryptedMode) "HIPAA Decrypted" else "AES Encrypted",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.viewDecryptedMode) MaterialTheme.colorScheme.primary else Color(0xFFEAB308),
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        // Connection Intermittent Simulator
                        IconButton(
                            onClick = { 
                                viewModel.isNetworkOnline = !viewModel.isNetworkOnline 
                                if (viewModel.isNetworkOnline) {
                                    viewModel.triggerOfflineSynchronisation()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (viewModel.isNetworkOnline) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Connection Status",
                                tint = if (viewModel.isNetworkOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        
                        // Offline Indicator Badge
                        Text(
                            text = if (viewModel.isNetworkOnline) "Online" else "Offline Clinic",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isNetworkOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            }
        },
        bottomBar = {
            if (viewModel.currentScreen != Screen.RoleSelection) {
                Column {
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = viewModel.currentScreen is Screen.Dashboard,
                            onClick = { viewModel.currentScreen = Screen.Dashboard },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Portal") }
                        )
                        NavigationBarItem(
                            selected = viewModel.currentScreen is Screen.SetupAssistant,
                            onClick = { viewModel.currentScreen = Screen.SetupAssistant },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "EPI Schedule") },
                            label = { Text("EPI Schedule") }
                        )
                        NavigationBarItem(
                            selected = viewModel.currentScreen is Screen.SecureMessaging,
                            onClick = { viewModel.currentScreen = Screen.SecureMessaging },
                            icon = { 
                                BadgedBox(badge = {
                                    val unread = messageList.size
                                    if (unread > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("$unread")
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Email, contentDescription = "Messaging")
                                }
                            },
                            label = { Text("Messages") }
                        )
                        NavigationBarItem(
                            selected = viewModel.currentScreen is Screen.AiAdvisorChat,
                            onClick = { viewModel.currentScreen = Screen.AiAdvisorChat },
                            icon = { Icon(Icons.Default.Face, contentDescription = "AI Voice & Chat Advisor") },
                            label = { Text("AI Advisor") }
                        )
                        if (viewModel.currentRole == UserRole.ADMIN_MOH) {
                            NavigationBarItem(
                                selected = viewModel.currentScreen is Screen.AnalyticsReporting,
                                onClick = { viewModel.currentScreen = Screen.AnalyticsReporting },
                                icon = { Icon(Icons.Default.Star, contentDescription = "Analytics") },
                                label = { Text("KPI Reports") }
                            )
                            NavigationBarItem(
                                selected = viewModel.currentScreen is Screen.EhrInteroperability,
                                onClick = { viewModel.currentScreen = Screen.EhrInteroperability },
                                icon = { Icon(Icons.Default.Refresh, contentDescription = "EHR Sync") },
                                label = { Text("EHR Gateway") }
                            )
                        }
                        NavigationBarItem(
                            selected = viewModel.currentScreen is Screen.ProblemFeedback,
                            onClick = { 
                                viewModel.currentScreen = Screen.ProblemFeedback 
                                viewModel.feedbackSubmitStatusMessage = ""
                            },
                            icon = { Icon(Icons.Default.Star, contentDescription = "Reporting & Feedback") },
                            label = { Text("Feedback") }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { viewModel.currentScreen = Screen.RoleSelection },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Log Out") },
                            label = { Text("Switch Portal") }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (viewModel.currentScreen is Screen.Dashboard && viewModel.currentRole != UserRole.ADMIN_MOH) {
                FloatingActionButton(
                    onClick = { 
                        if (viewModel.currentRole == UserRole.PARENT_GUARDIAN) {
                            showAddChildDialog = true 
                        } else if (viewModel.currentRole == UserRole.DOCTOR_CLINIC) {
                            showAddChildDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Register Newborn")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Live system alarm banner if anything triggered
                if (systemNotifications.isNotEmpty()) {
                    val latest = systemNotifications.last()
                    val decoded = EncryptionHelper.decrypt(latest.content)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications, 
                            contentDescription = "Push Reminder", 
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = decoded,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Main screen branching
                when (viewModel.currentScreen) {
                    is Screen.RoleSelection -> SecurityGatewayScreen(viewModel)
                    is Screen.Dashboard -> {
                        when (viewModel.currentRole) {
                            UserRole.PARENT_GUARDIAN -> ParentDashboardScreen(viewModel, childList, recordList, appointmentList)
                            UserRole.DOCTOR_CLINIC -> DoctorPortalScreen(
                                viewModel = viewModel,
                                childList = childList,
                                appointments = appointmentList,
                                onAdministerDose = { activeAdministerDoseAppointment = it },
                                onAddPatientClick = { showAddChildDialog = true }
                            )
                            UserRole.ADMIN_MOH -> AdminPortalScreen(
                                viewModel = viewModel,
                                childList = childList,
                                clinicList = clinicList,
                                records = recordList,
                                appointmentList = appointmentList,
                                doctorNurseList = doctorNurseList,
                                onOpenReportPdf = { showReportPdfModal = true },
                                onAddPatientClick = { showAddChildDialog = true }
                            )
                        }
                    }
                    is Screen.ChildDetail -> {
                        val childId = (viewModel.currentScreen as Screen.ChildDetail).childId
                        ChildProfileDetailsScreen(viewModel, childId, childList, recordList, appointmentList)
                    }
                    is Screen.SecureMessaging -> MessagingScreen(viewModel, childList, messageList)
                    is Screen.AnalyticsReporting -> {
                        if (viewModel.currentRole == UserRole.ADMIN_MOH) {
                            AdminPortalScreen(
                                viewModel = viewModel,
                                childList = childList,
                                clinicList = clinicList,
                                records = recordList,
                                appointmentList = appointmentList,
                                doctorNurseList = doctorNurseList,
                                onOpenReportPdf = { showReportPdfModal = true },
                                onAddPatientClick = { showAddChildDialog = true }
                            )
                        }
                    }
                    is Screen.EhrInteroperability -> {
                        EhrGatewayScreen(viewModel)
                    }
                    is Screen.SetupAssistant -> {
                        ZepiScheduleReferenceScreen(viewModel, childList, recordList)
                    }
                    is Screen.ProblemFeedback -> {
                        val feedbackList by viewModel.userFeedbacks.collectAsStateWithLifecycle()
                        ProblemFeedbackScreen(viewModel, feedbackList)
                    }
                    is Screen.AiAdvisorChat -> {
                        val chatViewModel: com.matshelela.childimmunisationtrackingsystemappv1.viewmodel.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()
                        com.matshelela.childimmunisationtrackingsystemappv1.ui.chat.ChatScreen(
                            uiState = chatUiState,
                            onSendMessage = { chatViewModel.sendMessage(it) },
                            onInputTextChanged = { chatViewModel.onInputTextChanged(it) },
                            onRetry = { chatViewModel.retryLastMessage() },
                            onClearChat = { chatViewModel.clearChat() },
                            onRoleSelected = { chatViewModel.selectRole(it) },
                            onModelSelected = { chatViewModel.selectModel(it) },
                            onToggleVoiceMode = { chatViewModel.toggleVoiceMode() },
                            onStartSpeechListening = { chatViewModel.startSpeechListening() },
                            onStopSpeechListening = { chatViewModel.stopSpeechListening() },
                            onBack = { viewModel.currentScreen = Screen.Dashboard }
                        )
                    }
                    else -> SecurityGatewayScreen(viewModel)
                }
            }

            // Sync Loader Overlay
            if (viewModel.syncInProgress) {
                Dialog(onDismissRequest = {}) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Synchronising with MoH Zimbabwe Cloud...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Preserving AES-256 local database encryption layers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Clinical Dosage Entry Dialog (Doctor Portal)
            activeAdministerDoseAppointment?.let { app ->
                val child = childList.find { it.id == app.childId }
                Dialog(onDismissRequest = { activeAdministerDoseAppointment = null }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Immunisation Record Entry",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Confirming vaccination dosage for ${child?.name ?: "Patient"}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text(
                                "Vaccine(s) Due: ${app.vaccineNames}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = viewModel.doseAdministeredBy,
                                onValueChange = { viewModel.doseAdministeredBy = it },
                                label = { Text("Physician/Nurse Name") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = viewModel.doseBatchNumber,
                                onValueChange = { viewModel.doseBatchNumber = it },
                                label = { Text("Vaccine Batch / Lot Code") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = viewModel.doseNotes,
                                onValueChange = { viewModel.doseNotes = it },
                                label = { Text("Clinical Observation / Side-effects") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { activeAdministerDoseAppointment = null }) {
                                    Text("Refuse")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.completeAndAdministerAppointment(
                                            app,
                                            viewModel.doseAdministeredBy,
                                            viewModel.doseBatchNumber,
                                            viewModel.doseNotes
                                        )
                                        activeAdministerDoseAppointment = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Confirm Administered")
                                }
                            }
                        }
                    }
                }
            }

            // Neonatal / Child Registration Dialog
            if (showAddChildDialog) {
                Dialog(onDismissRequest = { showAddChildDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Newborn Registration System",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "ZEPI National Immunisation Entry (Zimbabwe)",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = viewModel.childFormName,
                                onValueChange = { viewModel.childFormName = it },
                                label = { Text("Newborn Full Name") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = viewModel.childFormBirthDate,
                                        onValueChange = { viewModel.childFormBirthDate = it },
                                        label = { Text("Birthdate (YYYY-MM-DD)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    // Gender
                                    Column {
                                        Text("Gender", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row {
                                            FilterChip(
                                                selected = viewModel.childFormGender == "Girl",
                                                onClick = { viewModel.childFormGender = "Girl" },
                                                label = { Text("Girl") }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            FilterChip(
                                                selected = viewModel.childFormGender == "Boy",
                                                onClick = { viewModel.childFormGender = "Boy" },
                                                label = { Text("Boy") }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = viewModel.childFormMotherName,
                                onValueChange = { viewModel.childFormMotherName = it },
                                label = { Text("Mother / Guardian Name") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = viewModel.childFormPhone,
                                onValueChange = { viewModel.childFormPhone = it },
                                label = { Text("Contact Phone") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )

                            Text(
                                "Clinic Enrollment Location (Cached Locally in Room)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            val selectionOpts = if (clinicList.isNotEmpty()) {
                                clinicList.map { it.id to "${it.name} (${it.district})" }
                            } else {
                                listOf(
                                    1 to "Parirenyatwa Central Hospital (Harare)",
                                    2 to "Mpilo Central Hospital (Bulawayo)",
                                    3 to "Chitungwiza Central Clinic",
                                    4 to "Gweru Provincial Hospital"
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                selectionOpts.forEach { (id, label) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.childFormClinicId = id }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = viewModel.childFormClinicId == id,
                                            onClick = { viewModel.childFormClinicId = id }
                                        )
                                        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddChildDialog = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.registerChildChildProfile()
                                        showAddChildDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Register Child")
                                }
                            }
                        }
                    }
                }
            }

            // MoH Monthly Performance Report Simulator modal (PDF representation)
            if (showReportPdfModal) {
                Dialog(onDismissRequest = { showReportPdfModal = false }) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, Color(0xFF0060A9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Document Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "MINISTRY OF HEALTH & CHILD CARE",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF003D6D)
                                    )
                                    Text(
                                        "National EPI Performance & Efficiency Audit",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        "Report Reference: MoH-EPI-2026-M06",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 8.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .background(Color(0xFFF0F6FC), CircleShape)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "APPROVED\nEPI-ZW",
                                        color = Color(0xFF0060A9),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 7.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFF0060A9), thickness = 2.dp)
                            
                            // Scrollable PDF Body Content
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    "1. EXECUTIVE SUMMARY",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                                Text(
                                    "This official document represents the monthly aggregated immunization performance of health centers in major Zimbabwean districts. To comply with HIPAA international and Ministry security guidelines, clinical details are mapped into secure locally locked cryptographic chains.",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Text(
                                    "2. CLINICAL IMMUNISATION COVERAGE PERFORMANCE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // Table header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9))
                                        .padding(4.dp)
                                ) {
                                    Text("Facility Name", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.5f), color = Color.Black)
                                    Text("District", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1f), color = Color.Black)
                                    Text("Effic. %", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(0.8f), color = Color.Black)
                                    Text("Children", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(0.8f), color = Color.Black)
                                }
                                clinicList.forEach { clinic ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 4.dp)
                                    ) {
                                        Text(clinic.name, fontSize = 9.sp, modifier = Modifier.weight(1.5f), color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(clinic.district, fontSize = 9.sp, modifier = Modifier.weight(1f), color = Color.Black)
                                        Text("${clinic.efficiencyRate}%", fontSize = 9.sp, modifier = Modifier.weight(0.8f), color = Color.Black, fontWeight = FontWeight.Bold)
                                        Text("${clinic.totalChildren}", fontSize = 9.sp, modifier = Modifier.weight(0.8f), color = Color.Black)
                                    }
                                    Divider(color = Color.LightGray, thickness = 0.5.dp)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "3. VACCINE DELIVERIES & COLD CHAIN STATS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                                Text(
                                    "- ZEPI BCG Coverage: 98.2%\n- Oral Polio (OPV): 94.6%\n- Rotavirus Antigens: 88.4%\n- Inactivated Polio (IPV): 90.7%\n- Measles-Rubella Coverage: 84.8%",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )

                                Text(
                                    "4. CLINIC INTERMITTENT CONNECTIVITY AUDIT",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                                Text(
                                    "The clinics Mpilo Central, Gweru Provincial, and Mutare successfully achieved offline-synchronization routines with 100% data integrity preservation during intermittent blackouts.",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // Official Signature area
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Dr. Sustus Sibanda", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.width(100.dp))
                                        Text("Chief Inspector of Medical Services", fontSize = 7.sp, color = Color.Gray)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("MOHCC Zimbabwe Seal", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.width(100.dp))
                                        Text("National Office, Harare", fontSize = 7.sp, color = Color.Gray)
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF0060A9), thickness = 1.dp)

                            // PDF Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showReportPdfModal = false }) {
                                    Text("Close", color = Color.DarkGray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        // Save report trigger
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0060A9))
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "PDF Export", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export PDF Report")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// SECURITY GATEWAY & LOGIN SCREEN
// ============================================
@Composable
fun SecurityGatewayScreen(viewModel: AppViewModel) {
    val focusManager = LocalFocusManager.current
    var pinValue by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Banner Art & Ministry Emblem
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = com.matshelela.childimmunisationtrackingsystemappv1.R.drawable.img_hero_banner),
                        contentDescription = "Maternal and Child Immunisation Health",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 6.dp,
                            border = BorderStroke(2.5.dp, Color(0xFF0060A9)),
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = com.matshelela.childimmunisationtrackingsystemappv1.R.drawable.ic_mohc_cits_logo),
                                    contentDescription = "MOHCC Zimbabwe CITS Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "MOHCC Zimbabwe",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Child Immunisation Tracking System (CITS)",
                                color = Color.White.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Official National ZEPI Registry",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Zimbabwe Immunisation System",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "End-to-End Encrypted Health Records",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Portal Card Selector
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Workspace Access Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Role 1: Parent
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (viewModel.currentRole == UserRole.PARENT_GUARDIAN)
                                    MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable {
                                viewModel.selectRole(UserRole.PARENT_GUARDIAN)
                                pinValue = ""
                                errorMessage = ""
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle, 
                            contentDescription = "Parent Portal",
                            tint = if (viewModel.currentRole == UserRole.PARENT_GUARDIAN) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Parent / Guardian Mobile Portal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("View newborn records & book clinic appointments", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role 2: Doctor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (viewModel.currentRole == UserRole.DOCTOR_CLINIC)
                                    MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable {
                                viewModel.selectRole(UserRole.DOCTOR_CLINIC)
                                pinValue = ""
                                errorMessage = ""
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star, 
                            contentDescription = "Clinician Portal",
                            tint = if (viewModel.currentRole == UserRole.DOCTOR_CLINIC) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Doctor / Nurse Healthcare Portal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Approve appointments & update child clinical timeline", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role 3: Admin
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (viewModel.currentRole == UserRole.ADMIN_MOH)
                                    MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable {
                                viewModel.selectRole(UserRole.ADMIN_MOH)
                                pinValue = ""
                                errorMessage = ""
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn, 
                            contentDescription = "Admin Portal",
                            tint = if (viewModel.currentRole == UserRole.ADMIN_MOH) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("MoH Central Administrator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Zimbabwe-wide coverage maps, KPIs & EHR integrations", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // Security Prompt
                    if (viewModel.currentRole == UserRole.PARENT_GUARDIAN) {
                        Button(
                            onClick = { viewModel.currentScreen = Screen.Dashboard },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Enter Patient Dashboard")
                        }
                    } else {
                        // Secure biometric / pin gate
                        val requiredLabel = if (viewModel.currentRole == UserRole.ADMIN_MOH) "Central Admin PASS PIN (Try: 2026)" else "Doctor Portal PASS PIN (Try: 1234)"
                        Text(
                            text = requiredLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = pinValue,
                            onValueChange = { 
                                if (it.length <= 4) pinValue = it
                                errorMessage = "" 
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Enter 4-Digit Secure PIN") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        if (errorMessage.isNotBlank()) {
                            Text(
                                errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val success = viewModel.attemptUnlock(pinValue)
                                if (!success) {
                                    errorMessage = "Unauthorized PIN. Access Logged."
                                } else {
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = pinValue.length >= 4,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Authenticate", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authenticate & Unlock")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Compliance", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "HIPAA Compliant / AES-256 On-Device Encryption Local Database Flow",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================
// PARENT PORTAL - MOBILE DASHBOARD
// ============================================
@Composable
fun ParentDashboardScreen(
    viewModel: AppViewModel,
    childProfiles: List<ChildProfile>,
    records: List<VaccinationRecord>,
    appointments: List<Appointment>
) {
    var showBookAppointmentSheet by remember { mutableStateOf(false) }
    
    // Choose selected child context
    val currentChild = childProfiles.find { it.id == viewModel.selectedChildId } ?: childProfiles.firstOrNull()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "MOHCC ZIMBABWE CHILD HEALTH ACCOUNT",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                    Text(
                        "E-Cert Signature Ready",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 9.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = com.matshelela.childimmunisationtrackingsystemappv1.R.drawable.img_vaccine_shield),
                            contentDescription = "Child Health Protection",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                currentChild?.name ?: "No Child Enrolled",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Patient ID: ${currentChild?.healthPin ?: "ZW-MOH-PENDING"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            currentChild?.complianceStatus ?: "N/A",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentChild == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Enroll newborn via the '+' action button to create digital ZEPI timeline.")
            }
        } else {
            val childRecords = records.filter { it.childId == currentChild.id }
            val completedRecords = childRecords.filter { it.administeredDate != null }
            val completionPercentage = if (childRecords.isNotEmpty()) {
                (completedRecords.size.toFloat() / childRecords.size.toFloat())
            } else 0f

            // Emerald Compliance Status widget matching Professional Polish design exactly
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ComplianceGreenBg),
                border = BorderStroke(1.dp, ComplianceGreenBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(ComplianceGreenAccent, RoundedCornerShape(16.dp))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Schedule Compliant",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Schedule Compliant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ComplianceGreenText,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${completedRecords.size}/${childRecords.size} Vaccinations Completed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFD1FAE5).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${(completionPercentage * 100).toInt()}%",
                            color = Color(0xFF047857),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats Grid matching Professional Polish design exactly
            val clinics by viewModel.clinics.collectAsStateWithLifecycle()
            val childClinic = clinics.find { it.id == currentChild.clinicId } ?: Clinic(name = "Parirenyatwa General", district = "Harare")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "DIGITAL RECORD",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "E-Cert",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Official QR Signature",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "CLINIC",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            childClinic.name.substringBefore(" (").substringBefore(" Central").substringBefore(" General").substringBefore(" Provincial"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            childClinic.district,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showBookAppointmentSheet = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Book Appointment", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Schedule Visit", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.currentScreen = Screen.SecureMessaging },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Messaging", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Secure Chat", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calculated upcoming immunization due dates for registered children
            UpcomingImmunizationsDashboardComponent(
                viewModel = viewModel,
                childProfiles = childProfiles,
                records = records,
                onSelectChild = { childId -> viewModel.selectedChildId = childId },
                onBookClick = { showBookAppointmentSheet = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Booster Alarm Settings Widget
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Setting Alerts", tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Booster Reminder Configuration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Configure automated reminders for the upcoming Measles-Rubella Booster shot.",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E).copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text("SMS Push Alert") }
                        )
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text("WhatsApp Core") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text("EHR Doctor Recall") }
                        )
                    }
                }
            }

            // Timeline header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Digital Immunisation Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "View complete details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        viewModel.currentScreen = Screen.ChildDetail(currentChild.id)
                    }
                )
            }

            // Expanded checklist / timeline view for parent (shows 3 sample vaccines cards for quick look)
            childRecords.take(5).forEach { rec ->
                val isDone = rec.administeredDate != null
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDone) Color(0xFFECFDF5) else Color.White
                    ),
                    border = BorderStroke(
                        0.5.dp, 
                        if (isDone) Color(0xFF059669).copy(alpha = 0.4f) else Color.LightGray
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = "Status",
                            tint = if (isDone) Color(0xFF059669) else Color(0xFFEAB308),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${rec.vaccineName} (Dose ${rec.doseNumber})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Scheduled Due: ${rec.scheduledDate}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isDone) {
                                val cleanBatch = if (viewModel.viewDecryptedMode) {
                                    EncryptionHelper.decrypt(rec.batchNumber)
                                } else {
                                    rec.batchNumber ?: "N/A"
                                }
                                Text(
                                    "Administered: ${rec.administeredDate} | Batch: $cleanBatch",
                                    fontSize = 11.sp,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                        
                        // Secured Lock Indicator Badge
                        if (isDone) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFD1FAE5), RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock, 
                                    contentDescription = "AES-256 Encrypted on Database", 
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Schedule visit appointment modal sheet
    if (showBookAppointmentSheet && currentChild != null) {
        Dialog(onDismissRequest = { showBookAppointmentSheet = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Schedule Clinic Visit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Your request is routed directly to the clinic doctor portal.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.appDate,
                        onValueChange = { viewModel.appDate = it },
                        label = { Text("Appointment Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.appTime,
                        onValueChange = { viewModel.appTime = it },
                        label = { Text("Preferred Time (e.g., 10:00 AM)") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    Text(
                        "Select Required Vaccines/Doses",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    val vaccineCheckboxes = listOf(
                        "BCG", "OPV Dose 1", "OPV Dose 2", "Rotavirus Dose 1", 
                        "Pentavalent Dose 1", "PCV Dose 1", "Measles-Rubella Booster"
                    )
                    
                    vaccineCheckboxes.forEach { vName ->
                        val isChecked = viewModel.appSelectedVaccines.contains(vName)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.appSelectedVaccines = if (isChecked) {
                                        viewModel.appSelectedVaccines - vName
                                    } else {
                                        viewModel.appSelectedVaccines + vName
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    viewModel.appSelectedVaccines = if (isChecked) {
                                        viewModel.appSelectedVaccines - vName
                                    } else {
                                        viewModel.appSelectedVaccines + vName
                                    }
                                }
                            )
                            Text(vName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showBookAppointmentSheet = false }) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.scheduleAppointment(currentChild.id)
                                showBookAppointmentSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Confirm Booking Request")
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// DOCTOR PORTAL - HEALTHCARE PROVIDER SCREEN
// ============================================
@Composable
fun DoctorPortalScreen(
    viewModel: AppViewModel,
    childList: List<ChildProfile>,
    appointments: List<Appointment>,
    onAdministerDose: (Appointment) -> Unit,
    onAddPatientClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Clinic Doctor Portal Desk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Welcome, Dr. Sibusiso Sibanda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Registered Health Center: Mpilo Central Clinic, Bulawayo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Pending Appointment Confirmations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val pendingApps = appointments.filter { it.status == "Pending Approval" }
        if (pendingApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("All medical visits completely confirmed for today.")
            }
        } else {
            pendingApps.forEach { app ->
                val child = childList.find { it.id == app.childId }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(0.5.dp, Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                child?.name ?: "Pending Child Registration",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Pending approval", fontSize = 8.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Requested on: ${app.appointmentDate} at ${app.appointmentTime}", fontSize = 11.sp)
                        Text("Vaccine(s) scheduled: ${app.vaccineNames}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        // Action row for doctors
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.cancelAppointment(app) }) {
                                Text("Reject", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.approveAppointment(app) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve Appointment", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Approved but Outstanding Vaccination Desk (ready to inoculate)
        Text(
            "Approved Vaccinations Ready for Administration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val approvedApps = appointments.filter { it.status == "Approved" }
        if (approvedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No children waiting in the queue presently.")
            }
        } else {
            approvedApps.forEach { app ->
                val child = childList.find { it.id == app.childId }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                child?.name ?: "Pending Patient",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Approved / In clinic", fontSize = 8.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Scheduled: ${app.appointmentDate} | ${app.appointmentTime}", fontSize = 11.sp)
                        Text("Mandatory Antigens: ${app.vaccineNames}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onAdministerDose(app) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Inject", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Record Vaccine Administered", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CLINIC PATIENTS MANAGEMENT DESK
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Clinical Patients Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            "Audit registry of neighborhood children. View compliance status or register child.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                    Button(
                        onClick = onAddPatientClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("doctor_add_patient_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Enroll Patient", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Patient", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                var searchQuery by remember { mutableStateOf("") }
                
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search child by name or health PIN...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredChildren = childList.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.healthPin.contains(searchQuery, ignoreCase = true)
                }

                if (filteredChildren.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isNotBlank()) "No patients found matching '$searchQuery'." else "No patient records registered currently.",
                            color = TextGray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredChildren.forEach { child ->
                            var showConfirmDelete by remember { mutableStateOf(false) }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectedChildId = child.id
                                        viewModel.currentScreen = Screen.ChildDetail(child.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                child.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextDark
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            // Gender Badge
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (child.gender == "Girl") Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    child.gender,
                                                    fontSize = 9.sp,
                                                    color = if (child.gender == "Girl") Color(0xFFDB2777) else Color(0xFF0369A1),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Health PIN: ${child.healthPin} | Born: ${child.birthDate}",
                                            fontSize = 11.sp,
                                            color = TextGray
                                        )
                                        Text(
                                            "Guardian: ${child.motherName} | Phone: ${child.contactPhone}",
                                            fontSize = 11.sp,
                                            color = TextGray
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Compliance Status Badges and Delete Action
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val isCompliant = child.complianceStatus == "Compliant"
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isCompliant) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                child.complianceStatus,
                                                color = if (isCompliant) Color(0xFF065F46) else Color(0xFF991B1B),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = { showConfirmDelete = true },
                                            modifier = Modifier.testTag("delete_patient_${child.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Patient",
                                                tint = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }

                            if (showConfirmDelete) {
                                AlertDialog(
                                    onDismissRequest = { showConfirmDelete = false },
                                    title = { Text("Unregister Patient Record?") },
                                    text = { Text("Are you absolutely sure you want to remove ${child.name} (${child.healthPin}) from the immunization database? This is irreversible.") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.deleteChildProfile(child)
                                                showConfirmDelete = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) {
                                            Text("Remove")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showConfirmDelete = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// MINISTRY OF HEALTH - ADMINISTRATOR SCREEN
// ============================================
@Composable
fun AdminPortalScreen(
    viewModel: AppViewModel,
    childList: List<ChildProfile>,
    clinicList: List<Clinic>,
    records: List<VaccinationRecord>,
    appointmentList: List<Appointment>,
    doctorNurseList: List<DoctorNurse>,
    onOpenReportPdf: () -> Unit,
    onAddPatientClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // MoH Admin Top Welcome
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "MINISTRY OF HEALTH & CHILD CARE (HARARE CENTRAL OFFICE)",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "National EPI Operations & Interoperability Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Auditor access authenticated - Zimbabwe District System",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interoperability and Export Controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenReportPdf,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export Report", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("EPI Audit PDF Report", fontSize = 11.sp)
            }

            Button(
                onClick = { viewModel.currentScreen = Screen.EhrInteroperability },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Interoperability", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("EHR Interop Hub", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Analytics Graphics panel - Zimbabwe National EPI Vaccination Trends
        Text(
            "Zimbabwe National ZEPI Coverage Audits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, Color.LightGray),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Under-5 Completeness of Schedule (Target: >90%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Render beautiful custom canvas bars instead of third-party chart dependencies
                val vaccineCoverages = listOf(
                    "BCG (Tuberculosis)" to 0.98f,
                    "Oral Polio (OPV)" to 0.91f,
                    "Pentavalent (DPT-HepB)" to 0.88f,
                    "Rotavirus Antigen" to 0.86f,
                    "Measles-Rubella" to 0.82f
                )

                vaccineCoverages.forEach { (name, rate) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name, fontSize = 11.sp, color = TextDark)
                            Text("${(rate * 100).toInt()}% Secure", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress bar track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(rate)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Clinic efficiency & Health Facility Location Matrix
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Health Facilities Directory & Offline Cache",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            "Local Room Database Cache active for remote areas with low connectivity",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                    IconButton(
                        onClick = { viewModel.fetchAndCacheHealthFacilities(manualTrigger = true) },
                        modifier = Modifier.testTag("refresh_facilities_button")
                    ) {
                        if (viewModel.isFacilityCacheUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Facility Cache from Firestore",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cache status banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Cache Active",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = viewModel.facilityCacheStatusMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Facility Search Bar
                OutlinedTextField(
                    value = viewModel.facilitySearchQuery,
                    onValueChange = { viewModel.facilitySearchQuery = it },
                    placeholder = { Text("Search facility, district, province...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (viewModel.facilitySearchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.facilitySearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // District quick-filter chips
                val districts = listOf("All", "Harare", "Bulawayo", "Chitungwiza", "Gweru", "Mutare", "Gokwe South (Remote)", "Hwange (Remote)", "Beitbridge", "Chimanimani (Remote)")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    districts.forEach { dist ->
                        val isSelected = (dist == "All" && viewModel.facilityDistrictFilter == null) || (viewModel.facilityDistrictFilter == dist)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.facilityDistrictFilter = if (dist == "All") null else dist
                            },
                            label = { Text(dist, fontSize = 10.sp) }
                        )
                    }
                }

                val filteredFacilities = clinicList.filter { clinic ->
                    val matchesQuery = viewModel.facilitySearchQuery.isEmpty() ||
                        clinic.name.contains(viewModel.facilitySearchQuery, ignoreCase = true) ||
                        clinic.district.contains(viewModel.facilitySearchQuery, ignoreCase = true) ||
                        clinic.province.contains(viewModel.facilitySearchQuery, ignoreCase = true) ||
                        clinic.address.contains(viewModel.facilitySearchQuery, ignoreCase = true)

                    val matchesDistrict = viewModel.facilityDistrictFilter == null || clinic.district == viewModel.facilityDistrictFilter
                    matchesQuery && matchesDistrict
                }

                if (filteredFacilities.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching health facilities found in local Room cache.", color = TextGray, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredFacilities.forEach { clinic ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                clinic.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextDark
                                            )
                                            Text(
                                                "${clinic.district} • ${clinic.province} Province",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (clinic.efficiencyRate >= 92.0) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "${clinic.efficiencyRate}% Efficiency",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (clinic.efficiencyRate >= 92.0) Color(0xFF16A34A) else Color(0xFFD97706)
                                                )
                                            }
                                            Text(
                                                "${clinic.totalChildren} newborns",
                                                fontSize = 9.sp,
                                                color = TextGray,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Address & Location GPS
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            clinic.address,
                                            fontSize = 11.sp,
                                            color = TextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "GPS: ${"%.4f".format(clinic.latitude)}, ${"%.4f".format(clinic.longitude)}",
                                            fontSize = 10.sp,
                                            color = TextGray
                                        )
                                        Text(
                                            "Phone: ${clinic.contactPhone}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextDark
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Cold chain & offline badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = "Cold Chain", tint = Color(0xFF0284C7), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                clinic.coldChainStatus,
                                                fontSize = 10.sp,
                                                color = Color(0xFF0369A1),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text("Room Offline Cache", fontSize = 8.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // STAFF DIRECTORY & PERSONNEL REGISTRY
        var showAddStaffForm by remember { mutableStateOf(false) }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MoH Clinicians Directory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            "Register central medical professionals or remove inactive staff records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                    Button(
                        onClick = { showAddStaffForm = !showAddStaffForm },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showAddStaffForm) Color.Gray else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("admin_toggle_add_staff_button")
                    ) {
                        Icon(
                            imageVector = if (showAddStaffForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Toggle Staff Form",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAddStaffForm) "Close" else "Add Staff", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showAddStaffForm) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text("Add New Clinician / Provider", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = viewModel.doctorFormName,
                            onValueChange = { viewModel.doctorFormName = it },
                            placeholder = { Text("e.g. Dr. Olivia Mutizwa") },
                            label = { Text("Staff Full Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Text("Staff Professional Role", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = viewModel.doctorFormRole == "Doctor",
                                    onClick = { viewModel.doctorFormRole = "Doctor" }
                                )
                                Text("Doctor", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = viewModel.doctorFormRole == "Nurse",
                                    onClick = { viewModel.doctorFormRole = "Nurse" }
                                )
                                Text("Nurse", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        OutlinedTextField(
                            value = viewModel.doctorFormPhone,
                            onValueChange = { viewModel.doctorFormPhone = it },
                            label = { Text("Contact Phone") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = viewModel.doctorFormDesignatorCode,
                            onValueChange = { viewModel.doctorFormDesignatorCode = it },
                            placeholder = { Text("Leave blank for auto-generation") },
                            label = { Text("Designation / License Code") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Text("Select Affiliated Clinic", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray)
                        clinicList.forEach { clinic ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.doctorFormClinicId = clinic.id }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = viewModel.doctorFormClinicId == clinic.id,
                                    onClick = { viewModel.doctorFormClinicId = clinic.id }
                                )
                                Text(clinic.name, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.registerDoctorNurse()
                                showAddStaffForm = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Register Staff Member", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (doctorNurseList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No central provider accounts found in registry.", color = TextGray, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        doctorNurseList.forEach { provider ->
                            val clinicName = clinicList.find { it.id == provider.clinicId }?.name ?: "Zim MoH Primary Clinic"
                            val isDoc = provider.role == "Doctor"

                            var showDeleteStaffConfirm by remember { mutableStateOf(false) }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                provider.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextDark
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isDoc) Color(0xFFDBEAFE) else Color(0xFFE0F2FE),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    provider.role,
                                                    fontSize = 8.sp,
                                                    color = if (isDoc) Color(0xFF1E40AF) else Color(0xFF0369A1),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("License ID: ${provider.designatorCode} | Tel: ${provider.contactNo}", fontSize = 11.sp, color = TextGray)
                                        Text("Clinic: $clinicName", fontSize = 11.sp, color = TextGray)
                                    }

                                    IconButton(
                                        onClick = { showDeleteStaffConfirm = true },
                                        modifier = Modifier.testTag("delete_staff_${provider.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Staff",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }

                            if (showDeleteStaffConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteStaffConfirm = false },
                                    title = { Text("De-authorize Professional?") },
                                    text = { Text("Are you absolutely sure you want to remove ${provider.name} from professional clinician directory records?") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.removeDoctorNurse(provider)
                                                showDeleteStaffConfirm = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) {
                                            Text("De-authorize")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteStaffConfirm = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NATIONAL PATIENT DIRECTORY
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "National Patients Registry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            "Central registry audits. Enroll or remove patient records central databases.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                    Button(
                        onClick = onAddPatientClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("admin_add_patient_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Enroll Patient", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Patient", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                var adminPatientQuery by remember { mutableStateOf("") }
                
                // Search Input
                OutlinedTextField(
                    value = adminPatientQuery,
                    onValueChange = { adminPatientQuery = it },
                    placeholder = { Text("Search child by name or health PIN...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredChildren = childList.filter {
                    it.name.contains(adminPatientQuery, ignoreCase = true) ||
                    it.healthPin.contains(adminPatientQuery, ignoreCase = true)
                }

                if (filteredChildren.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No matching patients found in registry.", color = TextGray, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredChildren.forEach { child ->
                            val clinicName = clinicList.find { it.id == child.clinicId }?.name ?: "Zim MoH Primary Clinic"
                            var showDeletePatientConfirm by remember { mutableStateOf(false) }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                child.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextDark
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (child.gender == "Girl") Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    child.gender,
                                                    fontSize = 8.sp,
                                                    color = if (child.gender == "Girl") Color(0xFFDB2777) else Color(0xFF0369A1),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Health PIN: ${child.healthPin} | Born: ${child.birthDate}", fontSize = 11.sp, color = TextGray)
                                        Text("Guardian: ${child.motherName} | Clinic: $clinicName", fontSize = 11.sp, color = TextGray)
                                    }

                                    IconButton(
                                        onClick = { showDeletePatientConfirm = true },
                                        modifier = Modifier.testTag("admin_delete_patient_${child.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Child",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }

                            if (showDeletePatientConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showDeletePatientConfirm = false },
                                    title = { Text("Remove National Record?") },
                                    text = { Text("Are you absolutely certain you want to delete ${child.name} (${child.healthPin}) from central registration records?") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.deleteChildProfile(child)
                                                showDeletePatientConfirm = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) {
                                            Text("Delete")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeletePatientConfirm = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// EHR GATEWAY & INTEROPERABILITY HUB SCREEN
// ============================================
@Composable
fun EhrGatewayScreen(viewModel: AppViewModel) {
    var showExplanationDetail by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = "EHR Interoperability", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "HL7 FHIR Interoperability Gateway",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Standardized JSON import and export endpoints for clinic integration.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interoperability actions
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, Color.LightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Import Patient Bundle (HL7 FHIR Schema)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Paste raw JSON packet here to trigger automatic profile creation and ZV vaccination calendar population.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = viewModel.ehrImportJsonInput,
                    onValueChange = { viewModel.ehrImportJsonInput = it },
                    placeholder = { Text("Paste FHIR JSON Bundle...") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { viewModel.ehrImportJsonInput = viewModel.getEhrFhirTemplateCode() }
                    ) {
                        Text("Load Sample Bundle")
                    }
                    
                    Button(
                        onClick = { viewModel.importEhrRecord() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Ingest Patient")
                    }
                }

                if (viewModel.ehrSyncStatusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        viewModel.ehrSyncStatusMessage,
                        color = if (viewModel.ehrSyncStatusMessage.contains("Successfully")) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated offline sync controller
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, Color.LightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Offline Local Synchronisation Control",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Clinics in remote districts operate asynchronously under low internet. Tapping Synchronise merges pending offline edits into the Harare central registry.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = { viewModel.triggerOfflineSynchronisation() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Intermittent Manual Sync")
                }

                if (viewModel.lastSyncResult.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        viewModel.lastSyncResult,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ============================================
// MESSAGING BOARD - SECURE PATIENT/PROVIDER (WHATSAPP HD STYLE)
// ============================================
private fun formatChatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    viewModel: AppViewModel,
    childList: List<ChildProfile>,
    messages: List<ClinicMessage>
) {
    // Local state to keep track of the currently opened chat thread child profile
    var selectedChatChild by remember { mutableStateOf<ChildProfile?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // If a child is selected via the main dashboard's "View Chat" action, we open it
    LaunchedEffect(viewModel.selectedChildId) {
        if (viewModel.selectedChildId != null) {
            selectedChatChild = childList.find { it.id == viewModel.selectedChildId }
        }
    }

    if (selectedChatChild == null) {
        // =========================================================================
        // 1. MULTI-CHAT LIST SCREEN (WHATSAPP HOME STYLE)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F2F5)) // WhatsApp Light background for lists
        ) {
            // WhatsApp Header bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF008069)) // WhatsApp Green
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WhatsApp Clinic",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // WhatsApp Sub-Tabs Bar / Categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF008069))
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val filters = listOf("All", "Unread", "Doctors", "Moms")
                filters.forEach { filter ->
                    val isSelected = selectedCategoryFilter == filter
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedCategoryFilter = filter }
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(40.dp)
                                .background(if (isSelected) Color.White else Color.Transparent)
                        )
                    }
                }
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search or start a secure chat...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            // Gemini AI & Voice Assistant Quick Access Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clickable { viewModel.currentScreen = Screen.AiAdvisorChat }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF0284C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "AI Assistant",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "MOHCC AI & Voice Advisor",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF0369A1)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0284C7)
                            ) {
                                Text(
                                    "LIVE",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            "Instant ZEPI guidance, catch-up schedules & live voice",
                            fontSize = 11.sp,
                            color = Color(0xFF0C4A6E)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Open AI Advisor",
                        tint = Color(0xFF0284C7)
                    )
                }
            }

            // Chats List
            val filteredChildren = childList.filter { child ->
                child.name.contains(searchQuery, ignoreCase = true) ||
                child.motherName.contains(searchQuery, ignoreCase = true) ||
                child.healthPin.contains(searchQuery, ignoreCase = true)
            }

            if (filteredChildren.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No active clinical chats found", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Check search query or register a new child profile.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    items(filteredChildren) { child ->
                        // Get the message thread for this child
                        val threadMessages = messages.filter { it.childId == child.id || it.childId == null }
                        val latestMessage = threadMessages.lastOrNull()
                        
                        // Parse latest message content (decrypted)
                        val lastText = latestMessage?.let { msg ->
                            val rawText = EncryptionHelper.decrypt(msg.content)
                            if (msg.senderRole == "Doctor") "Dr. Sibanda: $rawText" else rawText
                        } ?: "No messages in thread yet. Tap to consult."

                        val lastTime = latestMessage?.timestamp?.let { formatChatTime(it) } ?: "08:30 AM"

                        // Mock unread state if latest message is not from our own role
                        val isOwnLast = latestMessage?.let { msg ->
                            when (viewModel.currentRole) {
                                UserRole.PARENT_GUARDIAN -> msg.senderRole == "Parent"
                                UserRole.DOCTOR_CLINIC -> msg.senderRole == "Doctor"
                                UserRole.ADMIN_MOH -> msg.senderRole == "MoH Admin"
                            }
                        } ?: true
                        val hasUnread = !isOwnLast && latestMessage != null

                        // Chat List Item
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedChatChild = child
                                        viewModel.selectedChildId = child.id
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rounded Profile Avatar
                                if (viewModel.currentRole == UserRole.PARENT_GUARDIAN) {
                                    Image(
                                        painter = painterResource(id = com.matshelela.childimmunisationtrackingsystemappv1.R.drawable.img_doctor_avatar),
                                        contentDescription = "Doctor Profile",
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val avatarColor = remember(child.id) {
                                        val colors = listOf(
                                            Color(0xFF128C7E), Color(0xFF075E54), Color(0xFF0D5E54),
                                            Color(0xFF34B7F1), Color(0xFF25D366), Color(0xFF1EBEA5)
                                        )
                                        colors[child.id % colors.size]
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(avatarColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = child.name.take(2).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Chat Details (Name, last message)
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (viewModel.currentRole == UserRole.PARENT_GUARDIAN) {
                                                "Dr. Sibusiso Sibanda (${child.name})"
                                            } else {
                                                "${child.motherName} (Mother of ${child.name})"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF111B21),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = lastTime,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasUnread) Color(0xFF25D366) else Color(0xFF667781),
                                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (latestMessage != null) {
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = "Read",
                                                tint = Color(0xFF53BDEB),
                                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = lastText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF667781),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (hasUnread) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(Color(0xFF25D366), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "1",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFFE9EDEF)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // =========================================================================
        // 2. HD CHAT WINDOW SCREEN
        // =========================================================================
        val child = selectedChatChild!!
        
        // Filter messages for selected child's doctor-parent secure line, plus admin advisories
        val filteredMessages = messages.filter { msg ->
            msg.childId == child.id || msg.childId == null
        }

        val chatListState = rememberLazyListState()
        // Scroll to the bottom whenever a new message is loaded
        LaunchedEffect(filteredMessages.size) {
            if (filteredMessages.isNotEmpty()) {
                chatListState.animateScrollToItem(filteredMessages.size)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEFEAE2)) // Classic WhatsApp Beige Wallpaper background
        ) {
            // WhatsApp Header bar with Recipient Photo and Back Navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF008069)) // WhatsApp Green
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            selectedChatChild = null
                            viewModel.selectedChildId = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Rounded Profile Avatar
                    if (viewModel.currentRole == UserRole.PARENT_GUARDIAN) {
                        Image(
                            painter = painterResource(id = com.matshelela.childimmunisationtrackingsystemappv1.R.drawable.img_doctor_avatar),
                            contentDescription = "Doctor Profile",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF075E54), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = child.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Name and Status
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (viewModel.currentRole == UserRole.PARENT_GUARDIAN) {
                                "Dr. Sibusiso Sibanda"
                            } else {
                                child.motherName
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Secure HIPAA Line • Online",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Media/Action shortcuts
                    IconButton(onClick = { viewModel.currentScreen = Screen.AiAdvisorChat }) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call AI Advisor", tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                    }
                }
            }

            // WhatsApp Message List Viewport (LazyColumn optimized with custom keys)
            LazyColumn(
                state = chatListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                item(key = "hipaa_notice") {
                    // HIPAA warning card styled as WhatsApp notice
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEECD)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = Color(0xFF7A5C00),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Messages secured with end-to-end medical AES encryption. Tap to toggle decrypted view.",
                                fontSize = 11.sp,
                                color = Color(0xFF7A5C00),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (filteredMessages.isEmpty()) {
                    item(key = "empty_chat_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No messages yet. Send a message to start.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredMessages,
                        key = { it.id } // Keyed to message ID to optimize recomposition performance
                    ) { msg ->
                        val isOwnMessage = when (viewModel.currentRole) {
                            UserRole.PARENT_GUARDIAN -> msg.senderRole == "Parent"
                            UserRole.DOCTOR_CLINIC -> msg.senderRole == "Doctor"
                            UserRole.ADMIN_MOH -> msg.senderRole == "MoH Admin"
                        }

                        val rawDecryptedContent = EncryptionHelper.decrypt(msg.content)
                        val contentToShow = if (viewModel.viewDecryptedMode) {
                            rawDecryptedContent
                        } else {
                            msg.content
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isOwnMessage) 12.dp else 2.dp,
                                    bottomEnd = if (isOwnMessage) 2.dp else 12.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOwnMessage) 
                                        Color(0xFFE2F1F8) // Sent Bubble: WhatsApp Tea Green
                                    else 
                                        Color.White // Received Bubble: Pure white
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    if (!isOwnMessage) {
                                        Text(
                                            text = msg.senderName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF008069),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = contentToShow,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (viewModel.viewDecryptedMode) Color(0xFF111B21) else Color(0xFFD97706)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.align(Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatChatTime(msg.timestamp),
                                            fontSize = 9.sp,
                                            color = Color(0xFF667781)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        if (isOwnMessage) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Delivered",
                                                tint = if (msg.isSynced) Color(0xFF53BDEB) else Color(0xFF8696A0),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Encrypted",
                                                tint = Color(0xFF8696A0),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // WhatsApp Style Floating Bottom Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input Container
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face, // Smile icon
                            contentDescription = "Emoji picker",
                            tint = Color(0xFF8696A0)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        BasicTextField(
                            value = viewModel.chatInput,
                            onValueChange = { viewModel.chatInput = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF111B21)),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (viewModel.chatInput.isEmpty()) {
                                    Text("Message...", color = Color(0xFF8696A0), fontSize = 15.sp)
                                }
                                innerTextField()
                            }
                        )

                        Icon(
                            imageVector = Icons.Default.Add, // Clip / attachment icon
                            contentDescription = "Attach file",
                            tint = Color(0xFF8696A0),
                            modifier = Modifier.clickable { }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Green Circular Send Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF008069), CircleShape)
                        .clickable {
                            if (viewModel.chatInput.isNotBlank()) {
                                val targetRecipient = if (viewModel.currentRole == UserRole.PARENT_GUARDIAN) "Doctor" else "Parent"
                                viewModel.sendClinicMessage(child.id, targetRecipient)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// EXTENDED PATIENT IMMUNISATION DETAILS (TIMELINE)
// ============================================
@Composable
fun ChildProfileDetailsScreen(
    viewModel: AppViewModel,
    childId: Int,
    childList: List<ChildProfile>,
    records: List<VaccinationRecord>,
    appointments: List<Appointment>
) {
    val child = childList.find { it.id == childId } ?: return
    val childRecords = records.filter { it.childId == child.id }
    var activeAdministerRecord by remember { mutableStateOf<VaccinationRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconButton(onClick = { viewModel.currentScreen = Screen.Dashboard }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Back")
            }
            Text("Back to Dashboard", style = MaterialTheme.typography.bodyMedium)
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, Color.LightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Zimbabwe Ministry of Health Record",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    child.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Text("Birthdate: ${child.birthDate}", fontSize = 12.sp)
                Text("Registered Clinic Enrollment: Mpilo Central Hospital Clinic", fontSize = 12.sp)
                Text("Mother's Name: ${child.motherName}", fontSize = 12.sp)
                Text("Parent Contact No: ${child.contactPhone}", fontSize = 12.sp)
                Text("Compliance Status Updates: [${child.complianceStatus}]", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "ZEPI Immunisation Dossier Timeline",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        childRecords.forEach { rec ->
            val isDone = rec.administeredDate != null
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDone) Color(0xFFECFDF5) else Color.White
                ),
                border = BorderStroke(
                    0.5.dp, 
                    if (isDone) Color(0xFF059669).copy(alpha = 0.4f) else Color.LightGray
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${rec.vaccineName} (Dose ${rec.doseNumber})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDone) Color(0xFF065F46) else TextDark
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isDone) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (isDone) "Administered" else "Upcoming Due",
                                fontSize = 8.sp,
                                color = if (isDone) Color(0xFF047857) else Color(0xFFB45309),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Scheduled Target: ${rec.scheduledDate}", fontSize = 11.sp)
                    
                    if (isDone) {
                        // Check if encrypted showing mode
                        val cleanBatch = if (viewModel.viewDecryptedMode) {
                            EncryptionHelper.decrypt(rec.batchNumber)
                        } else {
                            rec.batchNumber ?: ""
                        }
                        
                        val cleanNotes = if (viewModel.viewDecryptedMode) {
                            EncryptionHelper.decrypt(rec.notes)
                        } else {
                            rec.notes ?: ""
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp))
                        Text("Immunisation Date: ${rec.administeredDate}", fontSize = 11.sp, color = Color.DarkGray)
                        Text("Enrolling Clinic: ${rec.facilityName}", fontSize = 11.sp, color = Color.DarkGray)
                        Text("Authorised Staff: ${rec.administeredBy}", fontSize = 11.sp, color = Color.DarkGray)
                        Text("Vaccine Lot ID: $cleanBatch", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                        if (cleanNotes.isNotBlank()) {
                            Text("Medical History Notes: \"$cleanNotes\"", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    } else {
                        // If medical worker is active, they can instantly mark as administered
                        if (viewModel.currentRole == UserRole.DOCTOR_CLINIC || viewModel.currentRole == UserRole.ADMIN_MOH) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { activeAdministerRecord = rec },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Inject", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Record Administered", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    activeAdministerRecord?.let { rec ->
        AdministerDoseDialog(
            record = rec,
            childName = child.name,
            onDismiss = { activeAdministerRecord = null },
            onConfirm = { date, nurse, batch, notes ->
                viewModel.administerVaccineRecord(
                    record = rec,
                    administeredDate = date,
                    nurseName = nurse,
                    batch = batch,
                    feedbackNote = notes,
                    clinicName = "Mpilo Central Hospital Clinic"
                )
                activeAdministerRecord = null
            }
        )
    }
}

// ============================================
// UPCOMING IMMUNIZATION FORECAST DASHBOARD COMPONENT
// ============================================
@Composable
fun UpcomingImmunizationsDashboardComponent(
    viewModel: AppViewModel,
    childProfiles: List<ChildProfile>,
    records: List<VaccinationRecord>,
    onSelectChild: (Int) -> Unit,
    onBookClick: () -> Unit
) {
    // Calculate and memoize upcoming rounds for registered children
    val upcomingSchedules = remember(childProfiles, records) {
        childProfiles.mapNotNull { child ->
            val pending = records.filter { it.childId == child.id && it.administeredDate == null }
            if (pending.isEmpty()) {
                null
            } else {
                val grouped = pending.groupBy { it.scheduledDate }
                val earliestDateStr = grouped.keys.minOrNull()
                if (earliestDateStr == null) {
                    null
                } else {
                    val roundVaccines = grouped[earliestDateStr] ?: emptyList()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    
                    val daysRem = try {
                        val scheduledDate = sdf.parse(earliestDateStr)
                        if (scheduledDate != null) {
                            val today = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.time
                            
                            val diffMs = scheduledDate.time - today.time
                            Math.round(diffMs.toDouble() / 86400000.0)
                        } else {
                            999L
                        }
                    } catch (e: Exception) {
                        999L
                    }
                    
                    val outSdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
                    val formattedStr = try {
                        val d = sdf.parse(earliestDateStr)
                        if (d != null) outSdf.format(d) else earliestDateStr
                    } catch (e: Exception) {
                        earliestDateStr
                    }
                    
                    ChildUpcomingSchedule(
                        child = child,
                        scheduledDateStr = earliestDateStr,
                        daysRemaining = daysRem,
                        vaccines = roundVaccines,
                        formattedDate = formattedStr
                    )
                }
            }
        }.sortedBy { it.daysRemaining }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Forecast Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Upcoming Immunisation Forecast",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            "Calculated next-due immunization schedules for registered child accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (childProfiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No children currently registered. Enroll a child above to view schedules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (upcomingSchedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ComplianceGreenBg, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = ComplianceGreenAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "All children are vaccine compliant! No pending immunisations.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ComplianceGreenText
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    upcomingSchedules.forEach { schedule ->
                        val isSelected = viewModel.selectedChildId == schedule.child.id
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF0F6FC) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectChild(schedule.child.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Row left: info
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (schedule.child.gender == "Girl") Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = schedule.child.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = if (schedule.child.gender == "Girl") Color(0xFFDB2777) else Color(0xFF0369A1),
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                schedule.child.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextDark
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Due Date",
                                                    tint = TextGray,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Due Date: ${schedule.formattedDate}",
                                                    fontSize = 11.sp,
                                                    color = TextGray
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Row right: Badge showing days countdown status
                                    val badgeBgColor: Color
                                    val badgeTextColor: Color
                                    val badgeLabel: String
                                    
                                    when {
                                        schedule.daysRemaining < 0 -> {
                                            badgeBgColor = Color(0xFFFEE2E2)
                                            badgeTextColor = Color(0xFFEF4444)
                                            val absDays = Math.abs(schedule.daysRemaining)
                                            badgeLabel = "Overdue by $absDays ${if (absDays == 1L) "day" else "days"}"
                                        }
                                        schedule.daysRemaining == 0L -> {
                                            badgeBgColor = Color(0xFFFEF3C7)
                                            badgeTextColor = Color(0xFFD97706)
                                            badgeLabel = "Due Today"
                                        }
                                        schedule.daysRemaining in 1..7 -> {
                                            badgeBgColor = Color(0xFFFFEDD5)
                                            badgeTextColor = Color(0xFFF97316)
                                            badgeLabel = "Due in ${schedule.daysRemaining}d"
                                        }
                                        else -> {
                                            badgeBgColor = Color(0xFFE0F2FE)
                                            badgeTextColor = Color(0xFF0369A1)
                                            badgeLabel = "In ${schedule.daysRemaining}d"
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(badgeBgColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            badgeLabel,
                                            color = badgeTextColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Vaccine Dose List (Scrollable Row)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        schedule.vaccines.forEach { vac ->
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "${vac.vaccineName} D${vac.doseNumber}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Quick Appointment Trigger Text Link
                                    Text(
                                        "Book Visit",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .clickable {
                                                onSelectChild(schedule.child.id)
                                                viewModel.appSelectedVaccines = schedule.vaccines.map { "${it.vaccineName} ${it.doseNumber}" }.toSet()
                                                onBookClick()
                                            }
                                            .padding(4.dp)
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

data class ChildUpcomingSchedule(
    val child: ChildProfile,
    val scheduledDateStr: String,
    val daysRemaining: Long,
    val vaccines: List<VaccinationRecord>,
    val formattedDate: String
)

// ============================================
// ZIMBABWE EPI (ZEPI) STANDARD IMMUNISATION REFERENCE & LIVE TRACKER
// ============================================

data class ZepiScheduleItem(
    val ageMilestone: String,
    val vaccineName: String,
    val doseNumber: Int,
    val administrationRoute: String,
    val targetDiseases: String,
    val description: String,
    val color: Color
)

val standardZepiSchedule = listOf(
    ZepiScheduleItem("At Birth", "BCG", 1, "Intradermal (Right forearm)", "Tuberculosis (Meningitis & Miliary)", "Protects newborns against severe forms of childhood tuberculosis.", Color(0xFF3B82F6)),
    ZepiScheduleItem("At Birth", "OPV", 0, "Oral (Drops)", "Poliomyelitis", "Initial immunization to build early mucosal gut immunity against Polio.", Color(0xFF10B981)),
    
    ZepiScheduleItem("6 Weeks", "OPV", 1, "Oral (Drops)", "Poliomyelitis", "First dose of Oral Polio Vaccine to intensify polio immunity.", Color(0xFF10B981)),
    ZepiScheduleItem("6 Weeks", "Rotavirus", 1, "Oral (Liquid)", "Rotavirus Diarrhea", "Protects against severe rotaviral gastroenteritis and dehydration.", Color(0xFFF59E0B)),
    ZepiScheduleItem("6 Weeks", "Pentavalent", 1, "Intramuscular (Left thigh)", "Diphtheria, Pertussis, Tetanus, Hepatitis B, Haemophilus Influenzae type b", "Highly effective 5-in-1 combo protecting infants from fatal bacterial infections.", Color(0xFF8B5CF6)),
    ZepiScheduleItem("6 Weeks", "PCV", 1, "Intramuscular (Right thigh)", "Pneumococcal Pneumonia & Meningitis", "Shields against invasive pneumococcal streptococcus infections.", Color(0xFFEC4899)),
    
    ZepiScheduleItem("10 Weeks", "OPV", 2, "Oral (Drops)", "Poliomyelitis", "Second booster dose for robust polio immunity.", Color(0xFF10B981)),
    ZepiScheduleItem("10 Weeks", "Rotavirus", 2, "Oral (Liquid)", "Rotavirus Diarrhea", "Second and final dose of the primary rotavirus series.", Color(0xFFF59E0B)),
    ZepiScheduleItem("10 Weeks", "Pentavalent", 2, "Intramuscular (Left thigh)", "Diphtheria, Pertussis, Tetanus, Hepatitis B, Hib", "Follow-up dose to strengthen standard immune memory.", Color(0xFF8B5CF6)),
    ZepiScheduleItem("10 Weeks", "PCV", 2, "Intramuscular (Right thigh)", "Pneumococcal Pneumonia & Meningitis", "Shields against invasive streptococcus strains.", Color(0xFFEC4899)),
    
    ZepiScheduleItem("14 Weeks", "OPV", 3, "Oral (Drops)", "Poliomyelitis", "Third oral dose helping achieve complete herd immunity.", Color(0xFF10B981)),
    ZepiScheduleItem("14 Weeks", "IPV", 1, "Intramuscular (Left shoulder)", "Poliomyelitis", "Inactivated Polio injectible booster to provide systemic body immunity.", Color(0xFF14B8A6)),
    ZepiScheduleItem("14 Weeks", "Pentavalent", 3, "Intramuscular (Left thigh)", "Diphtheria, Pertussis, Tetanus, Hepatitis B, Hib", "Third dose completing the infant's primary 5-in-1 series.", Color(0xFF8B5CF6)),
    ZepiScheduleItem("14 Weeks", "PCV", 3, "Intramuscular (Right thigh)", "Pneumococcal Pneumonia & Meningitis", "Third dose completing the infant's primary pneumococcal series.", Color(0xFFEC4899)),
    
    ZepiScheduleItem("9 Months", "Measles-Rubella", 1, "Subcutaneous (Left upper arm)", "Measles & Rubella", "Protects against highly contagious measles virus outbreaks and congenital rubella.", Color(0xFFEF4444)),
    ZepiScheduleItem("9 Months", "TCV", 1, "Intramuscular (Left forearm)", "Typhoid Fever", "Typhoid Conjugate Vaccine introduced to eliminate localized typhoid outbreaks.", Color(0xFF6366F1)),
    
    ZepiScheduleItem("18 Months", "Measles-Rubella", 2, "Subcutaneous (Left upper arm)", "Measles & Rubella", "Booster dose to achieve 99%+_ lifelong immunity.", Color(0xFFEF4444))
)

@Composable
fun ZepiScheduleReferenceScreen(
    viewModel: AppViewModel,
    childList: List<ChildProfile>,
    records: List<VaccinationRecord>
) {
    var selectedChildIdState by remember { mutableStateOf<Int?>(viewModel.selectedChildId ?: childList.firstOrNull()?.id) }
    val activeChild = childList.find { it.id == selectedChildIdState }
    val childRecords = activeChild?.let { child -> records.filter { it.childId == child.id } } ?: emptyList()
    
    // Tab indicator: 0 = Pediatric dossier tracking, 1 = National ZEPI guidelines
    var activeTab by remember { mutableStateOf(0) }
    var activeAdministerRecord by remember { mutableStateOf<VaccinationRecord?>(null) }
    
    // Search query for vaccines in guidelines
    var referenceSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Upper Intro block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE0F2FE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange, 
                    contentDescription = "Schedule", 
                    tint = Color(0xFF0369A1),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "ZEPI Reference & Live Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    "Standard Expanded Programme on Immunisation in Zimbabwe",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }

        // Horizontal child selection bar if dossier is active or to track child
        Text(
            "Select Pediatric Profile to Trace Archive",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (childList.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No children profiles found. Go to Portal to register a child.", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                childList.forEach { child ->
                    val isSelected = selectedChildIdState == child.id
                    Card(
                        modifier = Modifier
                            .widthIn(min = 150.dp, max = 220.dp)
                            .clickable { selectedChildIdState = child.id },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF0F6FC) else Color.White
                        ),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 0.5.dp, 
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (child.gender == "Girl") Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(child.gender.take(1), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (child.gender == "Girl") Color(0xFFDB2777) else Color(0xFF0369A1))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(child.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextDark)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Born: ${child.birthDate}", fontSize = 10.sp, color = TextGray)
                            Text("Health PIN: ${child.healthPin}", fontSize = 10.sp, color = TextGray)
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (child.complianceStatus == "Compliant") Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(child.complianceStatus, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (child.complianceStatus == "Compliant") Color(0xFF065F46) else Color(0xFFB45309))
                            }
                        }
                    }
                }
            }
        }

        // Pill Toggle for Dossier trace vs Guidelines Reference
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                Button(
                    onClick = { activeTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 0) Color.White else Color.Transparent,
                        contentColor = if (activeTab == 0) MaterialTheme.colorScheme.primary else TextGray
                    ),
                    modifier = Modifier.weight(1f),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (activeTab == 0) 1.dp else 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Child Live Dossier", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { activeTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 1) Color.White else Color.Transparent,
                        contentColor = if (activeTab == 1) MaterialTheme.colorScheme.primary else TextGray
                    ),
                    modifier = Modifier.weight(1f),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (activeTab == 1) 1.dp else 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ZEPI National Guidelines", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (activeTab == 0) {
            // VIEW 1: INTERACTIVE DOSSIER
            if (activeChild == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select or register a child above to view their vaccine checklist.", color = TextGray, textAlign = TextAlign.Center)
                }
            } else {
                // Progress rate block
                val administered = childRecords.count { it.administeredDate != null }
                val total = childRecords.size
                val percentage = if (total > 0) (administered * 100) / total else 0
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Immunisation Progress Rate", style = MaterialTheme.typography.bodySmall, color = TextGray, fontWeight = FontWeight.Medium)
                                Text("${activeChild.name}'s Medical Chart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Text("$administered / $total Doses ($percentage%)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        LinearProgressIndicator(
                            progress = { if (total > 0) administered.toFloat() / total.toFloat() else 0f },
                            color = if (percentage == 100) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            trackColor = Color(0xFFE2E8F0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (percentage == 100) "🎉 Pediatric chart completed! Fully ZEPI compliant." else "Trace target dates below. Health officers can stamp administration on-the-spot.",
                            fontSize = 11.sp,
                            color = if (percentage == 100) Color(0xFF047857) else TextGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Standard timeline checklist
                Text(
                    "Standard Sequential Timeline Checklist",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (childRecords.isEmpty()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    childRecords.forEach { rec ->
                        val isDone = rec.administeredDate != null
                        val milestone = getMilestoneForVaccine(rec.vaccineName, rec.doseNumber)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDone) Color(0xFFECFDF5) else Color.White
                            ),
                            border = BorderStroke(
                                0.5.dp, 
                                if (isDone) Color(0xFF10B981).copy(alpha = 0.5f) else Color.LightGray
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "$milestone • ${rec.vaccineName} Dose ${rec.doseNumber}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDone) Color(0xFF064E3B) else TextDark
                                        )
                                        Text(
                                            "Scheduled Target Date: ${rec.scheduledDate}", 
                                            fontSize = 11.sp, 
                                            color = if (isDone) Color(0xFF047857).copy(alpha = 0.8f) else TextGray
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isDone) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            if (isDone) "Signed Off" else "Awaiting Check",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDone) Color(0xFF065F46) else Color(0xFFB45309)
                                        )
                                    }
                                }

                                if (isDone) {
                                    val cleanBatch = if (viewModel.viewDecryptedMode) EncryptionHelper.decrypt(rec.batchNumber) else rec.batchNumber ?: "N/A"
                                    val cleanNotes = if (viewModel.viewDecryptedMode) EncryptionHelper.decrypt(rec.notes) else rec.notes ?: ""
                                    
                                    HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                                    Text("Inoculated Date: ${rec.administeredDate}", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("Administering Physician / Staff: ${rec.administeredBy ?: "Sister Staff"}", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("Efficaceous Vaccine Batch ID: $cleanBatch", fontSize = 11.sp, color = Color.DarkGray)
                                    if (cleanNotes.isNotBlank()) {
                                        Text("Ancillary Observations: \"$cleanNotes\"", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                } else {
                                    // Let health workers record administration instantly
                                    if (viewModel.currentRole == UserRole.DOCTOR_CLINIC || viewModel.currentRole == UserRole.ADMIN_MOH) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { activeAdministerRecord = rec },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Add", modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mark Administered", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        // Parent hints
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "* Bring child to nearest health wing to satisfy the national health mandate.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFB45309),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // VIEW 2: GUIDELINES REFERENCE
            OutlinedTextField(
                value = referenceSearchQuery,
                onValueChange = { referenceSearchQuery = it },
                placeholder = { Text("Filter reference antigens (e.g. BCG, TCV, PCV)...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp)
            )

            val filteredSchedule = standardZepiSchedule.filter {
                it.vaccineName.contains(referenceSearchQuery, ignoreCase = true) ||
                it.targetDiseases.contains(referenceSearchQuery, ignoreCase = true) ||
                it.ageMilestone.contains(referenceSearchQuery, ignoreCase = true)
            }

            filteredSchedule.forEach { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(item.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${item.vaccineName} (Dose ${item.doseNumber})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(item.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    item.ageMilestone,
                                    color = item.color,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Target Pathogens: ${item.targetDiseases}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Admin Site/Route: ${item.administrationRoute}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                        
                        HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Secondary dialog triggered inside Dossier Checklist
    activeAdministerRecord?.let { rec ->
        AdministerDoseDialog(
            record = rec,
            childName = activeChild?.name ?: "Patient",
            onDismiss = { activeAdministerRecord = null },
            onConfirm = { date, nurse, batch, notes ->
                viewModel.administerVaccineRecord(
                    record = rec,
                    administeredDate = date,
                    nurseName = nurse,
                    batch = batch,
                    feedbackNote = notes,
                    clinicName = "Mpilo Central Hospital Clinic"
                )
                activeAdministerRecord = null
            }
        )
    }
}

// Simple helper to align vaccine names to the ZEPI milestones
fun getMilestoneForVaccine(vaccineName: String, dose: Int): String {
    return when (vaccineName) {
        "BCG" -> "At Birth"
        "OPV" -> {
            if (dose == 0) "At Birth"
            else if (dose == 1) "6 Weeks"
            else if (dose == 2) "10 Weeks"
            else "14 Weeks"
        }
        "Rotavirus" -> if (dose == 1) "6 Weeks" else "10 Weeks"
        "Pentavalent" -> if (dose == 1) "6 Weeks" else if (dose == 2) "10 Weeks" else "14 Weeks"
        "PCV" -> if (dose == 1) "6 Weeks" else if (dose == 2) "10 Weeks" else "14 Weeks"
        "IPV" -> "14 Weeks"
        "Measles-Rubella" -> if (dose == 1) "9 Months" else "18 Months"
        "TCV" -> "9 Months"
        else -> "Pediatric Check"
    }
}

@Composable
fun AdministerDoseDialog(
    record: VaccinationRecord,
    childName: String,
    onDismiss: () -> Unit,
    onConfirm: (administeredDate: String, nurseName: String, batch: String, notes: String) -> Unit
) {
    var nurseName by remember { mutableStateOf("Nurse Farai Moyo") }
    var batchValue by remember { mutableStateOf("ZEPI-${record.vaccineName.uppercase().take(3).trim()}-${(1000..9999).random()}") }
    var administeredDate by remember { mutableStateOf("2026-06-12") } // align current local date
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "ZEPI Live Record Administration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Patient Newborn: $childName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    "Vaccine: ${record.vaccineName} (Dose ${record.doseNumber})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = administeredDate,
                    onValueChange = { administeredDate = it },
                    label = { Text("Administration Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = nurseName,
                    onValueChange = { nurseName = it },
                    label = { Text("Administering Officer (Staff Name)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = batchValue,
                    onValueChange = { batchValue = it },
                    label = { Text("Vaccine Batch Code / Lot ID") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Clinical Remarks / Vital Signs") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (administeredDate.isNotBlank() && nurseName.isNotBlank() && batchValue.isNotBlank()) {
                                onConfirm(administeredDate, nurseName, batchValue, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Apply Inoculation")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemFeedbackScreen(viewModel: AppViewModel, feedbacks: List<UserFeedback>) {
    val focusManager = LocalFocusManager.current

    // Statistics calculations
    val totalCount = feedbacks.size
    val averageRating = if (feedbacks.isNotEmpty()) feedbacks.map { it.ratingsStars }.average() else 5.0
    val resolvedCount = feedbacks.count { it.resolutionStatus == "Resolved" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("feedback_screen_container")
    ) {
        // Upper Title & Subheader with custom typography
        Text(
            text = "EPI System Support & Feedback Hub",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "To monitor regional vaccination challenges, our digital platform tracks community satisfaction, vaccine stockouts, and clinic queue reviews for prompt response.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Statistics Cards
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Consolidated Quality & Support Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", averageRating),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Average Rating Stars",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Avg Rating",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$totalCount",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Reports",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$resolvedCount",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "Resolved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Section A: Feedback Form Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Submit a Rating & Problem Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Star Rating Selection
                Text(
                    text = "How would you rate your recent experience / system performance?",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { viewModel.feedbackFormRating = i },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$i Stars",
                                tint = if (i <= viewModel.feedbackFormRating) Color(0xFFF59E0B) else Color(0xFFDEE2E6),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (viewModel.feedbackFormRating) {
                            1 -> "Critically Frustrated"
                            2 -> "Encountered Issues"
                            3 -> "Satisfactory but Slow"
                            4 -> "Pleasant Experience"
                            5 -> "Excellent / Outstanding"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 2. Category tag selection
                Text(
                    text = "What is the primary category of your concerns?",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                val categories = listOf("App Bugs", "Vaccine Stockout", "Wait Times", "Intermittent Internet", "Other")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = viewModel.feedbackFormTag == cat
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.feedbackFormTag = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // 3. Name field (optional input)
                OutlinedTextField(
                    value = viewModel.feedbackFormSubmitterName,
                    onValueChange = { viewModel.feedbackFormSubmitterName = it },
                    label = { Text("Your Registered Name (Optional)") },
                    placeholder = { 
                        Text(
                            text = when (viewModel.currentRole) {
                                UserRole.PARENT_GUARDIAN -> "e.g., Mrs. Ruvimbo Moyo"
                                UserRole.DOCTOR_CLINIC -> "e.g., Nurse Farai Moyo"
                                UserRole.ADMIN_MOH -> "e.g., Registrar Sibanda"
                            }
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )

                // 4. Feedback comment field
                OutlinedTextField(
                    value = viewModel.feedbackFormComment,
                    onValueChange = { viewModel.feedbackFormComment = it },
                    label = { Text("Details / Description of Problem") },
                    placeholder = { Text("Please explain the problem you experienced at the clinic or visual bug on the app so the MoH Registry can review.") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("feedback_comment_input"),
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )

                // Status message indicator
                if (viewModel.feedbackSubmitStatusMessage.isNotBlank()) {
                    Text(
                        text = viewModel.feedbackSubmitStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (viewModel.feedbackSubmitStatusMessage.contains("Thank")) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // 5. Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submitFeedback()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_feedback_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Submit Report")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Report & Star Rating")
                }
            }
        }

        // Section B: List of Feedbacks & Resolution Board
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Community Feedback & Support Board",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "(${feedbacks.size} items)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (feedbacks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No Feedback Matches",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No problems reported yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            feedbacks.forEach { feed ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = feed.submitterName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${feed.submitterRole} • ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(feed.timestamp))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            // Dynamic Stars visual rating indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                for (star in 1..5) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (star <= feed.ratingsStars) Color(0xFFF59E0B) else Color(0xFFDEE2E6),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (feed.problemTag) {
                                        "App Bugs" -> Color(0xFFEFF6FF)
                                        "Vaccine Stockout" -> Color(0xFFFEE2E2)
                                        "Wait Times" -> Color(0xFFFFF7ED)
                                        "Intermittent Internet" -> Color(0xFFF3E8FF)
                                        else -> Color(0xFFF3F4F6)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = feed.problemTag.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (feed.problemTag) {
                                    "App Bugs" -> Color(0xFF1E40AF)
                                    "Vaccine Stockout" -> Color(0xFF991B1B)
                                    "Wait Times" -> Color(0xFFC2410C)
                                    "Intermittent Internet" -> Color(0xFF6B21A8)
                                    else -> Color(0xFF374151)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = feed.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status tag pill
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusBg = when (feed.resolutionStatus) {
                                    "Resolved" -> Color(0xFFD1FAE5)
                                    "Under Investigation" -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFF3F4F6)
                                }
                                val statusTextTextColor = when (feed.resolutionStatus) {
                                    "Resolved" -> Color(0xFF065F46)
                                    "Under Investigation" -> Color(0xFF92400E)
                                    else -> Color(0xFF374151)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(statusBg, shape = RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = feed.resolutionStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = statusTextTextColor
                                    )
                                }
                            }

                            // Resolution control actions — ONLY FOR MOH REGISTRY ADMIN OR DOCTOR PORTS
                            if (viewModel.currentRole == UserRole.ADMIN_MOH || viewModel.currentRole == UserRole.DOCTOR_CLINIC) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (feed.resolutionStatus != "Under Investigation") {
                                        IconButton(
                                            onClick = { viewModel.updateFeedbackStatus(feed, "Under Investigation") },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Investigate Issue",
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (feed.resolutionStatus != "Resolved") {
                                        IconButton(
                                            onClick = { viewModel.updateFeedbackStatus(feed, "Resolved") },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Resolve Issue",
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteFeedback(feed) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Report",
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

