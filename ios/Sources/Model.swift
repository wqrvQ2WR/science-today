import Foundation
import SwiftUI

enum Field: String, CaseIterable, Hashable {
    case chem, phys, bio, earth, space, body

    var label: String {
        switch self {
        case .chem: return "화학"
        case .phys: return "물리"
        case .bio: return "생물"
        case .earth: return "지구"
        case .space: return "우주"
        case .body: return "인체"
        }
    }
}

struct Source { let title: String; let publisher: String; let year: Int; let url: String }

enum Question {
    case ox(q: String, answer: Bool, explain: String)
    case choice(q: String, options: [String], answer: Int, explain: String)

    var q: String { switch self { case .ox(let q, _, _), .choice(let q, _, _, _): return q } }
    var explain: String { switch self { case .ox(_, _, let e), .choice(_, _, _, let e): return e } }
    /// 정답 보기 번호. OX는 0 = O, 1 = X.
    var correctPick: Int {
        switch self {
        case .ox(_, let a, _): return a ? 0 : 1
        case .choice(_, _, let a, _): return a
        }
    }
    func isCorrect(_ pick: Int) -> Bool { pick == correctPick }
}

struct Fact: Identifiable {
    let id: String
    let field: Field
    let brag: Bool
    let title: String
    let short: String
    let detail: String
    let terms: [String]
    let safety: String?
    let sources: [Source]
    let quiz: [Question]
}

struct Term: Identifiable {
    let id: String
    let word: String
    let en: String
    let one: String
    let detail: String
    let field: Field
}

/// 퀴즈 한 문제. key는 "상식id#문제번호"이고 오답노트에 이 값으로 저장한다.
struct QItem {
    let fact: Fact
    let index: Int
    var q: Question { fact.quiz[index] }
    var key: String { "\(fact.id)#\(index)" }
}

enum QuizMode: String { case daily, fact, field, random, wrong }

final class Library {
    let facts: [Fact]
    let terms: [Term]
    let factById: [String: Fact]
    let termById: [String: Term]
    let daily: [Fact]

    init(facts: [Fact], terms: [Term]) {
        self.facts = facts
        self.terms = terms
        factById = Dictionary(uniqueKeysWithValues: facts.map { ($0.id, $0) })
        termById = Dictionary(uniqueKeysWithValues: terms.map { ($0.id, $0) })
        daily = Daily.order(facts)
    }

    func factOfDay(_ day: Int) -> Fact { daily[Daily.index(day, daily.count)] }
    func factsUsing(_ termId: String) -> [Fact] { facts.filter { $0.terms.contains(termId) } }

    func searchTerms(_ query: String) -> [Term] {
        terms.filter { Hangul.matches($0.word, query) || Hangul.matches($0.en, query) || Hangul.matches($0.one, query) }
    }

    func factQuiz(_ f: Fact) -> [QItem] { f.quiz.indices.map { QItem(fact: f, index: $0) } }
    func dailyQuiz(_ day: Int) -> [QItem] { factQuiz(factOfDay(day)) }
    func fieldQuiz(_ field: Field, n: Int = 10) -> [QItem] {
        Array(facts.filter { $0.field == field }.flatMap(factQuiz).shuffled().prefix(n))
    }
    func randomQuiz(n: Int = 10) -> [QItem] { Array(facts.flatMap(factQuiz).shuffled().prefix(n)) }

    func item(_ key: String) -> QItem? {
        let p = key.split(separator: "#", omittingEmptySubsequences: false)
        guard p.count == 2, let f = factById[String(p[0])], let i = Int(p[1]), f.quiz.indices.contains(i) else { return nil }
        return QItem(fact: f, index: i)
    }

    static func parse(_ data: Data) throws -> Library {
        let root = try JSONSerialization.jsonObject(with: data) as! [String: Any]
        let facts: [Fact] = (root["facts"] as! [[String: Any]]).map { o in
            Fact(
                id: o["id"] as! String,
                field: Field(rawValue: o["field"] as! String)!,
                brag: (o["kind"] as! String) == "brag",
                title: o["title"] as! String,
                short: o["short"] as! String,
                detail: o["detail"] as! String,
                terms: o["terms"] as? [String] ?? [],
                safety: o["safety"] as? String,
                sources: (o["sources"] as? [[String: Any]] ?? []).map {
                    Source(title: $0["title"] as? String ?? "", publisher: $0["publisher"] as? String ?? "",
                           year: $0["year"] as? Int ?? 0, url: $0["url"] as? String ?? "")
                },
                quiz: (o["quiz"] as! [[String: Any]]).map { q in
                    if (q["type"] as! String) == "ox" {
                        return .ox(q: q["q"] as! String, answer: q["answer"] as! Bool, explain: q["explain"] as! String)
                    }
                    return .choice(q: q["q"] as! String, options: q["options"] as! [String], answer: q["answer"] as! Int, explain: q["explain"] as! String)
                }
            )
        }
        let terms: [Term] = (root["terms"] as! [[String: Any]]).map {
            Term(id: $0["id"] as! String, word: $0["word"] as! String, en: $0["en"] as! String,
                 one: $0["one"] as! String, detail: $0["detail"] as! String, field: Field(rawValue: $0["field"] as! String)!)
        }
        return Library(facts: facts, terms: terms)
    }
}

enum Daily {
    /// 첫 상식이 나오는 날(2026-11-05). 이 날부터 순서대로 돈다.
    static let startDay = 20762

    /// 분야별로 id 순서대로 줄 세운 뒤, 화학-물리-생물-지구-우주-인체 순으로 하나씩 번갈아 꺼낸다.
    static func order(_ facts: [Fact]) -> [Fact] {
        var queues = Field.allCases.map { f in facts.filter { $0.field == f }.sorted { $0.id < $1.id } }
        var out: [Fact] = []
        while queues.contains(where: { !$0.isEmpty }) {
            for i in queues.indices where !queues[i].isEmpty { out.append(queues[i].removeFirst()) }
        }
        return out
    }

    static func index(_ day: Int, _ size: Int) -> Int { ((day - startDay) % size + size) % size }
}

enum Today {
    /// 1970-01-01부터 센 오늘(기기의 현지 날짜 기준, 안드로이드의 LocalDate.toEpochDay 와 같다).
    static func day() -> Int {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        let d = utc.date(from: DateComponents(year: c.year, month: c.month, day: c.day))!
        return Int((d.timeIntervalSince1970 / 86400).rounded())
    }

    static func label() -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "M월 d일 EEEE"
        return f.string(from: Date())
    }
}

enum Hangul {
    private static let cho = Array("ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ")

    static func choseong(_ s: String) -> String {
        String(s.unicodeScalars.map { u -> Character in
            if (0xAC00...0xD7A3).contains(u.value) { return cho[Int(u.value - 0xAC00) / 588] }
            return Character(u)
        })
    }

    /// 검색어가 그대로 들어 있거나, 검색어가 전부 초성이면 초성끼리 비교한다. 띄어쓰기와 대소문자는 무시.
    static func matches(_ text: String, _ query: String) -> Bool {
        let q = query.replacingOccurrences(of: " ", with: "").lowercased()
        if q.isEmpty { return true }
        let t = text.replacingOccurrences(of: " ", with: "").lowercased()
        if t.contains(q) { return true }
        return q.allSatisfy { cho.contains($0) } && choseong(t).contains(q)
    }
}

enum Streak {
    /// 오늘(또는 아직 안 풀었으면 어제)부터 거꾸로 하루도 안 빠지고 이어진 날 수.
    static func current(_ days: Set<Int>, _ today: Int) -> Int {
        var d = days.contains(today) ? today : today - 1
        var n = 0
        while days.contains(d) { n += 1; d -= 1 }
        return n
    }

    static func best(_ days: Set<Int>) -> Int {
        var best = 0
        for d in days where !days.contains(d - 1) {
            var n = 0
            while days.contains(d + n) { n += 1 }
            best = max(best, n)
        }
        return best
    }
}

/// 본문에서 용어가 처음 나오는 자리(글자 단위). 겹치면 먼저 찾은 쪽을 쓴다. (시작, 끝, 용어id)
func linkRanges(_ text: String, _ words: [(String, String)]) -> [(Int, Int, String)] {
    let chars = Array(text)
    var out: [(Int, Int, String)] = []
    for (id, word) in words where !word.isEmpty {
        let w = Array(word)
        var from = 0
        while from + w.count <= chars.count {
            if Array(chars[from..<from + w.count]) == w {
                let end = from + w.count
                if !out.contains(where: { from < $0.1 && end > $0.0 }) { out.append((from, end, id)); break }
            }
            from += 1
        }
    }
    return out.sorted { $0.0 < $1.0 }
}
