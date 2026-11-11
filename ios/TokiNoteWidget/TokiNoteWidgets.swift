import ActivityKit
import SwiftUI
import TokiNoteCore
import WidgetKit

@main
struct TokiNoteWidgets: WidgetBundle {
    var body: some Widget {
        FocusHomeWidget()
        FocusLiveActivityWidget()
    }
}

struct FocusHomeWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: FocusWidgetKind.home, provider: FocusTimeline()) { entry in
            FocusWidgetView(snapshot: entry.snapshot)
                .containerBackground(for: .widget) {
                    Color(hex: 0xF3F4E9)
                }
        }
        .configurationDisplayName(String(localized: "widget_name", defaultValue: "专注"))
        .description(String(localized: "widget_description", defaultValue: "番茄钟、专注统计与计时器设置"))
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryRectangular, .accessoryCircular])
    }
}

struct FocusEntry: TimelineEntry {
    var date: Date
    var snapshot: FocusWidgetSnapshot
}

struct FocusTimeline: TimelineProvider {
    func placeholder(in context: Context) -> FocusEntry {
        FocusEntry(date: .now, snapshot: .placeholder)
    }

    func getSnapshot(in context: Context, completion: @escaping (FocusEntry) -> Void) {
        completion(FocusEntry(date: .now, snapshot: FocusWidgetStore.load() ?? .placeholder))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<FocusEntry>) -> Void) {
        let snapshot = FocusWidgetStore.load() ?? .placeholder
        let entry = FocusEntry(date: .now, snapshot: snapshot)
        let refresh: Date
        if snapshot.phase == FocusPhase.running.rawValue, let end = snapshot.endDate, !snapshot.indefinite {
            refresh = end.addingTimeInterval(1)
        } else if snapshot.phase == FocusPhase.running.rawValue {
            refresh = Date().addingTimeInterval(15 * 60)
        } else {
            refresh = Date().addingTimeInterval(30 * 60)
        }
        completion(Timeline(entries: [entry], policy: .after(refresh)))
    }
}

struct FocusWidgetView: View {
    var snapshot: FocusWidgetSnapshot
    @Environment(\.widgetFamily) private var family

    var body: some View {
        VStack(alignment: .leading, spacing: family == .accessoryCircular ? 2 : 6) {
            if family != .accessoryCircular {
                Text(snapshot.modeTitle)
                    .font(family == .accessoryRectangular ? .caption : .subheadline)
                    .foregroundStyle(.secondary)
            }
            clock
                .font(family == .systemMedium ? .system(size: 42, weight: .semibold, design: .rounded) : .title2.weight(.semibold))
                .monospacedDigit()
                .minimumScaleFactor(0.6)
            if family == .systemMedium || family == .accessoryRectangular {
                Text(snapshot.detail)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: family == .accessoryCircular ? .center : .leading)
    }

    @ViewBuilder
    private var clock: some View {
        if snapshot.phase == FocusPhase.running.rawValue, snapshot.indefinite, let start = snapshot.countUpStart {
            Text(start, style: .timer)
        } else if snapshot.phase == FocusPhase.running.rawValue, let end = snapshot.endDate {
            Text(end, style: .timer)
        } else {
            Text(snapshot.clockText)
        }
    }
}

struct FocusLiveActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: FocusActivityAttributes.self) { context in
            HStack(spacing: 16) {
                Image(systemName: "timer")
                    .font(.title2)
                    .foregroundStyle(Color(hex: 0x4C662B))
                VStack(alignment: .leading, spacing: 2) {
                    Text(context.state.modeTitle).font(.headline)
                    Text(context.state.detail).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                liveClock(context.state)
                    .font(.title.weight(.semibold).monospacedDigit())
            }
            .padding(.horizontal, 8)
            .activityBackgroundTint(Color(hex: 0xF3F4E9).opacity(0.9))
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label(context.state.modeTitle, systemImage: "timer")
                }
                DynamicIslandExpandedRegion(.trailing) {
                    liveClock(context.state).monospacedDigit()
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text(context.state.detail)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            } compactLeading: {
                Image(systemName: "timer")
            } compactTrailing: {
                liveClock(context.state).monospacedDigit().frame(maxWidth: 56)
            } minimal: {
                Image(systemName: "timer")
            }
        }
    }

    @ViewBuilder
    private func liveClock(_ state: FocusActivityAttributes.ContentState) -> some View {
        if state.paused || (state.endDate == nil && state.countUpStart == nil) {
            Text(state.clockText)
        } else if state.indefinite, let start = state.countUpStart {
            Text(start, style: .timer)
        } else if let end = state.endDate {
            Text(end, style: .timer)
        } else {
            Text(state.clockText)
        }
    }
}

private extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}
