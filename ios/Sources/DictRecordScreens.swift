import SwiftUI

struct DictScreen: View {
    @EnvironmentObject var nav: Nav
    @EnvironmentObject var progress: Progress
    @State private var query = ""
    @State private var onlyFav = false
    let lib = Store.lib

    var body: some View {
        let list = lib.searchTerms(query)
            .filter { !onlyFav || progress.favorites.contains($0.id) }
            .sorted { $0.word.compare($1.word, locale: Locale(identifier: "ko_KR")) == .orderedAscending }
        Page {
            BigTitle("과학 사전")
            SearchField(text: $query, placeholder: "용어 검색 (초성도 돼요: ㅋㄹㅍㅂ)")
            HStack {
                Text("\(list.count)개").font(pf(13)).foregroundStyle(Palette.dim)
                Spacer()
                Button { onlyFav.toggle() } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "heart.fill").font(.system(size: 14))
                        Text(onlyFav ? "전체 보기" : "즐겨찾기만").font(pf(13, .bold))
                    }
                    .foregroundStyle(onlyFav ? Palette.pink : Palette.dim).padding(.horizontal, 10).padding(.vertical, 6)
                }
                .buttonStyle(.plain)
            }
            if list.isEmpty {
                Text(onlyFav ? "즐겨찾기한 용어가 없어요. 용어 화면 오른쪽 위 하트를 눌러 추가해요." : "찾는 용어가 없어요.")
                    .font(pf(14)).foregroundStyle(Palette.dim).frame(maxWidth: .infinity, alignment: .leading)
            }
            ForEach(list) { t in
                Card(onTap: { nav.term(t.id) }) {
                    HStack(spacing: 12) {
                        Circle().fill(t.field.main).frame(width: 8, height: 8)
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 6) {
                                Text(t.word).font(pf(16, .bold)).foregroundStyle(Palette.ink)
                                Text(t.en).font(pf(12)).foregroundStyle(Palette.dim).lineLimit(1)
                            }
                            Text(t.one).font(pf(13)).foregroundStyle(Palette.dim).lineLimit(1)
                        }
                        Spacer(minLength: 0)
                        if progress.favorites.contains(t.id) { Image(systemName: "heart.fill").foregroundStyle(Palette.pink) }
                    }
                }
            }
        }
    }
}

struct RecordScreen: View {
    @EnvironmentObject var nav: Nav
    @EnvironmentObject var progress: Progress
    let lib = Store.lib

    var body: some View {
        let today = Today.day()
        let streak = Streak.current(progress.quizDays, today)
        let best = Streak.best(progress.quizDays)
        let solved = progress.stats.values.reduce(0) { $0 + $1.all }
        let ok = progress.stats.values.reduce(0) { $0 + $1.ok }
        let wrongItems = progress.wrong.sorted().compactMap(lib.item)
        Page {
            BigTitle("내 기록")
            HStack(spacing: 12) {
                StatBox(label: "연속", value: "\(streak)일")
                StatBox(label: "최고 연속", value: "\(best)일")
                StatBox(label: "정답률", value: solved == 0 ? "-" : "\(ok * 100 / solved)%")
            }
            Text(progress.dailyScore(today) == nil ? "오늘의 퀴즈를 풀면 연속 기록이 이어져요." : "오늘의 퀴즈 완료. 내일 또 만나요.")
                .font(pf(13)).foregroundStyle(Palette.dim).frame(maxWidth: .infinity, alignment: .leading)

            SectionTitle("분야별 정답률")
            Card {
                VStack(spacing: 14) {
                    ForEach(Field.allCases, id: \.self) { f in
                        let s = progress.stats[f]!
                        VStack(spacing: 6) {
                            HStack {
                                Text(f.label).font(pf(14, .bold)).foregroundStyle(Palette.ink)
                                Spacer()
                                Text(s.all == 0 ? "아직 안 풂" : "\(s.ok) / \(s.all)").font(pf(13)).foregroundStyle(Palette.dim)
                            }
                            Meter(ratio: s.all == 0 ? 0 : Double(s.ok) / Double(s.all), gradient: f.gradient)
                        }
                    }
                }
            }

            SectionTitle("오답노트")
            GradientButton(
                text: wrongItems.isEmpty ? "틀린 문제가 없어요" : "틀린 문제 \(wrongItems.count)개 다시 풀기",
                enabled: !wrongItems.isEmpty
            ) { nav.quiz(wrongItems, .wrong) }

            let favs = lib.terms.filter { progress.favorites.contains($0.id) }
            if !favs.isEmpty {
                SectionTitle("즐겨찾기 용어")
                ForEach(favs) { t in
                    Card(onTap: { nav.term(t.id) }) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(t.word).font(pf(16, .bold)).foregroundStyle(Palette.ink)
                            Text(t.one).font(pf(13)).foregroundStyle(Palette.dim)
                        }
                    }
                }
            }
        }
    }
}

private struct StatBox: View {
    let label: String
    let value: String
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 0) {
                Text(label).font(pf(12, .bold)).foregroundStyle(Palette.dim)
                Text(value).font(pf(22, .extra)).foregroundStyle(Palette.ink).minimumScaleFactor(0.7).lineLimit(1)
            }
        }
    }
}
