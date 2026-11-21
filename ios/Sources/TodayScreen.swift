import SwiftUI

struct TodayScreen: View {
    @EnvironmentObject var nav: Nav
    @EnvironmentObject var progress: Progress
    @State private var filter: Field? = nil
    let lib = Store.lib

    var body: some View {
        let day = Today.day()
        let fact = lib.factOfDay(day)
        Page {
            VStack(alignment: .leading, spacing: 0) {
                Text(Today.label()).font(pf(14, .bold)).foregroundStyle(Palette.dim)
                BigTitle("오늘의 과학")
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            HeroCard(f: fact, more: true) { nav.fact(fact.id) }

            let score = progress.dailyScore(day)
            Card(onTap: { nav.quiz(lib.dailyQuiz(day), .daily, day: day) }) {
                HStack(spacing: 12) {
                    Image(systemName: "checkmark.circle.fill").font(.system(size: 22)).foregroundStyle(.white)
                        .frame(width: 44, height: 44).background(brandGradient, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    VStack(alignment: .leading, spacing: 2) {
                        Text("오늘의 퀴즈").font(pf(16, .bold)).foregroundStyle(Palette.ink)
                        Text(score == nil ? "오늘 상식으로 \(fact.quiz.count)문제" : "\(fact.quiz.count)문제 중 \(score!)개 맞힘 · 다시 풀기")
                            .font(pf(13)).foregroundStyle(Palette.dim)
                    }
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.right").foregroundStyle(Palette.dim)
                }
            }

            SectionTitle("모든 상식")
            FieldFilter(selected: $filter)
            ForEach(lib.facts.filter { filter == nil || $0.field == filter }) { f in
                FactRow(f: f) { nav.fact(f.id) }
            }
        }
        .background(Palette.bg.ignoresSafeArea())
    }
}

struct HeroCard: View {
    let f: Fact
    var more = false
    var onTap: (() -> Void)? = nil

    var body: some View {
        let card = VStack(alignment: .leading, spacing: 10) {
            FieldChip(field: f.field, onColor: true, extra: f.brag ? "자랑하기 좋은" : "알아두면 쓸모 있는")
            Text(f.title).font(pf(26, .extra)).lineSpacing(6).foregroundStyle(.white)
            Text(f.short).font(pf(15)).lineSpacing(5).foregroundStyle(Color.white.opacity(0.92))
            if more {
                HStack(spacing: 2) {
                    Text("자세히 보기").font(pf(14, .bold))
                    Image(systemName: "chevron.right").font(.system(size: 12, weight: .bold))
                }
                .foregroundStyle(.white).padding(.top, 2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(22)
        .background(f.field.gradient, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        if let onTap { Button(action: onTap) { card }.buttonStyle(.plain) } else { card }
    }
}

struct FieldFilter: View {
    @Binding var selected: Field?
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                Pill(text: "전체", on: selected == nil) { selected = nil }
                ForEach(Field.allCases, id: \.self) { f in
                    Pill(text: f.label, on: selected == f, gradient: f.gradient) { selected = f }
                }
            }
        }
    }
}
