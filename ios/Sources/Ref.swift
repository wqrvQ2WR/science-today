import Foundation

/// 주기율표 한 칸. 값은 PubChem·위키백과 원문 그대로의 문자열이고, 없으면 빈 문자열.
struct Element: Identifiable {
    let z: Int
    let sym, ko, alt, en, mass: String
    let group: Int?
    let period, row, col: Int
    let kind, config, chi, radius, ie, ea, ox, state, mp, bp, density, year, heat, crust, origin: String
    var id: Int { z }
    var name: String { alt.isEmpty ? ko : "\(ko) (\(alt))" }
}

struct RefSource { let title, publisher, url, license: String }

enum TableRow {
    case section(String)
    case entry(ko: String, en: String, cells: [String])
}

struct DataTable: Identifiable {
    let id, title, desc: String
    let columns: [String]
    let numeric: [Bool]
    let rows: [TableRow]
    let source: RefSource
    var entryCount: Int { rows.reduce(0) { if case .entry = $1 { return $0 + 1 } else { return $0 } } }
}

final class RefData {
    let elements: [Element]
    let elementSources: [RefSource]
    let tables: [DataTable]
    let byZ: [Int: Element]
    let tableById: [String: DataTable]

    init(elements: [Element], elementSources: [RefSource], tables: [DataTable]) {
        self.elements = elements
        self.elementSources = elementSources
        self.tables = tables
        byZ = Dictionary(uniqueKeysWithValues: elements.map { ($0.z, $0) })
        tableById = Dictionary(uniqueKeysWithValues: tables.map { ($0.id, $0) })
    }

    func searchElements(_ q: String) -> [Element] {
        let t = q.trimmingCharacters(in: .whitespaces)
        return elements.filter {
            Hangul.matches($0.ko, q) || Hangul.matches($0.alt, q) || Hangul.matches($0.en, q) ||
                $0.sym.caseInsensitiveCompare(t) == .orderedSame || String($0.z) == t
        }
    }

    private static func src(_ o: [String: Any]) -> RefSource {
        RefSource(title: o["title"] as? String ?? "", publisher: o["publisher"] as? String ?? "",
                  url: o["url"] as? String ?? "", license: o["license"] as? String ?? "")
    }

    static func parse(elements ed: Data, tables td: Data) throws -> RefData {
        let e = try JSONSerialization.jsonObject(with: ed) as! [String: Any]
        let elements: [Element] = (e["elements"] as! [[String: Any]]).map { o in
            func s(_ k: String) -> String {
                guard let v = o[k], !(v is NSNull) else { return "" }
                return "\(v)"
            }
            return Element(
                z: o["z"] as! Int, sym: s("sym"), ko: s("ko"), alt: s("alt"), en: s("en"), mass: s("mass"),
                group: o["group"] as? Int, period: o["period"] as! Int, row: o["row"] as! Int, col: o["col"] as! Int,
                kind: s("kind"), config: s("config"), chi: s("chi"), radius: s("radius"), ie: s("ie"), ea: s("ea"),
                ox: s("ox"), state: s("state"), mp: s("mp"), bp: s("bp"), density: s("density"), year: s("year"),
                heat: s("heat"), crust: s("crust"), origin: s("origin"))
        }
        let t = try JSONSerialization.jsonObject(with: td) as! [String: Any]
        let tables: [DataTable] = (t["tables"] as! [[String: Any]]).map { o in
            DataTable(
                id: o["id"] as! String, title: o["title"] as! String, desc: o["desc"] as! String,
                columns: o["columns"] as! [String], numeric: o["numeric"] as! [Bool],
                rows: (o["rows"] as! [[String: Any]]).map { r in
                    if let s = r["section"] as? String { return .section(s) }
                    return .entry(ko: r["ko"] as! String, en: r["en"] as! String, cells: r["cells"] as! [String])
                },
                source: src(o["source"] as! [String: Any]))
        }
        return RefData(elements: elements, elementSources: (e["sources"] as! [[String: Any]]).map(src), tables: tables)
    }
}

enum Num {
    private static let sup: [Character: Character] = [
        "−": "⁻", "-": "⁻", "+": "⁺", "0": "⁰", "1": "¹", "2": "²", "3": "³", "4": "⁴",
        "5": "⁵", "6": "⁶", "7": "⁷", "8": "⁸", "9": "⁹",
    ]

    /// "1.59×10^−8" -> "1.59×10⁻⁸". 위첨자로 못 바꾸는 글자가 나오면 거기서 멈춘다.
    static func pretty(_ s: String) -> String {
        var out = ""
        let c = Array(s)
        var i = 0
        while i < c.count {
            if c[i] == "^" {
                i += 1
                while i < c.count, let u = sup[c[i]] { out.append(u); i += 1 }
                continue
            }
            out.append(c[i]); i += 1
        }
        return out
    }

    /// 정렬용으로 맨 앞 숫자만 읽는다. "0.170–0.200" -> 0.17, "7.49 × 10^−6" -> 7.49e-6, "10^23 to 10^25" -> 1e23.
    static func first(_ s: String) -> Double? {
        let t = s.replacingOccurrences(of: ",", with: "").replacingOccurrences(of: "−", with: "-")
            .replacingOccurrences(of: "~", with: "").replacingOccurrences(of: "≈", with: "")
            .replacingOccurrences(of: ">", with: "").replacingOccurrences(of: "<", with: "")
            .trimmingCharacters(in: .whitespaces)
        if let m = t.firstMatch(#"^(-?\d+(?:\.\d+)?)?\s*(?:[×x]\s*)?10\^(-?\d+)"#) {
            let mant = m[1].isEmpty ? 1.0 : Double(m[1]) ?? 1.0
            return mant * pow(10.0, Double(m[2]) ?? 0)
        }
        if let m = t.firstMatch(#"^-?\d+(?:\.\d+)?(?:[eE]-?\d+)?"#) { return Double(m[0]) }
        return nil
    }

    /// "[Ar]4s2 3d6" -> "[Ar]4s² 3d⁶". 오비탈 글자(s,p,d,f) 바로 뒤 숫자만 위첨자로 바꾼다.
    static func config(_ c: String) -> String {
        let re = try! NSRegularExpression(pattern: "([spdf])(\\d+)")
        let ns = c as NSString
        var out = "", at = 0
        for m in re.matches(in: c, range: NSRange(location: 0, length: ns.length)) {
            out += ns.substring(with: NSRange(location: at, length: m.range.location - at))
            out += ns.substring(with: m.range(at: 1)) + ns.substring(with: m.range(at: 2)).map { String(sup[$0] ?? $0) }.joined()
            at = m.range.location + m.range.length
        }
        return out + ns.substring(from: at)
    }

    /// 켈빈 문자열 -> "1234.5 K (961.4 °C)". 숫자가 아니면 그대로.
    static func kelvin(_ k: String) -> String {
        guard let v = Double(k) else { return k }
        return "\(k) K (\(String(format: "%.1f", v - 273.15)) °C)"
    }
}

private extension String {
    /// 정규식 첫 일치의 그룹들(0번은 전체). 없으면 nil. 없는 그룹은 빈 문자열.
    func firstMatch(_ pattern: String) -> [String]? {
        let re = try! NSRegularExpression(pattern: pattern)
        let ns = self as NSString
        guard let m = re.firstMatch(in: self, range: NSRange(location: 0, length: ns.length)) else { return nil }
        return (0..<m.numberOfRanges).map { m.range(at: $0).location == NSNotFound ? "" : ns.substring(with: m.range(at: $0)) }
    }
}

/// 법칙·원리 한 개. 식과 발견자는 출처에 있는 것만 적고 없으면 빈 문자열.
struct Law: Identifiable {
    let id, ko, en: String
    let field: Field
    let formula, statement, why, who: String
    let source: RefSource

    static func parse(_ data: Data) throws -> [Law] {
        let root = try JSONSerialization.jsonObject(with: data) as! [String: Any]
        return (root["laws"] as! [[String: Any]]).map { o in
            let s = o["source"] as! [String: Any]
            return Law(id: o["id"] as! String, ko: o["ko"] as! String, en: o["en"] as! String,
                       field: Field(rawValue: o["field"] as! String)!, formula: o["formula"] as! String,
                       statement: o["statement"] as! String, why: o["why"] as! String, who: o["who"] as? String ?? "",
                       source: RefSource(title: s["title"] as? String ?? "", publisher: s["publisher"] as? String ?? "",
                                         url: s["url"] as? String ?? "", license: s["license"] as? String ?? ""))
        }
    }
}
