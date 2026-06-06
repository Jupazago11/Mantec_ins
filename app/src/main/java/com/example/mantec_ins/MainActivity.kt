package com.example.mantec_ins

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Log
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import com.example.mantec_ins.util.NetworkUtils
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mantec_ins.data.local.AppDatabase
import com.example.mantec_ins.data.local.DatabaseProvider
import com.example.mantec_ins.data.local.SessionManager
import com.example.mantec_ins.data.repository.AuthRepository
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import com.example.mantec_ins.data.repository.InspectionLocalRepository
import com.example.mantec_ins.data.repository.RemoteCatalogRepository
import com.example.mantec_ins.data.repository.SyncRepository
import com.example.mantec_ins.data.remote.RetrofitClient
import com.example.mantec_ins.presentation.navigation.AppScreen
import com.example.mantec_ins.presentation.ui.HomeScreen
import com.example.mantec_ins.presentation.ui.LoginScreen
import com.example.mantec_ins.presentation.ui.MainScreenHost
import com.example.mantec_ins.presentation.ui.MeasurementThicknessScreen
import com.example.mantec_ins.presentation.ui.UnsupportedRoleScreen
import com.example.mantec_ins.presentation.viewmodel.AppNavigationViewModel
import com.example.mantec_ins.presentation.viewmodel.CatalogViewModel
import com.example.mantec_ins.presentation.viewmodel.CatalogViewModelFactory
import com.example.mantec_ins.presentation.viewmodel.DashboardViewModel
import com.example.mantec_ins.presentation.viewmodel.DashboardViewModelFactory
import com.example.mantec_ins.presentation.viewmodel.InspectionViewModel
import com.example.mantec_ins.presentation.viewmodel.InspectionViewModelFactory
import com.example.mantec_ins.presentation.viewmodel.InspectorProfileViewModel
import com.example.mantec_ins.presentation.viewmodel.LoginViewModel
import com.example.mantec_ins.presentation.viewmodel.LoginViewModelFactory
import com.example.mantec_ins.presentation.viewmodel.MeasurementPendingViewModel
import com.example.mantec_ins.presentation.viewmodel.MeasurementPendingViewModelFactory
import com.example.mantec_ins.presentation.viewmodel.ReportDetailViewModel
import com.example.mantec_ins.presentation.viewmodel.MeasurementThicknessViewModel
import com.example.mantec_ins.presentation.viewmodel.MeasurementThicknessViewModelFactory
import com.example.mantec_ins.presentation.viewmodel.ReportDetailViewModelFactory
import com.example.mantec_ins.presentation.viewmodel.ReportListViewModel
import com.example.mantec_ins.presentation.viewmodel.ReportListViewModelFactory
import com.example.mantec_ins.presentation.viewmodel.SyncViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mantec_ins.data.remote.TokenExpirationEvent
import com.example.mantec_ins.presentation.viewmodel.SyncViewModelFactory
import com.example.mantec_ins.data.repository.MeasurementThicknessRepository
import com.example.mantec_ins.ui.theme.Mantec_insTheme
import com.example.mantec_ins.data.repository.WeeklyElementStatusRepository
import com.example.mantec_ins.data.repository.PendingDiagnosticsRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var authRepositoryRef: AuthRepository
    private lateinit var syncRepositoryRef: SyncRepository
    private lateinit var reportVMRef: ReportListViewModel
    private lateinit var dashboardVMRef: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar sync en background con política segura.
        // Si aún no existe agrupación local, se programará como solo WiFi.
        // Luego, al cargar el catálogo, se actualizará según group.autoSync.
        com.example.mantec_ins.sync.SyncWorkManager.start(this)

        val db = DatabaseProvider.getDatabase(this)

        val catalogRepository = CatalogLocalRepository(db)
        val inspectionRepository = InspectionLocalRepository(db)

        val syncRepository = SyncRepository(
            context = this,
            db = db,
            api = RetrofitClient.createSyncApiService(this)
        )

        val sessionManager = SessionManager(this)

        val authRepository = AuthRepository(
            apiService = RetrofitClient.createAuthApiService(this),
            sessionManager = sessionManager
        )

        val remoteCatalogRepository = RemoteCatalogRepository(
            apiService = RetrofitClient.createAuthApiService(this),
            database = db
        )

        val pendingDiagnosticsRepository = PendingDiagnosticsRepository(
            apiService = RetrofitClient.createAuthApiService(this),
            cacheDao = db.pendingDiagnosticCacheDao()
        )

        val weeklyElementStatusRepository = WeeklyElementStatusRepository(
            apiService = RetrofitClient.createAuthApiService(this),
            cacheDao = db.weeklyElementStatusCacheDao()
        )

        val measurementThicknessRepository = MeasurementThicknessRepository(
            context = this,
            database = db,
            api = RetrofitClient.createMeasurementApiService(this)
        )

        val catalogVM = ViewModelProvider(
            this,
            CatalogViewModelFactory(
                repository = catalogRepository,
                remoteCatalogRepository = remoteCatalogRepository
            )
        )[CatalogViewModel::class.java]

        val loginVM = ViewModelProvider(
            this,
            LoginViewModelFactory(
                authRepository = authRepository,
                remoteCatalogRepository = remoteCatalogRepository
            )
        )[LoginViewModel::class.java]

        val inspectionVM = ViewModelProvider(
            this,
            InspectionViewModelFactory(inspectionRepository)
        )[InspectionViewModel::class.java]

        val syncVM = ViewModelProvider(
            this,
            SyncViewModelFactory(syncRepository)
        )[SyncViewModel::class.java]

        val reportVM = ViewModelProvider(
            this,
            ReportListViewModelFactory(inspectionRepository)
        )[ReportListViewModel::class.java]

        val reportDetailVM = ViewModelProvider(
            this,
            ReportDetailViewModelFactory(
                inspectionRepository = inspectionRepository,
                catalogRepository = catalogRepository
            )
        )[ReportDetailViewModel::class.java]

        val dashboardVM = ViewModelProvider(
            this,
            DashboardViewModelFactory(
                catalogRepository = catalogRepository,
                inspectionRepository = inspectionRepository,
                pendingDiagnosticsRepository = pendingDiagnosticsRepository,
                weeklyElementStatusRepository = weeklyElementStatusRepository
            )
        )[DashboardViewModel::class.java]

        val measurementThicknessVM = ViewModelProvider(
            this,
            MeasurementThicknessViewModelFactory(
                repository = measurementThicknessRepository
            )
        )[MeasurementThicknessViewModel::class.java]

        val measurementPendingVM = ViewModelProvider(
            this,
            MeasurementPendingViewModelFactory(
                repository = measurementThicknessRepository
            )
        )[MeasurementPendingViewModel::class.java]

        val navigationVM = ViewModelProvider(this)[AppNavigationViewModel::class.java]
        val profileVM = ViewModelProvider(this)[InspectorProfileViewModel::class.java]

        authRepositoryRef = authRepository
        syncRepositoryRef = syncRepository
        reportVMRef = reportVM
        dashboardVMRef = dashboardVM

        val seedCompleted = mutableStateOf(false)

        lifecycleScope.launch {
            try {
                seedDatabase(db)
                seedCompleted.value = true
                reportVM.loadReports()
                reportVM.loadPendingSyncItems()
                reportVM.loadLocalPendingDiagnosticItemsForCurrentWeek()
                dashboardVM.loadRecentReports24h()
            } catch (e: Exception) {
                Log.e("INIT", "Error inicializando base local", e)
            }
        }

        val savedSession = authRepository.getSavedSession()

        if (savedSession == null) {
            navigationVM.goToLogin()
        } else {
            lifecycleScope.launch {
                try {
                    val session = savedSession

                    if (session.roleKey != "inspector") {
                        profileVM.setProfile(
                            userId = session.userId,
                            userName = session.userName,
                            username = session.username,
                            roleKey = session.roleKey,
                            clientId = session.clientId ?: 0L,
                            clientName = session.clientName ?: "",
                            groupId = null,
                            groupName = "",
                            groupDescription = null,
                            groupAutoSync = false,
                            specialtyId = null,
                            specialtyName = "",
                            availableElementTypes = emptyList()
                        )

                        navigationVM.goToUnsupportedRole()
                        return@launch
                    }

                    if (session.clientId == null) {
                        throw IllegalStateException("Sesión de inspector sin cliente asignado.")
                    }

                    var elementTypes = catalogRepository.getElementTypesByClient(session.clientId)
                    var group = catalogRepository.getAssignedGroup()

                    if (group == null) {
                        Log.w(
                            "SESSION_RESTORE",
                            "Sesión encontrada pero agrupación local vacía. Re-descargando offline-catalog."
                        )

                        remoteCatalogRepository.syncOfflineCatalog()

                        elementTypes = catalogRepository.getElementTypesByClient(session.clientId)
                        group = catalogRepository.getAssignedGroup()
                    }

                    val singleType = if (elementTypes.size == 1) elementTypes.first() else null

                    profileVM.setProfile(
                        userId = session.userId,
                        userName = session.userName,
                        username = session.username,
                        roleKey = session.roleKey,
                        clientId = session.clientId,
                        clientName = session.clientName ?: "",
                        groupId = group?.id,
                        groupName = group?.name ?: "",
                        groupDescription = group?.description,
                        groupAutoSync = group?.autoSync ?: false,
                        specialtyId = singleType?.id,
                        specialtyName = singleType?.name ?: "",
                        availableElementTypes = elementTypes
                    )

                    navigationVM.goToHome()
                } catch (e: Exception) {
                    Log.e(
                        "SESSION_RESTORE",
                        "Error restaurando sesión/catálogo offline",
                        e
                    )

                    authRepository.logout()
                    profileVM.clearProfile()
                    navigationVM.goToLogin()
                }
            }
        }

        fun refreshCatalogAndWorkPolicyAfterSync() {
            lifecycleScope.launch {
                try {
                    remoteCatalogRepository.syncOfflineCatalog()
                    com.example.mantec_ins.sync.SyncWorkManager.start(this@MainActivity)

                    authRepository.getSavedSession()?.let { session ->
                        if (session.clientId != null) {
                            val elementTypes = catalogRepository.getElementTypesByClient(session.clientId)
                            val group = catalogRepository.getAssignedGroup()
                            val singleType = if (elementTypes.size == 1) elementTypes.first() else null

                            profileVM.setProfile(
                                userId = session.userId,
                                userName = session.userName,
                                username = session.username,
                                roleKey = session.roleKey,
                                clientId = session.clientId,
                                clientName = session.clientName ?: "",
                                groupId = group?.id,
                                groupName = group?.name ?: "",
                                groupDescription = group?.description,
                                groupAutoSync = group?.autoSync ?: false,
                                specialtyId = singleType?.id,
                                specialtyName = singleType?.name ?: "",
                                availableElementTypes = elementTypes
                            )
                        }
                    }

                    Log.d("CATALOG_REFRESH", "Catálogo y política WorkManager actualizados")
                } catch (e: Exception) {
                    Log.e("CATALOG_REFRESH", "Error refrescando catálogo tras sync", e)
                }
            }
        }

        fun trySyncPendingIfLoggedIn(
            autoSync: Boolean,
            selectedElementIdProvider: () -> Long?,
            selectedAreaIdProvider: () -> Long?,
            selectedElementTypeIdProvider: () -> Long?,
            visibleElementsProvider: () -> List<com.example.mantec_ins.data.local.ElementEntity> = { emptyList() },
            refreshCatalogAfterSync: Boolean = false,
            onBlocked: (String) -> Unit = {},
            onFinished: (Int) -> Unit = {}
        ) {
            val session = authRepository.getSavedSession() ?: return

            if (session.roleKey != "inspector") {
                return
            }

            if (!NetworkUtils.canSyncByPolicy(this, autoSync)) {
                val message = if (autoSync) {
                    "No hay conexión a internet para sincronizar."
                } else {
                    "Esta agrupación tiene sincronización manual. Usa el botón de sincronización desde el inicio."
                }

                Log.w("AUTO_SYNC", message)
                onBlocked(message)
                onFinished(0)
                return
            }

            lifecycleScope.launch {
                try {
                    Log.d(
                        "AUTO_SYNC",
                        "Intentando sincronizar pendientes para userId=${session.userId}, autoSync=$autoSync"
                    )

                    val syncedCount = syncRepository.syncPendingReports()

                    if (refreshCatalogAfterSync) {
                        remoteCatalogRepository.syncOfflineCatalog()
                        com.example.mantec_ins.sync.SyncWorkManager.start(this@MainActivity)
                    }

                    reportVM.loadPendingSyncItems()
                    reportVM.loadReports()
                    reportVM.loadLocalPendingDiagnosticItemsForCurrentWeek()
                    dashboardVM.loadRecentReports24h()

                    selectedElementIdProvider()?.let { elementId ->
                        dashboardVM.loadPendingDiagnosticsForElement(
                            elementId = elementId,
                            tryServerRefresh = true
                        )
                    }

                    val visibleElements = visibleElementsProvider()

                    if (visibleElements.isNotEmpty()) {
                        dashboardVM.loadWeeklyElementsStatusForElements(
                            elements = visibleElements,
                            tryServerRefresh = true
                        )
                    }

                    Log.d(
                        "AUTO_SYNC",
                        "Sincronización finalizada. syncedCount=$syncedCount"
                    )

                    onFinished(syncedCount)
                } catch (e: Exception) {
                    Log.e("AUTO_SYNC", "Error en sincronización", e)
                    onFinished(0)
                }
            }
        }


        setContent {
            Mantec_insTheme {
                val seedReady = seedCompleted.value

                val profile by profileVM.profile.collectAsState()
                val availableElementTypes = profile.availableElementTypes
                val selectedElementTypeId = profile.specialtyId


                val areas by catalogVM.areas.collectAsState()
                val elements by catalogVM.elements.collectAsState()
                val components by catalogVM.components.collectAsState()
                val diagnostics by catalogVM.diagnostics.collectAsState()
                val conditions by catalogVM.conditions.collectAsState()

                val selectedElementId by catalogVM.selectedElementId.collectAsState()
                val selectedComponentId by catalogVM.selectedComponentId.collectAsState()

                val inspectionState by inspectionVM.uiState.collectAsState()
                val loginState by loginVM.uiState.collectAsState()
                val reports by reportVM.reports.collectAsState()
                val pendingSyncItems by reportVM.pendingSyncItems.collectAsState()
                val localPendingDiagnosticItems by reportVM.localPendingDiagnosticItems.collectAsState()
                val reportDetailState by reportDetailVM.uiState.collectAsState()
                val dashboardState by dashboardVM.uiState.collectAsState()
                val currentScreen by navigationVM.currentScreen.collectAsState()

                val measurementState by measurementThicknessVM.uiState.collectAsState()
                val pendingMeasurementDraftCount by measurementPendingVM.pendingDraftCount.collectAsState()
                val hasMeasurementAccess by measurementPendingVM.hasMeasurementAccess.collectAsState()

                var selectedAreaId by remember { mutableStateOf<Long?>(null) }
                var photoUri by remember { mutableStateOf<Uri?>(null) }
                var videoUri by remember { mutableStateOf<Uri?>(null) }
                var homeSyncMessage by remember { mutableStateOf<String?>(null) }
                var homeSyncWarning by remember { mutableStateOf<String?>(null) }
                var isManualSyncRunning by remember { mutableStateOf(false) }
                var connectionLabel by remember { mutableStateOf(NetworkUtils.connectionLabel(this)) }
                var pendingTakePhoto by remember { mutableStateOf(false) }
                var pendingRecordVideo by remember { mutableStateOf(false) }



                val takePictureLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success && photoUri != null) {
                        inspectionVM.addEvidence(photoUri.toString(), "image")
                    }
                }

                val captureVideoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CaptureVideo()
                ) { success ->
                    if (success && videoUri != null) {
                        inspectionVM.addEvidence(videoUri.toString(), "video")
                    }
                }

                val pickFromGalleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris ->
                    uris.forEach { uri ->
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: SecurityException) { }
                        val mimeType = contentResolver.getType(uri) ?: ""
                        val mediaType = if (mimeType.startsWith("video")) "video" else "image"
                        inspectionVM.addEvidence(uri.toString(), mediaType)
                    }
                }

                fun createImageUri(): Uri? {
                    val resolver = applicationContext.contentResolver
                    val contentValues = ContentValues().apply {
                        put(
                            MediaStore.Images.Media.DISPLAY_NAME,
                            "mantec_${System.currentTimeMillis()}.jpg"
                        )
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                "Pictures/ManTec"
                            )
                        }
                    }
                    return resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                }

                fun createVideoUri(): Uri? {
                    val resolver = applicationContext.contentResolver
                    val contentValues = ContentValues().apply {
                        put(
                            MediaStore.Video.Media.DISPLAY_NAME,
                            "mantec_${System.currentTimeMillis()}.mp4"
                        )
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                MediaStore.Video.Media.RELATIVE_PATH,
                                "Movies/ManTec"
                            )
                        }
                    }
                    return resolver.insert(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                }

                val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        if (pendingTakePhoto) {
                            pendingTakePhoto = false
                            val uri = createImageUri()
                            if (uri != null) {
                                photoUri = uri
                                takePictureLauncher.launch(uri)
                            }
                        } else if (pendingRecordVideo) {
                            pendingRecordVideo = false
                            val uri = createVideoUri()
                            if (uri != null) {
                                videoUri = uri
                                captureVideoLauncher.launch(uri)
                            }
                        }
                    } else {
                        pendingTakePhoto = false
                        pendingRecordVideo = false
                    }
                }

                val speechRecognizerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val matches = result.data
                            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val recognized = matches?.firstOrNull()
                        if (!recognized.isNullOrBlank()) {
                            val current = inspectionVM.uiState.value.recommendation
                            val updated = if (current.isBlank()) recognized
                                         else "$current $recognized"
                            inspectionVM.setRecommendation(updated)
                        }
                    }
                }

                val requestAudioPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta la recomendación")
                        }
                        speechRecognizerLauncher.launch(intent)
                    }
                }

                LaunchedEffect(currentScreen, elements) {
                    if (
                        currentScreen == AppScreen.Report &&
                        elements.isNotEmpty()
                    ) {
                        dashboardVM.loadWeeklyElementsStatusForElements(
                            elements = elements,
                            tryServerRefresh = true
                        )
                    }
                }

                LaunchedEffect(profile.clientId, seedReady) {
                    if (seedReady && profile.clientId != 0L && profile.availableElementTypes.isEmpty()) {
                        val elementTypes = catalogRepository.getElementTypesByClient(profile.clientId)
                        profileVM.setAvailableElementTypes(elementTypes)
                    }
                }


                LaunchedEffect(profile.clientId, profile.groupId, seedReady) {
                    if (seedReady && profile.clientId != 0L && profile.groupId != null) {
                        catalogVM.loadAreas(profile.clientId)
                        catalogVM.loadElementsByGroup(profile.groupId!!)
                    } else {
                        catalogVM.clearElements()
                        catalogVM.clearComponents()
                        catalogVM.clearDiagnostics()
                        catalogVM.clearConditions()
                    }
                }


                LaunchedEffect(currentScreen, profile.groupAutoSync) {
                    connectionLabel = NetworkUtils.connectionLabel(this@MainActivity)

                    if (currentScreen == AppScreen.Home && profile.roleKey == "inspector") {
                        if (profile.groupAutoSync) {
                            trySyncPendingIfLoggedIn(
                                autoSync = profile.groupAutoSync,
                                selectedElementIdProvider = { selectedElementId },
                                selectedAreaIdProvider = { selectedAreaId },
                                selectedElementTypeIdProvider = { selectedElementTypeId },
                                visibleElementsProvider = { elements },
                                refreshCatalogAfterSync = false,
                                onBlocked = { message ->
                                    homeSyncWarning = message
                                }
                            ) { syncedCount ->
                                lifecycleScope.launch {
                                    val syncedMeasurementDrafts = measurementThicknessRepository.syncAllPendingDrafts()
                                    measurementPendingVM.loadPendingDraftCount()

                                    when {
                                        syncedCount > 0 && syncedMeasurementDrafts > 0 -> {
                                            homeSyncMessage =
                                                "Se sincronizaron $syncedCount reportes y $syncedMeasurementDrafts borradores de mediciones."
                                        }

                                        syncedCount > 0 -> {
                                            homeSyncMessage = if (syncedCount == 1) {
                                                "Se subió 1 reporte correctamente."
                                            } else {
                                                "Se subieron $syncedCount reportes correctamente."
                                            }
                                        }

                                        syncedMeasurementDrafts > 0 -> {
                                            homeSyncMessage = if (syncedMeasurementDrafts == 1) {
                                                "Se sincronizó 1 borrador de mediciones."
                                            } else {
                                                "Se sincronizaron $syncedMeasurementDrafts borradores de mediciones."
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }



                LaunchedEffect(inspectionState.saveSuccess) {
                    if (inspectionState.saveSuccess) {
                        if (profile.groupAutoSync) {
                            trySyncPendingIfLoggedIn(
                                autoSync = true,
                                selectedElementIdProvider = { selectedElementId },
                                selectedAreaIdProvider = { selectedAreaId },
                                selectedElementTypeIdProvider = { selectedElementTypeId },
                                visibleElementsProvider = { elements },
                                refreshCatalogAfterSync = true,
                                onBlocked = { message ->
                                    homeSyncWarning = message
                                }
                            ) { syncedCount ->
                                Log.d(
                                    "SAVE_REFRESH",
                                    "Sincronización automática tras guardar finalizada. syncedCount=$syncedCount"
                                )

                                reportVM.loadPendingSyncItems()
                                reportVM.loadReports()
                                reportVM.loadLocalPendingDiagnosticItemsForCurrentWeek()
                                dashboardVM.loadRecentReports24h()

                                inspectionVM.clearSaveState()
                            }
                        } else {
                            reportVM.loadPendingSyncItems()
                            reportVM.loadReports()
                            reportVM.loadLocalPendingDiagnosticItemsForCurrentWeek()
                            dashboardVM.loadRecentReports24h()

                            inspectionVM.clearSaveState()
                        }
                    }
                }



                LaunchedEffect(homeSyncMessage) {
                    if (homeSyncMessage != null) {
                        delay(3000)
                        homeSyncMessage = null
                    }
                }

                LaunchedEffect(homeSyncWarning) {
                    if (homeSyncWarning != null) {
                        delay(5000)
                        homeSyncWarning = null
                    }
                }

                LaunchedEffect(currentScreen) {
                    if (currentScreen == AppScreen.MeasurementThickness) {
                        measurementThicknessVM.loadInitialData()
                    }
                }

                LaunchedEffect(currentScreen) {
                    if (currentScreen == AppScreen.Home) {
                        measurementPendingVM.loadPendingDraftCount()
                        measurementPendingVM.loadMeasurementAccess()
                    }
                }


                LaunchedEffect(Unit) {
                    TokenExpirationEvent.flow.collect {
                        authRepository.logout()
                        profileVM.clearProfile()
                        selectedAreaId = null
                        catalogVM.clearAllSelections()
                        inspectionVM.clearSelectionsFromAreaChange()
                        navigationVM.logout()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        AppScreen.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF8F4EE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = Color(0xFFD94D33)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Cargando...",
                                        fontSize = 16.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }

                        AppScreen.Login -> {
                            var username by remember { mutableStateOf("") }
                            var password by remember { mutableStateOf("") }

                            LaunchedEffect(loginState.loginSuccess) {
                                if (loginState.loginSuccess) {
                                    authRepository.getSavedSession()?.let { session ->
                                        if (session.roleKey != "inspector") {
                                            profileVM.setProfile(
                                                userId = session.userId,
                                                userName = session.userName,
                                                username = session.username,
                                                roleKey = session.roleKey,
                                                clientId = session.clientId ?: 0L,
                                                clientName = session.clientName ?: "",
                                                groupId = null,
                                                groupName = "",
                                                groupDescription = null,
                                                groupAutoSync = false,
                                                specialtyId = null,
                                                specialtyName = "",
                                                availableElementTypes = emptyList()
                                            )

                                            selectedAreaId = null
                                            catalogVM.clearAllSelections()
                                            inspectionVM.clearSelectionsFromAreaChange()

                                            navigationVM.goToUnsupportedRole()
                                            return@let
                                        }

                                        if (session.clientId != null) {
                                            val elementTypes = catalogRepository.getElementTypesByClient(session.clientId)
                                            val group = catalogRepository.getAssignedGroup()
                                            val singleType = if (elementTypes.size == 1) elementTypes.first() else null

                                            profileVM.setProfile(
                                                userId = session.userId,
                                                userName = session.userName,
                                                username = session.username,
                                                roleKey = session.roleKey,
                                                clientId = session.clientId,
                                                clientName = session.clientName ?: "",
                                                groupId = group?.id,
                                                groupName = group?.name ?: "",
                                                groupDescription = group?.description,
                                                groupAutoSync = group?.autoSync ?: false,
                                                specialtyId = singleType?.id,
                                                specialtyName = singleType?.name ?: "",
                                                availableElementTypes = elementTypes
                                            )

                                            selectedAreaId = null
                                            catalogVM.clearAllSelections()
                                            inspectionVM.clearSelectionsFromAreaChange()

                                            navigationVM.goToHome()
                                        }
                                    }
                                }
                            }


                            LoginScreen(
                                username = username,
                                password = password,
                                isLoading = loginState.isLoading,
                                errorMessage = loginState.errorMessage,
                                onUsernameChange = { username = it },
                                onPasswordChange = { password = it },
                                onLoginClick = {
                                    loginVM.login(username, password)
                                }
                            )
                            if (loginState.isLoading) {
                                AlertDialog(
                                    onDismissRequest = {
                                        // Bloqueado intencionalmente mientras termina la sincronización.
                                    },
                                    confirmButton = {},
                                    title = null,
                                    text = {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(42.dp)
                                            )

                                            Text(
                                                text = "Sincronizando...",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        AppScreen.Home -> {
                            HomeScreen(
                                userName = profile.userName,
                                groupName = profile.groupName,
                                groupAutoSync = profile.groupAutoSync,
                                connectionLabel = connectionLabel,
                                pendingSyncItems = pendingSyncItems,
                                pendingMeasurementDraftCount = pendingMeasurementDraftCount,
                                showMeasurementsButton = hasMeasurementAccess,
                                syncSuccessMessage = homeSyncMessage,
                                syncWarningMessage = homeSyncWarning,
                                isManualSyncRunning = isManualSyncRunning,
                                onManualSyncClick = {
                                    connectionLabel = NetworkUtils.connectionLabel(this@MainActivity)

                                    if (!NetworkUtils.canManualSync(this@MainActivity)) {
                                        homeSyncWarning = "No hay conexión a internet para sincronizar."
                                        return@HomeScreen
                                    }

                                    if (isManualSyncRunning) {
                                        return@HomeScreen
                                    }

                                    isManualSyncRunning = true

                                    trySyncPendingIfLoggedIn(
                                        autoSync = true,
                                        selectedElementIdProvider = { selectedElementId },
                                        selectedAreaIdProvider = { selectedAreaId },
                                        selectedElementTypeIdProvider = { selectedElementTypeId },
                                        visibleElementsProvider = { elements },
                                        refreshCatalogAfterSync = true,
                                        onBlocked = { message ->
                                            homeSyncWarning = message
                                        }
                                    ) { syncedCount ->
                                        lifecycleScope.launch {
                                            val syncedMeasurementDrafts = measurementThicknessRepository.syncAllPendingDrafts()
                                            measurementPendingVM.loadPendingDraftCount()

                                            isManualSyncRunning = false

                                            homeSyncMessage = when {
                                                syncedCount > 0 && syncedMeasurementDrafts > 0 -> {
                                                    "Se sincronizaron $syncedCount reportes y $syncedMeasurementDrafts borradores de mediciones."
                                                }

                                                syncedCount == 1 -> {
                                                    "Se subió 1 reporte correctamente y se actualizó la información."
                                                }

                                                syncedCount > 1 -> {
                                                    "Se subieron $syncedCount reportes correctamente y se actualizó la información."
                                                }

                                                syncedMeasurementDrafts == 1 -> {
                                                    "Se sincronizó 1 borrador de mediciones y se actualizó la información."
                                                }

                                                syncedMeasurementDrafts > 1 -> {
                                                    "Se sincronizaron $syncedMeasurementDrafts borradores de mediciones y se actualizó la información."
                                                }

                                                else -> {
                                                    "No había reportes ni borradores de mediciones pendientes por sincronizar. La información fue actualizada."
                                                }
                                            }

                                            connectionLabel = NetworkUtils.connectionLabel(this@MainActivity)
                                        }
                                    }
                                },
                                onGoToReport = {
                                    navigationVM.goToReport()
                                },
                                onGoToMeasurements = {
                                    navigationVM.goToMeasurementThickness()
                                },
                                onLogout = {
                                    authRepository.logout()
                                    profileVM.clearProfile()
                                    selectedAreaId = null
                                    catalogVM.clearAllSelections()
                                    inspectionVM.clearSelectionsFromAreaChange()
                                    navigationVM.logout()
                                }
                            )

                        }

                        AppScreen.UnsupportedRole -> {
                            UnsupportedRoleScreen(
                                userName = profile.userName,
                                roleKey = profile.roleKey,
                                onLogout = {
                                    authRepository.logout()
                                    profileVM.clearProfile()
                                    selectedAreaId = null
                                    catalogVM.clearAllSelections()
                                    inspectionVM.clearSelectionsFromAreaChange()
                                    navigationVM.logout()
                                }
                            )
                        }

                        AppScreen.Report -> {
                            MainScreenHost(
                                clientName = profile.clientName,
                                groupName = profile.groupName,
                                availableElementTypes = availableElementTypes,
                                selectedAreaId = selectedAreaId,
                                selectedElementTypeId = selectedElementTypeId,


                                areas = areas,
                                elements = elements,
                                components = components,
                                diagnostics = diagnostics,
                                conditions = conditions,
                                selectedCatalogElementId = selectedElementId,
                                selectedCatalogComponentId = selectedComponentId,
                                inspectionUiState = inspectionState,
                                reports = reports,
                                reportDetailUiState = reportDetailState,
                                dashboardUiState = dashboardState,
                                localPendingDiagnosticItems = localPendingDiagnosticItems,
                                onAreaClick = { areaId ->
                                    selectedAreaId = areaId

                                    catalogVM.clearElementSelection()
                                    catalogVM.clearComponentSelection()
                                    catalogVM.clearComponents()
                                    catalogVM.clearDiagnostics()
                                    catalogVM.clearConditions()

                                    inspectionVM.clearSelectionsFromAreaChange()
                                },

                                onElementTypeClick = { elementTypeId ->
                                    val selectedType = availableElementTypes.firstOrNull { it.id == elementTypeId }

                                    profileVM.setSelectedSpecialty(
                                        specialtyId = elementTypeId,
                                        specialtyName = selectedType?.name ?: ""
                                    )

                                    catalogVM.clearElementSelection()
                                    catalogVM.clearComponentSelection()
                                    catalogVM.clearComponents()
                                    catalogVM.clearDiagnostics()
                                    catalogVM.clearConditions()

                                    inspectionVM.clearSelectionsFromAreaChange()
                                },
                                onElementClick = { elementId ->
                                    val selectedElement = elements.firstOrNull { it.id == elementId }

                                    selectedAreaId = selectedElement?.areaId

                                    selectedElement?.let { element ->
                                        val selectedType = availableElementTypes.firstOrNull { it.id == element.elementTypeId }

                                        profileVM.setSelectedSpecialty(
                                            specialtyId = element.elementTypeId,
                                            specialtyName = selectedType?.name ?: ""
                                        )
                                    }

                                    catalogVM.onElementSelected(elementId)
                                    inspectionVM.setSelectedElement(elementId)

                                    catalogVM.clearComponents()
                                    catalogVM.clearDiagnostics()
                                    catalogVM.clearConditions()

                                    catalogVM.loadComponents(elementId)

                                    dashboardVM.loadPendingDiagnosticsForElement(
                                        elementId = elementId,
                                        tryServerRefresh = true
                                    )

                                    dashboardVM.loadWeeklyElementsStatusForElements(
                                        elements = elements,
                                        tryServerRefresh = true
                                    )
                                },
                                onComponentClick = { componentId ->
                                    catalogVM.onComponentSelected(componentId)
                                    inspectionVM.setSelectedComponent(componentId)

                                    catalogVM.clearDiagnostics()
                                    catalogVM.clearConditions()

                                    catalogVM.loadDiagnostics(componentId)

                                    selectedElementId?.let { elementId ->
                                        catalogVM.loadConditionsForComponent(
                                            elementId = elementId,
                                            componentId = componentId
                                        )
                                    }
                                },
                                onDiagnosticClick = { diagnosticId ->
                                    inspectionVM.setSelectedDiagnostic(diagnosticId)
                                },
                                onConditionClick = { conditionId ->
                                    inspectionVM.setSelectedCondition(conditionId)
                                },
                                onRecommendationChange = { value ->
                                    inspectionVM.setRecommendation(value)
                                },
                                onBeltChangeSelected = { value ->
                                    inspectionVM.setIsBeltChange(value)
                                },
                                onVoiceInputClick = {
                                    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta la recomendación")
                                        }
                                        speechRecognizerLauncher.launch(intent)
                                    } else {
                                        requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                onTakePhotoClick = {
                                    if (checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        val uri = createImageUri()
                                        if (uri != null) {
                                            photoUri = uri
                                            takePictureLauncher.launch(uri)
                                        }
                                    } else {
                                        pendingTakePhoto = true
                                        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                onRecordVideoClick = {
                                    if (checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        val uri = createVideoUri()
                                        if (uri != null) {
                                            videoUri = uri
                                            captureVideoLauncher.launch(uri)
                                        }
                                    } else {
                                        pendingRecordVideo = true
                                        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                onPickFromGallery = {
                                    pickFromGalleryLauncher.launch(
                                        PickVisualMediaRequest(PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onRemoveEvidenceClick = { path ->
                                    inspectionVM.removeEvidence(path)
                                },
                                onBackClick = {
                                    navigationVM.goToHome()
                                },
                                onSaveClick = {
                                    val selectedElement = elements.firstOrNull { it.id == selectedElementId }
                                    val resolvedAreaId = selectedElement?.areaId ?: selectedAreaId ?: 0L

                                    inspectionVM.saveInspectionReport(
                                        clientId = profile.clientId,
                                        areaId = resolvedAreaId,
                                        userId = profile.userId
                                    )

                                    reportVM.loadReports()
                                    reportVM.loadPendingSyncItems()
                                    reportVM.loadLocalPendingDiagnosticItemsForCurrentWeek()
                                    dashboardVM.loadRecentReports24h()
                                },
                                onReportClick = { reportLocalId ->
                                    reportDetailVM.loadReportDetail(reportLocalId)
                                }
                            )
                        }

                        AppScreen.MeasurementThickness -> {
                            MeasurementThicknessScreen(
                                clientName = profile.clientName,
                                uiState = measurementState,
                                onBackClick = {
                                    navigationVM.goToHome()
                                },
                                onElementTypeSelected = { elementType ->
                                    measurementThicknessVM.selectElementType(elementType)
                                },
                                onAreaSelected = { area ->
                                    measurementThicknessVM.selectArea(area)
                                },
                                onElementSelected = { element ->
                                    measurementThicknessVM.selectElement(
                                        clientId = profile.clientId,
                                        element = element
                                    )
                                },
                                onLineValueChange = { coverNumber, field, value ->
                                    measurementThicknessVM.updateLineValue(
                                        coverNumber = coverNumber,
                                        field = field,
                                        rawValue = value
                                    )
                                },
                                onAddCoverClick = {
                                    measurementThicknessVM.addCover()
                                },
                                onRemoveLastCoverClick = {
                                    measurementThicknessVM.removeLastCover()
                                },
                                onSaveDraftClick = {
                                    measurementThicknessVM.saveDraft(
                                        clientId = profile.clientId,
                                        userId = profile.userId
                                    )

                                    measurementPendingVM.loadPendingDraftCount()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val session = authRepositoryRef.getSavedSession() ?: return

        if (session.roleKey != "inspector") {
            return
        }

        lifecycleScope.launch {
            try {
                val db = DatabaseProvider.getDatabase(this@MainActivity)
                val group = db.groupDao().getFirst()

                val autoSync = group?.autoSync ?: false

                if (!autoSync) {
                    Log.d(
                        "AUTO_SYNC",
                        "onResume -> no sincroniza porque la agrupación no está habilitada."
                    )
                    return@launch
                }

                if (!NetworkUtils.canSyncByPolicy(this@MainActivity, autoSync)) {
                    Log.w(
                        "AUTO_SYNC",
                        "onResume -> no hay conexión para sincronización automática."
                    )
                    return@launch
                }

                Log.d(
                    "AUTO_SYNC",
                    "onResume -> intentando sincronizar pendientes para userId=${session.userId}, autoSync=$autoSync"
                )

                val syncedCount = syncRepositoryRef.syncPendingReports()
                val measurementRepository = MeasurementThicknessRepository(
                    context = this@MainActivity,
                    database = db,
                    api = RetrofitClient.createMeasurementApiService(this@MainActivity)
                )

                val syncedMeasurementDrafts = measurementRepository.syncAllPendingDrafts()

                if (autoSync) {
                    // Si la agrupación permite sync automático, también refrescamos catálogo.
                    val remoteCatalogRepository = RemoteCatalogRepository(
                        apiService = RetrofitClient.createAuthApiService(this@MainActivity),
                        database = db
                    )
                    remoteCatalogRepository.syncOfflineCatalog()
                    com.example.mantec_ins.sync.SyncWorkManager.start(this@MainActivity)
                }

                reportVMRef.loadPendingSyncItems()
                reportVMRef.loadReports()
                dashboardVMRef.loadRecentReports24h()

                val selectedElementId = reportVMRef
                    .reports
                    .value
                    .firstOrNull()
                    ?.elementId

                selectedElementId?.let { elementId ->
                    dashboardVMRef.loadPendingDiagnosticsForElement(
                        elementId = elementId,
                        tryServerRefresh = true
                    )
                }

                Log.d(
                    "AUTO_SYNC",
                    "onResume -> sincronización completada. syncedCount=$syncedCount, syncedMeasurementDrafts=$syncedMeasurementDrafts"
                )
            } catch (e: Exception) {
                Log.e("AUTO_SYNC", "onResume -> error sincronizando", e)
            }
        }
    }





    private suspend fun seedDatabase(db: AppDatabase) {
        /*
        val clientDao = db.clientDao()
        val areaDao = db.areaDao()
        val typeDao = db.elementTypeDao()
        val elementDao = db.elementDao()
        val componentDao = db.componentDao()
        val diagnosticDao = db.diagnosticDao()
        val conditionDao = db.conditionDao()
        val ecDao = db.elementComponentDao()
        val cdDao = db.componentDiagnosticDao()

        clientDao.deleteAll()
        areaDao.deleteAll()
        typeDao.deleteAll()
        elementDao.deleteAll()
        componentDao.deleteAll()
        diagnosticDao.deleteAll()
        conditionDao.deleteAll()
        ecDao.deleteAll()
        cdDao.deleteAll()

        // =========================
        // CLIENTE
        // =========================
        clientDao.insertAll(
            listOf(
                ClientEntity(
                    id = 1,
                    name = "CORONA",
                    obs = "Cliente base de pruebas",
                    status = true
                )
            )
        )

        // =========================
        // TIPO DE ACTIVO
        // =========================
        typeDao.insertAll(
            listOf(
                ElementTypeEntity(
                    id = 100,
                    clientId = 1,
                    name = "Banda transportadora",
                    description = null,
                    status = true
                )
            )
        )

        // =========================
        // ÁREAS
        // =========================
        areaDao.insertAll(
            listOf(
                AreaEntity(10, 1, "TRITURACION", "TRI", true),
                AreaEntity(11, 1, "PREHOMO", "PRH", true),
                AreaEntity(12, 1, "APILADO ADITIVOS", "APA", true),
                AreaEntity(13, 1, "RECLAMADOR ADITIVOS", "REA", true),
                AreaEntity(14, 1, "APILADO CARBON", "APC", true),
                AreaEntity(15, 1, "MOLINO CARBON", "MCA", true),
                AreaEntity(16, 1, "MOLINO CRUDO", "MCR", true),
                AreaEntity(17, 1, "MOLINO CEMENTO", "MCE", true),
                AreaEntity(18, 1, "ALTERNOS", "ALT", true)
            )
        )

        // =========================
        // ACTIVOS
        // =========================
        elementDao.insertAll(
            listOf(
                // TRITURACION
                ElementEntity(1000, 10, 100, "211BT01", "211BT01", null, true),
                ElementEntity(1001, 10, 100, "291BT01", "291BT01", null, true),
                ElementEntity(1002, 10, 100, "291SM01", "291SM01", null, true),
                ElementEntity(1003, 10, 100, "291BT03", "291BT03", null, true),

                // PREHOMO
                ElementEntity(1100, 11, 100, "291BT04", "291BT04", null, true),
                ElementEntity(1101, 11, 100, "311BT01", "311BT01", null, true),
                ElementEntity(1102, 11, 100, "311BT02", "311BT02", null, true),
                ElementEntity(1103, 11, 100, "291BT02", "291BT02", null, true),

                // APILADO ADITIVOS
                ElementEntity(1200, 12, 100, "K11BT01", "K11BT01", null, true),
                ElementEntity(1201, 12, 100, "K11BT02", "K11BT02", null, true),
                ElementEntity(1202, 12, 100, "K11BT03", "K11BT03", null, true),
                ElementEntity(1203, 12, 100, "K21BT01", "K21BT01", null, true),
                ElementEntity(1204, 12, 100, "K21BT02", "K21BT02", null, true),

                // RECLAMADOR ADITIVOS
                ElementEntity(1300, 13, 100, "K91BT01", "K91BT01", null, true),
                ElementEntity(1301, 13, 100, "K91BT02", "K91BT02", null, true),
                ElementEntity(1302, 13, 100, "K91BT03", "K91BT03", null, true),

                // APILADO CARBON
                ElementEntity(1400, 14, 100, "L11BT01", "L11BT01", null, true),
                ElementEntity(1401, 14, 100, "L11BT02", "L11BT02", null, true),
                ElementEntity(1402, 14, 100, "L11BT03", "L11BT03", null, true),
                ElementEntity(1403, 14, 100, "L11BT04", "L11BT04", null, true),
                ElementEntity(1404, 14, 100, "L11BT05", "L11BT05", null, true),
                ElementEntity(1405, 14, 100, "L11BT06", "L11BT06", null, true),

                // MOLINO CARBON
                ElementEntity(1500, 15, 100, "L21BT01", "L21BT01", null, true),
                ElementEntity(1501, 15, 100, "L21BT02", "L21BT02", null, true),
                ElementEntity(1502, 15, 100, "L21BT03", "L21BT03", null, true),
                ElementEntity(1503, 15, 100, "L21SM01", "L21SM01", null, true),
                ElementEntity(1504, 15, 100, "L61BP01", "L61BP01", null, true),

                // MOLINO CRUDO
                ElementEntity(1600, 16, 100, "K91BT05", "K91BT05", null, true),
                ElementEntity(1601, 16, 100, "321BP01", "321BP01", null, true),
                ElementEntity(1602, 16, 100, "321BP02", "321BP02", null, true),
                ElementEntity(1603, 16, 100, "321BP03", "321BP03", null, true),
                ElementEntity(1604, 16, 100, "331BT01", "331BT01", null, true),
                ElementEntity(1605, 16, 100, "361BT01", "361BT01", null, true),
                ElementEntity(1606, 16, 100, "361BT02", "361BT02", null, true),
                ElementEntity(1607, 16, 100, "361BT03", "361BT03", null, true),
                ElementEntity(1608, 16, 100, "K91BT04", "K91BT04", null, true),

                // MOLINO CEMENTO
                ElementEntity(1700, 17, 100, "K91BT04", "K91BT04", null, true),
                ElementEntity(1701, 17, 100, "521BT01", "521BT01", null, true),
                ElementEntity(1702, 17, 100, "521BT02", "521BT02", null, true),
                ElementEntity(1703, 17, 100, "521BP01", "521BP01", null, true),
                ElementEntity(1704, 17, 100, "521BP02", "521BP02", null, true),
                ElementEntity(1705, 17, 100, "521BP03", "521BP03", null, true),
                ElementEntity(1706, 17, 100, "531BT01", "531BT01", null, true),
                ElementEntity(1707, 17, 100, "511BT01", "511BT01", null, true),
                ElementEntity(1708, 17, 100, "511BP01", "511BP01", null, true),
                ElementEntity(1709, 17, 100, "561BT01", "561BT01", null, true),
                ElementEntity(1710, 17, 100, "561SM01", "561SM01", null, true),
                ElementEntity(1711, 17, 100, "561BT02", "561BT02", null, true),
                ElementEntity(1712, 17, 100, "561BT03", "561BT03", null, true),
                ElementEntity(1713, 17, 100, "561BT04", "561BT04", null, true),
                ElementEntity(1714, 17, 100, "491BT01", "491BT01", null, true),

                // ALTERNOS
                ElementEntity(1800, 18, 100, "L71BP01", "L71BP01", null, true),
                ElementEntity(1801, 18, 100, "L71TN01", "L71TN01", null, true)
            )
        )

        // =========================
        // COMPONENTES
        // =========================
        componentDao.insertAll(
            listOf(
                ComponentEntity(2000, 1, "Banda", null, 100, false, false, true),
                ComponentEntity(2001, 1, "Chute de descarga", null, 100, false, false, true),
                ComponentEntity(2002, 1, "Encausadores", null, 100, false, false, true),
                ComponentEntity(2003, 1, "Guardilla", null, 100, false, false, true),
                ComponentEntity(2004, 1, "Cama de impacto", null, 100, false, false, true),
                ComponentEntity(2005, 1, "Rodillos de carga", null, 100, false, false, true),
                ComponentEntity(2006, 1, "Rodillos de retorno", null, 100, false, false, true),
                ComponentEntity(2007, 1, "Rodillos laterales", null, 100, false, false, true),
                ComponentEntity(2008, 1, "Tambor de cola", null, 100, false, false, true),
                ComponentEntity(2009, 1, "Tambor de inflexion", null, 100, false, false, true),
                ComponentEntity(2010, 1, "Tambor de contra pesa", null, 100, false, false, true),
                ComponentEntity(2011, 1, "Tambor snub", null, 100, false, false, true),
                ComponentEntity(2012, 1, "Tambor motriz", null, 100, false, false, true),
                ComponentEntity(2013, 1, "Motorreductor", null, 100, false, false, true),
                ComponentEntity(2014, 1, "Material acumulado", null, 100, false, false, true),
                ComponentEntity(2015, 1, "Condicion de seguridad", null, 100, false, false, true),
                ComponentEntity(2016, 1, "Cubiertas", null, 100, false, false, true),
                ComponentEntity(2017, 1, "Otros", null, 100, false, false, true),
                ComponentEntity(2018, 1, "Limpiador primario", null, 100, false, false, true),
                ComponentEntity(2019, 1, "Limpiador secundario", null, 100, false, false, true),
                ComponentEntity(2020, 1, "Limpiador tipo arado", null, 100, false, false, true),
                ComponentEntity(2021, 1, "Limpiador transversal", null, 100, false, false, true)
            )
        )

        // =========================
        // DIAGNÓSTICOS
        // =========================
        diagnosticDao.insertAll(
            listOf(
                DiagnosticEntity(3000, 1, "Estado", "Inspección general del estado del componente.", true),
                DiagnosticEntity(3001, 1, "Empalme", "Revisión de la condición del empalme de la banda.", true),
                DiagnosticEntity(3002, 1, "Alineacion", "Verificación de alineación del componente o sistema.", true),
                DiagnosticEntity(3003, 1, "Temperatura", "Verificación de temperatura de operación.", true),
                DiagnosticEntity(3004, 1, "Pantalla de sacrificio", "Inspección de desgaste o condición de la pantalla de sacrificio.", true),
                DiagnosticEntity(3005, 1, "Lamina de sacrificio", "Inspección de desgaste o condición de la lámina de sacrificio.", true),
                DiagnosticEntity(3006, 1, "Recubrimiento", "Inspección del recubrimiento del componente.", true),
                DiagnosticEntity(3007, 1, "Rodamientos", "Inspección del estado y funcionamiento de los rodamientos.", true),
                DiagnosticEntity(3008, 1, "Aseo", "Verificación de limpieza y orden del componente o zona.", true)
            )
        )

        // =========================
        // CONDICIONES
        // =========================
        conditionDao.insertAll(
            listOf(
                ConditionEntity(4000, 1, "ALTA", "PF1", 1, "#ff0000", true),
                ConditionEntity(4001, 1, "MEDIA", "PF2", 2, "#fffb00", true),
                ConditionEntity(4002, 1, "BAJA", "PF3", 3, "#00a2ff", true),
                ConditionEntity(4003, 1, "ALTA", "SEG C", 1, "#ff0000", true),
                ConditionEntity(4004, 1, "MEDIA", "SEG D", 2, "#fffb00", true),
                ConditionEntity(4005, 1, "ASEO", "ASEO", 0, "#ffae00", true),
                ConditionEntity(4006, 1, "OBSERVACIÓN", "OBSERV", 0, "#6e34cc", true),
                ConditionEntity(4007, 1, "OK", "OK", 0, "#11a152", true)
            )
        )

        // =========================
        // RELACIÓN ACTIVO -> COMPONENTE
        // (como en tu ElementComponentSeeder: todos los componentes para todos los activos)
        // =========================
        val allElementIds = listOf(
            1000L, 1001L, 1002L, 1003L,
            1100L, 1101L, 1102L, 1103L,
            1200L, 1201L, 1202L, 1203L, 1204L,
            1300L, 1301L, 1302L,
            1400L, 1401L, 1402L, 1403L, 1404L, 1405L,
            1500L, 1501L, 1502L, 1503L, 1504L,
            1600L, 1601L, 1602L, 1603L, 1604L, 1605L, 1606L, 1607L, 1608L,
            1700L, 1701L, 1702L, 1703L, 1704L, 1705L, 1706L, 1707L, 1708L, 1709L, 1710L, 1711L, 1712L, 1713L, 1714L,
            1800L, 1801L
        )

        val allComponentIds = listOf(
            2000L, 2001L, 2002L, 2003L, 2004L, 2005L, 2006L, 2007L,
            2008L, 2009L, 2010L, 2011L, 2012L, 2013L, 2014L, 2015L,
            2016L, 2017L, 2018L, 2019L, 2020L, 2021L
        )

        ecDao.insertAll(
            allElementIds.flatMap { elementId ->
                allComponentIds.map { componentId ->
                    ElementComponentCrossRef(
                        elementId = elementId,
                        componentId = componentId
                    )
                }
            }
        )

        // =========================
        // RELACIÓN COMPONENTE -> DIAGNÓSTICOS
        // =========================
        cdDao.insertAll(
            listOf(
                // Banda
                ComponentDiagnosticCrossRef(2000, 3000),
                ComponentDiagnosticCrossRef(2000, 3001),
                ComponentDiagnosticCrossRef(2000, 3002),
                ComponentDiagnosticCrossRef(2000, 3003),

                // Chute de descarga
                ComponentDiagnosticCrossRef(2001, 3000),
                ComponentDiagnosticCrossRef(2001, 3004),
                ComponentDiagnosticCrossRef(2001, 3005),

                // Encausadores
                ComponentDiagnosticCrossRef(2002, 3000),

                // Guardilla
                ComponentDiagnosticCrossRef(2003, 3000),

                // Cama de impacto
                ComponentDiagnosticCrossRef(2004, 3000),

                // Rodillos de carga
                ComponentDiagnosticCrossRef(2005, 3000),

                // Rodillos de retorno
                ComponentDiagnosticCrossRef(2006, 3000),

                // Rodillos laterales
                ComponentDiagnosticCrossRef(2007, 3000),

                // Tambor de cola
                ComponentDiagnosticCrossRef(2008, 3006),
                ComponentDiagnosticCrossRef(2008, 3007),

                // Tambor de inflexion
                ComponentDiagnosticCrossRef(2009, 3006),
                ComponentDiagnosticCrossRef(2009, 3007),

                // Tambor de contra pesa
                ComponentDiagnosticCrossRef(2010, 3006),
                ComponentDiagnosticCrossRef(2010, 3007),

                // Tambor snub
                ComponentDiagnosticCrossRef(2011, 3006),
                ComponentDiagnosticCrossRef(2011, 3007),

                // Tambor motriz
                ComponentDiagnosticCrossRef(2012, 3006),
                ComponentDiagnosticCrossRef(2012, 3007),

                // Motorreductor
                ComponentDiagnosticCrossRef(2013, 3000),
                ComponentDiagnosticCrossRef(2013, 3003),

                // Material acumulado
                ComponentDiagnosticCrossRef(2014, 3008),

                // Condicion de seguridad
                ComponentDiagnosticCrossRef(2015, 3000),

                // Cubiertas
                ComponentDiagnosticCrossRef(2016, 3000),

                // Otros
                ComponentDiagnosticCrossRef(2017, 3000),

                // Limpiador primario
                ComponentDiagnosticCrossRef(2018, 3000),

                // Limpiador secundario
                ComponentDiagnosticCrossRef(2019, 3000),

                // Limpiador tipo arado
                ComponentDiagnosticCrossRef(2020, 3000),

                // Limpiador transversal
                ComponentDiagnosticCrossRef(2021, 3000)
            )
        )

        */
    }


}
