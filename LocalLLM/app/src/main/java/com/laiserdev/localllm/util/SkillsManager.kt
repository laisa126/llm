package com.laiserdev.localllm.util

import android.content.Context
import com.laiserdev.localllm.data.model.Skill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * SkillsManager: reusable AI prompt templates ("skills") that the user
 * can create, edit, and invoke — similar to Claude's built-in skills.
 *
 * Each skill has:
 *  - name, description, icon
 *  - systemPrompt (how the AI should behave)
 *  - userPromptTemplate (with {{VARIABLE}} placeholders)
 *  - category (code, debug, explain, refactor, test, docs, shell, custom)
 */
class SkillsManager(private val context: Context) {

    private val skillsDir = File(context.filesDir, "skills").also { it.mkdirs() }
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    // ─── Built-in skills ──────────────────────────────────────────────────────

    val builtinSkills = listOf(
        Skill(
            id = "generate_project",
            name = "Generate Project",
            description = "Create a full project from a description",
            icon = "🚀",
            category = "code",
            systemPrompt = """You are an expert software architect. Generate complete, production-ready projects.
Respond with ONLY a JSON object:
{"projectName":"name","files":[{"path":"relative/path","content":"full content"}]}
Include ALL necessary files. Make the code work out of the box.""",
            userPromptTemplate = "Create a {{PROJECT_TYPE}} project that {{DESCRIPTION}}"
        ),
        Skill(
            id = "fix_bug",
            name = "Fix Bug",
            description = "Analyze and fix code bugs",
            icon = "🐛",
            category = "debug",
            systemPrompt = """You are an expert debugger. Analyze code, identify bugs, and provide fixes.
Format: 
1. Root cause analysis
2. Fixed code (complete, not partial)
3. Explanation of changes""",
            userPromptTemplate = "Fix this bug in {{LANGUAGE}} code:\n\n{{CODE}}\n\nError: {{ERROR}}"
        ),
        Skill(
            id = "explain_code",
            name = "Explain Code",
            description = "Explain what code does in plain English",
            icon = "📖",
            category = "explain",
            systemPrompt = "You are a patient senior developer who explains code clearly to any skill level.",
            userPromptTemplate = "Explain this {{LANGUAGE}} code step by step:\n\n{{CODE}}"
        ),
        Skill(
            id = "refactor_code",
            name = "Refactor",
            description = "Improve code quality and structure",
            icon = "♻️",
            category = "refactor",
            systemPrompt = """Refactor the given code for: readability, performance, maintainability.
Return the complete refactored file with comments explaining key changes.""",
            userPromptTemplate = "Refactor this {{LANGUAGE}} code. Goal: {{GOAL}}\n\n{{CODE}}"
        ),
        Skill(
            id = "write_tests",
            name = "Write Tests",
            description = "Generate unit and integration tests",
            icon = "🧪",
            category = "test",
            systemPrompt = "You write comprehensive tests. Cover happy path, edge cases, and error conditions.",
            userPromptTemplate = "Write {{TEST_FRAMEWORK}} tests for:\n\n{{CODE}}"
        ),
        Skill(
            id = "write_docs",
            name = "Write Docs",
            description = "Generate documentation and README",
            icon = "📝",
            category = "docs",
            systemPrompt = "You write clear, complete technical documentation in Markdown.",
            userPromptTemplate = "Write {{DOC_TYPE}} documentation for:\n\n{{CODE}}"
        ),
        Skill(
            id = "shell_script",
            name = "Shell Script",
            description = "Generate bash/shell scripts",
            icon = "⚡",
            category = "shell",
            systemPrompt = "You write robust shell scripts with error handling, comments, and best practices.",
            userPromptTemplate = "Write a shell script that {{DESCRIPTION}}"
        ),
        Skill(
            id = "code_review",
            name = "Code Review",
            description = "Review code for issues and improvements",
            icon = "🔍",
            category = "code",
            systemPrompt = """Review code as a senior engineer. Check for:
- Security vulnerabilities
- Performance issues
- Logic errors
- Code style and maintainability
- Missing error handling
Rate severity: 🔴 Critical 🟡 Warning 🟢 Suggestion""",
            userPromptTemplate = "Review this {{LANGUAGE}} code:\n\n{{CODE}}"
        ),
        Skill(
            id = "optimize_perf",
            name = "Optimize Performance",
            description = "Find and fix performance bottlenecks",
            icon = "⚡",
            category = "refactor",
            systemPrompt = "You are a performance engineer. Identify bottlenecks and provide optimized versions.",
            userPromptTemplate = "Optimize this {{LANGUAGE}} code for performance:\n\n{{CODE}}"
        ),
        Skill(
            id = "convert_code",
            name = "Convert Language",
            description = "Translate code between languages",
            icon = "🔄",
            category = "code",
            systemPrompt = "You are an expert in all programming languages. Translate code preserving logic and best practices of the target language.",
            userPromptTemplate = "Convert this {{FROM_LANGUAGE}} code to {{TO_LANGUAGE}}:\n\n{{CODE}}"
        ),
        Skill(
            id = "api_integration",
            name = "API Integration",
            description = "Generate API client code",
            icon = "🔌",
            category = "code",
            systemPrompt = "You generate complete API integration code with error handling, retry logic, and TypeScript types.",
            userPromptTemplate = "Generate {{LANGUAGE}} code to integrate with {{API_NAME}} API. Requirements: {{REQUIREMENTS}}"
        ),
        Skill(
            id = "regex_builder",
            name = "Regex Builder",
            description = "Build and explain regular expressions",
            icon = "🔤",
            category = "code",
            systemPrompt = "You are a regex expert. Build regexes and explain each part clearly.",
            userPromptTemplate = "Build a regex that matches: {{DESCRIPTION}}\nLanguage: {{LANGUAGE}}"
        )
    )

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    suspend fun getAllSkills(): List<Skill> = withContext(Dispatchers.IO) {
        val custom = skillsDir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { f ->
                try { json.decodeFromString<Skill>(f.readText()) }
                catch (e: Exception) { null }
            } ?: emptyList()
        builtinSkills + custom
    }

    suspend fun saveSkill(skill: Skill): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(skillsDir, "${skill.id}.json").writeText(json.encodeToString(skill))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteSkill(skillId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(skillsDir, "$skillId.json").delete()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ─── Apply skill template with variables ──────────────────────────────────

    fun applyTemplate(skill: Skill, variables: Map<String, String>): String {
        var prompt = skill.userPromptTemplate
        variables.forEach { (key, value) ->
            prompt = prompt.replace("{{$key}}", value)
        }
        return prompt
    }

    // ─── Extract variable names from template ─────────────────────────────────

    fun extractVariables(template: String): List<String> {
        val regex = Regex("\\{\\{([A-Z_]+)\\}\\}")
        return regex.findAll(template).map { it.groupValues[1] }.distinct().toList()
    }
}
