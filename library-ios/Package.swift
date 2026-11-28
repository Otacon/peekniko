// swift-tools-version: 5.9
// Local development package. Build the framework first:
//   ./gradlew :library-ios:assemblePeeknikoCoreDebugXCFramework
// Releases derive the root Package.swift from this file with .github/scripts/generate-package-swift.sh,
// which rewrites the `root` and `core` lines below. Keep each of them on a single line.
import PackageDescription

let root = ""
let core = Target.binaryTarget(name: "PeeknikoCore", path: "build/XCFrameworks/debug/PeeknikoCore.xcframework")

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
            path: root + "peekniko",
            swiftSettings: [.define("DEBUG", .when(configuration: .debug))]
        ),
        core,
        .target(
            name: "PeeknikoCoreNoop",
            path: root + "noop"
        ),
    ]
)
