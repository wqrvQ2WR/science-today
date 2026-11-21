import SwiftUI
import UIKit

/// 점검용 실행 인자. 평소엔 아무 일도 안 한다.
///   --tab 0..4   --open fact|term|quiz|laws|periodic|element:26|table:planets   --pick N(퀴즈 보기 고르기)
///   --size 390x844 (맥에서 창 크기)   --shot 파일.png   --after 초
enum Debug {
    static var args: [String] { CommandLine.arguments }
    static func value(_ k: String) -> String? { args.firstIndex(of: k).flatMap { $0 + 1 < args.count ? args[$0 + 1] : nil } }

    static func applySize() {
        guard let s = value("--size")?.split(separator: "x").compactMap({ Double($0) }), s.count == 2 else { return }
        #if targetEnvironment(macCatalyst)
        for scene in UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }) {
            scene.sizeRestrictions?.minimumSize = CGSize(width: s[0], height: s[1])
            scene.sizeRestrictions?.maximumSize = CGSize(width: s[0], height: s[1])
        }
        #endif
    }

    static func route(_ lib: Library) -> Route? {
        guard let o = value("--open") else { return nil }
        let p = o.split(separator: ":", maxSplits: 1).map(String.init)
        switch p[0] {
        case "fact": return .fact(p.count > 1 ? p[1] : lib.factOfDay(Today.day()).id)
        case "term": return .term(p.count > 1 ? p[1] : lib.terms[0].id)
        case "quiz": return .quiz(QuizSpec(keys: lib.dailyQuiz(Today.day()).map(\.key), mode: .daily, day: Today.day()))
        case "laws": return .laws
        case "periodic": return .periodic
        case "element": return .element(Int(p.last ?? "26") ?? 26)
        case "table": return .table(p.count > 1 ? p[1] : "planets")
        default: return nil
        }
    }

    static func shotIfAsked() {
        guard let path = value("--shot") else { return }
        let after = Double(value("--after") ?? "2") ?? 2
        DispatchQueue.main.asyncAfter(deadline: .now() + after) {
            guard let w = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).flatMap(\.windows).first else { exit(2) }
            let r = UIGraphicsImageRenderer(bounds: w.bounds)
            let img = r.image { _ in w.drawHierarchy(in: w.bounds, afterScreenUpdates: true) }
            try? img.pngData()?.write(to: URL(fileURLWithPath: path))
            exit(0)
        }
    }
}
