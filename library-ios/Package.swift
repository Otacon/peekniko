// swift-tools-version: 5.9
// Local development package: run `./gradlew :library-ios:assemblePeeknikoCoreDebugXCFramework`,
// then add this folder to Xcode. Releases use the generated root Package.swift instead.
import PackageDescription

let package = Package(
    name: "PeeknikoKit",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "PeeknikoKit", targets: ["PeeknikoCore"]),
        .library(name: "PeeknikoKitNoop", targets: ["PeeknikoCoreNoop"]),
    ],
    targets: [
        .binaryTarget(
            name: "PeeknikoCore",
            path: "build/XCFrameworks/debug/PeeknikoCore.xcframework"
        ),
        .target(
            name: "PeeknikoCoreNoop",
            path: "noop"
        ),
    ]
)
