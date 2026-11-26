// Swift Package Manager can't attach linker settings to a binary target, so the
// PeeknikoKit product goes through this target, which links SQLite for the
// framework's SQLDelight storage. Apps can `import PeeknikoCore` (or `PeeknikoKit`).
@_exported import PeeknikoCore
