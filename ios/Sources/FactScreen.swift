import SwiftUI

struct FactScreen: View {
    let f: Fact
    @EnvironmentObject var nav: Nav
    @State private var shareImage: Image? = nil
    let lib = Store.lib

    private var detail: AttributedString {
        let words = f.terms.compactMap { id in lib.termById[id].map { (id, $0.word) } }
        let chars = Array(f.detail)
        func piece(_ a: Int, _ b: Int) -> AttributedString { AttributedString(String(chars[a..<b])) }
        var out = AttributedString()
        var at = 0
        for (s, e, id) in linkRanges(f.detail, words) {
            out += piece(at, s)
            var link = piece(s, e)
            link.link = URL(string: "sciencetoday://term/\(id)")
            link.foregroundColor = f.field.main
            link.underlineStyle = .single
            link.font = pf(16, .bold)
            out += link
            at = e
        }
        return out + piece(at, chars.count)
    }

    var body: some View {
        Page {
            HeroCard(f: f)

            if let s = f.safety {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(Palette.warnInk)
                    Text(s).font(pf(14, .bold)).lineSpacing(4).foregroundStyle(Palette.warnInk)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(14)
                .background(Palette.warnBg, in: cardShape)
            }

            SectionTitle("왜 그럴까?")
            Card {
                Text(detail).font(pf(16)).lineSpacing(8).foregroundStyle(Palette.ink)
                    .environment(\.openURL, OpenURLAction { url in
                        if url.scheme == "sciencetoday", url.host == "term" {
                            nav.term(url.lastPathComponent)
                            return .handled
                        }
                        return .systemAction
                    })
            }

            if !f.terms.isEmpty {
                SectionTitle("관련 용어")
                FlowLayout {
                    ForEach(f.terms.compactMap { lib.termById[$0] }) { t in
                        Button { nav.term(t.id) } label: {
                            Text(t.word).font(pf(14, .bold)).foregroundStyle(Palette.ink)
                                .padding(.horizontal, 14).padding(.vertical, 8)
                                .background(Palette.card, in: Capsule())
                                .overlay(Capsule().stroke(Palette.line, lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            GradientButton(text: "이 상식으로 퀴즈 풀기", gradient: f.field.gradient) { nav.quiz(lib.factQuiz(f), .fact) }

            SectionTitle("출처")
            ForEach(Array(f.sources.enumerated()), id: \.offset) { _, s in
                SourceCard(title: s.title, sub: s.year > 0 ? "\(s.publisher) · \(s.year)" : s.publisher, url: s.url)
            }
        }
        .screen(f.field.label)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if let img = shareImage {
                    ShareLink(item: img, message: Text("오늘의 과학: \(f.title)"), preview: SharePreview(f.title, image: img)) {
                        Image(systemName: "square.and.arrow.up").foregroundStyle(Palette.ink)
                    }
                }
            }
        }
        .onAppear { shareImage = ShareCard.render(f) }
    }
}

struct SourceCard: View {
    let title: String
    let sub: String
    let url: String
    @Environment(\.openURL) private var openURL

    var body: some View {
        Card(onTap: url.isEmpty ? nil : { if let u = URL(string: url) { openURL(u) } }) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(pf(14, .bold)).foregroundStyle(url.isEmpty ? Palette.ink : Palette.purple)
                if !sub.isEmpty { Text(sub).font(pf(12)).foregroundStyle(Palette.dim) }
            }
        }
    }
}

struct TermScreen: View {
    let t: Term
    @EnvironmentObject var nav: Nav
    @EnvironmentObject var progress: Progress
    let lib = Store.lib

    var body: some View {
        let fav = progress.favorites.contains(t.id)
        Page {
            VStack(alignment: .leading, spacing: 8) {
                FieldChip(field: t.field, onColor: false)
                Text(t.word).font(pf(30, .extra)).foregroundStyle(Palette.ink)
                Text(t.en).font(pf(15)).foregroundStyle(Palette.dim)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Text(t.one).font(pf(18, .bold)).lineSpacing(5).foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading).padding(18)
                .background(t.field.gradient, in: cardShape)

            Card { Text(t.detail).font(pf(16)).lineSpacing(8).foregroundStyle(Palette.ink) }

            let used = lib.factsUsing(t.id)
            if !used.isEmpty {
                SectionTitle("이 용어가 나오는 상식")
                ForEach(used) { f in FactRow(f: f) { nav.fact(f.id) } }
            }
        }
        .screen("사전")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button { progress.toggleFavorite(t.id) } label: {
                    Image(systemName: fav ? "heart.fill" : "heart").foregroundStyle(fav ? Palette.pink : Palette.ink)
                }
                .accessibilityLabel(fav ? "즐겨찾기 빼기" : "즐겨찾기")
            }
        }
    }
}
