package com.scb.scbbillingandcollection.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Renderers for  Effect, used to pass to the observers
 */

abstract class ActionViewModel<A : BaseAction>(
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO
) :
    ViewModel() {

    private val tag by lazy { javaClass.simpleName }

    /**
     * Bind the Actions to produce the respective States
     */
    protected abstract suspend fun bindActions()

    /**
     * Action is the representation of a UI event that will trigger a new State
     */
    private val _actions = MutableSharedFlow<A>()
    protected val actions = _actions.asSharedFlow()


    init {
        viewModelScope.launch {
            bindActions()
        }
    }

    /**
     * Utility method to dispatch an Action from UI that will produce a new State
     */
    fun dispatch(action: A) {
        viewModelScope.launch { _actions.emit(action) }
    }
}
