import SwiftUI

/// 상식을 인스타·카톡에 올리기 좋은 4:5 카드 이미지(1080x1350)로 그려서 공유한다.
enum ShareCard {
    @MainActor static func render(_ f: Fact) -> Image? {
        let r = ImageRenderer(content: ShareCardView(f: f).frame(width: 1080, height: 1350))
        r.scale = 1
        guard let ui = r.uiImage else { return nil }
        return Image(uiImage: ui)
    }
}

private struct ShareCardView: View {
    let f: Fact
    var body: some View {
        ZStack(alignment: .topLeading) {
            LinearGradient(colors: [f.field.colors.0, f.field.colors.1], startPoint: .topLeading, endPoint: .bottomTrailing)
            // 배경에 흐릿한 동그라미 두 개.
            Circle().fill(Color.white.opacity(0.12)).frame(width: 600, height: 600).position(x: 1080 * 0.92, y: 1350 * 0.12)
            Circle().fill(Color.white.opacity(0.12)).frame(width: 520, height: 520).position(x: 1080 * 0.05, y: 1350 * 0.95)

            VStack(alignment: .leading, spacing: 0) {
                Text("오늘의 과학 · \(f.field.label)").font(pf(38, .extra)).foregroundStyle(.white)
                    .padding(.horizontal, 32).frame(height: 84)
                    .background(Color.white.opacity(0.22), in: Capsule())
                Text(f.title).font(pf(92, .extra)).lineSpacing(12).foregroundStyle(.white)
                    .fixedSize(horizontal: false, vertical: true).padding(.top, 110)
                Text(f.short).font(pf(46)).lineSpacing(14).foregroundStyle(Color.white.opacity(0.92))
                    .fixedSize(horizontal: false, vertical: true).padding(.top, 56)
                Spacer(minLength: 0)
                Text("오늘의 과학").font(pf(36, .extra)).foregroundStyle(Color.white.opacity(0.8))
            }
            .padding(96)
            .frame(width: 1080, height: 1350, alignment: .topLeading)
        }
        .frame(width: 1080, height: 1350)
        .clipped()
    }
}
