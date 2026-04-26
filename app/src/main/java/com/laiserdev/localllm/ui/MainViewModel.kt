package com.laiserdev.localllm.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.laiserdev.localllm.LocalLLMApp
import com.laiserdev.localllm.data.model.*
import com.laiserdev.localllm.data.repository.ChatHistoryRepository
import com.laiserdev.localllm.data.repository.ChatSession
import com.laiserdev.localllm.data.repository.ModelDownloadService
import com.laiserdev.localllm.server.LLMServerService
import com.laiserdev.localllm.ui.screens.chat.AgentStep
import com.laiserdev.localllm.ui.screens.chat.StepStatus
import com.laiserdev.localllm.util.AgentEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LocalLLMApp

    // ─── Chat State ───────────────────────────────────────────────────────────

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // ─── Project State ────────────────────────────────────────────────────────

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject.asStateFlow()

    private val _fileTree = MutableStateFlow<ProjectFile?>(null)
    val fileTree: StateFlow<ProjectFile?> = _fileTree.asStateFlow()

    private val _openFiles = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // path -> content
    val openFiles: StateFlow<List<Pair<String, String>>> = _openFiles.asStateFlow()

    private val _activeFilePath = MutableStateFlow<String?>(null)
    val activeFilePath: StateFlow<String?> = _activeFilePath.asStateFlow()

    // ─── Terminal State ───────────────────────────────────────────────────────

    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    private val _isRunningCommand = MutableStateFlow(false)
    val isRunningCommand: StateFlow<Boolean> = _isRunningCommand.asStateFlow()

    // ─── Model State ──────────────────────────────────────────────────────────

    private val _models = MutableStateFlow(AVAILABLE_MODELS)
    val models: StateFlow<List<LLMModel>> = _models.asStateFlow()

    private val _modelLoadingState = MutableStateFlow<String?>(null)
    val modelLoadingState: StateFlow<String?> = _modelLoadingState.asStateFlow()

    // ─── Settings ─────────────────────────────────────────────────────────────

    val settings = app.settingsManager.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings()
    )

    // ─── Skills ───────────────────────────────────────────────────────────────

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    // ─── Agent State ──────────────────────────────────────────────────────────

    private val _agentEvents = MutableSharedFlow<AgentEvent>()
    val agentEvents: SharedFlow<AgentEvent> = _agentEvents.asSharedFlow()

    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning: StateFlow<Boolean> = _isAgentRunning.asStateFlow()

    private val _agentSteps = MutableStateFlow<List<AgentStep>>(emptyList())
    val agentSteps: StateFlow<List<AgentStep>> = _agentSteps.asStateFlow()

    private val _agentThinking = MutableStateFlow<String?>(null)
    val agentThinking: StateFlow<String?> = _agentThinking.asStateFlow()

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    // ─── Server State ─────────────────────────────────────────────────────────

    private val _serverRunning = MutableStateFlow(false)
    val serverRunning: StateFlow<Boolean> = _serverRunning.asStateFlow()

    init {
        loadProjects()
        loadSkills()
        observeDownloadProgress()
        restoreLastSession()
    }

    private fun restoreLastSession() {
        viewModelScope.launch {
            val msgs = app.chatHistoryRepository.loadLastSession()
            if (msgs.isNotEmpty()) _messages.value = msgs
            refreshChatSessions()
        }
    }

    fun refreshChatSessions() {
        viewModelScope.launch {
            _chatSessions.value = app.chatHistoryRepository.listSessions()
        }
    }

    fun loadChatSession(fileName: String) {
        viewModelScope.launch {
            val msgs = app.chatHistoryRepository.loadSession(fileName)
            if (msgs.isNotEmpty()) _messages.value = msgs
        }
    }

    fun deleteChatSession(fileName: String) {
        viewModelScope.launch {
            app.chatHistoryRepository.deleteSession(fileName)
            refreshChatSessions()
        }
    }

    // ─── Onboarding ───────────────────────────────────────────────────────────

    val onboardingDone = app.settingsManager.onboardingDone

    fun completeOnboarding() {
        viewModelScope.launch { app.settingsManager.setOnboardingDone() }
    }

    // ─── Chat ─────────────────────────────────────────────────────────────────

    fun sendMessage(content: String, imageUri: Uri? = null) {
        viewModelScope.launch {
            // Guard: no model loaded
            if (!app.llmRepository.isLoaded()) {
                _messages.update {
                    it + ChatMessage(role = MessageRole.USER, content = content) +
                    ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "⚠️ No model loaded. Go to the **Models** tab, download a model, then tap **Load Model** before chatting."
                    )
                }
                return@launch
            }

            val userMsg = ChatMessage(role = MessageRole.USER, content = content, imageUri = imageUri?.toString())
            _messages.update { it + userMsg }
            _selectedImageUri.value = null

            val assistantMsg = ChatMessage(role = MessageRole.ASSISTANT, content = "", isStreaming = true)
            _messages.update { it + assistantMsg }
            _isGenerating.value = true

            val sysPrompt = settings.value.systemPrompt
            val buffer = StringBuilder()
            val start = System.currentTimeMillis()

            app.llmRepository.generateStream(content, sysPrompt).collect { token ->
                buffer.append(token)
                _messages.update { msgs ->
                    msgs.dropLast(1) + assistantMsg.copy(content = buffer.toString(), isStreaming = true)
                }
            }

            val tps = buffer.length.toFloat() / ((System.currentTimeMillis() - start) / 1000f)
            _messages.update { msgs ->
                msgs.dropLast(1) + assistantMsg.copy(
                    content = buffer.toString(), isStreaming = false, tokensPerSecond = tps
                )
            }
            _isGenerating.value = false
            // Persist after each completed exchange
            app.chatHistoryRepository.saveSession(_messages.value)
        }
    }

    fun stopGeneration() { _isGenerating.value = false }
    fun clearChat() { _messages.value = emptyList() }
    fun setSelectedImage(uri: Uri?) { _selectedImageUri.value = uri }

    // ─── Agent (tool-use) ─────────────────────────────────────────────────────

    fun runAgent(prompt: String) {
        val project = _activeProject.value ?: run {
            _messages.update {
                it + ChatMessage(role = MessageRole.USER, content = prompt) +
                ChatMessage(role = MessageRole.ASSISTANT,
                    content = "⚠️ No project open. Go to the **Editor** tab and open or create a project first.")
            }
            return
        }
        if (!app.llmRepository.isLoaded()) {
            _messages.update {
                it + ChatMessage(role = MessageRole.USER, content = prompt) +
                ChatMessage(role = MessageRole.ASSISTANT,
                    content = "⚠️ No model loaded. Go to the **Models** tab, download and load a model first.")
            }
            return
        }
        viewModelScope.launch {
            _isAgentRunning.value = true
            _agentSteps.value = emptyList()
            _agentThinking.value = "Analyzing task..."

            // Build rich project context for the agent system prompt
            val fileList = try {
                File(project.path).walkTopDown()
                    .filter { it.isFile }
                    .take(60)
                    .map { it.relativeTo(File(project.path)).path }
                    .joinToString("\n")
            } catch (_: Exception) { "" }

            val projectContext = buildString {
                appendLine("Project: ${project.name}")
                appendLine("Language: ${project.language.ifBlank { "auto-detect" }}")
                appendLine("Path: ${project.path}")
                if (fileList.isNotBlank()) {
                    appendLine("Files:")
                    appendLine(fileList)
                }
            }

            val agentMsg = ChatMessage(role = MessageRole.ASSISTANT, content = "", isStreaming = true)
            _messages.update { it + ChatMessage(role = MessageRole.USER, content = prompt) + agentMsg }

            val buffer = StringBuilder()
            var currentStepId: String? = null
            var stepStartMs = System.currentTimeMillis()

            app.toolEngine.agentLoop(prompt, project.path, systemExtra = projectContext).collect { event ->
                _agentEvents.emit(event)
                when (event) {
                    is AgentEvent.Thinking -> {
                        _agentThinking.value = event.message
                    }
                    is AgentEvent.Token -> {
                        _agentThinking.value = null
                        buffer.append(event.text)
                        _messages.update { msgs ->
                            msgs.dropLast(1) + agentMsg.copy(content = buffer.toString(), isStreaming = true)
                        }
                    }
                    is AgentEvent.ToolCalling -> {
                        _agentThinking.value = null
                        stepStartMs = System.currentTimeMillis()
                        val step = AgentStep(
                            toolName = event.name,
                            args = event.args,
                            status = StepStatus.RUNNING
                        )
                        currentStepId = step.id
                        _agentSteps.update { it + step }
                        addTerminalLine("🔧 ${event.name}(${event.args.take(60)})", TerminalLine.LineType.TOOL)
                    }
                    is AgentEvent.ToolResult -> {
                        val duration = System.currentTimeMillis() - stepStartMs
                        _agentSteps.update { steps ->
                            steps.map { s ->
                                if (s.id == currentStepId) s.copy(
                                    output = event.output,
                                    status = if (event.isError) StepStatus.ERROR else StepStatus.SUCCESS,
                                    durationMs = duration
                                ) else s
                            }
                        }
                        addTerminalLine(
                            if (event.isError) "❌ ${event.output.take(120)}" else "✅ ${event.output.take(120)}",
                            if (event.isError) TerminalLine.LineType.ERROR else TerminalLine.LineType.INFO
                        )
                        refreshFileTree()
                    }
                    is AgentEvent.FinalAnswer -> {
                        _agentThinking.value = null
                        _messages.update { msgs ->
                            msgs.dropLast(1) + agentMsg.copy(content = event.text, isStreaming = false)
                        }
                    }
                    is AgentEvent.Error -> {
                        _agentThinking.value = null
                        addTerminalLine("⚠ ${event.message}", TerminalLine.LineType.ERROR)
                        _messages.update { msgs ->
                            msgs.dropLast(1) + agentMsg.copy(
                                content = buffer.toString().ifBlank { "⚠ ${event.message}" },
                                isStreaming = false
                            )
                        }
                    }
                }
            }
            _agentThinking.value = null
            _isAgentRunning.value = false
        }
    }

    // ─── Projects ─────────────────────────────────────────────────────────────

    fun loadProjects() {
        viewModelScope.launch {
            _projects.value = app.projectRepository.listProjects()
        }
    }

    fun openProject(project: Project) {
        _activeProject.value = project
        app.terminalExecutor.workingDir = File(project.path)
        addTerminalLine("📁 Opened project: ${project.name}", TerminalLine.LineType.INFO)
        addTerminalLine("Path: ${project.path}", TerminalLine.LineType.INFO)
        refreshFileTree()
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            app.projectRepository.createProject(name).onSuccess { project ->
                loadProjects()
                openProject(project)
            }
        }
    }

    fun generateProjectFromPrompt(description: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            val assistantMsg = ChatMessage(role = MessageRole.ASSISTANT, content = "🚀 Generating project...", isStreaming = true)
            _messages.update { it + ChatMessage(role = MessageRole.USER, content = description) + assistantMsg }

            app.llmRepository.generateProject(description).onSuccess { jsonResponse ->
                app.projectRepository.createFromAIJson(jsonResponse).onSuccess { project ->
                    _messages.update { msgs ->
                        msgs.dropLast(1) + assistantMsg.copy(
                            content = "✅ Project **${project.name}** created with ${File(project.path).walkTopDown().filter { it.isFile }.count()} files.",
                            isStreaming = false
                        )
                    }
                    loadProjects()
                    openProject(project)
                }.onFailure { e ->
                    _messages.update { msgs ->
                        msgs.dropLast(1) + assistantMsg.copy(content = "❌ Error: ${e.message}", isStreaming = false)
                    }
                }
            }.onFailure { e ->
                _messages.update { msgs ->
                    msgs.dropLast(1) + assistantMsg.copy(content = "❌ Error: ${e.message}", isStreaming = false)
                }
            }
            _isGenerating.value = false
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            app.projectRepository.deleteProject(project.id)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null; _fileTree.value = null
            }
            loadProjects()
        }
    }

    fun exportProject() {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            app.projectRepository.exportProjectAsZip(project.path).onSuccess { zipFile ->
                addTerminalLine("✅ Exported: ${zipFile.absolutePath}", TerminalLine.LineType.INFO)
            }.onFailure { addTerminalLine("❌ Export failed: ${it.message}", TerminalLine.LineType.ERROR) }
        }
    }

    fun importProject(zipUri: Uri) {
        viewModelScope.launch {
            app.projectRepository.importProjectFromZip(zipUri).onSuccess { project ->
                loadProjects(); openProject(project)
                addTerminalLine("✅ Imported project: ${project.name}", TerminalLine.LineType.INFO)
            }.onFailure { addTerminalLine("❌ Import failed: ${it.message}", TerminalLine.LineType.ERROR) }
        }
    }

    // ─── Files ────────────────────────────────────────────────────────────────

    fun refreshFileTree() {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            _fileTree.value = app.projectRepository.getFileTree(project.path)
        }
    }

    fun openFile(absolutePath: String) {
        viewModelScope.launch {
            if (_openFiles.value.any { it.first == absolutePath }) {
                _activeFilePath.value = absolutePath; return@launch
            }
            app.projectRepository.readFile(absolutePath).onSuccess { content ->
                _openFiles.update { it + (absolutePath to content) }
                _activeFilePath.value = absolutePath
            }
        }
    }

    fun saveFile(absolutePath: String, content: String) {
        viewModelScope.launch {
            app.projectRepository.writeFile(absolutePath, content)
            _openFiles.update { files -> files.map { if (it.first == absolutePath) absolutePath to content else it } }
        }
    }

    fun closeFile(absolutePath: String) {
        _openFiles.update { it.filter { f -> f.first != absolutePath } }
        if (_activeFilePath.value == absolutePath) {
            _activeFilePath.value = _openFiles.value.lastOrNull()?.first
        }
    }

    fun createNewFile(relativePath: String) {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            app.projectRepository.createFile(project.path, relativePath).onSuccess { abs ->
                refreshFileTree(); openFile(abs)
            }
        }
    }

    fun deleteFile(absolutePath: String) {
        viewModelScope.launch {
            app.projectRepository.deleteFile(absolutePath)
            closeFile(absolutePath); refreshFileTree()
        }
    }

    // ─── Terminal ─────────────────────────────────────────────────────────────

    fun runCommand(command: String) {
        viewModelScope.launch {
            addTerminalLine("$ $command", TerminalLine.LineType.COMMAND)
            _isRunningCommand.value = true

            // Handle builtins
            val builtin = app.terminalExecutor.handleBuiltin(command, app.terminalExecutor.workingDir.absolutePath)
            if (builtin != null) {
                val (_, output) = builtin
                if (output.isNotBlank()) addTerminalLine(output, TerminalLine.LineType.OUTPUT)
                _isRunningCommand.value = false
                return@launch
            }

            app.terminalExecutor.execute(command).collect { (line, isErr) ->
                addTerminalLine(line, if (isErr) TerminalLine.LineType.ERROR else TerminalLine.LineType.OUTPUT)
            }
            _isRunningCommand.value = false
            refreshFileTree()
        }
    }

    fun runProjectAutoDetect() {
        val project = _activeProject.value ?: return
        val cmd = app.packageManager.detectRunCommand(project.path)
        if (cmd != null) runCommand(cmd)
        else addTerminalLine("⚠ Could not auto-detect run command for this project", TerminalLine.LineType.INFO)
    }

    fun installPackages(manager: String, packages: List<String>) {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            addTerminalLine("📦 Installing: ${packages.joinToString(", ")} via $manager", TerminalLine.LineType.INFO)
            _isRunningCommand.value = true
            val result = app.packageManager.install(manager, packages, project.path)
            addTerminalLine(result, TerminalLine.LineType.OUTPUT)
            _isRunningCommand.value = false
            refreshFileTree()
        }
    }

    fun killCurrentProcess() {
        app.terminalExecutor.killCurrentProcess()
        _isRunningCommand.value = false
        addTerminalLine("⏹ Process killed", TerminalLine.LineType.INFO)
    }

    fun clearTerminal() { _terminalLines.value = emptyList() }

    private fun addTerminalLine(text: String, type: TerminalLine.LineType) {
        _terminalLines.update { it + TerminalLine(text, type) }
    }

    // ─── Models ───────────────────────────────────────────────────────────────

    fun downloadModel(model: LLMModel) {
        val intent = Intent(getApplication(), ModelDownloadService::class.java).apply {
            putExtra(ModelDownloadService.EXTRA_MODEL_ID, model.id)
            putExtra(ModelDownloadService.EXTRA_MODEL_NAME, model.name)
            putExtra(ModelDownloadService.EXTRA_DOWNLOAD_URL, model.downloadUrl)
            putExtra(ModelDownloadService.EXTRA_FILE_NAME, model.fileName)
            putExtra(ModelDownloadService.EXTRA_HF_TOKEN, settings.value.hfToken)
        }
        getApplication<Application>().startForegroundService(intent)
        updateModelStatus(model.id, ModelStatus.DOWNLOADING)
    }

    fun loadModel(model: LLMModel) {
        viewModelScope.launch {
            _modelLoadingState.value = "Loading ${model.name}..."
            updateModelStatus(model.id, ModelStatus.LOADING)
            app.llmRepository.loadModel(model.id, model.fileName).onSuccess {
                updateModelStatus(model.id, ModelStatus.LOADED)
                app.settingsManager.setActiveModel(model.id)
                _modelLoadingState.value = null
            }.onFailure { e ->
                updateModelStatus(model.id, ModelStatus.ERROR, e.message)
                _modelLoadingState.value = null
            }
        }
    }

    fun isModelDownloaded(model: LLMModel): Boolean {
        val f = File(getApplication<Application>().filesDir, "models/${model.fileName}")
        return f.exists()
    }

    private fun updateModelStatus(id: String, status: ModelStatus, error: String? = null) {
        _models.update { models ->
            models.map { if (it.id == id) it.copy(status = status, errorMessage = error) else it }
        }
    }

    private fun observeDownloadProgress() {
        viewModelScope.launch {
            ModelDownloadService.downloadProgress.collect { progressMap ->
                _models.update { models ->
                    models.map { model ->
                        val progress = progressMap[model.id]
                        if (progress != null) model.copy(downloadProgress = progress) else model
                    }
                }
            }
        }
        viewModelScope.launch {
            ModelDownloadService.downloadStatus.collect { statusMap ->
                _models.update { models ->
                    models.map { model ->
                        val status = statusMap[model.id]
                        if (status == "ready") model.copy(status = ModelStatus.READY)
                        else if (status?.startsWith("error") == true)
                            model.copy(status = ModelStatus.ERROR, errorMessage = status.removePrefix("error:"))
                        else model
                    }
                }
            }
        }
    }

    // ─── Skills ───────────────────────────────────────────────────────────────

    fun loadSkills() {
        viewModelScope.launch { _skills.value = app.skillsManager.getAllSkills() }
    }

    fun applySkill(skill: Skill, variables: Map<String, String>) {
        val prompt = app.skillsManager.applyTemplate(skill, variables)
        sendMessage(prompt)
    }

    fun saveCustomSkill(skill: Skill) {
        viewModelScope.launch { app.skillsManager.saveSkill(skill); loadSkills() }
    }

    // ─── Server ───────────────────────────────────────────────────────────────

    fun toggleServer(enable: Boolean) {
        val ctx = getApplication<Application>()
        if (enable) {
            ctx.startForegroundService(Intent(ctx, LLMServerService::class.java))
        } else {
            ctx.stopService(Intent(ctx, LLMServerService::class.java))
        }
        _serverRunning.value = enable
        viewModelScope.launch { app.settingsManager.setServerEnabled(enable) }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    fun updateSystemPrompt(prompt: String) {
        viewModelScope.launch { app.settingsManager.setSystemPrompt(prompt) }
    }
    fun updateTemperature(t: Float) {
        viewModelScope.launch { app.settingsManager.setTemperature(t) }
    }
    fun updateMaxTokens(n: Int) {
        viewModelScope.launch { app.settingsManager.setMaxTokens(n) }
    }
    fun updateFontSize(size: Int) {
        viewModelScope.launch { app.settingsManager.setFontSize(size) }
    }

    fun updateHfToken(token: String) {
        viewModelScope.launch { app.settingsManager.setHfToken(token) }
    }
}
