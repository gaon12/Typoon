package xyz.gaon.typoon.core.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.gaon.typoon.core.data.repository.ExceptionRepository
import xyz.gaon.typoon.core.engine.ConversionEngine
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversionEngineExceptionSync
    @Inject
    constructor(
        private val exceptionRepository: ExceptionRepository,
        private val conversionEngine: ConversionEngine,
    ) {
        private val started = AtomicBoolean(false)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun start() {
            if (!started.compareAndSet(false, true)) return

            scope.launch {
                exceptionRepository.getAll().collectLatest { words ->
                    conversionEngine.setExceptions(words.map(String::trim).filter(String::isNotEmpty).toSet())
                }
            }
        }
    }
