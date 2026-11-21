// 1024x1024 아이콘: 분홍-보라-하늘 그라디언트 위에 안드로이드 아이콘과 같은 원자 모양(타원 셋 + 핵).
import AppKit
let S = 1024
let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: S, pixelsHigh: S, bitsPerSample: 8, samplesPerPixel: 4,
                           hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
let ctx = NSGraphicsContext.current!.cgContext
let cs = CGColorSpaceCreateDeviceRGB()
let g = CGGradient(colorsSpace: cs, colors: [CGColor(red: 1, green: 0.3, blue: 0.616, alpha: 1), CGColor(red: 0.608, green: 0.361, blue: 1, alpha: 1), CGColor(red: 0.239, green: 0.663, blue: 1, alpha: 1)] as CFArray, locations: [0, 0.5, 1])!
ctx.drawLinearGradient(g, start: CGPoint(x: 0, y: CGFloat(S)), end: CGPoint(x: CGFloat(S), y: 0), options: [])
ctx.translateBy(x: 512, y: 512)
let k = 1024.0 / 108.0
ctx.setStrokeColor(.white); ctx.setLineWidth(3.2 * k * 1.25)
for deg in [0.0, 60, 120] {
    ctx.saveGState(); ctx.rotate(by: deg * .pi / 180)
    ctx.strokeEllipse(in: CGRect(x: -24 * k * 1.25, y: -9 * k * 1.25, width: 48 * k * 1.25, height: 18 * k * 1.25))
    ctx.restoreGState()
}
ctx.setFillColor(.white); ctx.fillEllipse(in: CGRect(x: -5 * k * 1.25, y: -5 * k * 1.25, width: 10 * k * 1.25, height: 10 * k * 1.25))
NSGraphicsContext.restoreGraphicsState()
try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: CommandLine.arguments[1]))
