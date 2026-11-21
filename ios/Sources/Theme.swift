import SwiftUI

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255, opacity: opacity)
    }
}

enum Palette {
    static let bg = Color(hex: 0xF6F5FB)
    static let card = Color.white
    static let ink = Color(hex: 0x1E1B2E)
    static let dim = Color(hex: 0x6E6A80)
    static let line = Color(hex: 0xE8E6F0)
    static let pink = Color(hex: 0xFF4D9D)
    static let purple = Color(hex: 0x9B5CFF)
    static let sky = Color(hex: 0x3DA9FF)
    static let good = Color(hex: 0x16A34A)
    static let goodBg = Color(hex: 0xE7F8EE)
    static let bad = Color(hex: 0xE11D48)
    static let badBg = Color(hex: 0xFDECEF)
    static let warnBg = Color(hex: 0xFFF3E6)
    static let warnInk = Color(hex: 0xB45309)
}

func linear(_ colors: [Color]) -> LinearGradient {
    LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing)
}

let brandGradient = linear([Palette.pink, Palette.purple, Palette.sky])

extension Field {
    /// 분야마다 다른 그라디언트.
    var colors: (Color, Color) {
        switch self {
        case .chem: return (Color(hex: 0xFF4D9D), Color(hex: 0xB44DFF))
        case .phys: return (Color(hex: 0x3DA9FF), Color(hex: 0x5B5CFF))
        case .bio: return (Color(hex: 0x22C55E), Color(hex: 0x0EA5A0))
        case .earth: return (Color(hex: 0xF59E0B), Color(hex: 0xEA580C))
        case .space: return (Color(hex: 0x7C3AED), Color(hex: 0x2563EB))
        case .body: return (Color(hex: 0xFF5A6E), Color(hex: 0xFF8A4C))
        }
    }
    var gradient: LinearGradient { linear([colors.0, colors.1]) }
    var main: Color { colors.0 }
}

enum PW { case regular, bold, extra }

/// Pretendard. 번들에 같이 들어 있고 Info.plist 의 UIAppFonts 로 등록한다.
func pf(_ size: CGFloat, _ w: PW = .regular) -> Font {
    .custom(w == .regular ? "Pretendard-Regular" : w == .bold ? "Pretendard-Bold" : "Pretendard-ExtraBold", size: size)
}
