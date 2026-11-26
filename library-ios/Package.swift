// swift-tools-version: 5.9
// Local development package: run `./gradlew :library-ios:assemblePeeknikoCoreDebugXCFramework`,
// then add this folder to Xcode. Releases use the generated root Package.swift instead.
import PackageDescription

let package = Package(
    name: "Peekniko",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "Peekniko", targets: ["Peekniko"]),
    ],
    targets: [
        .target(
            name: "Peekniko",
            dependencies: ["PeeknikoCore", "PeeknikoCoreNoop"],
            path: "peekniko",
            swiftSettings: [.define("DEBUG", .when(configuration: .debug))]
        ),
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
