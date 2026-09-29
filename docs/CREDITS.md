# クレジットと権利関係

PEAA Reforged は、既存 MOD の系譜の上にあります。調査した範囲での権利の連鎖と、
その根拠を残しておきます。**一次情報に当たれていない箇所は、そう明記しています。**

## 系譜

| 段階 | 作者 | 内容 |
|---|---|---|
| 原典 | **AK** | EEAA（EE2 Addon "Advanced Alchemy"、Minecraft 1.2.5 向け） |
| 移植 | **Ryokusitai** | EEAA を ProjectE 1.7.10 向けにしたもの = PEAA |
| 再配布 | **GiftedWhitewolf** | PEAA を CurseForge に掲載（MMPL_J 1.0.1 を指定） |
| 本 MOD | **nokopi** | PEAA を NeoForge 1.21.1 向けに新規実装 |

## ライセンス

本 MOD は **MMPL_J 1.0.1**（Minecraft Mod Public License 日本語訳）で配布します。
全文と選定理由は `LICENSE` を参照してください。

PEAA が MMPL_J 1.0.1 であることの出典:
<https://www.curseforge.com/minecraft/mc-mods/projecte-advanced-alchemy/license>

MMPL 1.0.1 第 6 条により、**バイナリを配布する場合はソース一式を無償で入手可能に
する義務**があります。本リポジトリの公開がそれに当たります。

## 流用しているもの / していないもの

| 種別 | 出所 | 扱い |
|---|---|---|
| テクスチャ 14 点 | PEAA | **そのまま流用**。対応表は `docs/ASSETS.md` |
| ブロック名・アイテム名の表示文字列 | PEAA（`en_US.lang` / `ja_JP.lang`） | 流用。`en_us.json` / `ja_jp.json` に変換 |
| 仕様（EMC レート・レシピ・挙動） | PEAA | 読み取って `docs/SPEC.md` に起こし、そこから再実装 |
| **コード** | — | **1 行も流用していない。** 1.7.10 のコードは 1.21.1 では動作しないため、全面的に新規実装 |

## 依存

**ProjectE**（sinkillerj 他 / MIT License, Copyright (c) 2020 Sin Tachikawa）に依存します。
ProjectE のコードは同梱していません。MMPL 1.0.1 第 6 条は依存先に「MMPL と同等の条件」を
求めますが、MIT はより緩いライセンスであり、また NeoForge は同条の除外対象（mod loading
framework）です。

## 未確認事項

- **AK 氏の許諾条件。** PEAA の CurseForge ページに「AK 氏が開発を再開するまでという
  条件付きで、AK 氏から公式に許諾を得ている」旨の記載がありますが、これは
  GiftedWhitewolf 氏による再配布ページの記述であり、**元の日本語スレッドも AK 氏本人の
  発言も未確認**です。条件の具体的な内容も不明です
- **MMPL_J を選定したのが誰か。** MMPL_J を掲げているのは上記の再配布ページで、
  Ryokusitai 氏自身のリポジトリ（<https://github.com/Ryokusitai/PEAA>）には LICENSE
  ファイルがありません。Ryokusitai 氏本人の選択か、再配布時の指定かは未確認です
- **MMPL が素材を対象に含むか。** MMPL の条文は code / sources / classes を対象に
  書かれており、テクスチャのような素材への明示的な言及がありません。本 MOD が流用して
  いるのは主に素材であるため、安全側に倒して MMPL_J で配布しています
- **`mcmod.info` の作者欄。** PEAA の `mcmod.info` は `authorList` が `takanasayo`、
  `credits` が `By ryokusitai` と食い違っています。CurseForge・Minecraft Japan Wiki・
  git のコミット（`Ryokusitai <...>` の 1 件のみ）はいずれも Ryokusitai 氏を作者と
  しているため、本ファイルでもそれに従っています

## 関連リンク

- PEAA（1.7.10）: <https://www.curseforge.com/minecraft/mc-mods/projecte-advanced-alchemy>
- PEAA ソース: <https://github.com/Ryokusitai/PEAA>
- ProjectE: <https://github.com/sinkillerj/ProjectE>
- MMPL 1.0.1 原文: <https://mod-buildcraft.com/MMPL-1.0.txt>
