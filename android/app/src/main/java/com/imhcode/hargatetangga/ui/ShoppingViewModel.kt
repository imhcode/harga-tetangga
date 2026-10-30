package com.imhcode.hargatetangga.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imhcode.hargatetangga.data.ChatMessage
import com.imhcode.hargatetangga.data.EstimateDto
import com.imhcode.hargatetangga.data.ListItemRow
import com.imhcode.hargatetangga.data.NewListItem
import com.imhcode.hargatetangga.data.ParsedItem
import com.imhcode.hargatetangga.data.Proposal
import com.imhcode.hargatetangga.data.ShoppingListRow
import com.imhcode.hargatetangga.data.ShoppingRepository
import com.imhcode.hargatetangga.data.VoiceRecorder
import kotlinx.coroutines.launch

// A parsed or proposed item waiting for the person's tick (H4/H5).
class Candidate(val label: String, val quantity: Double, val productId: String?, val productName: String?) {
    var checked by mutableStateOf(true)
}

class ShoppingViewModel : ViewModel() {
    var list by mutableStateOf<ShoppingListRow?>(null)
        private set
    val items = mutableStateListOf<ListItemRow>()
    var draftText by mutableStateOf("")
    val parsed = mutableStateListOf<Candidate>()
    var estimate by mutableStateOf<EstimateDto?>(null)
        private set
    var bufferPct by mutableStateOf(10)
        private set
    var savedAt by mutableStateOf<String?>(null)
        private set
    val chat = mutableStateListOf<ChatMessage>()
    val proposals = mutableStateListOf<Candidate>()
    var question by mutableStateOf("")
    var transcript by mutableStateOf<String?>(null) // what the recogniser heard, shown above the ticks
        private set
    private var parsedSource = "text"
    private val recorder = VoiceRecorder()
    var recording by mutableStateOf(false)
        private set
    var recordedSeconds by mutableStateOf(0)
        private set
    var busy by mutableStateOf<String?>(null) // what is running, shown to the person
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    private fun run(label: String, block: suspend () -> Unit) {
        busy = label
        error = null
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                error = "$label gagal: ${e.message ?: e::class.simpleName}"
            } finally {
                busy = null
            }
        }
    }

    fun refresh() = run("Memuat daftar") { reload() }

    private suspend fun reload() {
        val current = ShoppingRepository.currentList()
        list = current
        items.clear()
        items.addAll(ShoppingRepository.items(current.id))
        estimate = if (items.isEmpty()) null else ShoppingRepository.estimate(current.id, bufferPct)
    }

    fun parse() {
        val text = draftText.trim()
        if (text.isEmpty()) return
        run("Mengurai daftar") {
            val result = ShoppingRepository.parse(text)
            transcript = null
            parsedSource = "text"
            parsed.clear()
            parsed.addAll(result.items.map { it.toCandidate() })
        }
    }

    fun startVoice() {
        if (recording) return
        error = null
        recorder.start(viewModelScope)
        recording = true
        viewModelScope.launch {
            while (recording) {
                recordedSeconds = recorder.seconds
                if (recordedSeconds >= VoiceRecorder.MAX_SECONDS) stopVoice()
                kotlinx.coroutines.delay(250)
            }
        }
    }

    fun stopVoice() {
        if (!recording) return
        recording = false
        run("Mendengarkan") {
            val wav = recorder.stop()
            if (wav.size < 44 + 24_000) throw IllegalStateException("Rekaman terlalu pendek. Bicara sedikit lebih lama.")
            val result = ShoppingRepository.parseVoice(wav)
            transcript = result.transcript ?: ""
            parsedSource = "voice"
            parsed.clear()
            parsed.addAll(result.items.map { it.toCandidate() })
        }
    }

    private fun ParsedItem.toCandidate() = Candidate(label, quantity, productId, productName)

    fun addParsed() {
        val listId = list?.id ?: return
        val chosen = parsed.filter { it.checked }
        run("Menambah barang") {
            ShoppingRepository.addItems(chosen.map { NewListItem(listId, it.label, it.productId, it.quantity, parsedSource) })
            parsed.clear()
            transcript = null
            draftText = ""
            reload()
        }
    }

    fun cancelParsed() {
        parsed.clear()
        transcript = null
    }

    fun removeItem(item: ListItemRow) = run("Menghapus barang") {
        ShoppingRepository.deleteItem(item.id)
        reload()
    }

    fun setBuffer(pct: Int) {
        bufferPct = pct
        val listId = list?.id ?: return
        if (items.isNotEmpty()) run("Menghitung") { estimate = ShoppingRepository.estimate(listId, pct) }
    }

    // F2-08: snapshot before leaving, compared later with the receipt total.
    fun startShopping() {
        val listId = list?.id ?: return
        run("Menyimpan estimasi") {
            estimate = ShoppingRepository.estimate(listId, bufferPct, save = true)
            savedAt = java.time.LocalTime.now().withSecond(0).withNano(0).toString()
            list = list?.copy(status = "shopping")
        }
    }

    fun finishShopping() {
        val listId = list?.id ?: return
        run("Menutup daftar") {
            ShoppingRepository.finishList(listId)
            savedAt = null
            chat.clear()
            proposals.clear()
            reload()
        }
    }

    fun ask(text: String = question) {
        val listId = list?.id ?: return
        val message = text.trim()
        if (message.isEmpty()) return
        val history = chat.toList()
        chat.add(ChatMessage("user", message))
        question = ""
        run("Asisten berpikir") {
            val reply = ShoppingRepository.ask(listId, message, history)
            chat.add(ChatMessage("assistant", reply.reply))
            proposals.clear()
            proposals.addAll(reply.proposals.map { it.toCandidate() })
        }
    }

    private fun Proposal.toCandidate() = Candidate(label, quantity, null, null)

    // H5: the agent only proposed these; they join the list when the person taps the button.
    fun acceptProposals() {
        val listId = list?.id ?: return
        val chosen = proposals.filter { it.checked }
        run("Menambah barang") {
            ShoppingRepository.addItems(chosen.map { NewListItem(listId, it.label, null, it.quantity, "agent") })
            proposals.clear()
            reload()
        }
    }
}
