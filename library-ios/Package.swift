// swift-tools-version: 5.9
// Local development package: run `./gradlew :library-ios:assemblePeeknikoCoreDebugXCFramework`,
// then add this folder to Xcode. Releases use the generated root Package.swift instead.
import PackageDescription

let package = Package(
    name: "PeeknikoKit",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "PeeknikoKit", targets: ["PeeknikoKit"]),
    ],
    targets: [
        // Binary targets can't declare linker settings, so this wrapper links SQLite
        // for the SQLDelight storage bundled in PeeknikoCore.
        .target(
            name: "PeeknikoKit",
            dependencies: ["PeeknikoCore"],
            path: "swift",
            linkerSettings: [.linkedLibrary("sqlite3")]
        ),
        .binaryTarget(
            name: "PeeknikoCore",
            path: "build/XCFrameworks/debug/PeeknikoCore.xcframework"
        ),
    ]
)
