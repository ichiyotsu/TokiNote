import SwiftUI

enum TokiPalette {
    static func primary(_ choice: PaletteChoice, dark: Bool) -> Color {
        switch choice {
        case .matcha: return dark ? Color(hex: 0xB1D18A) : Color(hex: 0x4C662B)
        case .blue: return dark ? Color(hex: 0xA6C8FF) : Color(hex: 0x2E5D8C)
        case .purple: return dark ? Color(hex: 0xD5BBFF) : Color(hex: 0x6A4C93)
        case .orange: return dark ? Color(hex: 0xFFB870) : Color(hex: 0x9A5B12)
        }
    }

    static func onPrimary(_ choice: PaletteChoice, dark: Bool) -> Color {
        switch choice {
        case .matcha: return dark ? Color(hex: 0x1F3701) : .white
        case .blue: return dark ? Color(hex: 0x002F52) : .white
        case .purple: return dark ? Color(hex: 0x2F1A4A) : .white
        case .orange: return dark ? Color(hex: 0x4A2A00) : .white
        }
    }

    static func container(_ choice: PaletteChoice, dark: Bool) -> Color {
        switch choice {
        case .matcha: return dark ? Color(hex: 0x354E16) : Color(hex: 0xCDEDA3)
        case .blue: return dark ? Color(hex: 0x1E3F63) : Color(hex: 0xCFE4FF)
        case .purple: return dark ? Color(hex: 0x4A3168) : Color(hex: 0xEADDFF)
        case .orange: return dark ? Color(hex: 0x663D0E) : Color(hex: 0xFFDCC2)
        }
    }
}

extension Color {
    init(hex: UInt32) {
        let red = Double((hex >> 16) & 0xFF) / 255
        let green = Double((hex >> 8) & 0xFF) / 255
        let blue = Double(hex & 0xFF) / 255
        self.init(red: red, green: green, blue: blue)
    }
}

struct TokiScreenBackground: View {
    var palette: PaletteChoice
    var amoled: Bool
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let dark = scheme == .dark
        ZStack {
            if amoled && dark {
                Color.black
            } else {
                Color(.systemBackground)
            }
            RadialGradient(
                colors: [TokiPalette.primary(palette, dark: dark).opacity(dark ? 0.28 : 0.22), .clear],
                center: .top,
                startRadius: 20,
                endRadius: 520
            )
        }
        .backgroundExtensionEffect()
        .ignoresSafeArea()
    }
}
