package xyz.gaon.typoon.feature.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.gaon.typoon.core.data.repository.ExceptionRepository
import javax.inject.Inject

@HiltViewModel
class DictionaryViewModel
    @Inject
    constructor(
        private val exceptionRepository: ExceptionRepository,
    ) : ViewModel() {
        val words: StateFlow<List<String>> =
            exceptionRepository
                .getAll()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val _inputWord = MutableStateFlow("")
        val inputWord: StateFlow<String> = _inputWord.asStateFlow()
        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        val filteredWords: StateFlow<List<String>> =
            combine(words, _searchQuery) { list, query ->
                if (query.isBlank()) {
                    list
                } else {
                    list.filter { it.contains(query, ignoreCase = true) }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun onInputChange(text: String) {
            _inputWord.value = text
        }

        fun onSearchQueryChange(query: String) {
            _searchQuery.value = query
        }

        fun onAddWord() {
            val word = _inputWord.value.trim()
            if (word.isBlank()) return

            viewModelScope.launch {
                exceptionRepository.add(word)
                _inputWord.value = ""
            }
        }

        fun onRemoveWord(word: String) {
            viewModelScope.launch {
                exceptionRepository.remove(word)
            }
        }
    }
