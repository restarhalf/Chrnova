package restarhalf.stellar.schedule.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val AppIoDispatcher: CoroutineDispatcher = Dispatchers.IO
