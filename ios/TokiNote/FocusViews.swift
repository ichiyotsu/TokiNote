import SwiftUI
import TokiNoteCore

struct FocusScreen: View {
    @Environment(AppModel.self) private var model
    @Environment(\.accessibilityReduceMotion) private var systemReduce
    @Environment(\.colorScheme) private var scheme
    @State private var hidingDigits = false

    var body: some View {
        let state = model.focus.state
        let reduce = model.settings.reduceMotion || systemReduce
        let dark = scheme == .dark
        let accent = TokiPalette.primary(model.settings.palette, dark: dark)
        let onAccent = TokiPalette.onPrimary(model.settings.palette, dark: dark)
        TimelineView(.animation(minimumInterval: state.running && !reduce ? 0.1 : 1, paused: !state.running)) { context in
            let now = model.focus.elapsedNow
            let _ = context.date
            GeometryReader { proxy in
                let wide = proxy.size.width > proxy.size.height && proxy.size.width > 700
                Group {
                    if wide {
                        HStack(spacing: 28) {
                            ring(state: state, now: now, accent: accent, maxSide: min(480, proxy.size.height * 0.82))
                            actions(state: state, accent: accent, onAccent: onAccent)
                                .frame(maxWidth: 420)
                        }
                        .padding(.horizontal, 32)
                    } else {
                        VStack(spacing: 28) {
                            ring(state: state, now: now, accent: accent, maxSide: min(390, proxy.size.width - 36))
                            actions(state: state, accent: accent, onAccent: onAccent)
                        }
                        .padding(.horizontal, 20)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle(L10n.focusTitle)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                NavigationLink {
                    TimerSettingsView()
                } label: {
                    Image(systemName: "slider.horizontal.3")
                }
                .accessibilityLabel(L10n.focusSettings)
            }
            ToolbarItem(placement: .topBarLeading) {
                NavigationLink {
                    FocusSummaryScreen()
                } label: {
                    Image(systemName: "chart.bar")
                }
                .accessibilityLabel(L10n.focusSummary)
            }
        }
    }

    private func ring(state: FocusState, now: Int64, accent: Color, maxSide: CGFloat) -> some View {
        let seconds = displaySeconds(state, now: now)
        let remain = liveRemainingMillis(state, now: now)
        let progress: Double = {
            if state.indefinite || state.totalMillis <= 0 { return 0 }
            return min(1, max(0, 1 - Double(remain) / Double(state.totalMillis)))
        }()
        return ZStack {
            TimerRing(progress: progress, accent: accent)
            VStack(spacing: 8) {
                Text(state.indefinite ? L10n.focusIndefinite : L10n.modeTitle(state.mode))
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(accent)
                Text(formatElapsed(seconds))
                    .font(.system(size: maxSide > 360 ? 76 : 64, weight: .semibold, design: .rounded))
                    .monospacedDigit()
                    .contentTransition(.numericText())
                    .scaleEffect(hidingDigits ? 1.35 : 1)
                    .opacity(hidingDigits ? 0 : 1)
                Text(state.indefinite ? L10n.focusIndefiniteHint : L10n.focusSession(state.sessionIndex, max(state.settings.sessionsBeforeLongBreak, 1)))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: maxSide, height: maxSide)
        .contentShape(Circle())
        .onTapGesture(count: 2) { requestIndefinite(state) }
        .onLongPressGesture(minimumDuration: 0.45) { requestIndefinite(state) }
        .accessibilityLabel(formatElapsed(seconds))
        .accessibilityAddTraits(.updatesFrequently)
    }

    private func requestIndefinite(_ state: FocusState) {
        guard !state.running, !state.indefinite, state.mode == .focus, !hidingDigits else { return }
        let reduce = model.settings.reduceMotion
        if reduce {
            model.focus.scheduleIndefinite(delayMs: 0)
            return
        }
        withAnimation(.easeIn(duration: 0.42)) { hidingDigits = true }
        model.focus.scheduleIndefinite(delayMs: FocusTimerEngine.indefiniteAnimationMillis)
        Task {
            try? await Task.sleep(nanoseconds: 430_000_000)
            hidingDigits = false
        }
    }

    private func actions(state: FocusState, accent: Color, onAccent: Color) -> some View {
        VStack(spacing: 18) {
            GlassEffectContainer(spacing: 16) {
                HStack(spacing: 12) {
                    Button {
                        model.focus.toggle()
                    } label: {
                        Label(state.running ? L10n.focusPause : L10n.focusPlay, systemImage: state.running ? "pause.fill" : "play.fill")
                            .font(.title3.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .frame(height: 64)
                    }
                    .buttonStyle(.glassProminent)
                    .tint(accent)
                    .keyboardShortcut(.space, modifiers: [])
                    .hoverEffect(.lift)

                    Button {
                        model.focus.reset()
                    } label: {
                        Image(systemName: "arrow.counterclockwise")
                            .font(.title3)
                            .frame(width: 64, height: 64)
                    }
                    .buttonStyle(.glass)
                    .accessibilityLabel(L10n.focusReset)
                    .keyboardShortcut("r", modifiers: [.command, .shift])

                    Button {
                        model.focus.skip()
                    } label: {
                        Image(systemName: "forward.end.fill")
                            .font(.title3)
                            .frame(width: 64, height: 64)
                    }
                    .buttonStyle(.glass)
                    .accessibilityLabel(L10n.focusSkip)
                    .keyboardShortcut(".", modifiers: [.command, .shift])
                }
            }
            VStack(spacing: 4) {
                Text(L10n.focusUpNext)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
                Text(L10n.focusNextHint(
                    formatElapsed(state.nextMode.minutes(state.settings) * 60),
                    L10n.modeTitle(state.nextMode)
                ))
                .font(.body)
                .foregroundStyle(.secondary)
            }
            sessionDots(state: state, accent: accent)
        }
    }

    private func sessionDots(state: FocusState, accent: Color) -> some View {
        let total = max(state.settings.sessionsBeforeLongBreak, 1)
        let filled = filledSessionCount(completedInCycle: state.completedInCycle, cycleLength: total, mode: state.mode)
        return HStack(spacing: 7) {
            ForEach(0..<total, id: \.self) { index in
                Circle()
                    .fill(index < filled ? accent : Color.secondary.opacity(0.35))
                    .frame(width: index == filled ? 8 : 6, height: index == filled ? 8 : 6)
            }
        }
        .accessibilityLabel(L10n.focusSession(filled, total))
    }
}

struct TimerRing: View {
    var progress: Double
    var accent: Color

    var body: some View {
        Canvas { context, size in
            let line: CGFloat = 18
            let rect = CGRect(
                x: line / 2,
                y: line / 2,
                width: size.width - line,
                height: size.height - line
            )
            var track = Path()
            track.addEllipse(in: rect)
            context.stroke(track, with: .color(Color.primary.opacity(0.08)), style: StrokeStyle(lineWidth: line, lineCap: .round))
            guard progress > 0.002 else { return }
            var arc = Path()
            arc.addArc(
                center: CGPoint(x: size.width / 2, y: size.height / 2),
                radius: (min(size.width, size.height) - line) / 2,
                startAngle: .degrees(-90),
                endAngle: .degrees(-90 + 360 * progress),
                clockwise: false
            )
            context.stroke(arc, with: .color(accent), style: StrokeStyle(lineWidth: line, lineCap: .round))
        }
    }
}

struct TimerSettingsView: View {
    @Environment(AppModel.self) private var model
    @State private var mode: FocusMode = .focus
    @State private var showCustom = false
    @State private var customMinutes = 25

    private var minutes: Int { mode.minutes(model.settings.appSettings) }
    private var presets: [Int] {
        switch mode {
        case .focus: return [15, 25, 45]
        case .shortBreak: return [3, 5, 10]
        case .longBreak: return [10, 15, 30]
        }
    }

    var body: some View {
        @Bindable var settings = model.settings
        Form {
            Section {
                VStack(spacing: 6) {
                    Text(formatClock(minutes * 60))
                        .font(.system(size: 64, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(TokiPalette.primary(settings.palette, dark: false))
                    Text(L10n.modeTitle(mode))
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
                Picker(L10n.settingsTimer, selection: $mode) {
                    Text(L10n.statsFocus).tag(FocusMode.focus)
                    Text(L10n.focusShortBreak).tag(FocusMode.shortBreak)
                    Text(L10n.focusLongBreak).tag(FocusMode.longBreak)
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
            }
            Section {
                ForEach(presets, id: \.self) { value in
                    Button {
                        apply(value)
                    } label: {
                        HStack {
                            Text("\(value)")
                            Spacer()
                            if value == minutes {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                    .foregroundStyle(.primary)
                }
                Button(L10n.timerCustom) {
                    customMinutes = minutes
                    showCustom = true
                }
            }
            Section {
                Stepper(value: $settings.sessionsBeforeLongBreak, in: 1...12) {
                    VStack(alignment: .leading) {
                        Text(L10n.timerSessions)
                        Text(L10n.sessionsValue(settings.sessionsBeforeLongBreak))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .onChange(of: settings.sessionsBeforeLongBreak) { _, _ in model.saveSettings() }
                Toggle(isOn: $settings.autoStartNext) {
                    VStack(alignment: .leading) {
                        Text(L10n.timerAutoStart)
                        Text(L10n.timerAutoStartDesc)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .onChange(of: settings.autoStartNext) { _, _ in model.saveSettings() }
            }
        }
        .navigationTitle(L10n.settingsTimer)
        .sheet(isPresented: $showCustom) {
            NavigationStack {
                VStack {
                    Picker(L10n.timerPickDuration, selection: $customMinutes) {
                        ForEach(1...180, id: \.self) { minute in
                            Text(formatMinutes(minute)).tag(minute)
                        }
                    }
                    .pickerStyle(.wheel)
                }
                .navigationTitle(L10n.timerPickDuration)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(L10n.actionCancel) { showCustom = false }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button(L10n.timerConfirm) {
                            apply(customMinutes)
                            showCustom = false
                        }
                    }
                }
            }
            .presentationDetents([.medium])
        }
    }

    private func apply(_ value: Int) {
        switch mode {
        case .focus: model.settings.focusMinutes = value
        case .shortBreak: model.settings.shortBreakMinutes = value
        case .longBreak: model.settings.longBreakMinutes = value
        }
        model.saveSettings()
    }
}

struct FocusSummaryScreen: View {
    @Environment(AppModel.self) private var model
    @Environment(\.colorScheme) private var scheme
    @Query(sort: \FocusSessionRecord.endedAt) private var records: [FocusSessionRecord]

    var body: some View {
        let summary = FocusStats.summarize(sessions: records.map(\.session), today: .now)
        let accent = TokiPalette.primary(model.settings.palette, dark: scheme == .dark)
        let container = TokiPalette.container(model.settings.palette, dark: scheme == .dark)
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                Text(L10n.statsToday).font(.title3.weight(.semibold))
                HStack(spacing: 12) {
                    statCard(L10n.statsFocus, formatDurationLabel(summary.todayFocus), container)
                    statCard(L10n.statsBreak, formatDurationLabel(summary.todayBreak), Color.teal.opacity(scheme == .dark ? 0.35 : 0.2))
                }
                chartBlock(title: L10n.statsLastWeek, average: summary.weekAverage, values: summary.weekFocus, labels: summary.weekDays.map(weekday), accent: accent)
                chartBlock(title: L10n.statsLastMonth, average: summary.monthAverage, values: summary.monthFocus, labels: nil, accent: accent)
            }
            .padding(20)
        }
        .navigationTitle(L10n.focusSummary)
    }

    private func statCard(_ title: String, _ value: String, _ fill: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.subheadline).foregroundStyle(.secondary)
            Text(value).font(.title2.weight(.semibold))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(fill, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private func chartBlock(title: String, average: Int, values: [Int], labels: [String]?, accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.title3.weight(.semibold))
            Text(formatDurationLabel(average))
                .font(.system(size: 40, weight: .semibold, design: .rounded))
            Text(L10n.statsAvgFocus).foregroundStyle(.secondary)
            barChart(values: values, labels: labels, accent: accent)
        }
    }

    private func barChart(values: [Int], labels: [String]?, accent: Color) -> some View {
        let maxValue = max(values.max() ?? 0, 30)
        return HStack(alignment: .bottom, spacing: labels == nil ? 3 : 8) {
            ForEach(values.indices, id: \.self) { index in
                VStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                        .fill(values[index] > 0 ? accent : accent.opacity(0.2))
                        .frame(height: max(4, 120 * CGFloat(values[index]) / CGFloat(maxValue)))
                    if let labels {
                        Text(labels[index]).font(.caption2).foregroundStyle(.secondary)
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
        .frame(height: labels == nil ? 80 : 150, alignment: .bottom)
        .accessibilityLabel(L10n.statsAvgFocus)
    }

    private func weekday(_ date: Date) -> String {
        date.formatted(.dateTime.weekday(.narrow).locale(Locale(identifier: "zh_CN")))
    }
}
