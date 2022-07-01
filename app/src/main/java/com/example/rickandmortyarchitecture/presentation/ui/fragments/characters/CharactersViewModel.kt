package com.example.rickandmortyarchitecture.presentation.ui.fragments.characters

import androidx.lifecycle.viewModelScope
import com.bacon.common.either.Either
import com.bacon.domain.usecase.FetchCharactersUseCase
import com.bacon.domain.usecase.FetchEpisodesDetailUseCase
import com.example.rickandmortyarchitecture.base.BaseFetch
import com.example.rickandmortyarchitecture.base.BaseViewModel
import com.example.rickandmortyarchitecture.presentation.models.CharactersUI
import com.example.rickandmortyarchitecture.presentation.models.EpisodesUI
import com.example.rickandmortyarchitecture.presentation.models.toUI
import com.example.rickandmortyarchitecture.presentation.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharactersViewModel @Inject constructor(
    private val fetchCharactersUseCase: FetchCharactersUseCase,
    private val fetchEpisodesDetailUseCase: FetchEpisodesDetailUseCase,
) : BaseViewModel(), BaseFetch {
    private val _charactersState = MutableStateFlow<UIState<List<CharactersUI>>>(UIState.Loading())
    val charactersState: StateFlow<UIState<List<CharactersUI>>> = _charactersState

    private val _fetchFirstSeenIn = MutableUIStateFlow<EpisodesUI>()
    val fetchFirstSeenIn = _fetchFirstSeenIn.asStateFlow()
    override var page: Int = 1

    init {
        fetchRick(1)
    }

    override fun fetchRick(page: Int) {
        fetchCharactersUseCase(page).collectRequest(_charactersState) { it ->
            it.map {
                it.toUI()
            }
        }
    }

    fun fetchEpisode(id: Int) {
        fetchEpisodesDetailUseCase(id).collectResource(_fetchFirstSeenIn) {
            it.toUI()
        }
    }

    private fun <T, S> Flow<Either<String, T>>.collectResource(
        state: MutableStateFlow<UIState<S>>,
        mappedData: (T) -> S,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            state.value = UIState.Loading()
            this@collectResource.collect {
                when (it) {
                    is Either.Left -> state.value = UIState.Error(it.value)
                    is Either.Right -> state.value = UIState.Success(mappedData(it.value))
                }
            }
        }
    }
}
