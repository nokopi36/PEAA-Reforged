# PEAA Reforged

ProjectE Advanced Alchemy（PEAA / 1.7.10 / 作者 Ryokusitai）を、
**NeoForge 1.21.1 + ProjectE 1.21.1 のアドオン**として書き直したものです。

コードは 1 行も流用せず、仕様を読み取って新規に実装しています。
テクスチャと表示名は原典のものを使わせていただいています。

| | |
|---|---|
| Minecraft | 1.21.1 / NeoForge |
| mod id | `peaa_reforged` |
| ライセンス | **MMPL_J 1.0.1**（[LICENSE](LICENSE)） |
| 原典 | [PEAA](https://www.curseforge.com/minecraft/mc-mods/projecte-advanced-alchemy) by Ryokusitai / さらにその原典は EEAA by AK |
| 依存 | [ProjectE](https://github.com/sinkillerj/ProjectE) 1.1.0 以上（必須） |

権利関係の詳細は [docs/CREDITS.md](docs/CREDITS.md) を参照してください。

## 構成

| パス | 内容 |
|---|---|
| `src/` | MOD 本体（mod id: `peaa_reforged`） |
| `docs/SPEC.md` | 原典の仕様書。実装の正 |
| `docs/SETUP.md` | 開発環境のセットアップ（**ProjectE jar の配置手順を含む**） |
| `docs/ASSETS.md` | 原典テクスチャ → 1.21.1 配置の対応表 |
| `docs/DEVIATIONS.md` | SPEC から意図的に変えた点の記録 |
| `docs/CREDITS.md` | クレジットと権利関係の調査結果 |
| `LICENSE` | MMPL 1.0.1 全文と、このライセンスを選んだ理由 |
| `CLAUDE.md` | 実装時に守るルール（1.7.10 API 禁止、Mixin は事前確認など） |

## ビルド

```bash
./gradlew build             # コンパイル + jar 生成
./gradlew runClient         # クライアント起動
./gradlew runGameTestServer # GameTest 実行（33 件）
```

ProjectE は公開 Maven を持たないため **CurseMaven** 経由で自動取得します。jar の手配置は不要です。
詳細と注意点は [docs/SETUP.md](docs/SETUP.md)。

## 実装の考え方

原典のコードを変換したのではなく、**仕様書 [`docs/SPEC.md`](docs/SPEC.md) を正として書き直しています。**
SPEC から意図的に外した点は [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md) に D-001 〜 D-021 として
理由つきで記録しています。

数値ロジック（EMC 生成量、鉱石倍化率、指輪の消費 EMC など）は GameTest で固定しています。
