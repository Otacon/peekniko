// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "PeeknikoKit",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "PeeknikoKit", targets: ["PeeknikoKit"]),
    ],
    targets: [
        .binaryTarget(
            name: "PeeknikoCore",
            path: "build/XCFrameworks/debug/PeeknikoCore.xcframework"
        ),
        .target(
            name: "PeeknikoKit",
            dependencies: ["PeeknikoCore"],
            path: "src/iosMain/swift"
        ),
    ]
)
