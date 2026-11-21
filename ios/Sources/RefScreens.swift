import SwiftUI

private let kindColors: [(String, Color)] = [
    ("알칼리 금속", Color(hex: 0xFF5A6E)), ("알칼리 토금속", Color(hex: 0xFF8A4C)), ("전이 금속", Color(hex: 0xF5A300)),
    ("전이후 금속", Color(hex: 0x22C55E)), ("준금속", Color(hex: 0x14B8A6)), ("비금속", Color(hex: 0x3DA9FF)),
    ("할로젠", Color(hex: 0x5B5CFF)), ("비활성 기체", Color(hex: 0x9B5CFF)), ("란타넘족", Color(hex: 0xFF4D9D)),
    ("악티늄족", Color(hex: 0xC850C0)),
]
func kindColor(_ k: String) -> Color { kindColors.first { $0.0 == k }?.1 ?? Color(hex: 0x9CA3AF) }

/// 주기율표를 무엇으로 칠할지.
private enum PaintBy: CaseIterable {
    case kind, density, mp, chi, radius, ie
    var label: String { ["분류", "밀도", "녹는점", "전기음성도", "원자 반지름", "이온화 에너지"][PaintBy.allCases.firstIndex(of: self)!] }
    func pick(_ e: Element) -> String {
        switch self { case .kind: return ""; case .density: return e.density; case .mp: return e.mp
        case .chi: return e.chi; case .radius: return e.radius; case .ie: return e.ie }
    }
}

struct RefHomeScreen: View {
    @EnvironmentObject var nav: Nav
    let ref = Store.ref

    var body: some View {
        Page {
            BigTitle("과학 자료")
            bigCard("주기율표", "원소 \(ref.elements.count)개 · 원자량, 전자 배치, 녹는점, 밀도, 이름 유래까지") { nav.periodic() }
            bigCard("법칙·원리", "옴, 뉴턴, 멘델, 케플러… 법칙·원리 \(Store.laws.count)개를 식과 함께") { nav.laws() }
            let total = ref.tables.reduce(0) { $0 + $1.entryCount }
            SectionTitle("데이터 표 \(ref.tables.count)개 · \(total.formatted())줄")
            ForEach(ref.tables) { t in
                Card(onTap: { nav.table(t.id) }) {
                    HStack(spacing: 8) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(t.title).font(pf(16, .bold)).foregroundStyle(Palette.ink)
                            Text(t.desc).font(pf(13)).lineSpacing(3).foregroundStyle(Palette.dim).lineLimit(2).multilineTextAlignment(.leading)
                        }
                        Spacer(minLength: 0)
                        Text("\(t.entryCount)").font(pf(15, .extra)).foregroundStyle(Palette.purple)
                    }
                }
            }
        }
    }

    private func bigCard(_ title: String, _ sub: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                Text(title).font(pf(24, .extra))
                Text(sub).font(pf(14)).opacity(0.92).multilineTextAlignment(.leading)
            }
            .foregroundStyle(.white).frame(maxWidth: .infinity, alignment: .leading).padding(22)
            .background(brandGradient, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private let cell: CGFloat = 46

struct PeriodicScreen: View {
    @EnvironmentObject var nav: Nav
    @State private var paint = PaintBy.kind
    @State private var query = ""
    let ref = Store.ref

    private var range: (Double, Double) {
        let vs = ref.elements.compactMap { Num.first(paint.pick($0)) }
        return (vs.min() ?? 0, vs.max() ?? 1)
    }

    private func colorOf(_ e: Element, _ r: (Double, Double)) -> Color {
        if paint == .kind { return kindColor(e.kind) }
        guard let v = Num.first(paint.pick(e)) else { return Color(hex: 0xD1D5DB) }
        let t = (v - r.0) / max(r.1 - r.0, 1e-9)
        return t < 0.5 ? mixColor(Palette.sky, Palette.purple, t * 2) : mixColor(Palette.purple, Palette.pink, (t - 0.5) * 2)
    }

    var body: some View {
        let r = range
        Page {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) { ForEach(PaintBy.allCases, id: \.self) { p in Pill(text: p.label, on: p == paint) { paint = p } } }
            }

            // 18칸이라 폰 화면보다 넓다. 옆으로 밀어서 본다.
            ScrollView(.horizontal, showsIndicators: false) {
                VStack(spacing: 3) {
                    ForEach(0...9, id: \.self) { row in
                        if row == 7 { Color.clear.frame(height: 8) }
                        else {
                            HStack(spacing: 3) {
                                ForEach(1...18, id: \.self) { col in
                                    if let e = ref.elements.first(where: { $0.row == row && $0.col == col }) {
                                        ElementCell(e: e, color: colorOf(e, r)) { nav.element(e.z) }
                                    } else if row == 5 && col == 3 { GapCell(text: "57–71") }
                                    else if row == 6 && col == 3 { GapCell(text: "89–103") }
                                    else { Color.clear.frame(width: cell, height: cell) }
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 1)
            }

            if paint == .kind {
                FlowLayout(spacing: 12, lineSpacing: 6) {
                    ForEach(kindColors, id: \.0) { k, c in
                        HStack(spacing: 5) {
                            RoundedRectangle(cornerRadius: 3).fill(c).frame(width: 12, height: 12)
                            Text(k).font(pf(12)).foregroundStyle(Palette.ink)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                VStack(alignment: .leading, spacing: 2) {
                    RoundedRectangle(cornerRadius: 5)
                        .fill(LinearGradient(colors: [Palette.sky, Palette.purple, Palette.pink], startPoint: .leading, endPoint: .trailing))
                        .frame(height: 10)
                    HStack { Text("낮음"); Spacer(); Text("높음") }.font(pf(12)).foregroundStyle(Palette.dim)
                    Text("회색 칸은 값이 알려지지 않은 원소").font(pf(12)).foregroundStyle(Palette.dim)
                }
            }

            SearchField(text: $query, placeholder: "원소 찾기 (이름, 기호, 번호, 초성)")
            if !query.trimmingCharacters(in: .whitespaces).isEmpty {
                ForEach(ref.searchElements(query).prefix(30)) { e in
                    Card(onTap: { nav.element(e.z) }) {
                        HStack(spacing: 12) {
                            ElementCell(e: e, color: kindColor(e.kind)) { nav.element(e.z) }
                            VStack(alignment: .leading, spacing: 2) {
                                Text(e.name).font(pf(16, .bold)).foregroundStyle(Palette.ink)
                                Text("\(e.en) · \(e.kind)").font(pf(13)).foregroundStyle(Palette.dim)
                            }
                        }
                    }
                }
            }
            SourceList(sources: ref.elementSources)
        }
        .screen("주기율표")
    }
}

func mixColor(_ a: Color, _ b: Color, _ t: Double) -> Color {
    let x = UIColor(a), y = UIColor(b)
    var (r1, g1, b1, a1, r2, g2, b2, a2) = (CGFloat(0), CGFloat(0), CGFloat(0), CGFloat(0), CGFloat(0), CGFloat(0), CGFloat(0), CGFloat(0))
    x.getRed(&r1, green: &g1, blue: &b1, alpha: &a1); y.getRed(&r2, green: &g2, blue: &b2, alpha: &a2)
    let k = CGFloat(t)
    return Color(red: Double(r1 + (r2 - r1) * k), green: Double(g1 + (g2 - g1) * k), blue: Double(b1 + (b2 - b1) * k))
}

private struct ElementCell: View {
    let e: Element
    let color: Color
    let action: () -> Void
    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 8, style: .continuous)
        Button(action: action) {
            VStack(spacing: 0) {
                Text(String(e.z)).font(pf(8)).foregroundStyle(Palette.dim).frame(maxWidth: .infinity, alignment: .leading)
                Text(e.sym).font(pf(15, .extra)).foregroundStyle(Palette.ink)
                Text(e.ko).font(pf(7)).foregroundStyle(Palette.ink).lineLimit(1)
            }
            .padding(.horizontal, 3).padding(.vertical, 2)
            .frame(width: cell, height: cell)
            .background(color.opacity(0.18), in: shape)
            .overlay(shape.stroke(color.opacity(0.55), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

private struct GapCell: View {
    let text: String
    var body: some View {
        Text(text).font(pf(8)).foregroundStyle(Palette.dim).multilineTextAlignment(.center)
            .frame(width: cell, height: cell)
            .overlay(RoundedRectangle(cornerRadius: 8, style: .continuous).stroke(Palette.line, lineWidth: 1))
    }
}

struct ElementScreen: View {
    let e: Element
    @EnvironmentObject var nav: Nav
    let ref = Store.ref

    var body: some View {
        let c = kindColor(e.kind)
        Page {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(e.z)번 · \(e.kind)").font(pf(13, .bold)).opacity(0.9)
                    Text(e.name).font(pf(28, .extra))
                    Text(e.en).font(pf(15)).opacity(0.9)
                }
                Spacer(minLength: 8)
                Text(e.sym).font(pf(56, .extra))
            }
            .foregroundStyle(.white).padding(22)
            .background(linear([c, mixColor(c, Palette.purple, 0.45)]),
                        in: RoundedRectangle(cornerRadius: 28, style: .continuous))

            Card {
                VStack(spacing: 10) {
                    Prop("원자량", e.mass, "u")
                    Prop("족 · 주기", "\(e.group.map { "\($0)족" } ?? "f 블록") · \(e.period)주기", "")
                    Prop("상온 상태", e.state, "")
                    Prop("전자 배치", Num.config(e.config), "")
                    Prop("산화수", e.ox, "")
                    Prop("전기음성도", e.chi, "(폴링)")
                    Prop("원자 반지름", e.radius, "pm")
                    Prop("이온화 에너지", e.ie, "eV")
                    Prop("전자 친화도", e.ea, "eV")
                    Prop("녹는점", e.mp.isEmpty ? "" : Num.kelvin(e.mp), "")
                    Prop("끓는점", e.bp.isEmpty ? "" : Num.kelvin(e.bp), "")
                    Prop("밀도", e.density, "g/cm³")
                    if let d = Num.first(e.density), e.state.hasPrefix("고체") || e.state.hasPrefix("액체") {
                        Prop("물에서", d < 1.0 ? "뜬다" : "가라앉는다", "(밀도로 계산)")
                    }
                    Prop("비열", e.heat, "J/g·K")
                    Prop("지각 속 함량", e.crust, "mg/kg")
                    Prop("발견", e.year == "Ancient" ? "고대부터 알려짐" : e.year, "")
                }
            }
            if !e.origin.isEmpty {
                SectionTitle("이름 유래")
                Card { Text(e.origin).font(pf(15)).lineSpacing(5).foregroundStyle(Palette.ink) }
            }
            Text("빈 칸은 출처에 값이 없는 항목이다. 지어내서 채우지 않았다.").font(pf(12)).foregroundStyle(Palette.dim)
                .frame(maxWidth: .infinity, alignment: .leading)
            SourceList(sources: ref.elementSources)
        }
        .screen("원소")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                Button { nav.replaceElement(e.z - 1) } label: { Image(systemName: "chevron.left") }
                    .disabled(e.z <= 1).accessibilityLabel("이전 원소")
                Button { nav.replaceElement(e.z + 1) } label: { Image(systemName: "chevron.right") }
                    .disabled(e.z >= ref.elements.count).accessibilityLabel("다음 원소")
            }
        }
    }
}

private struct Prop: View {
    let label, value, unit: String
    init(_ l: String, _ v: String, _ u: String) { label = l; value = v; unit = u }
    var body: some View {
        HStack(alignment: .top) {
            Text(label).font(pf(14)).foregroundStyle(Palette.dim).frame(width: 110, alignment: .leading)
            Text(value.trimmingCharacters(in: .whitespaces).isEmpty ? "-" : Num.pretty(value) + (unit.isEmpty ? "" : " \(unit)"))
                .font(pf(14, .bold)).foregroundStyle(value.trimmingCharacters(in: .whitespaces).isEmpty ? Palette.line : Palette.ink)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

struct TableScreen: View {
    let t: DataTable
    @State private var query = ""
    @State private var sortCol = -1   // -1: 원래 순서, 0..: 그 열로 정렬
    @State private var desc = true

    private var rows: [TableRow] {
        let filtered = t.rows.filter { r in
            switch r {
            case .section: return true
            case .entry(let ko, let en, _): return Hangul.matches(ko, query) || Hangul.matches(en, query)
            }
        }
        if sortCol < 0 && query.trimmingCharacters(in: .whitespaces).isEmpty { return filtered }
        let entries = filtered.filter { if case .entry = $0 { return true } else { return false } }
        if sortCol < 0 { return entries }
        func key(_ r: TableRow) -> Double? { if case .entry(_, _, let c) = r { return Num.first(c[sortCol]) } else { return nil } }
        let has = entries.filter { key($0) != nil }, none = entries.filter { key($0) == nil }
        let sorted = has.sorted { key($0)! < key($1)! }
        return (desc ? sorted.reversed() : sorted) + none
    }

    var body: some View {
        let rows = self.rows
        Page {
            Card { Text(t.desc).font(pf(14)).lineSpacing(5).foregroundStyle(Palette.ink) }
            SearchField(text: $query, placeholder: "물질 찾기 (한글, 영어, 초성)")
            if t.numeric.contains(true) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        Pill(text: "원래 순서", on: sortCol < 0, size: 13) { sortCol = -1 }
                        ForEach(Array(t.columns.enumerated()), id: \.offset) { i, name in
                            if t.numeric[i] {
                                let on = sortCol == i
                                Pill(text: on ? "\(name) \(desc ? "큰 순" : "작은 순")" : name, on: on, size: 13) {
                                    if on { desc.toggle() } else { sortCol = i; desc = true }
                                }
                            }
                        }
                    }
                }
            }
            Text("\(rows.filter { if case .entry = $0 { return true } else { return false } }.count)개")
                .font(pf(13)).foregroundStyle(Palette.dim).frame(maxWidth: .infinity, alignment: .leading)
            ForEach(Array(rows.enumerated()), id: \.offset) { _, r in
                switch r {
                case .section(let s):
                    Text(s).font(pf(15, .extra)).foregroundStyle(Palette.purple)
                        .frame(maxWidth: .infinity, alignment: .leading).padding(.top, 6)
                case .entry(let ko, let en, let cells):
                    Card {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(ko).font(pf(16, .bold)).foregroundStyle(Palette.ink)
                            if en != ko { Text(en).font(pf(12)).foregroundStyle(Palette.dim) }
                            FlowLayout(spacing: 14, lineSpacing: 4) {
                                ForEach(Array(cells.enumerated()), id: \.offset) { ci, v in
                                    if !v.trimmingCharacters(in: .whitespaces).isEmpty {
                                        VStack(alignment: .leading, spacing: 0) {
                                            Text(t.columns[ci]).font(pf(11)).foregroundStyle(Palette.dim)
                                            Text(Num.pretty(v)).font(pf(14, .bold)).foregroundStyle(ci == sortCol ? Palette.purple : Palette.ink)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            SourceList(sources: [t.source])
        }
        .screen(t.title)
    }
}

struct SourceList: View {
    let sources: [RefSource]
    var body: some View {
        VStack(spacing: 6) {
            SectionTitle("출처")
            ForEach(Array(sources.enumerated()), id: \.offset) { _, s in
                SourceCard(title: s.title, sub: [s.publisher, s.license].filter { !$0.isEmpty }.joined(separator: " · "), url: s.url)
            }
        }
    }
}

struct LawsScreen: View {
    @State private var field: Field? = nil
    @State private var query = ""
    let laws = Store.laws

    var body: some View {
        let list = laws.filter { l in
            (field == nil || l.field == field) &&
                (Hangul.matches(l.ko, query) || Hangul.matches(l.en, query) || Hangul.matches(l.formula, query) || Hangul.matches(l.statement, query))
        }
        Page {
            SearchField(text: $query, placeholder: "법칙 찾기 (이름, 영어, 초성)")
            FieldFilter(selected: $field)
            Text("\(list.count)개").font(pf(13)).foregroundStyle(Palette.dim).frame(maxWidth: .infinity, alignment: .leading)
            if list.isEmpty { Text("찾는 법칙이 없어요.").font(pf(14)).foregroundStyle(Palette.dim).frame(maxWidth: .infinity, alignment: .leading) }
            ForEach(list) { l in LawCard(l: l) }
            Text("식과 설명은 교과서 수준의 요약이고, 아래 위키백과 문서로 자세한 내용을 확인할 수 있다.")
                .font(pf(12)).foregroundStyle(Palette.dim).frame(maxWidth: .infinity, alignment: .leading)
        }
        .screen("법칙·원리")
    }
}

private struct LawCard: View {
    let l: Law
    @Environment(\.openURL) private var openURL
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 10) {
                FieldChip(field: l.field, onColor: false, extra: l.who.isEmpty ? nil : l.who)
                Text(l.ko).font(pf(18, .extra)).foregroundStyle(Palette.ink)
                Text(l.en).font(pf(12)).foregroundStyle(Palette.dim)
                Text(l.formula).font(pf(16, .bold)).foregroundStyle(.white)
                    .frame(maxWidth: .infinity, alignment: .leading).padding(12)
                    .background(l.field.gradient, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                Text(l.statement).font(pf(15, .bold)).lineSpacing(5).foregroundStyle(Palette.ink)
                Text(l.why).font(pf(14)).lineSpacing(5).foregroundStyle(Palette.dim)
                Button { if let u = URL(string: l.source.url) { openURL(u) } } label: {
                    Text("출처: \(l.source.title)").font(pf(12, .bold)).foregroundStyle(Palette.purple).multilineTextAlignment(.leading)
                }
                .buttonStyle(.plain)
            }
        }
    }
}
