package peekniko

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import peekniko.internal.bodyrenderer.*
import peekniko.internal.notifications.Notifications
import peekniko.internal.notifications.RepositoryNotificationObserver
import peekniko.internal.notifications.createNotifications
import peekniko.internal.support.ViewModelFactory
import peekniko.internal.support.ViewModelStore
import peekniko.internal.support.initializer
import peekniko.internal.support.viewModelFactory
import peekniko.ui.details.NetworkCallDetailViewModel
import peekniko.ui.details.RequestViewModel
import peekniko.ui.details.ResponseViewModel
import peekniko.ui.details.SummaryViewModel
import peekniko.ui.networkcalls.PeeknikoViewModel

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

    internal val bodyRenderer: BodyRendererComposite by lazy {
        val spannedTextFactory = defaultSpannedTextBuilderFactory()
        BodyRendererComposite(
            renderers = listOf(
                BodyRendererJson(spannedTextFactory),
                BodyRendererXml(spannedTextFactory),
                BodyRendererHtml(spannedTextFactory),
                BodyRendererFormUrlEncoded(spannedTextFactory),
            ),
            fallback = BodyRendererPlainText(spannedTextFactory),
        )
    }

    internal val peeknikoViewModel: ViewModelFactory by lazy {
        viewModelFactory {
            initializer { PeeknikoViewModel(networkRepository) }
        }
    }

    internal fun getPeeknikoViewModel(store: ViewModelStore): PeeknikoViewModel =
        store.getOrCreate(PeeknikoViewModel::class, peeknikoViewModel)

    internal fun createNetworkCallDetailViewModel(transactionId: String?): NetworkCallDetailViewModel =
        NetworkCallDetailViewModel(networkRepository, bodyRenderer, transactionId)

    internal fun createSummaryViewModel(transactionId: String): SummaryViewModel =
        SummaryViewModel(networkRepository, transactionId)

    internal fun createRequestViewModel(transactionId: String): RequestViewModel {
        val spannedTextFactory = defaultSpannedTextBuilderFactory()
        return RequestViewModel(
            repository = networkRepository,
            themeProvider = BodyRendererThemeProvider(),
            bodyRenderer = bodyRenderer,
            noopBodyRenderer = BodyRendererPlainText(spannedTextFactory),
            transactionId = transactionId
        )
    }

    internal fun createResponseViewModel(transactionId: String): ResponseViewModel {
        val spannedTextFactory = defaultSpannedTextBuilderFactory()
        return ResponseViewModel(
            repository = networkRepository,
            themeProvider = BodyRendererThemeProvider(),
            bodyRenderer = bodyRenderer,
            noopBodyRenderer = BodyRendererPlainText(spannedTextFactory),
            transactionId = transactionId,
        )
    }
}
