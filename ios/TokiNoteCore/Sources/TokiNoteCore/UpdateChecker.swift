import Foundation

public struct IOSRelease: Equatable, Sendable, Identifiable {
    public var tag: String
    public var version: String
    public var notes: String
    public var ipaURL: URL?
    public var pageURL: URL?
    public var publishedAt: Date?
    public var id: String { tag }

    public init(tag: String, version: String, notes: String, ipaURL: URL?, pageURL: URL?, publishedAt: Date?) {
        self.tag = tag
        self.version = version
        self.notes = notes
        self.ipaURL = ipaURL
        self.pageURL = pageURL
        self.publishedAt = publishedAt
    }
}

public enum UpdateCheckResult: Equatable, Sendable {
    case update(IOSRelease)
    case upToDate
    case noRelease
    case failed
}

public enum IOSReleaseIndex {
    public static let releasesURL = URL(string: "https://api.github.com/repos/ichiyotsu/TokiNote/releases?per_page=100")!

    public static func isNewer(_ remote: String, than current: String) -> Bool {
        let left = ParsedVersion.parse(remote)
        let right = ParsedVersion.parse(current)
        let core = compareCore(left.core, right.core)
        if core > 0 { return true }
        if core < 0 { return false }
        if left.preRelease.isEmpty { return !right.preRelease.isEmpty }
        if right.preRelease.isEmpty { return false }
        return left.preRelease > right.preRelease
    }

    public static func version(fromTag tag: String) -> String? {
        let trimmed = tag.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.lowercased().hasPrefix("ios-") else { return nil }
        var version = String(trimmed.dropFirst(4))
        if version.lowercased().hasPrefix("v") {
            version.removeFirst()
        }
        return version
    }

    public static func parse(data: Data) -> [IOSRelease]? {
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        guard let raw = try? decoder.decode([GitHubRelease].self, from: data) else { return nil }
        return raw.compactMap { release in
            if release.draft == true { return nil }
            guard let version = version(fromTag: release.tagName) else { return nil }
            let ipa = release.assets?
                .filter { $0.name.lowercased().hasSuffix(".ipa") }
                .sorted { lhs, rhs in
                    let leftPreferred = lhs.name.lowercased().contains("tokinote")
                    let rightPreferred = rhs.name.lowercased().contains("tokinote")
                    if leftPreferred != rightPreferred { return leftPreferred && !rightPreferred }
                    return lhs.name < rhs.name
                }
                .first
                .flatMap { URL(string: $0.browserDownloadUrl) }
            return IOSRelease(
                tag: release.tagName,
                version: version,
                notes: release.body ?? "",
                ipaURL: ipa,
                pageURL: release.htmlUrl.flatMap { URL(string: $0) },
                publishedAt: parseDate(release.publishedAt)
            )
        }
    }

    public static func newest(_ releases: [IOSRelease]) -> IOSRelease? {
        releases.max { lhs, rhs in
            if isNewer(rhs.version, than: lhs.version) { return true }
            if isNewer(lhs.version, than: rhs.version) { return false }
            return (lhs.publishedAt ?? .distantPast) < (rhs.publishedAt ?? .distantPast)
        }
    }

    public static func check(data: Data, currentVersion: String) -> UpdateCheckResult? {
        guard let releases = parse(data: data) else { return nil }
        guard let latest = newest(releases) else { return .noRelease }
        if isNewer(latest.version, than: currentVersion) {
            return .update(latest)
        }
        return .upToDate
    }

    public static func check(currentVersion: String, session: URLSession = .shared) async -> UpdateCheckResult {
        do {
            let data = try await fetchAll(session: session)
            return check(data: data, currentVersion: currentVersion) ?? .failed
        } catch {
            return .failed
        }
    }

    public static func sideStoreInstallURL(for ipa: URL) -> URL? {
        var components = URLComponents()
        components.scheme = "sidestore"
        components.host = "install"
        components.queryItems = [URLQueryItem(name: "url", value: ipa.absoluteString)]
        return components.url
    }

    private static func fetchAll(session: URLSession) async throws -> Data {
        var next: URL? = releasesURL
        var combined: [GitHubRelease] = []
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        var pages = 0
        while let url = next, pages < 5 {
            pages += 1
            var request = URLRequest(url: url)
            request.timeoutInterval = 20
            request.setValue("TokiNote-iOS", forHTTPHeaderField: "User-Agent")
            request.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                throw URLError(.badServerResponse)
            }
            combined.append(contentsOf: try decoder.decode([GitHubRelease].self, from: data))
            next = nextPage(from: http.value(forHTTPHeaderField: "Link"))
        }
        return try JSONEncoder().encode(combined.map(\.wire))
    }

    private static func nextPage(from header: String?) -> URL? {
        guard let header else { return nil }
        for part in header.split(separator: ",") {
            let pieces = part.split(separator: ";").map { $0.trimmingCharacters(in: .whitespaces) }
            guard pieces.count >= 2, pieces[1].contains("rel=\"next\"") else { continue }
            let raw = pieces[0].trimmingCharacters(in: CharacterSet(charactersIn: "<>"))
            return URL(string: raw)
        }
        return nil
    }

    private static func parseDate(_ raw: String?) -> Date? {
        guard let raw else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        if let date = formatter.date(from: raw) { return date }
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.date(from: raw)
    }

    private static func compareCore(_ lhs: [Int], _ rhs: [Int]) -> Int {
        for index in 0..<max(lhs.count, rhs.count) {
            let left = index < lhs.count ? lhs[index] : 0
            let right = index < rhs.count ? rhs[index] : 0
            if left != right { return left < right ? -1 : 1 }
        }
        return 0
    }
}

private struct ParsedVersion {
    var core: [Int]
    var preRelease: String

    static func parse(_ raw: String) -> ParsedVersion {
        var trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.lowercased().hasPrefix("v") {
            trimmed.removeFirst()
        }
        let corePart = trimmed.split(separator: "-", maxSplits: 1).first.map(String.init) ?? trimmed
        let pre = trimmed.split(separator: "-", maxSplits: 1).dropFirst().first.map(String.init) ?? ""
        let numbers = corePart.split(separator: ".").map { Int($0) ?? 0 }
        return ParsedVersion(
            core: (0..<3).map { $0 < numbers.count ? numbers[$0] : 0 },
            preRelease: pre
        )
    }
}

private struct GitHubAsset: Decodable {
    var name: String
    var browserDownloadUrl: String
}

private struct GitHubRelease: Decodable {
    var tagName: String
    var draft: Bool?
    var body: String?
    var htmlUrl: String?
    var publishedAt: String?
    var assets: [GitHubAsset]?

    var wire: GitHubReleaseWire {
        GitHubReleaseWire(
            tagName: tagName,
            draft: draft ?? false,
            body: body,
            htmlUrl: htmlUrl,
            publishedAt: publishedAt,
            assets: (assets ?? []).map { GitHubAssetWire(name: $0.name, browserDownloadUrl: $0.browserDownloadUrl) }
        )
    }
}

private struct GitHubReleaseWire: Encodable {
    var tagName: String
    var draft: Bool
    var body: String?
    var htmlUrl: String?
    var publishedAt: String?
    var assets: [GitHubAssetWire]
}

private struct GitHubAssetWire: Encodable {
    var name: String
    var browserDownloadUrl: String
}
