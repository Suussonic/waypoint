package com.waypoint.app

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class QuestSnapshot(
    val quests: List<QuestRecord> = emptyList(),
    val rewards: List<QuestReward> = emptyList(),
    val focus: FocusRecord? = null,
    val profile: QuestProfile = QuestProfile(),
    val loading: Boolean = true,
    val error: String? = null
) {
    val progress get() = QuestProgress(rewards.sumOf { it.xp }, rewards.size, rewards.count { it.focused })
}

class QuestController(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val actions = Mutex()
    val repository = QuestRepository(QuestDatabase.open(context), AndroidQuestTime(context))
    val reminders = QuestReminders(context, repository)
    private val _state = MutableStateFlow(QuestSnapshot())
    val state = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    init {
        scope.launch {
            combine(repository.dao.observeQuests(), repository.dao.observeRewards(), repository.dao.observeFocus(), repository.dao.observeProfile()) { quests, rewards, focus, profile ->
                QuestSnapshot(quests, rewards, focus, profile ?: QuestProfile(), loading = false)
            }.catch { _state.update { it.copy(loading = false, error = "Your quests could not be opened. Close and reopen Waypoint to try again.") } }
                .collect { snapshot ->
                    _state.value = snapshot.copy(error = _state.value.error)
                    reminders.sync(snapshot.quests, snapshot.focus)
                }
        }
        refresh()
        scope.launch { runCatching { QuestImages.cleanAbandoned(context, repository.dao.imageNames().toSet()) } }
    }

    private fun act(block: suspend () -> Unit) { scope.launch {
        actions.withLock {
            _busy.value = true
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: IllegalArgumentException) { _state.update { it.copy(error = error.message ?: "Check the quest details and try again.") } }
            catch (error: IllegalStateException) { _state.update { it.copy(error = error.message ?: "This quest changed. Open it again to continue.") } }
            catch (_: Exception) { _state.update { it.copy(error = "That change could not be saved. Check available storage and try again.") } }
            finally { _busy.value = false }
        }
    } }
    fun dismissError() { _state.update { it.copy(error = null) } }
    fun save(quest: QuestRecord, onSaved: () -> Unit) = act {
        val previous = repository.dao.quest(quest.id)
        repository.save(quest)
        if (previous?.imageName != quest.imageName) previous?.imageName?.let { QuestImages.delete(context, it) }
        onSaved()
    }
    fun complete(id: String, onCompleted: () -> Unit = {}) = act {
        if (repository.complete(id)) { reminders.cancelQuest(id); onCompleted() }
    }
    fun reopen(id: String) = act { repository.reopen(id) }
    fun delete(id: String, onDeleted: () -> Unit = {}) = act {
        repository.delete(id)?.imageName?.let { QuestImages.delete(context, it) }
        reminders.cancelQuest(id)
        onDeleted()
    }
    fun startFocus(id: String, alerts: Boolean) = act { repository.startFocus(id, alerts) }
    fun focusAction(id: String, action: String) = act { repository.changeFocus(id, action) }
    fun refresh() = act {
        repository.reconcileFocus()?.let { reminders.showFocus(it) }
        reminders.restore()
    }
    fun presented(ids: List<String>) = act { repository.dao.presented(ids) }
    fun showProgress(value: Boolean) = act { repository.updateProfile { it.copy(showProgress = value) } }
    fun sound(value: Boolean) = act { repository.updateProfile { it.copy(sound = value) } }
    fun frame(value: String) = act { if (value in _state.value.progress.frames) repository.updateProfile { it.copy(frame = value) } }
}
