import SwiftUI

let cardShape = RoundedRectangle(cornerRadius: 20, style: .continuous)

struct Card<Content: View>: View {
    var onTap: (() -> Void)? = nil
    @ViewBuilder var content: () -> Content

    var body: some View {
        let inner = content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Palette.card, in: cardShape)
            .overlay(cardShape.stroke(Palette.line, lineWidth: 1))
            .contentShape(cardShape)
        if let onTap { Button(action: onTap) { inner }.buttonStyle(.plain) } else { inner }
    }
}

/// 분야 이름표. 그라디언트 위에서는 반투명 흰색, 흰 바탕에서는 분야 색.
struct FieldChip: View {
    let field: Field
    var onColor: Bool
    var extra: String? = nil

    var body: some View {
        Text(extra.map { "\(field.label) · \($0)" } ?? field.label)
            .font(pf(12, .bold))
            .foregroundStyle(onColor ? Color.white : field.main)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(onColor ? Color.white.opacity(0.22) : field.main.opacity(0.12), in: Capsule())
    }
}

struct SectionTitle: View {
    let text: String
    init(_ t: String) { text = t }
    var body: some View {
        Text(text).font(pf(18, .extra)).foregroundStyle(Palette.ink)
            .frame(maxWidth: .infinity, alignment: .leading).padding(.top, 8)
    }
}

struct BigTitle: View {
    let text: String
    init(_ t: String) { text = t }
    var body: some View {
        Text(text).font(pf(30, .extra)).foregroundStyle(brandGradient)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct FactRow: View {
    let f: Fact
    let onTap: () -> Void
    var body: some View {
        Card(onTap: onTap) {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 3).fill(f.field.gradient).frame(width: 6, height: 40)
                VStack(alignment: .leading, spacing: 2) {
                    Text(f.field.label).font(pf(12, .bold)).foregroundStyle(f.field.main)
                    Text(f.title).font(pf(15, .bold)).foregroundStyle(Palette.ink).lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
            }
        }
    }
}

/// 그라디언트 큰 버튼.
struct GradientButton: View {
    let text: String
    var gradient: LinearGradient = brandGradient
    var enabled = true
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(text).font(pf(16, .bold)).foregroundStyle(enabled ? Color.white : Palette.dim)
                .frame(maxWidth: .infinity).frame(height: 54)
                .background(enabled ? AnyShapeStyle(gradient) : AnyShapeStyle(Palette.line),
                            in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain).disabled(!enabled)
    }
}

/// 0~1 막대.
struct Meter: View {
    let ratio: Double
    let gradient: LinearGradient
    var body: some View {
        GeometryReader { g in
            ZStack(alignment: .leading) {
                Capsule().fill(Palette.line)
                if ratio > 0 { Capsule().fill(gradient).frame(width: g.size.width * min(max(ratio, 0), 1)) }
            }
        }
        .frame(height: 8)
    }
}

struct Pill: View {
    let text: String
    let on: Bool
    var gradient: LinearGradient = brandGradient
    var size: CGFloat = 14
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(text).font(pf(size, .bold)).foregroundStyle(on ? Color.white : Palette.ink)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(on ? AnyShapeStyle(gradient) : AnyShapeStyle(Palette.card), in: Capsule())
                .overlay(Capsule().stroke(on ? Color.clear : Palette.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

struct SearchField: View {
    @Binding var text: String
    let placeholder: String
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").foregroundStyle(Palette.dim)
            TextField("", text: $text, prompt: Text(placeholder).foregroundColor(Palette.dim))
                .font(pf(15)).foregroundStyle(Palette.ink)
                .autocorrectionDisabled().textInputAutocapitalization(.never)
            if !text.isEmpty {
                Button { text = "" } label: { Image(systemName: "xmark.circle.fill").foregroundStyle(Palette.dim) }
                    .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14).frame(height: 52)
        .background(Palette.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Palette.line, lineWidth: 1))
    }
}

/// 줄이 넘치면 아래로 내려가는 배치.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    var lineSpacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let w = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowH: CGFloat = 0, maxX: CGFloat = 0
        for s in subviews {
            let sz = s.sizeThatFits(.unspecified)
            if x > 0, x + sz.width > w { x = 0; y += rowH + lineSpacing; rowH = 0 }
            x += sz.width + spacing; rowH = max(rowH, sz.height); maxX = max(maxX, x - spacing)
        }
        return CGSize(width: proposal.width ?? maxX, height: y + rowH)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowH: CGFloat = 0
        for s in subviews {
            let sz = s.sizeThatFits(.unspecified)
            if x > bounds.minX, x + sz.width > bounds.maxX { x = bounds.minX; y += rowH + lineSpacing; rowH = 0 }
            s.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(sz))
            x += sz.width + spacing; rowH = max(rowH, sz.height)
        }
    }
}

/// 화면 공통 바탕과 안쪽 여백. 폰에서는 화면 폭, 아이패드에서는 가운데 640까지만 쓴다.
struct Page<Content: View>: View {
    @ViewBuilder var content: () -> Content
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) { content() }
                .padding(.horizontal, 16).padding(.top, 16).padding(.bottom, 24)
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
        }
        .background(Palette.bg.ignoresSafeArea())
    }
}
