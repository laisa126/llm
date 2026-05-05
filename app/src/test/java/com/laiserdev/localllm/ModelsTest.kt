package com.laiserdev.localllm

import com.laiserdev.localllm.data.model.AVAILABLE_MODELS
import com.laiserdev.localllm.data.model.ChatMessage
import com.laiserdev.localllm.data.model.LLMModel
import com.laiserdev.localllm.data.model.MessageRole
import com.laiserdev.localllm.data.model.ModelStatus
import com.laiserdev.localllm.data.repository.ModelBootstrap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for data-model invariants and the bootstrap lookup fix.
 *
 * These run on the JVM (no Android framework required) so they execute fast
 * in CI via `./gradlew testDebugUnitTest`.
 */
class ModelsTest {

    // ── AVAILABLE_MODELS catalogue ─────────────────────────────────────────

    @Test
    fun `AVAILABLE_MODELS is not empty`() {
        assertTrue(AVAILABLE_MODELS.isNotEmpty())
    }

    @Test
    fun `every model has a unique id`() {
        val ids = AVAILABLE_MODELS.map { it.id }
        assertEquals("Duplicate model IDs found", ids.size, ids.distinct().size)
    }

    @Test
    fun `every model has a non-blank downloadUrl`() {
        AVAILABLE_MODELS.forEach { model ->
            assertTrue("downloadUrl blank for ${model.id}", model.downloadUrl.isNotBlank())
        }
    }

    @Test
    fun `every model has a non-blank fileName ending in task or litertlm`() {
        AVAILABLE_MODELS.forEach { model ->
            val ext = model.fileName.substringAfterLast(".")
            assertTrue(
                "Unexpected file extension '.$ext' for ${model.id}",
                ext == "task" || ext == "litertlm"
            )
        }
    }

    @Test
    fun `vision models are a strict subset of code models`() {
        // Every vision model in our catalogue also supports code.
        val visionOnlyNoCode = AVAILABLE_MODELS.filter { it.supportsVision && !it.supportsCode }
        assertTrue(
            "Unexpected vision-only-no-code models: ${visionOnlyNoCode.map { it.id }}",
            visionOnlyNoCode.isEmpty()
        )
    }

    // ── Bootstrap bundled model lookup ─────────────────────────────────────

    @Test
    fun `BUNDLED_MODEL_ID exists in AVAILABLE_MODELS`() {
        val bundled = AVAILABLE_MODELS.firstOrNull { it.id == ModelBootstrap.BUNDLED_MODEL_ID }
        assertNotNull(
            "BUNDLED_MODEL_ID '${ModelBootstrap.BUNDLED_MODEL_ID}' not found in AVAILABLE_MODELS",
            bundled
        )
    }

    @Test
    fun `bundled model supportsVision matches catalogue entry`() {
        val bundled = AVAILABLE_MODELS.first { it.id == ModelBootstrap.BUNDLED_MODEL_ID }
        // gemma3-1b is the default bundled model — it does NOT support vision.
        // This test encodes the expected value so a future change to BUNDLED_MODEL_ID
        // or the catalogue entry will break here loudly.
        val expectedVision = bundled.supportsVision
        assertFalse(
            "Bundled model '${bundled.id}' unexpectedly reports supportsVision=true",
            expectedVision
        )
    }

    @Test
    fun `BUNDLED_MODEL_FILE matches catalogue fileName`() {
        val bundled = AVAILABLE_MODELS.first { it.id == ModelBootstrap.BUNDLED_MODEL_ID }
        assertEquals(
            "BUNDLED_MODEL_FILE does not match catalogue fileName for ${bundled.id}",
            bundled.fileName,
            ModelBootstrap.BUNDLED_MODEL_FILE
        )
    }

    // ── ChatMessage defaults ────────────────────────────────────────────────

    @Test
    fun `ChatMessage defaults are sane`() {
        val msg = ChatMessage(role = MessageRole.USER, content = "hello")
        assertFalse(msg.id.isBlank())
        assertNull(msg.imageUri)
        assertFalse(msg.isStreaming)
        assertEquals(0f, msg.tokensPerSecond)
        assertTrue(msg.toolCalls.isEmpty())
    }

    @Test
    fun `ChatMessage with imageUri preserves it`() {
        val uri = "content://media/external/images/media/42"
        val msg = ChatMessage(role = MessageRole.USER, content = "look at this", imageUri = uri)
        assertEquals(uri, msg.imageUri)
    }

    // ── LLMModel defaults ───────────────────────────────────────────────────

    @Test
    fun `LLMModel default status is NOT_DOWNLOADED`() {
        val model = AVAILABLE_MODELS.first()
        assertEquals(ModelStatus.NOT_DOWNLOADED, model.status)
    }

    @Test
    fun `LLMModel copy with status preserves other fields`() {
        val original = AVAILABLE_MODELS.first()
        val loaded = original.copy(status = ModelStatus.LOADED)
        assertEquals(original.id, loaded.id)
        assertEquals(original.supportsVision, loaded.supportsVision)
        assertEquals(ModelStatus.LOADED, loaded.status)
    }
}
