import SwiftUI

/// 퀴즈 한 판의 진행 상태. 화면을 떠나면 버린다.
final class QuizSession: ObservableObject {
    let items: [QItem]
    let mode: QuizMode
    let day: Int
    @Published var pos = 0
    @Published var picked: Int? = nil
    @Published var score = 0
    @Published var missed: [QItem] = []

    init(_ spec: QuizSpec) {
        items = spec.keys.compactMap(Store.lib.item)
        mode = spec.mode
        day = spec.day
    }
    var done: Bool { pos >= items.count }
}

struct QuizHomeScreen: View {
    @EnvironmentObject var nav: Nav
    @EnvironmentObject var progress: Progress
    let lib = Store.lib

    var body: some View {
        let day = Today.day()
        let score = progress.dailyScore(day)
        let todayFact = lib.factOfDay(day)
        let wrongItems = progress.wrong.sorted().compactMap(lib.item)
        Page {
            VStack(alignment: .leading, spacing: 0) {
                Text("연속 \(Streak.current(progress.quizDays, day))일째").font(pf(14, .bold)).foregroundStyle(Palette.dim)
                BigTitle("퀴즈")
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button { nav.quiz(lib.dailyQuiz(day), .daily, day: day) } label: {
                VStack(alignment: .leading, spacing: 6) {
                    Text("오늘의 퀴즈").font(pf(22, .extra))
                    Text(todayFact.title).font(pf(14)).opacity(0.92).multilineTextAlignment(.leading)
                    Text(score == nil ? "\(todayFact.quiz.count)문제 풀고 연속 기록 잇기" : "\(todayFact.quiz.count)문제 중 \(score!)개 맞힘 · 다시 풀기")
                        .font(pf(14, .bold)).padding(.top, 6)
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading).padding(22)
                .background(brandGradient, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
            }
            .buttonStyle(.plain)

            SectionTitle("분야별 10문제")
            let cols = [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]
            LazyVGrid(columns: cols, spacing: 10) {
                ForEach(Field.allCases, id: \.self) { f in
                    Button { nav.quiz(lib.fieldQuiz(f), .field) } label: {
                        Text(f.label).font(pf(18, .extra)).foregroundStyle(.white)
                            .frame(maxWidth: .infinity, minHeight: 72, alignment: .bottomLeading).padding(14)
                            .background(f.gradient, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }

            GradientButton(text: "전체 섞어서 10문제") { nav.quiz(lib.randomQuiz(), .random) }.padding(.top, 4)
            GradientButton(
                text: wrongItems.isEmpty ? "오답노트가 비어 있어요" : "오답노트 \(wrongItems.count)문제",
                gradient: linear([Palette.bad, Palette.pink]), enabled: !wrongItems.isEmpty
            ) { nav.quiz(wrongItems, .wrong) }
        }
    }
}

struct QuizScreen: View {
    @StateObject private var s: QuizSession
    init(spec: QuizSpec) { _s = StateObject(wrappedValue: QuizSession(spec)) }

    private var title: String {
        switch s.mode {
        case .daily: return "오늘의 퀴즈"
        case .fact: return "상식 퀴즈"
        case .field: return "\(s.items.first?.fact.field.label ?? "") 퀴즈"
        case .random: return "섞어서 퀴즈"
        case .wrong: return "오답노트"
        }
    }

    var body: some View {
        Group {
            if s.done { QuizResult(s: s) } else { Asking(s: s) }
        }
        .screen(title)
    }
}

private struct Asking: View {
    @ObservedObject var s: QuizSession
    @EnvironmentObject var nav: Nav
    @EnvironmentObject var progress: Progress

    func pick(_ i: Int, _ item: QItem) {
        guard s.picked == nil else { return }
        s.picked = i
        let ok = item.q.isCorrect(i)
        if ok { s.score += 1 } else { s.missed.append(item) }
        progress.record(item, ok: ok)
    }

    var body: some View {
        let item = s.items[s.pos]
        let q = item.q
        let picked = s.picked
        ScrollViewReader { proxy in
            Page {
                HStack(spacing: 12) {
                    Text("\(s.pos + 1) / \(s.items.count)").font(pf(14, .bold)).foregroundStyle(Palette.dim)
                    Meter(ratio: Double(s.pos + (picked != nil ? 1 : 0)) / Double(s.items.count), gradient: brandGradient)
                }
                Card {
                    VStack(alignment: .leading, spacing: 10) {
                        if case .ox = q { FieldChip(field: item.fact.field, onColor: false, extra: "OX") }
                        else { FieldChip(field: item.fact.field, onColor: false, extra: "4지선다") }
                        Text(q.q).font(pf(20, .bold)).lineSpacing(6).foregroundStyle(Palette.ink)
                    }
                }

                switch q {
                case .ox:
                    HStack(spacing: 12) {
                        ForEach(0..<2, id: \.self) { i in
                            Option(text: i == 0 ? "O" : "X", index: i, answer: q.correctPick, picked: picked, big: true) { pick(i, item) }
                        }
                    }
                case .choice(_, let options, _, _):
                    VStack(spacing: 10) {
                        ForEach(Array(options.enumerated()), id: \.offset) { i, label in
                            Option(text: "\(i + 1).  \(label)", index: i, answer: q.correctPick, picked: picked, big: false) { pick(i, item) }
                        }
                    }
                }

                if let picked {
                    let ok = q.isCorrect(picked)
                    VStack(alignment: .leading, spacing: 6) {
                        Text(ok ? "정답" : "아쉽다, 오답").font(pf(17, .extra)).foregroundStyle(ok ? Palette.good : Palette.bad)
                        Text(q.explain).font(pf(15)).lineSpacing(4).foregroundStyle(Palette.ink)
                        Button { nav.fact(item.fact.id) } label: {
                            Text("상식 다시 보기").font(pf(14, .bold)).foregroundStyle(Palette.purple)
                        }
                        .buttonStyle(.plain).padding(.top, 4)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading).padding(16)
                    .background(ok ? Palette.goodBg : Palette.badBg, in: cardShape)
                    .id("explain")

                    let last = s.pos == s.items.count - 1
                    GradientButton(text: last ? "결과 보기" : "다음 문제") {
                        if last && s.mode == .daily { progress.finishDaily(s.day, score: s.score) }
                        s.picked = nil
                        s.pos += 1
                    }
                }
            }
            .onChange(of: s.picked) { p in
                if p != nil { withAnimation { proxy.scrollTo("explain", anchor: .center) } }
            }
        }
    }
}

private struct Option: View {
    let text: String
    let index: Int
    let answer: Int
    let picked: Int?
    let big: Bool
    let action: () -> Void

    var body: some View {
        let (bg, border, ink): (Color, Color, Color) = {
            if picked == nil { return (Palette.card, Palette.line, Palette.ink) }
            if index == answer { return (Palette.goodBg, Palette.good, Palette.good) }
            if index == picked { return (Palette.badBg, Palette.bad, Palette.bad) }
            return (Palette.card, Palette.line, Palette.dim)
        }()
        let strong = picked != nil && (index == answer || index == picked)
        let shape = RoundedRectangle(cornerRadius: 18, style: .continuous)
        Button(action: action) {
            Text(text).font(pf(big ? 44 : 16, big ? .extra : .bold)).foregroundStyle(ink).multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, minHeight: big ? 110 : 0, alignment: big ? .center : .leading)
                .padding(16)
                .background(bg, in: shape)
                .overlay(shape.stroke(border, lineWidth: strong ? 2 : 1))
        }
        .buttonStyle(.plain).disabled(picked != nil)
    }
}

private struct QuizResult: View {
    @ObservedObject var s: QuizSession
    @EnvironmentObject var nav: Nav

    var body: some View {
        let all = s.items.count
        Page {
            Text("\(s.score) / \(all)").font(pf(40, .extra)).foregroundStyle(.white)
                .frame(width: 170, height: 170).background(brandGradient, in: Circle()).padding(.top, 24)
            Text(s.score == all ? "전부 맞혔다!" : s.score == 0 ? "하나도 못 맞혔다. 상식부터 다시 보자" : "\(s.score)개 맞혔다")
                .font(pf(22, .extra)).foregroundStyle(Palette.ink).multilineTextAlignment(.center)
            if s.mode == .daily { Text("오늘의 퀴즈 완료. 연속 기록이 이어졌어요.").font(pf(14)).foregroundStyle(Palette.dim) }
            if !s.missed.isEmpty {
                SectionTitle("틀린 문제")
                var seen = Set<String>()
                ForEach(s.missed.filter { seen.insert($0.fact.id).inserted }, id: \.key) { m in
                    FactRow(f: m.fact) { nav.fact(m.fact.id) }
                }
                Text("틀린 문제는 오답노트에 모아 뒀어요.").font(pf(13)).foregroundStyle(Palette.dim)
            }
            GradientButton(text: "돌아가기") { nav.back() }.padding(.top, 4)
        }
    }
}
