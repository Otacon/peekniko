package peekniko.internal.notifications

import peekniko.NetworkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class RepositoryNotificationObserver(
    repository: NetworkRepository,
    notifications: Notifications,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            repository.observeCallUpdates(notifications.callLimit).collect { notifications.show(it) }
        }
    }
}
