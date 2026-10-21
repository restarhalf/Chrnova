package restarhalf.stellar.schedule.di

import org.koin.dsl.module

val commonAppModule = module {
    includes(portModule, useCaseModule, viewModelModule)
}
