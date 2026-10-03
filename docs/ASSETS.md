# アセット対応表

原典 `refs/peaa/src/main/resources/assets/peaa/textures/` のテクスチャを、
1.21.1 のパス規則（`textures/items` → `textures/item`、`textures/blocks` → `textures/block`、
ファイル名は小文字スネークケース）に合わせて配置し直したものの一覧。

配置先ルート: `src/main/resources/assets/peaa_reforged/textures/`

## ブロック

| 原典 | 配置先 | サイズ | 参照元（原典コード） |
|---|---|---|---|
| `blocks/aegu/aegu1.png` | `block/aegu_mk1.png` | 16×16 | `blocks/AEGU.java:107`（tier=1, 停止形） |
| `blocks/aegu/aegu1gene.png` | `block/aegu_mk1_on.png` | 16×16 | `blocks/AEGU.java:107`（tier=1, 稼働形） |
| `blocks/aegu/aegu2.png` | `block/aegu_mk2.png` | 16×16 | `blocks/AEGU.java:107`（tier=2, 停止形） |
| `blocks/aegu/aegu2gene.png` | `block/aegu_mk2_on.png` | 16×16 | `blocks/AEGU.java:107`（tier=2, 稼働形） |
| `blocks/aegu/aegu3.png` | `block/aegu_mk3.png` | 16×16 | `blocks/AEGU.java:107`（tier=3, 停止形） |
| `blocks/aegu/aegu3gene.png` | `block/aegu_mk3_on.png` | 16×16 | `blocks/AEGU.java:107`（tier=3, 稼働形） |
| `blocks/collectors/front.png` | `block/collector_front.png` | 16×16 | `blocks/CollectorPEAA.java:69`（MK4/MK5 共用） |
| `blocks/collectors/other_4.png` | `block/collector_mk4_side.png` | 16×16 | `blocks/CollectorPEAA.java:68`（側面） |
| `blocks/collectors/top_4.png` | `block/collector_mk4_top.png` | 16×16 | `blocks/CollectorPEAA.java:70`（上面） |
| `blocks/collectors/other_5.png` | `block/collector_mk5_side.png` | 16×16 | `blocks/CollectorPEAA.java:68`（側面） |
| `blocks/collectors/top_5.png` | `block/collector_mk5_top.png` | 16×16 | `blocks/CollectorPEAA.java:70`（上面） |
| `blocks/condenser_mk2.png` | `peaa_condenser_texture/assets/projecte/textures/block/condenser_mk2.png` | **64×64** | ASM `MK2TextureTransformer`（下記参照） |

AEGU は全 6 面が同じ 1 枚テクスチャ（原典の `getIcon` は常に `blockIcon` を返す / `blocks/AEGU.java:112-116`）。

## アイテム

| 原典 | 配置先 | サイズ | 参照元（原典コード） |
|---|---|---|---|
| `items/ring/ringFlightTeleport_off.png` | `item/ring_of_the_space.png` | 16×16 | `items/RingFlightTeleport.java:185` |
| `items/ring/ringFlightTeleport_on.png` | `item/ring_of_the_space_on.png` | 16×16 | `items/RingFlightTeleport.java:186` |

## 移植しないもの

| 原典 | 理由 |
|---|---|
| `blocks/aegu/aegu.pdn` | Paint.NET 作業ファイル |
| `blocks/aegu/top.pdn` | Paint.NET 作業ファイル（対応する PNG は存在しない） |
| `blocks/aegu/top_4.pdn` | Paint.NET 作業ファイル（同上） |
| `blocks/collectors/other.pdn` | Paint.NET 作業ファイル |
| `blocks/collectors/other.png` | 原典コードから未参照（`registerBlockIcons` は `other_4` / `other_5` しか読まない / `blocks/CollectorPEAA.java:68`） |
| `gui/collector3.png` | **ProjectE 1.21.1 の `assets/projecte/textures/gui/collector3.png` と md5 完全一致**（`233137f635b5b7cd45619203c3e77cf4`）。下記参照 |

## 調査メモ

### `gui/collector3.png`（コレクター GUI）
原典の PEAA が同梱していたファイルは ProjectE の `collector3.png` のバイト単位のコピーでした
（SPEC 付録 B の [要確認] はこれで解消）。
コレクター MK4/MK5 の Container / Screen は ProjectE 1.21.1 の MK3 と座標まで一致するため
（SPEC §3.1.5 / §3.1.6）、**自前でコピーを持たず `projecte:textures/gui/collector3.png` を直接参照する**方針とし、
重複アセットは置きませんでした。ProjectE 側がテクスチャを更新した場合も追従できます。

### `blocks/condenser_mk2.png`（64×64）
これは 16×16 のブロック面テクスチャではなく、**専用レンダラーが使うモデルアトラス**です。

当初は「1.21.1 の JSON モデル方式にそのまま挿せない」と記載していましたが、調査の結果
**ProjectE 1.21.1 も同じ方式**でした。`assets/projecte/models/block/condenser_mk2.json` は
`particle` テクスチャのみを持ち要素が無く、描画は専用レンダラーが行います。
ProjectE 側の `condenser_mk2.png` も同じ 64×64・同じ UV レイアウトです（内容は別物で、青緑系 対 赤／橙系）。

判断 13 により差し替えを採用したため、このファイルは
`src/main/resources/peaa_condenser_texture/assets/projecte/textures/block/condenser_mk2.png`
に配置し、内蔵リソースパックとして ProjectE のものに上書き適用しています（`docs/DEVIATIONS.md` の D-019）。

## 仮テクスチャ

現時点で仮テクスチャを使っているものはありません。

## 参照状況（Step 2+3 時点）

| テクスチャ | 参照元 |
|---|---|
| `block/collector_mk4_side` / `_top` / `collector_front` | `models/block/collector_mk4.json`（`minecraft:block/orientable`） |
| `block/collector_mk5_side` / `_top` / `collector_front` | `models/block/collector_mk5.json` |
| `block/aegu_mk{1,2,3}` | `models/block/aegu_mk{1,2,3}.json`（`generating=false`） |
| `block/aegu_mk{1,2,3}_on` | `models/block/aegu_mk{1,2,3}_on.json`（`generating=true`） |
| `assets/projecte/.../block/condenser_mk2`（内蔵パック内） | ProjectE のテクスチャを上書き（D-019） |
| `item/ring_of_the_space{,_on}` | **未参照**。SPEC §3.5 の実装待ち |

モデル・blockstate は `com.nokopi.peaareforged.datagen.PEAABlockStateProvider` が生成し、`src/generated/resources/` に出力されます。
