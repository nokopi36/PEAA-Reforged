# PEAA Reforged

**日本語** | [English](README.en.md)

ProjectE Advanced Alchemy（PEAA / 1.7.10 / 作者 Ryokusitai）を、**NeoForge + ProjectE のアドオン**として書き直したものです。

1.7.10 のコードは 1 行も流用していません。挙動を仕様書に起こしたうえで、現行の ProjectE の作法で新規に実装しています。テクスチャと表示名は原典のものを使わせていただいています。

| | |
|---|---|
| 対応バージョン | Minecraft 1.21.1 / NeoForge |
| mod id | `peaa_reforged` |
| 依存 | **[ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) 1.1.0 以上（必須）** |
| ライセンス | [MMPL_J 1.0.1](LICENSE) |
| 原典 | [PEAA](https://www.curseforge.com/minecraft/mc-mods/projecte-advanced-alchemy) by Ryokusitai／さらにその原典は EEAA by AK |

## ダウンロード

[Releases](https://github.com/nokopi36/PEAA-Reforged/releases) から jar を取得し、ProjectE と一緒に `mods/` に入れてください。**クライアントとサーバーの両方**に必要です。

## 追加される要素

| 要素 | 内容 |
|---|---|
| **EMCコレクター MK4 / MK5** | 真上の AEGU を太陽光とみなし、明るさに関係なく 320 / 1,280 EMC/秒 |
| **錬金術的エネルギー生成装置 / 発展型AEGU / 究極型AEGU** | 40 / 1,000 / 20,000 EMC/秒。EMCコンデンサー MK2 を 25 個以上で囲むと稼働 |
| **司空の指輪** | ジャンプ 2 度押しで飛行（速度 4 段階）、右クリックで 30 ブロック以内へテレポート、落下ダメージ無効 |
| **マターかまど** | DM かまども鉱石を常に倍化。搬出先が上以外の 5 方向に |
| **ジェムブーツ** | 指輪の所持中は空中加速を抑制（config で変更可） |
| その他 | 水オーブと火オーブの相殺、EMCコンデンサー MK2 のテクスチャ差し替え、究極型AEGU 用の EMC マッパー（既定で無効） |

## ビルド

```bash
./gradlew build             # コンパイル + jar 生成
./gradlew runClient         # クライアント起動
./gradlew runGameTestServer # GameTest 実行（33 件）
```

ProjectE は公開 Maven を持たないため **CurseMaven** 経由で自動取得します。jar の手配置は不要で、クローンしてすぐビルドできます。

## 実装の考え方

原典のコードを変換したのではなく、**仕様書 [`docs/SPEC.md`](docs/SPEC.md) を正として書き直しています。** SPEC から意図的に外した点は [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md) に D-001 〜 D-021 として理由つきで記録しています。

ProjectE 本体の挙動を変える必要がある箇所は、まず API・イベント・リソースで代替し、どうしても無理な 2 箇所だけ Mixin を使っています。

数値ロジック（EMC 生成量、鉱石倍化率、指輪の消費 EMC など）は GameTest 33 件で固定しています。

## ドキュメント

| パス | 内容 |
|---|---|
| [`docs/SPEC.md`](docs/SPEC.md) | 原典の仕様書。実装の正 |
| [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md) | SPEC から意図的に変えた点と、その理由 |
| [`docs/CREDITS.md`](docs/CREDITS.md) | クレジットと権利関係の調査結果 |
| [`docs/ASSETS.md`](docs/ASSETS.md) | 原典テクスチャ → 現行配置の対応表 |
| [`docs/SETUP.md`](docs/SETUP.md) | 開発環境のセットアップと注意点 |
| [`CLAUDE.md`](CLAUDE.md) | 実装時に守るルール |

## ライセンスとクレジット

原典の PEAA と同じ **MMPL_J 1.0.1** で配布します。同ライセンスの定めにより、ソースコード一式を無償で公開しています。全文と選定理由は [LICENSE](LICENSE)、権利関係の詳細は [docs/CREDITS.md](docs/CREDITS.md) を参照してください。

- 原作: **PEAA** / Ryokusitai 様
- 原典: **EEAA** / AK 様
- 依存: **ProjectE** / sinkillerj 様ほか（MIT License）

本 MOD は有志による移植であり、PEAA・EEAA・ProjectE の作者の方々が開発やサポートに関わっているものではありません。不具合のご報告は [Issues](https://github.com/nokopi36/PEAA-Reforged/issues) へお願いします。
