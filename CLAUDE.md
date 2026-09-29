# PEAA 1.21.1 書き直しプロジェクト

ProjectE Advanced Alchemy（PEAA, 1.7.10 / 作者 Ryokusitai）の仕様を、
**NeoForge 1.21.1 + ProjectE 1.21.1 のアドオン**として新規実装するプロジェクト。
1.7.10 のコードを「変換」するのではなく、仕様書（`docs/SPEC.md`）を正として書き直す。

## 環境
- Minecraft 1.21.1 / NeoForge 21.1.x / Java 21
- ビルド: ModDevGradle（`refs/neoforge-mdk` をテンプレートにする）
- 依存: ProjectE 1.21.1（API は `refs/projecte/src/api` を参照）

## 参考資料（refs/ — 読み取り専用・リポジトリには含めない）
他者の著作物なので Git 管理外。取得手順と固定コミットは `docs/SETUP.md` §1。

| パス | 用途 |
|---|---|
| `refs/peaa/` | 元の 1.7.10 ソース。**仕様の出典としてのみ**読む。書き方は真似しない |
| `refs/projecte/` | ProjectE 1.21.1。**1.21.1 での正しい書き方の第一の見本** |
| `refs/projecte/src/api/` | ProjectE API（EMC、Knowledge、Capability、BaseEmcBlockEntity、IRelay など） |
| `refs/neoforge-mdk/` | プロジェクト雛形（build.gradle / gradle.properties / neoforge.mods.toml） |
| `refs/neoforge-docs/versioned_docs/version-1.21.1/` | NeoForge 1.21.1 公式ドキュメント |

## 絶対に守るルール
- `refs/` 配下は**編集しない・実行しない**（`refs/*/gradlew` も含む）。コピーしてよいのは雛形作成時の MDK と、PEAA のテクスチャ・言語ファイルのみ（下記「ライセンスと利用範囲」参照）。
- 1.7.10 の API（`cpw.mods.fml.*`、`IIcon`、`TileEntity`、NBT 直書き、`GameRegistry.register*`、`IFMLLoadingPlugin`）を書かない。
- NeoForge / ProjectE の API を**記憶で書かない**。使う前に `refs/projecte` か `refs/neoforge-docs` で実在とシグネチャを確認する。見つからなければ推測で埋めず、その旨を報告する。
- 仕様（数値・レシピ・挙動）を変えるときは、先に `docs/SPEC.md` を更新し、変更理由を `docs/DEVIATIONS.md` に記録する。
- 依存の追加やビルド設定の変更は理由を説明してから行う。

## 1.7.10 → 1.21.1 の対応方針
- 登録: `DeferredRegister` / `DeferredHolder`（ProjectE の `PEBlocks` 等の構成を参考）
- アイテムのデータ: NBT ではなく Data Components
- TileEntity → `BlockEntity`（EMC を持つものは ProjectE の `BaseEmcBlockEntity` / `IRelay` を優先的に使う）
- GUI: `AbstractContainerMenu` + `AbstractContainerScreen`、`MenuType` 登録
- 通信: `CustomPacketPayload` + `StreamCodec`
- アセット: JSON モデル・blockstate・言語ファイル（`en_us.json` / `ja_jp.json`）、可能な限りデータ生成で出力
- レシピ: データパック JSON（特殊レシピは `CustomRecipe` / `RecipeSerializer`）
- **PEAA の ASM コアモッド（`refs/peaa/src/main/java/peaa/asm/transform/`）**は ProjectE 本体の挙動を書き換えている。
  1.21.1 では原則として (1) ProjectE の API・イベント・継承で代替、(2) 無理な場合のみ Mixin を使う。
  Mixin を使う前に、何をどこに注入するかを説明して確認を取る。

## ワークフロー
1. 仕様は `docs/SPEC.md` が正。挙動を変えるなら先にここを更新する
2. 実装は**1 機能ずつ**。着手前に計画を提示して承認を得る
4. 各機能で `./gradlew build` が通ること、`./gradlew runClient` で確認手順を提示すること
5. 数値ロジック（EMC 生成量など）は可能な範囲で GameTest を書く

## ライセンスと利用範囲
**MMPL_J 1.0.1 で公開する**（CurseForge / Modrinth）。経緯と根拠は `docs/CREDITS.md`、全文は `LICENSE`。
- **ソース公開は MMPL 第 6 条の義務**。バイナリを配る以上、リポジトリを非公開に戻してはいけない。
- 元のテクスチャ（`refs/peaa/src/main/resources/assets/peaa/textures/`）はプロジェクトにコピーして使ってよい。
  1.21.1 のパス規則に合わせて配置し直すこと（`textures/items` → `textures/item`、`textures/blocks` → `textures/block`、ファイル名は小文字・スネークケース）。
  配置先とファイル名の対応表を `docs/ASSETS.md` に残す。
- 言語ファイルの表示名・説明文（`en_US.lang` / `ja_JP.lang`）も流用してよい（`en_us.json` / `ja_jp.json` に変換）。
- コードは 1.7.10 のものをコピーしない（1.21.1 では動かないため）。仕様を参照して ProjectE 1.21.1 の書き方で再実装する。
- `mod_license` は `MMPL_J 1.0.1` から変えない。MMPL は「この MOD の配布物は MMPL のままであること」を要求する。
- 流用物を増やしたときは `docs/ASSETS.md` と `docs/CREDITS.md` を必ず更新する。
