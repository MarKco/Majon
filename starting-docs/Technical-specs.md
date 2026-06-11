# SPECIFICHE TECNICHE - App Contarighe (Android Nativo - Kotlin)

## 1. Architettura Generale

L'app segue il pattern **MVVM (Model-View-ViewModel)** con Clean Architecture principles, suddivisa in layer:
- **Presentation Layer** (UI - Jetpack Compose)
- **Domain Layer** (Business logic)
- **Data Layer** (Local persistence - Room Database)

```
┌─────────────────────────────────────────┐
│       Jetpack Compose (UI)              │
├─────────────────────────────────────────┤
│   ViewModel + State Management          │
├─────────────────────────────────────────┤
│   Repository Pattern (Data Access)      │
├─────────────────────────────────────────┤
│   Room Database (SQLite)                │
└─────────────────────────────────────────┘
```

---

## 2. Stack Tecnologico

### Core Framework
- **Language**: Kotlin 1.9+
- **Minimum SDK**: Android 8.0 (API Level 26)
- **Target SDK**: Android 15+ (API Level 35+)
- **Compile SDK**: Android 15 (API Level 35)

### UI Framework
- **Jetpack Compose** - Modern declarative UI toolkit
- **Material Design 3** - Material3 components library

### Data Persistence
- **Room Database** - SQLite ORM
- **DataStore** - Preferences storage (future use)

### Dependency Injection
- **Hilt** - Dependency injection framework

### State Management
- **ViewModel** - UI state container
- **StateFlow** / **MutableStateFlow** - Reactive state management
- **Coroutines** - Asynchronous operations

### Testing
- **JUnit 4** - Unit testing
- **Mockito** / **MockK** - Mocking framework
- **Compose Testing** - UI testing

### Build Tools
- **Gradle** 8.x
- **AGP** (Android Gradle Plugin) 8.x
- **BuildConfig** - Build variants

---

## 3. Struttura del Progetto

```
app/
├── src/main/
│   ├── kotlin/com/contarighe/
│   │   ├── di/
│   │   │   └── AppModule.kt (Hilt DI configuration)
│   │   ├── data/
│   │   │   ├── database/
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── dao/
│   │   │   │   │   ├── ProjectDao.kt
│   │   │   │   │   ├── PartDao.kt
│   │   │   │   │   └── NoteDao.kt
│   │   │   │   └── entity/
│   │   │   │       ├── ProjectEntity.kt
│   │   │   │       ├── PartEntity.kt
│   │   │   │       └── NoteEntity.kt
│   │   │   └── repository/
│   │   │       ├── ProjectRepository.kt
│   │   │       ├── ProjectRepositoryImpl.kt
│   │   │       ├── PartRepository.kt
│   │   │       ├── PartRepositoryImpl.kt
│   │   │       ├── NoteRepository.kt
│   │   │       └── NoteRepositoryImpl.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Project.kt
│   │   │   │   ├── Part.kt
│   │   │   │   └── Note.kt
│   │   │   └── usecase/
│   │   │       ├── CreateProjectUseCase.kt
│   │   │       ├── GetProjectsUseCase.kt
│   │   │       ├── AddPartUseCase.kt
│   │   │       ├── GetPartsUseCase.kt
│   │   │       ├── AddNoteUseCase.kt
│   │   │       ├── IncrementRowUseCase.kt
│   │   │       └── CalculateProgressUseCase.kt
│   │   ├── presentation/
│   │   │   ├── navigation/
│   │   │   │   └── NavGraph.kt
│   │   │   ├── screen/
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── project_detail/
│   │   │   │   │   ├── ProjectDetailScreen.kt
│   │   │   │   │   └── ProjectDetailViewModel.kt
│   │   │   │   ├── counter/
│   │   │   │   │   ├── CounterScreen.kt
│   │   │   │   │   └── CounterViewModel.kt
│   │   │   │   ├── add_project/
│   │   │   │   │   ├── AddProjectScreen.kt
│   │   │   │   │   └── AddProjectViewModel.kt
│   │   │   │   ├── add_note/
│   │   │   │   │   ├── AddNoteScreen.kt
│   │   │   │   │   └── AddNoteViewModel.kt
│   │   │   │   └── dialogs/
│   │   │   │       ├── ConfirmationDialog.kt
│   │   │   │       ├── RowCompletedDialog.kt
│   │   │   │       └── ColorPickerDialog.kt
│   │   │   ├── component/
│   │   │   │   ├── ProjectCard.kt
│   │   │   │   ├── PartProgressItem.kt
│   │   │   │   ├── NoteItem.kt
│   │   │   │   ├── IconSelector.kt
│   │   │   │   ├── ColorPicker.kt
│   │   │   │   └── ProgressBar.kt
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Typography.kt
│   │   │   │   └── Theme.kt
│   │   │   └── state/
│   │   │       ├── UiState.kt
│   │   │       └── UiEvent.kt
│   │   ├── utils/
│   │   │   ├── Constants.kt
│   │   │   ├── IconUtils.kt
│   │   │   └── ColorUtils.kt
│   │   └── MainActivity.kt
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

---

## 4. Entità Database (Room)

### ProjectEntity
```kotlin
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconType: String, // enum: SWEATER, SCARF, etc.
    val iconColor: String, // hex color (#RRGGBB)
    val yarnType: String?, // optional text
    val needleNumber: Int?, // optional number
    val needleTypeCircular: Boolean? = null, // optional, not prioritary
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### PartEntity
```kotlin
@Entity(
    tableName = "parts",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val totalRows: Int, // numero totale righe
    val completedRows: Int = 0, // righe completate
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### NoteEntity
```kotlin
@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(
        entity = PartEntity::class,
        parentColumns = ["id"],
        childColumns = ["partId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partId: Long,
    val rowStart: Int, // riga inizio
    val rowEnd: Int, // riga fine (stessa se nota singola)
    val noteText: String,
    val ruleType: String? = null, // NONE, ODD_ROWS, EVEN_ROWS
    val createdAt: Long = System.currentTimeMillis()
)
```

---

## 5. DAO (Data Access Objects)

### ProjectDao
```kotlin
@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?
}
```

### PartDao
```kotlin
@Dao
interface PartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(part: PartEntity): Long

    @Delete
    suspend fun delete(part: PartEntity)

    @Update
    suspend fun update(part: PartEntity)

    @Query("SELECT * FROM parts WHERE projectId = :projectId ORDER BY id ASC")
    fun getPartsByProjectId(projectId: Long): Flow<List<PartEntity>>

    @Query("SELECT * FROM parts WHERE id = :partId")
    suspend fun getPartById(partId: Long): PartEntity?

    @Query("UPDATE parts SET completedRows = :newValue WHERE id = :partId")
    suspend fun updateCompletedRows(partId: Long, newValue: Int)
}
```

### NoteDao
```kotlin
@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Delete
    suspend fun delete(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE partId = :partId ORDER BY rowStart ASC")
    fun getNotesByPartId(partId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE partId = :partId AND rowStart <= :currentRow AND rowEnd >= :currentRow")
    suspend fun getNoteForRow(partId: Long, currentRow: Int): NoteEntity?
}
```

---

## 6. Domain Layer - Modelli

### Project (Domain Model)
```kotlin
data class Project(
    val id: Long,
    val name: String,
    val iconType: IconType,
    val iconColor: String,
    val yarnType: String?,
    val needleNumber: Int?,
    val needleTypeCircular: Boolean?,
    val createdAt: Long,
    val updatedAt: Long
)

enum class IconType {
    SWEATER, SCARF, HAT, SOCKS, GLOVES
}
```

### Part (Domain Model)
```kotlin
data class Part(
    val id: Long,
    val projectId: Long,
    val name: String,
    val totalRows: Int,
    val completedRows: Int,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun progressPercentage(): Float = 
        if (totalRows == 0) 0f else (completedRows.toFloat() / totalRows) * 100f
}
```

### Note (Domain Model)
```kotlin
data class Note(
    val id: Long,
    val partId: Long,
    val rowStart: Int,
    val rowEnd: Int,
    val noteText: String,
    val ruleType: NoteRuleType?
)

enum class NoteRuleType {
    NONE, ODD_ROWS, EVEN_ROWS
}
```

---

## 7. ViewModel - State Management

### CounterViewModel
```kotlin
@HiltViewModel
class CounterViewModel @Inject constructor(
    private val incrementRowUseCase: IncrementRowUseCase,
    private val getNoteForRowUseCase: GetNoteForRowUseCase,
    private val calculateProgressUseCase: CalculateProgressUseCase,
    private val getPartUseCase: GetPartUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CounterUiState>(CounterUiState.Loading)
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    private val _rowCompletedEvent = MutableSharedFlow<RowCompletedEvent>()
    val rowCompletedEvent: SharedFlow<RowCompletedEvent> = _rowCompletedEvent.asSharedFlow()

    fun initializeCounter(partId: Long) {
        viewModelScope.launch {
            val part = getPartUseCase(partId)
            part?.let {
                _uiState.value = CounterUiState.Counter(
                    currentRow = it.completedRows + 1,
                    totalRows = it.totalRows,
                    partName = it.name
                )
            }
        }
    }

    fun incrementRow(partId: Long) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state is CounterUiState.Counter) {
                val newRow = state.currentRow + 1
                
                // Update database
                incrementRowUseCase(partId)
                
                // Get note for next row
                val nextRowNote = if (newRow <= state.totalRows) {
                    getNoteForRowUseCase(partId, newRow)
                } else null
                
                // Calculate progress
                val progress = calculateProgressUseCase(partId)
                
                // Emit event for dialog
                _rowCompletedEvent.emit(RowCompletedEvent(
                    completedRow = state.currentRow,
                    nextRow = newRow,
                    nextRowNote = nextRowNote,
                    progress = progress
                ))
                
                // Update state
                _uiState.value = state.copy(
                    currentRow = newRow,
                    progressPercentage = progress
                )
            }
        }
    }

    fun resetCounter(partId: Long) {
        viewModelScope.launch {
            // Implementation for reset
        }
    }
}

sealed class CounterUiState {
    object Loading : CounterUiState()
    data class Counter(
        val currentRow: Int,
        val totalRows: Int,
        val partName: String,
        val progressPercentage: Float = 0f
    ) : CounterUiState()
    data class Error(val message: String) : CounterUiState()
}

data class RowCompletedEvent(
    val completedRow: Int,
    val nextRow: Int,
    val nextRowNote: Note?,
    val progress: Float
)
```

### HomeViewModel
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProjectsUseCase: GetProjectsUseCase,
    private val calculateProjectProgressUseCase: CalculateProjectProgressUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            getProjectsUseCase()
                .collect { projects ->
                    val projectsWithProgress = projects.map { project ->
                        val progress = calculateProjectProgressUseCase(project.id)
                        ProjectUiModel(
                            id = project.id,
                            name = project.name,
                            iconType = project.iconType,
                            iconColor = project.iconColor,
                            progressPercentage = progress
                        )
                    }
                    _uiState.value = HomeUiState.ProjectsList(projectsWithProgress)
                }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            deleteProjectUseCase(projectId)
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class ProjectsList(val projects: List<ProjectUiModel>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

data class ProjectUiModel(
    val id: Long,
    val name: String,
    val iconType: IconType,
    val iconColor: String,
    val progressPercentage: Float
)
```

---

## 8. UI Layer - Jetpack Compose

### CounterScreen
```kotlin
@Composable
fun CounterScreen(
    partId: Long,
    viewModel: CounterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val rowCompletedEvent by viewModel.rowCompletedEvent.collectAsState(
        initial = null,
        context = Dispatchers.Main.immediate
    )

    LaunchedEffect(partId) {
        viewModel.initializeCounter(partId)
    }

    when (val state = uiState) {
        is CounterUiState.Loading -> {
            LoadingScreen()
        }
        is CounterUiState.Counter -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
                    Text(
                        text = state.partName,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    // Progress bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = state.progressPercentage / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                        Text(
                            text = "${state.progressPercentage.toInt()}%",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    // Counter display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Riga ${state.currentRow}",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "di ${state.totalRows}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Main button
                    Button(
                        onClick = { viewModel.incrementRow(partId) },
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = CircleShape
                    ) {
                        Text("+", style = MaterialTheme.typography.displayMedium)
                    }
                }

                // Dialog for row completion
                rowCompletedEvent?.let { event ->
                    RowCompletedDialog(
                        event = event,
                        onDismiss = { /* dialog dismiss */ }
                    )
                }
            }
        }
        is CounterUiState.Error -> {
            ErrorScreen(message = state.message)
        }
    }
}
```

### HomeScreen
```kotlin
@Composable
fun HomeScreen(
    onProjectSelected: (Long) -> Unit,
    onAddProjectClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProjectClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                LoadingScreen()
            }
            is HomeUiState.ProjectsList -> {
                if (state.projects.isEmpty()) {
                    EmptyState(onAddProjectClick = onAddProjectClick)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.projects) { project ->
                            ProjectCard(
                                project = project,
                                onProjectClick = { onProjectSelected(project.id) }
                            )
                        }
                    }
                }
            }
            is HomeUiState.Error -> {
                ErrorScreen(message = state.message)
            }
        }
    }
}
```

---

## 9. Navigazione

### NavGraph (Navigation Compose)
```kotlin
@Composable
fun ContarrigheNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onProjectSelected = { projectId ->
                    navController.navigate("project/$projectId")
                },
                onAddProjectClick = {
                    navController.navigate("add_project")
                }
            )
        }

        composable(
            "project/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ProjectDetailScreen(
                projectId = projectId,
                onPartSelected = { partId ->
                    navController.navigate("counter/$partId")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            "counter/{partId}",
            arguments = listOf(navArgument("partId") { type = NavType.LongType })
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getLong("partId") ?: return@composable
            CounterScreen(partId = partId)
        }

        composable("add_project") {
            AddProjectScreen(
                onProjectCreated = {
                    navController.popBackStack()
                    navController.navigate("home")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            "add_note/{partId}",
            arguments = listOf(navArgument("partId") { type = NavType.LongType })
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getLong("partId") ?: return@composable
            AddNoteScreen(
                partId = partId,
                onNoteSaved = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
```

---

## 10. Gestione Permessi

L'app **non richiede permessi critici**:
- ❌ Camera
- ❌ Localizzazione
- ❌ Contatti
- ❌ Microfono

**Permessi consigliati nel Manifest**:
```xml
<!-- No permissions required for MVP -->
```

---

## 11. Performance e Ottimizzazioni

### Coroutines e Threading
- Tutti i database operation avvengono su **Dispatcher.IO**
- UI updates avvengono su **Dispatcher.Main**
- ViewModel mantiene operazioni asincrone sicure

### Memory Management
- Utilizzo di **StateFlow** invece di LiveData (memore efficiente)
- Cleanup automatico di coroutines in onCleared()
- No memory leaks nel ViewModel

### Database Optimization
- Indici su `projectId` e `partId` nelle tabelle correlate
- Query ottimizzate con SELECT specifici
- Batch operations dove possibile

---

## 12. Build Configuration

### build.gradle.kts
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.contarighe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.contarighe"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.x"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.compose.runtime:runtime:1.6.0")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ViewModel & State
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0")
}
```

---

## 13. Testing Strategy

### Unit Tests
- Test logica use case
- Test calcolo percentuali
- Test validazione input

### UI Tests
- Test navigation flow
- Test increment button interaction
- Test dialog appearance

### Database Tests
- Test insert/update/delete entities
- Test query filtering

---

## 14. Deployment e Release

### Target Distribution
- **Google Play Store** - Primary
- **APK Download** - Secondary

### Versioning
- Semantic Versioning: `major.minor.patch`
- V1.0.0 - MVP release
- V1.1.0 - First iteration (photo support, etc.)

### Build Variants
- `debug` - Development build
- `release` - Production build (ProGuard enabled)

---

## 15. Sicurezza

### Data Storage
- Tutti i dati salvati localmente (Room Database)
- No cloud sync in MVP
- No user authentication richiesta

### Code Protection
- ProGuard/R8 obfuscation su release build
- No API keys exposed nel codice

### Permissions
- Minimal permissions requested (none in MVP)

---

## 16. Accessibility

- Testo minimo 14sp
- Pulsante minimo 48x48dp
- Supporto per screen readers (ContentDescription)
- High contrast colors in Material3

---

## 17. Changelog Versione 1.0

- ✅ Counter di righe con pulsante "+"
- ✅ Sistema note per righe
- ✅ Multi-progetto e multi-parte
- ✅ Calcolo percentuale completamento
- ✅ Icone standard con colori personalizzabili
- ✅ Persistenza dati con Room
- ✅ UI moderna con Compose

---

## 18. Future Enhancements (Post V1.0)

- 📸 Foto per progetto
- 📊 Statistiche avanzate (conteggio maglie)
- ☁️ Cloud sync
- 🔄 Import/Export progetti
- 🌙 Dark mode nativo
- 📱 Widget home screen
- 🎯 Backup automatico

---

**Versione**: 1.0  
**Data**: 2025-11-13  
**Status**: Initial Specification