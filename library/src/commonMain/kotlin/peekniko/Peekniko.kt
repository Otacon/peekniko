package peekniko

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import peekniko.internal.bodyrenderer.*
import peekniko.internal.formatting.ReadableTimeFormatter
import peekniko.internal.notifications.Notifications
import peekniko.internal.notifications.RepositoryNotificationObserver
import peekniko.internal.notifications.createNotifications
import peekniko.ui.details.BodyViewModel
import peekniko.ui.details.NetworkCallDetailViewModel
import peekniko.ui.details.SummaryViewModel

public object Peekniko {
    private val initializationLock = SynchronizedObject()
    private lateinit var repository: NetworkRepository
    private lateinit var notificationObserver: RepositoryNotificationObserver
    private lateinit var notificationService: Notifications

    public val networkRepository: NetworkRepository
        get() = synchronized(initializationLock) { repository }

    internal val notifications: Notifications
        get() = synchronized(initializationLock) { notificationService }

    public fun configure(configure: Configuration.() -> Unit) {
        val configuration = Configuration().apply(configure)
        synchronized(initializationLock) {
            if (::notificationObserver.isInitialized) return
            repository = configuration.storage
            notificationService = createNotifications()
            notificationObserver = RepositoryNotificationObserver(repository, notificationService)
        }
    }

    public class Configuration internal constructor() {
        public var storage: NetworkRepository = NetworkRepositoryInMemory()

        public fun inMemoryStorage(maxApiCalls: Int = 100) {
            require(maxApiCalls > 0) { "maxApiCalls must be positive" }
            storage = NetworkRepositoryInMemory(maxApiCalls)
        }
    }

    internal val bodyRenderer: BodyRenderer by lazy {
        BodyRendererComposite(
            renderers = listOf(
                BodyRendererJson(),
                BodyRendererXml(),
                BodyRendererHtml(),
                BodyRendererFormUrlEncoded(),
            ),
        )
    }

    internal fun createNetworkCallDetailViewModel(transactionId: String?): NetworkCallDetailViewModel =
        NetworkCallDetailViewModel(
            transactionId = transactionId,
            networkRepository = networkRepository
        )

    internal fun createSummaryViewModel(transactionId: String): SummaryViewModel =
        SummaryViewModel(
            transactionId = transactionId,
            repository = networkRepository,
            timeFormatter = ReadableTimeFormatter(includeMillis = true)
        )

    internal fun createBodyViewModel(transactionId: String, isRequest: Boolean): BodyViewModel =
        BodyViewModel(
            repository = networkRepository,
            themeProvider = BodyRendererThemeProvider(),
            bodyRenderer = bodyRenderer,
            transactionId = transactionId,
            isRequest = isRequest,
        )
}
