package ru.hoster.inprogress.di

import javax.inject.Qualifier

/**
 * Qualifier annotation for injecting the application-wide CoroutineScope.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope