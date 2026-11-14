import SwiftUI
import UIKit

/// Palette aligned with `BodyRendererTheme.kt` in `commonMain`.
struct SwiftBodyPalette {
    let key: Color
    let string: Color
    let number: Color
    let boolean: Color
    let nullValue: Color
    let separator: Color
    let tag: Color
    let attributeName: Color
    let attributeValue: Color
    let comment: Color
    let doctype: Color
    let text: Color

    static let `default` = SwiftBodyPalette.dynamic()

    static func dynamic() -> SwiftBodyPalette {
        func dynamicColor(light: UIColor, dark: UIColor) -> Color {
            Color(uiColor: UIColor { traits in
                traits.userInterfaceStyle == .dark ? dark : light
            })
        }

        return SwiftBodyPalette(
            key: dynamicColor(
                light: UIColor(red: 0x04 / 255.0, green: 0x51 / 255.0, blue: 0xA5 / 255.0, alpha: 1.0),
                dark: UIColor(red: 0x9C / 255.0, green: 0xDC / 255.0, blue: 0xFE / 255.0, alpha: 1.0)
            ),
            string: dynamicColor(
                light: UIColor(red: 0xA3 / 255.0, green: 0x15 / 255.0, blue: 0x15 / 255.0, alpha: 1.0),
                dark: UIColor(red: 0xCE / 255.0, green: 0x91 / 255.0, blue: 0x78 / 255.0, alpha: 1.0)
            ),
            number: dynamicColor(
                light: UIColor(red: 0x09 / 255.0, green: 0x86 / 255.0, blue: 0x58 / 255.0, alpha: 1.0),
                dark: UIColor(red: 0xB5 / 255.0, green: 0xCE / 255.0, blue: 0xA8 / 255.0, alpha: 1.0)
            ),
            boolean: dynamicColor(
                light: UIColor(red: 0x00 / 255.0, green: 0x00 / 255.0, blue: 0xFF / 255.0, alpha: 1.0),
                dark: UIColor(red: 0x56 / 255.0, green: 0x9C / 255.0, blue: 0xD6 / 255.0, alpha: 1.0)
            ),
            nullValue: dynamicColor(
                light: UIColor(red: 0x00 / 255.0, green: 0x00 / 255.0, blue: 0xFF / 255.0, alpha: 1.0),
                dark: UIColor(red: 0x56 / 255.0, green: 0x9C / 255.0, blue: 0xD6 / 255.0, alpha: 1.0)
            ),
            separator: dynamicColor(
                light: UIColor(red: 0x33 / 255.0, green: 0x33 / 255.0, blue: 0x33 / 255.0, alpha: 1.0),
                dark: UIColor(red: 0xD4 / 255.0, green: 0xD4 / 255.0, blue: 0xD4 / 255.0, alpha: 1.0)
            ),
            tag: dynamicColor(
                light: UIColor(red: 0x80 / 255.0, green: 0x00 / 255.0, blue: 0x00 / 255.0, alpha: 1.0),
                dark: UIColor(red: 0x56 / 255.0, green: 0x9C / 255.0, blue: 0xD6 / 255.0, alpha: 1.0)
            ),
            attributeName: dynamicColor(
                light: UIColor(red: 0xFF / 255.0, green: 0x00 / 255.0, blue: 0x00 / 255.0, alpha: 1.0),
                dark: UIColor(red: 0x9C / 255.0, green: 0xDC / 255.0, blue: 0xFE / 255.0, alpha: 1.0)
            ),
            attributeValue: dynamicColor(
                light: UIColor(red: 0x00 / 255.0, green: 0x00 / 255.0, blue: 0xFF / 255.0, alpha: 1.0),
                dark: UIColor(red: 0xCE / 255.0, green: 0x91 / 255.0, blue: 0x78 / 255.0, alpha: 1.0)
            ),
            comment: dynamicColor(
                light: UIColor(red: 0x00 / 255.0, green: 0x80 / 255.0, blue: 0x00 / 255.0, alpha: 1.0),
                dark: UIColor(red: 0x6A / 255.0, green: 0x99 / 255.0, blue: 0x55 / 255.0, alpha: 1.0)
            ),
            doctype: dynamicColor(
                light: UIColor(red: 0x80 / 255.0, green: 0x00 / 255.0, blue: 0x80 / 255.0, alpha: 1.0),
                dark: UIColor(red: 0xC5 / 255.0, green: 0x86 / 255.0, blue: 0xC0 / 255.0, alpha: 1.0)
            ),
            text: dynamicColor(
                light: UIColor(red: 0x20 / 255.0, green: 0x20 / 255.0, blue: 0x20 / 255.0, alpha: 1.0),
                dark: UIColor(red: 0xD4 / 255.0, green: 0xD4 / 255.0, blue: 0xD4 / 255.0, alpha: 1.0)
            )
        )
    }
}
