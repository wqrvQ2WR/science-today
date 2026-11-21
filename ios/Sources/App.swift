import SwiftUI

/// 번들 안의 JSON 을 한 번만 읽는다.
enum Store {
    static let lib: Library = {
        let u = Bundle.main.url(forResource: "facts", withExtension: "json")!
        return try! Library.parse(Data(contentsOf: u))
    }()
    static let ref: RefData = {
        let e = Bundle.main.url(forResource: "elements", withExtension: "json")!
        let t = Bundle.main.url(forResource: "tables", withExtension: "json")!
        return try! RefData.parse(elements: Data(contentsOf: e), tables: Data(contentsOf: t))
    }()
}

extension Store {
    static let laws: [Law] = {
        let u = Bundle.main.url(forResource: "laws", withExtension: "json")!
        return try! Law.parse(Data(contentsOf: u))
    }()
}

struct QuizSpec: Hashable {
    let keys: [String]
    let mode: QuizMode
    let day: Int
    let uid = UUID()
}

enum Route: Hashable {
    case fact(String)
    case term(String)
    case quiz(QuizSpec)
    case periodic
    case laws
    case element(Int)
    case table(String)
}

final class Nav: ObservableObject {
    @Published var path: [Route] = []

    func fact(_ id: String) { path.append(.fact(id)) }
    func term(_ id: String) { path.append(.term(id)) }
    func quiz(_ items: [QItem], _ mode: QuizMode, day: Int = Today.day()) {
        if !items.isEmpty { path.append(.quiz(QuizSpec(keys: items.map(\.key), mode: mode, day: day))) }
    }
    func periodic() { path.append(.periodic) }
    func laws() { path.append(.laws) }
    func element(_ z: Int) { path.append(.element(z)) }
    func table(_ id: String) { path.append(.table(id)) }
    /// 원소 화면에서 이전/다음으로 넘길 때는 뒤로 가기 기록을 쌓지 않는다.
    func replaceElement(_ z: Int) { if !path.isEmpty { path[path.count - 1] = .element(z) } }
    func back() { if !path.isEmpty { path.removeLast() } }
}

@main
struct ScienceTodayApp: App {
    @StateObject private var progress = Progress()
    @StateObject private var nav = Nav()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(progress)
                .environmentObject(nav)
                .tint(Palette.purple)
                .preferredColorScheme(.light)
        }
    }
}

private enum Tab: Int, CaseIterable {
    case today, quiz, dict, ref, record
    var label: String { ["오늘", "퀴즈", "사전", "자료", "기록"][rawValue] }
    var icon: String { ["house.fill", "checkmark.circle.fill", "magnifyingglass", "info.circle.fill", "list.bullet"][rawValue] }
}

struct RootView: View {
    @EnvironmentObject var nav: Nav
    @EnvironmentObject var progress: Progress
    @State private var tab = Tab.today
    let lib = Store.lib
    let ref = Store.ref

    var body: some View {
        NavigationStack(path: $nav.path) {
            TabView(selection: $tab) {
                ForEach(Tab.allCases, id: \.self) { t in
                    content(t)
                        .tabItem { Label(t.label, systemImage: t.icon).font(pf(11, .bold)) }
                        .tag(t)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .onAppear {
                Debug.applySize()
                if let t = Debug.value("--tab").flatMap(Int.init), let tt = Tab(rawValue: t) { tab = tt }
                if let r = Debug.route(lib) { nav.path = [r] }
                Debug.shotIfAsked()
            }
            .navigationDestination(for: Route.self) { r in
                switch r {
                case .fact(let id): if let f = lib.factById[id] { FactScreen(f: f) }
                case .term(let id): if let t = lib.termById[id] { TermScreen(t: t) }
                case .quiz(let s): QuizScreen(spec: s)
                case .periodic: PeriodicScreen()
                case .laws: LawsScreen()
                case .element(let z): if let e = ref.byZ[z] { ElementScreen(e: e) }
                case .table(let id): if let t = ref.tableById[id] { TableScreen(t: t) }
                }
            }
        }
    }

    @ViewBuilder private func content(_ t: Tab) -> some View {
        switch t {
        case .today: TodayScreen()
        case .quiz: QuizHomeScreen()
        case .dict: DictScreen()
        case .ref: RefHomeScreen()
        case .record: RecordScreen()
        }
    }
}

/// 눌러서 들어간 화면의 위쪽 막대 설정(제목, 배경).
extension View {
    func screen(_ title: String) -> some View {
        self.navigationTitle(title).navigationBarTitleDisplayMode(.inline).background(Palette.bg.ignoresSafeArea())
    }
}
