package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.Campus as DataCampus
import restarhalf.stellar.schedule.domain.model.Campus as DomainCampus

fun DataCampus.toDomain(): DomainCampus {
    return when (this) {
        DataCampus.Development -> DomainCampus.Development
        DataCampus.Jinshitan -> DomainCampus.Jinshitan
    }
}

fun DomainCampus.toData(): DataCampus {
    return when (this) {
        DomainCampus.Development -> DataCampus.Development
        DomainCampus.Jinshitan -> DataCampus.Jinshitan
    }
}
