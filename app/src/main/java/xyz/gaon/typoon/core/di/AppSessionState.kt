package xyz.gaon.typoon.core.di

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionState
    @Inject
    constructor() {
        var hasHandledHomeAutoRead: Boolean = false
    }
