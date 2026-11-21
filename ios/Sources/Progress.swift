import Foundation

/// 퀴즈 기록, 오답노트, 즐겨찾기. 전부 UserDefaults 한 곳에 둔다.
final class Progress: ObservableObject {
    private let d: UserDefaults

    /// 오늘의 퀴즈를 끝낸 날들(epochDay). 연속 기록 계산에 쓴다.
    @Published private(set) var quizDays: Set<Int>
    @Published private(set) var wrong: Set<String>
    @Published private(set) var favorites: Set<String>
    /// 분야별 (맞힌 수, 푼 수).
    @Published private(set) var stats: [Field: (ok: Int, all: Int)]

    init(_ d: UserDefaults = .standard) {
        self.d = d
        quizDays = Set((d.stringArray(forKey: "quizDays") ?? []).compactMap(Int.init))
        wrong = Set(d.stringArray(forKey: "wrong") ?? [])
        favorites = Set(d.stringArray(forKey: "fav") ?? [])
        stats = Dictionary(uniqueKeysWithValues: Field.allCases.map { ($0, (d.integer(forKey: "ok_\($0.rawValue)"), d.integer(forKey: "all_\($0.rawValue)"))) })
    }

    func dailyScore(_ day: Int) -> Int? { d.object(forKey: "daily_\(day)") as? Int }

    func record(_ item: QItem, ok: Bool) {
        let f = item.fact.field
        let s = stats[f]!
        stats[f] = (ok ? s.ok + 1 : s.ok, s.all + 1)
        if ok { wrong.remove(item.key) } else { wrong.insert(item.key) }
        d.set(stats[f]!.ok, forKey: "ok_\(f.rawValue)")
        d.set(stats[f]!.all, forKey: "all_\(f.rawValue)")
        d.set(Array(wrong), forKey: "wrong")
    }

    func finishDaily(_ day: Int, score: Int) {
        quizDays.insert(day)
        d.set(quizDays.map(String.init), forKey: "quizDays")
        d.set(max(score, dailyScore(day) ?? 0), forKey: "daily_\(day)")
    }

    func toggleFavorite(_ id: String) {
        if favorites.contains(id) { favorites.remove(id) } else { favorites.insert(id) }
        d.set(Array(favorites), forKey: "fav")
    }
}
