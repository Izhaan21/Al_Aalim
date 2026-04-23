package com.example.al_aalim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.al_aalim.models.ChatConversation
import com.example.al_aalim.repository.AuthRepository
import com.example.al_aalim.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ChatHistoryState {
    object Loading : ChatHistoryState()
    data class Success(val conversations: List<ChatConversation>) : ChatHistoryState()
    data class Error(val message: String) : ChatHistoryState()
}

class MainViewModel(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val userId: String? get() = authRepository.currentUser?.uid

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    fun setActiveConversation(id: String?) {
        _activeConversationId.value = id
    }

    private val _chatHistoryState = MutableStateFlow<ChatHistoryState>(ChatHistoryState.Loading)
    val chatHistoryState: StateFlow<ChatHistoryState> = _chatHistoryState.asStateFlow()

    fun loadConversations() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { chatRepository.getConversations() }
            if (result.isSuccess) {
                _chatHistoryState.value = ChatHistoryState.Success(result.getOrNull() ?: emptyList())
            } else {
                _chatHistoryState.value = ChatHistoryState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun deleteConversation(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { chatRepository.deleteConversation(id) }
            onComplete(result.isSuccess)
            if (result.isSuccess) {
                if (_activeConversationId.value == id) {
                    _activeConversationId.value = null
                }
                loadConversations()
            }
        }
    }

    fun deleteAllConversations(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { chatRepository.deleteAllConversations() }
            onComplete(result.isSuccess)
            if (result.isSuccess) {
                _activeConversationId.value = null
                loadConversations()
            }
        }
    }

    fun renameConversation(id: String, newTitle: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { chatRepository.updateConversationTitle(id, newTitle) }
            onComplete(result.isSuccess)
            if (result.isSuccess) {
                loadConversations()
            }
        }
    }
}
