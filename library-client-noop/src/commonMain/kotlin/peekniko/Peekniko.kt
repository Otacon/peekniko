package peekniko

public object Peekniko {

    public fun configure(configure: Configuration.() -> Unit): Unit = Unit

    public class Configuration internal constructor() {

        public fun inMemoryStorage(maxApiCalls: Int = 100): Unit = Unit
    }
}
