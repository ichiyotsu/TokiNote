// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "TokiNoteCore",
    platforms: [
        .iOS(.v17),
        .macOS(.v14),
    ],
    products: [
        .library(name: "TokiNoteCore", targets: ["TokiNoteCore"]),
    ],
    targets: [
        .target(name: "TokiNoteCore"),
        .testTarget(name: "TokiNoteCoreTests", dependencies: ["TokiNoteCore"]),
    ]
)
