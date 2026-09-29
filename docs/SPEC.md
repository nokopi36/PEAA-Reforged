# PEAA 仕様書（1.7.10 原典 → NeoForge 1.21.1 再実装用）

出典: `refs/peaa/`（ProjectE Advanced Alchemy 1.7.10 / 作者 Ryokusitai, commit `8759154`）
照合先: `refs/projecte/`（ProjectE 1.21.1 / commit `f432b0c`）

本書の行番号はすべて `refs/peaa/src/main/java/peaa/...` 配下のファイル（特記あるものは `refs/projecte/...`）。
**[推測]** = 1.7.10 版 ProjectE のソースが refs に無いため、1.21.1 版から逆算した推定。
**[要確認]** = 数値・挙動の確信が持てない箇所。

---

## 1. 概要

### 1.1 MOD の目的
ProjectE（1.7.10）に対する**エンドゲーム向け拡張アドオン**。
「EMC の生産量を桁違いに引き上げる設備」と「高速移動アイテム」を追加し、
あわせて ProjectE 本体のいくつかの挙動を ASM コアモッドで書き換える。

### 1.2 追加要素の全体像

| カテゴリ | 内容 |
|---|---|
| EMC 生成 | Energy Collector MK4 / MK5（太陽光ではなく **AEGU** を動力源にする） |
| EMC 生成 | AEGU MK1 / MK2 / MK3（単体では動かず、Energy Condenser MK2 を 26 個で取り囲んで初めて稼働） |
| 移動 | 司空の指輪 (Ring of the Space) — クリエイティブ風飛行 + 視線先へのテレポート |
| ProjectE 改変 | Energy Condenser MK2 に「AEGU からの EMC 生成」「隣接インベントリへの自動排出」を追加 |
| ProjectE 改変 | DM/RM かまどに「隣接インベントリからの自動搬入・搬出」「鉱石 100% 倍化」「EMC 消費 1.6/tick」 |
| ProjectE 改変 | 水の祭器の水オーブが火のオーブと相殺して消える |
| ProjectE 改変 | ジェムブーツの飛行付与を、司空の指輪所持中は無効化 |
| ProjectE 改変 | Condenser のプログレスバーが int オーバーフローで壊れるのを修正 |
| ProjectE 改変 | Energy Condenser MK2 のテクスチャを差し替え |
| 設定 | 大天使の指輪のレシピをレッドマター版に差し替える（既定 OFF） |

### 1.3 依存関係（ProjectE のどの機能に依存するか）

| PEAA が依存しているもの | 1.7.10 のクラス | 1.21.1 の対応 |
|---|---|---|
| コレクターの本体ロジック | `moze_intel.projecte.gameObjs.tiles.CollectorMK1Tile` | `gameObjs/block_entities/CollectorMK1BlockEntity` |
| コレクターのブロック | `gameObjs.blocks.Collector` | `gameObjs/blocks/Collector`（`EnumCollectorTier` を取る） |
| コレクターのスロット | `container.slots.collector.SlotCollectorInv` / `SlotCollectorLock` | `container/slots/ValidatedSlot` + `SlotPredicates.COLLECTOR_INV` / `SlotGhost` + `COLLECTOR_LOCK` |
| コンデンサー MK2 | `blocks.CondenserMK2` / `tiles.CondenserMK2Tile` | `blocks/CondenserMK2` / `block_entities/CondenserMK2BlockEntity` |
| マターかまど | `blocks.MatterFurnace` / `tiles.RMFurnaceTile` / `tiles.DMFurnaceTile` | `blocks/MatterFurnace` / `block_entities/RMFurnaceBlockEntity` / `DMFurnaceBlockEntity` |
| EMC 付きアイテム基底 | `gameObjs.items.ItemCharge`（charge 段階つき） | `api/capabilities/item/IItemCharge` + `items/ItemPE` + `PEDataComponentTypes.STORED_EMC` |
| クラインの星 | `gameObjs.items.KleinStar` | `gameObjs/items/KleinStar`（`IItemEmcHolder`） |
| EMC 値取得 | `utils.EMCHelper.getEmcValue` | `api/proxy/IEMCProxy.INSTANCE.getValue` / `getSellValue` |
| 燃料アップグレード | `emc.FuelMapper` | `emc/FuelMapper`（変わらず） |
| 素材アイテム | `ObjHandler.matter`(meta 0=DM,1=RM), `kleinStars`(meta 0..5), `collectorMK3`, `dmPedestal`, `swrg`, `voidRing`, `angelSmite`, `ironBand` | `PEItems.DARK_MATTER` / `RED_MATTER` / `KLEIN_STAR_*` / `PEBlocks.COLLECTOR_MK3` / `DARK_MATTER_PEDESTAL` / `PEItems.SWIFTWOLF_RENDING_GALE` / `VOID_RING` / `ARCHANGEL_SMITE` / `IRON_BAND` |
| クリエイティブタブ | `ObjHandler.cTab` | `PECreativeTabs` |
| ジェムブーツ | `items.armor.GemFeet` | `gameObjs/items/armor/GemFeet` |
| 水の祭器 / 水オーブ / 火オーブ | `items.EvertideAmulet` / `entity.EntityWaterProjectile` / `EntityLavaProjectile` | `gameObjs/items/EvertideAmulet` / `entity/EntityWaterProjectile` / `EntityLavaProjectile` |
| GUI ID 定数 | `utils.Constants.CONDENSER_MK2_GUI` / `CONDENSER_MK2_RENDER_ID` | 廃止（`MenuType` + `player.openMenu(MenuProvider)`） |

`PEAACore.java:21` に `dependencies="required-after:ProjectE;"`。ProjectE 必須。

---

## 2. 追加コンテンツ一覧

### 2.1 ブロック

| 内部名（registry） | 非ローカライズ名 | 表示名 (en) | 表示名 (ja) | 種類 | 元ソース |
|---|---|---|---|---|---|
| `Collector MK4` | `pe_collector_MK4` | Energy Collector MK4 | EMCコレクター MK4 | EMC 生成 + GUI | `blocks/CollectorPEAA.java` |
| `Collector MK5` | `pe_collector_MK5` | Energy Collector MK5 | EMCコレクター MK5 | EMC 生成 + GUI | 同上（tier=5） |
| `AEGU MK1` | `AEGU_MK1` | Alchemical Energy Generating Unit | 錬金術的エネルギー生成装置 | 装飾兼 EMC ソース（停止形） | `blocks/AEGU.java` |
| `AEGU MK1_on` | `AEGU_MK1` | （同上・稼働形） | （同上） | 稼働形 | 同上 |
| `AEGU MK2` | `AEGU_MK2` | Advanced AEGU | 発展型AEGU | 停止形 | 同上 |
| `AEGU MK2_on` | `AEGU_MK2` | （稼働形） | （稼働形） | 稼働形 | 同上 |
| `AEGU MK3` | `AEGU_MK3` | Ultimate AEGU | 究極型AEGU | 停止形 | 同上 |
| `AEGU MK3_on` | `AEGU_MK3` | （稼働形） | （稼働形） | 稼働形 | 同上 |

登録は `gameObjs/ObjHandlerPEAA.java:36-44`。
`_on` 変種は `setCreativeTab` を呼ばない（`blocks/AEGU.java:35-36`）ためクリエイティブタブに出ない。
`_on` は専用の言語エントリを持たない（en_US.lang / ja_JP.lang に `AEGU_MK1` しかない）が、
`setBlockName` は `_off`/`_on` とも同じ `AEGU_MK{tier}` なので表示名は共通になる（`blocks/AEGU.java:37`）。

**1.21.1 提案**: `_off`/`_on` の 2 ブロックは 1 ブロック + BlockState `BooleanProperty generating` に統合する。

### 2.2 ProjectE 側のブロックを差し替えるもの（ASM 経由・独自 registry 名なし）

| PEAA クラス | 置き換える ProjectE の対象 | 元ソース |
|---|---|---|
| `CondenserMK2PEAA` | `ObjHandler.condenserMk2`（Energy Condenser MK2） | `blocks/CondenserMK2PEAA.java` |
| `MatterFurnacePEAA(false,true)` | `ObjHandler.rmFurnaceOff` | `blocks/MatterFurnacePEAA.java` |
| `MatterFurnacePEAA(true,true)` | `ObjHandler.rmFurnaceOn` | 同上 |

### 2.3 アイテム

| 内部名（registry） | 非ローカライズ名 | 表示名 (en) | 表示名 (ja) | 種類 | 元ソース |
|---|---|---|---|---|---|
| `ring_space_teleport` | `pe_RingFlightTeleport` **[推測]**（1.7.10 の `ItemPE` が `pe_` を前置） | Ring of the Space | 司空の指輪 | チャージ式・飛行 + テレポート | `items/RingFlightTeleport.java` |

`ObjHandlerPEAA.java:51` で登録。

### 2.4 ProjectE 側のアイテムを差し替えるもの

| PEAA クラス | 置き換える対象 | 元ソース |
|---|---|---|
| `EvertideAmuletPEAA` | `ObjHandler.everTide`（水の祭器） | `items/EvertideAmuletPEAA.java` |
| `GemFeetPEAA` | `ObjHandler.gemFeet`（ジェムブーツ） | `items/armor/GemFeetPEAA.java` |

### 2.5 BlockEntity（TileEntity）

| 登録名 | クラス | 登録場所 |
|---|---|---|
| `Energy Collector MK4 Tile` | `tiles/CollectorMK4Tile` | `ObjHandlerPEAA.java:47` |
| `Energy Collector MK5 Tile` | `tiles/CollectorMK5Tile` | `ObjHandlerPEAA.java:48` |
| `CondenserMK2 Tile  PEAA`（スペース2個） | `tiles/CondenserMK2TilePEAA` | ASM `asm/transform/ObjHandlerTransformer.java:168-170` |
| `RM Furnace Tile PEAA` | `tiles/RMFurnaceTilePEAA` | ASM `ObjHandlerTransformer.java:173-175` |

### 2.6 エンティティ

| 登録名 | クラス | ID | 追跡範囲 | 更新頻度 | 速度同期 | 登録場所 |
|---|---|---|---|---|---|---|
| `Water Water PEAA` | `entity/EntityWaterProjectilePEAA` | 10 | 256 | 10 | true | ASM `ObjHandlerTransformer.java:178-185` |

加えて `ObjHandlerTransformer.java:189-195` で、ProjectE 自身の `register()` 内にある
`EntityWaterProjectile.class` の定数参照を `EntityWaterProjectilePEAA.class` に置換している。

### 2.7 GUI / Container

| GUI ID | Container | Screen | 元ソース |
|---|---|---|---|
| 30 (`COLLECTOR4_GUI`) | `container/CollectorMK4Container` | `gui/GUICollectorMK4` | `utils/ConstantsPEAA.java:10` |
| 31 (`COLLECTOR5_GUI`) | `container/CollectorMK5Container` | `gui/GUICollectorMK5` | `utils/ConstantsPEAA.java:11` |

`utils/GuiHandlerPEAA.java` が `IGuiHandler` として振り分け。

### 2.8 アーマー

新規アーマーは無し。`items/armor/GemFeetPEAA.java` が ProjectE のジェムブーツを置換するのみ。

---

## 3. 機能ごとの詳細仕様

### 3.1 Energy Collector MK4 / MK5

#### 3.1.1 数値

`utils/ConstantsPEAA.java:5-8`
```
COLLECTOR_MK4_MAX = 60000   // EMC 容量
COLLECTOR_MK5_MAX = 60000   // EMC 容量
COLLECTOR_MK4_GEN = 320     // 生成レート
COLLECTOR_MK5_GEN = 1280    // 生成レート
```

`tiles/CollectorMK4Tile.java:21` / `tiles/CollectorMK5Tile.java:8`
```
super(MAX, GEN, 17, 18)   // (maxEmc, emcGen, upgradedSlot, lockSlot)
```

生成式（ProjectE 側、1.21.1 `CollectorMK1BlockEntity.java:193`）:
```
unprocessedEMC += emcGen * (getSunLevel() / 320.0f)   // 毎 tick
```
`getSunLevel()` の最大は 16 なので、最大時 `emcGen * 16/320 = emcGen/20` EMC/tick = **`emcGen` EMC/秒**。
→ MK4 = 320 EMC/s、MK5 = 1280 EMC/s。`events/ToolTipEvent.java:31,37` のツールチップ表記と一致。

比較: 1.21.1 の MK1=4 / MK2=12 / MK3=40 EMC/s、容量 10,000 / 30,000 / 60,000
（`refs/projecte/.../gameObjs/EnumCollectorTier.java:8-10`）。
→ **MK4 は MK3 の 8 倍、MK5 は 32 倍。容量は MK3 と同じ 60,000**。

#### 3.1.2 挙動（MK1〜MK3 との唯一の差分）

`tiles/CollectorMK4Tile.java:32-39`
```java
public int getSunLevel() {
    if (worldObj.getBlock(xCoord, yCoord + 1, zCoord) instanceof AEGU) return 16;
    return 0;
}
```
- **明るさを一切見ない**。真上（y+1）のブロックが AEGU（MK1/MK2/MK3、稼働形・停止形問わず）なら光レベル 16 相当、それ以外は 0。
- つまり MK4/MK5 は「AEGU を真上に置いたときのみフル稼働、それ以外は完全停止」。AEGU 側が稼働中かどうかは**見ていない**。
- ネザー判定（1.21.1 の `level.dimensionType().ultraWarm()` → 16）は上書きにより消える。

それ以外（燃料アップグレード、クラインの星充填、隣接 IEmcStorage への送出、リレーボーナス）は
ProjectE の `CollectorMK1Tile` / `CollectorMK1BlockEntity` の実装をそのまま継承する。

#### 3.1.3 ブロック挙動

`blocks/CollectorPEAA.java`
- `super(3)` で ProjectE の MK3 コレクターとして構築（L26）。MK3 と同じ硬度・光レベル・描画。
- `onBlockActivated`（L32-46）: サーバ側でのみ tier に応じて GUI 30 / 31 を開く。戻り値は常に `true`。
- `createTileEntity`（L49-62）: tier 4 → `CollectorMK4Tile`、tier 5 → `CollectorMK5Tile`。
- テクスチャ（L66-71）: `peaa:collectors/other_{tier}`（側面）、`peaa:collectors/front`（正面）、`peaa:collectors/top_{tier}`（上面）。
- `getIcon`（L75-88）: `meta==0 && side==3` → front、`side==1` → top、`side != meta` → 側面、それ以外 → front。
  （= ProjectE のコレクターと同じ「向き付き」ロジック。1.21.1 では `BlockDirection` + blockstate JSON で表現）

#### 3.1.4 状態として保持するデータ

BlockEntity 側（**1.21.1 でも BlockEntity**）:

| データ | 型 | 備考 |
|---|---|---|
| 貯蔵 EMC | long | `BaseEmcBlockEntity` が持つ |
| `unprocessedEMC` | double | 端数 |
| 入力スロット 16 個 | `ItemStackHandler` | 燃料アップグレード用 |
| aux スロット 3 個 | `ItemStackHandler` | UPGRADING(0) / UPGRADE(1) / LOCK(2) |

アイテム側に持たせるデータは無い（Data Components 不要）。

#### 3.1.5 GUI のスロット構成

`container/CollectorMK4Container.java:27-47`（MK5 も完全に同一 / `CollectorMK5Container.java:26-46`）

| スロット index | 役割 | 座標 (x, y) |
|---|---|---|
| 0 | クラインの星 / 燃料投入（UPGRADING） | 158, 58 |
| 1〜16 | 燃料ストレージ 4×4 | `18 + i*18`, `8 + j*18`（i,j = 0..3） |
| 17 | アップグレード結果（取り出し専用） | 158, 13 |
| 18 | ロック（ゴーストスロット） | 187, 36 |
| 19〜45 | プレイヤーインベントリ 3×9 | `30 + j*18`, `84 + i*18` |
| 46〜54 | ホットバー 9 | `30 + i*18`, 142 |

**これは ProjectE 1.21.1 の `CollectorMK3Container`（`refs/projecte/.../container/CollectorMK3Container.java:23-35`）と座標まで完全一致**。
→ 1.21.1 では `CollectorMK3Container` をそのまま流用（or 継承）できる。

`transferStackInSlot`（L87-129）:
- slotIndex ≤ 18 → プレイヤー側 19..54 へ移動
- slotIndex 19..54 → 燃料かつ最大燃料でない場合のみ 1..16 へ
- `canInteractWith` は常に `true`（L132-135。距離チェック無し）

#### 3.1.6 GUI の描画・表示値

`gui/GUICollectorMK4.java`（MK5 も同一 / `GUICollectorMK5.java`）
- テクスチャ: `peaa:textures/gui/collector3.png`（L17）
- サイズ: `xSize = 218`, `ySize = 165`（L24-25）
- 文字（L31-36）: 貯蔵 EMC を (91, 32)、クラインの星の充電量を (91, 44)、色 `0x404040`
- バー（L51-64）:
  - 光レベル（最大 12）: `(x+160, y+49-progress)` ← `(220, 13-progress)` サイズ `12 × progress`
  - EMC 貯蔵（最大 48）: `(x+98, y+18)` ← `(0, 166)` サイズ `progress × 10`
  - クラインの星の充電（最大 48）: `(x+98, y+58)` ← `(0, 166)` サイズ `progress × 10`
  - 燃料進捗（最大 24）: `(x+172, y+55-progress)` ← `(219, 38-progress)` サイズ `10 × (progress+1)`

**1.21.1 の `AbstractCollectorScreen.MK3`（`refs/projecte/.../gui/AbstractCollectorScreen.java:96-117`）と同一**
（MK3 は `getBonusXShift()=34`, `getTextureBonusXShift()=43` → `64+34=98`, `126+34=160`, `138+34=172`, `177+43=220`, `176+43=219`）。
→ 1.21.1 では `AbstractCollectorScreen` を継承して同じ値を使えばよい。

#### 3.1.7 クライアント⇔サーバー間でやり取りするデータ

`container/CollectorMK4Container.java:51-77`
- progress bar id 0: `tile.displaySunLevel`（GUI を開いた瞬間の初期値）
- progress bar id 1: `tile.getSunLevel()`（毎 tick、変化時のみ送信）
- `updateProgressBar` は id を見ず、常に `tile.displaySunLevel` に代入している（L73-77）。
- その他（貯蔵 EMC、クライン充電、燃料進捗）は ProjectE 側 `CollectorMK1Tile` の同期に依存 **[推測]**。

**1.21.1 提案**: `CollectorMK1Container` の `DataSlot`（`sunLevel` / `kleinChargeProgress` / `fuelProgress`）と
`BoxedLong`（`emc` / `kleinEmc`）機構がそのまま使える。独自パケットは不要。

#### 3.1.8 config で変更できる項目
なし。

---

### 3.2 AEGU（Alchemical Energy Generating Unit）

`blocks/AEGU.java`。**AEGU は単体では何もしない**。Energy Condenser MK2 を 26 個で取り囲むと稼働する。

#### 3.2.1 ブロック定義

`blocks/AEGU.java:33-44`
```
Material.grass
setBlockName("AEGU_MK" + tier)
setHardness(0.3F)
setLightLevel(1.0F)          // 光源レベル 15
setStepSound(soundTypeGlass)
setCreativeTab(ObjHandler.cTab)   // isGenerate == false のときだけ
```
`getItemDropped`（L126-137）: tier に応じて常に `_off` 版のアイテムをドロップ。
テクスチャ（L105-108）: `peaa:aegu/aegu{tier}` / 稼働形は `peaa:aegu/aegu{tier}gene`。

#### 3.2.2 生成レート

`tiles/CondenserMK2TilePEAA.java:28`
```java
private final int[] generateEmcOfAEGU = {0, 40, 1000, 20000};  // index = tier
```

| tier | EMC/秒 | ツールチップ（`events/ToolTipEvent.java:46,51,56`） |
|---|---|---|
| MK1 | 40 | `Generation Rate: 40 EMC/s` |
| MK2 | 1000 | `Generation Rate: 1000 EMC/s` |
| MK3 | 20000 | `Generation Rate: 20000 EMC/s` |

実際の加算は `tiles/CondenserMK2TilePEAA.java:54`
```java
this.addEMC((double)generateEmc / 20);   // 毎 tick
```
→ `generateEmc` は「合計 EMC/秒」。**理論最大 = 26 × 20000 = 520,000 EMC/s**（＝ 26,000 EMC/tick）。

#### 3.2.3 設置時の処理

`blocks/AEGU.java:47-56` `onBlockPlaced`（サーバ側のみ）
1. `checkMK2()`（L142-169）: **自分を中心とする 3×3×3（自分自身を含む 27 マス）** を走査し、`CondenserMK2PEAA` を探す。
   - 1 個だけ見つかった → `true`
   - 2 個以上 → `false`（登録しない）
   - 0 個 → `false`
2. `true` なら `notifyToMK2()`（L175-184）: その座標の `CondenserMK2TilePEAA` に `storeAEGUCoord(this, x, y, z)` を呼ぶ。

**注意**: `setBlock` による設置（＝稼働形⇔停止形の切り替え）では `onBlockPlaced` は呼ばれない（L48 のコメント）。これが `changeGenerate` で登録が壊れない理由。

#### 3.2.4 破壊時の処理

`blocks/AEGU.java:96-102` `onBlockDestroyedByPlayer` → `notifyBreak()`（L189-212）
- 3×3×3 を走査し、`CondenserMK2PEAA` かつ `CondenserMK2TilePEAA` を持つブロックを見つけたら
  `tile.destoreAEGUCoord(this, x, y, z)` を呼び、成功したら即 `true` を返して終了。

#### 3.2.5 右クリック

`blocks/AEGU.java:59-66` → `openMK2GUI()`（L217-236）
- 3×3×3 を走査し、`CondenserMK2TilePEAA` を探す。
- その MK2 に自分の座標が登録されていて（`checkStore(x,y,z) != -1`）、かつ `tile.isGenerate == true` なら
  **その MK2 コンデンサーの GUI を開く**（`Constants.CONDENSER_MK2_GUI`）。
- `onBlockActivated` の戻り値は `isGenerate`（稼働形なら true）。

#### 3.2.6 稼働形／停止形の切り替え

`blocks/AEGU.java:68-93` `changeGenerate(world, x, y, z)`
- tier に応じて `world.setBlock(x, y, z, aeguMK{tier}_{on|off})` で**ブロック自体を差し替える**。
- 呼び出し元は `CondenserMK2TilePEAA.changeGenerate(boolean)`（`tiles/CondenserMK2TilePEAA.java:430-448`）のみ。

**1.21.1 提案**: `BlockState` の `BooleanProperty GENERATING` にして `level.setBlockAndUpdate` で切り替える。

#### 3.2.7 状態として保持するデータ

AEGU ブロック自身は**インスタンス変数 `xMK2 / yMK2 / zMK2`（String）を持つが、これは検索用の一時変数**であり
ブロックインスタンスは全座標で共有されるため状態としては機能していない（`blocks/AEGU.java:31`）。
**永続データは一切持たない**（BlockEntity なし、NBT なし）。
稼働状態は「どのブロック ID か」でのみ表現される。

**1.21.1 提案**: 稼働状態は BlockState、AEGU の座標リストは Condenser 側の BlockEntity に持たせる（現行と同じ）。

#### 3.2.8 config
なし。

---

### 3.3 Energy Condenser MK2 の拡張（`CondenserMK2PEAA` / `CondenserMK2TilePEAA`）

ProjectE の `ObjHandler.condenserMk2` を ASM で `CondenserMK2PEAA` に差し替えることで有効になる。

#### 3.3.1 ブロック側（`blocks/CondenserMK2PEAA.java`）

| メソッド | 挙動 |
|---|---|
| `getItemDropped`（L34-37） | ProjectE の `condenserMk2` アイテムをドロップ（= 見た目上 ProjectE 本体のブロックのまま） |
| `getRenderType`（L40-43） | `Constants.CONDENSER_MK2_RENDER_ID`（ProjectE 側の描画 ID）を維持 |
| `createTileEntity`（L46-54） | `CondenserMK2TilePEAA` を生成。サーバ側なら `checkAroundAEGU(world, x, y, z)` を呼ぶ |
| `onBlockActivated`（L57-66） | サーバ側で `Constants.CONDENSER_MK2_GUI` を開く |
| `onBlockPlaced`（L69-75） | ブロックインスタンスのフィールド `x/y/z` に設置座標を保存 |
| `breakBlock`（L78-111） | スロット 1 以降を全ドロップ → 生成モードだったら `changeGenerate(false)` で周囲 AEGU を停止形に戻す |
| `registerBlockIcons`（L115-118） | `blockIcon` を vanilla の `obsidian` に設定 |

**[要確認] バグ注意**: `createTileEntity` が使う `x/y/z` は `onBlockPlaced` でブロックインスタンス（全座標共有）に書かれた値。
ワールド再読み込み時など `onBlockPlaced` を経由しない場合は古い／初期値 (0,0,0) が使われる。
→ 1.21.1 では `BlockEntity#onLoad()` / `setLevel()` で自分の `worldPosition` を使って周囲探索すべき。

#### 3.3.2 EMC 生成（AEGU 連携）

`tiles/CondenserMK2TilePEAA.java`

保持データ（L28-32）:
```java
private final int[] generateEmcOfAEGU = {0, 40, 1000, 20000};
private String[][] coordAEGU = new String[3][26];  // [0]=x, [1]=y, [2]=z、最大 26 個
private int numAEGU = 0;
public boolean isGenerate = false;
int generateEmc = 0;
```

NBT（L159-195）:

| キー | 型 | 内容 |
|---|---|---|
| `IsGenerate` | boolean | 稼働中か |
| `NumAEGU` | int | 登録済み AEGU 個数 |
| `GenerateEmc` | int | 合計生成レート (EMC/s) |
| `coordAEGUX{i}` / `coordAEGUY{i}` / `coordAEGUZ{i}` | String | i = 0..25 の AEGU 座標（文字列） |

毎 tick（`updateEntity`、L41-58、サーバ側のみ）:
1. `super.updateEntity()`（ProjectE の凝縮処理）
2. 自分のブロックが `CondenserMK2` なら `pushToInventories()`
3. `checkGenerate()`
4. `isGenerate` なら `addEMC(generateEmc / 20.0)`

`checkGenerate()`（L389-403）:
```
numAEGU < 0 || numAEGU > 26  → 何もしない
numAEGU >= 25               → (未稼働 && checkCanUseAEGU()==false) なら return
                              → changeGenerate(true); isGenerate = true
それ以外 && isGenerate      → changeGenerate(false); isGenerate = false
```
→ **稼働条件: 自分を囲む 26 マスのうち 25 個以上が AEGU**。

`checkCanUseAEGU()`（L411-428）: 登録済み AEGU のどれか 1 つでも既に稼働形（= 他の MK2 に使われている）なら `false`。
`changeGenerate(bool)`（L430-448）: 登録済み全 AEGU の稼働形／停止形を `bool` に合わせる。
`checkAroundAEGU(world,x,y,z)`（L479-490）: 3×3×3 を走査して AEGU を全部登録。
`addCoord` / `decCoord` / `checkStore`（L338-369, L458-470）: 配列の空きスロット管理。

**[要確認]** `checkStore`（L461-467）は `coordAEGU[0][i]` が `null` のとき `NullPointerException` になりうる
（`addCoord` は `null` チェックしているが `checkStore` はしていない）。
NBT から読み戻した直後は `getString` が `""` を返すので実害は出にくい。

#### 3.3.3 凝縮処理の差し替え（`condense`）

`tiles/CondenserMK2TilePEAA.java:61-102`
- 入力スロット `INPUT_SLOTS = {1, 42}`、出力スロット `OUTPUT_SLOTS = {43, 84}`（L23-24）。スロット 0 はロック。
- 1 tick で 1 スロット分を処理し `break`。
- **スタック単位で一括変換**: `addEMC(EMCHelper.getEmcValue(stack) * stack.stackSize)` してスロットを空にする（L85-86）。
- オーバーフロー判定（L72）: `(long)emcValue * stackSize + (int)storedEmc > maxEmc` なら 1 個ずつ変換（L80-82）。
- 出力側は `while (hasSpace() && storedEmc >= requiredEmc) { pushStack(); removeEMC(requiredEmc); }`（L97-101）。

**1.21.1 では既に同等**: `CondenserMK2BlockEntity.condense()`
（`refs/projecte/.../block_entities/CondenserMK2BlockEntity.java:55-70`）が
`getSellValue(stack) * stack.getCount()` の一括変換 + `while` ループを実装済み。
EMC も `long` なのでオーバーフロー対策も不要。**この改変は 1.21.1 では作業不要**。

#### 3.3.4 隣接インベントリへの自動排出（MK2 のみ）

`tiles/CondenserMK2TilePEAA.java:199-304` `pushToInventories()`
- 対象方向: `ForgeDirection.VALID_DIRECTIONS` のうち **`offsetY > 0`（＝ UP）を除く 5 方向**（L203-207）。
- 除外ブロック: `Relay`（反物質リレー）と `MatterFurnace`（マターかまど）は対象外（L222-226）。
- 相手が `ISidedInventory` → `getAccessibleSlotsFromSide` を取り、出力スロット `43..83`（`j < outputStorage[1]`）を押し込む。
- 相手が `IInventory` → `ItemHelper.pushStackInInv` で `43..84` を押し込む。
- **[要確認] バグ**: `iSide` が常に 0 のまま（L201、ループ内で更新されない）ので、
  `ISidedInventory` に対しては常に `ForgeDirection.OPPOSITES[0]`（= UP）の面として扱われる。
- **[要確認] バグ**: `ISidedInventory` 側のループが `j < outputStorage[1]`（= 84 未満）なので最後のスロット 84 が排出されない。

**1.21.1 提案**: `IItemHandler` capability + `ItemHandlerHelper.insertItemStacked` で書き直し、
方向は「UP 以外の 5 方向」を素直に実装する。除外は「相手が ProjectE の Relay / MatterFurnace ブロックか」で判定。

#### 3.3.5 その他のオーバーライド
- `hasSpace()` / `getSlotForStack()`（L105-144）: 出力スロット `43..84` を見るように変更。
- `isItemValidForSlot`（L147-155）: スロット 0 と 43 以上は不可。ロック品と同一でなく EMC を持つアイテムのみ可。

#### 3.3.6 config
なし。

---

### 3.4 マターかまどの拡張（`MatterFurnacePEAA` / `RMFurnaceTilePEAA`）

#### 3.4.1 ブロック（`blocks/MatterFurnacePEAA.java`）

```java
createTileEntity → isHighTier(=isRM) ? new RMFurnaceTilePEAA() : new DMFurnaceTile()
```
ASM で `rmFurnaceOff` / `rmFurnaceOn` だけが `MatterFurnacePEAA(active, true)` に差し替えられるため、
実際に生成されるのは常に `RMFurnaceTilePEAA`。
一方 DM かまどは `DMFurnaceTileTransformer` によって `DMFurnaceTile extends RMFurnaceTilePEAA` にされる
（§5.2）ので、結果として **DM / RM 両方のかまどが PEAA 挙動になる**。

#### 3.4.2 数値

`tiles/RMFurnaceTilePEAA.java:22`
```java
private final float EMC_CONSUMPTION = 1.6f;   // 毎 tick 消費 EMC
```
（1.21.1 の `DMFurnaceBlockEntity.java:73` は `EMC_CONSUMPTION = 2`。**[推測]** 1.7.10 も 2 だったものを 0.8 倍に緩和している）

`getItemBurnTime`（L519-523）
```java
(TileEntityFurnace.getItemBurnTime(stack) * ticksBeforeSmelt) / 200 * efficiencyBonus
```
（1.21.1 `DMFurnaceBlockEntity.java:402` と同一式）

#### 3.4.3 毎 tick の処理（`updateEntity`、L29-117）

1. `furnaceBurnTime > 0` なら `--furnaceBurnTime`
2. サーバ側: `pullFromInventories()` → `pushSmeltStack()`
3. スロット 0 がクラインの星で EMC ≥ 1.6 → 星から 1.6 抜いて自分に加算（L47-54）
4. 貯蔵 EMC ≥ 1.6 → `furnaceBurnTime = 1`、EMC を 1.6 消費（L56-60）
   → **EMC があれば燃料なしで永久稼働**
5. `furnaceBurnTime == 0 && canSmelt()` → 通常燃料を消費して着火（L62-80）
6. `furnaceBurnTime > 0 && canSmelt()` → `++furnaceCookTime`、`ticksBeforeSmelt` に達したら `smeltItem()`
7. 燃焼状態が変わったらブロックの on/off を切替（L95-104）
8. サーバ側: `pushOutput()` → `pushToInventories()`

#### 3.4.4 鉱石の倍化

`tiles/RMFurnaceTilePEAA.java:466-487` `smeltItem()`
```java
if (ItemHelper.getOreDictionaryName(toSmelt).startsWith("ore")) {
    smeltResult.stackSize *= 2;      // 確率ではなく 100% 倍化
}
```
1.21.1 では RM かまどが `getOreDoubleChance() = 1F`（`RMFurnaceBlockEntity.java:26-28`）、
DM かまどが `0.5F`（`DMFurnaceBlockEntity.java:174-176`）。
→ **RM は 1.21.1 と同等。DM が 0.5 → 1.0 になるのが PEAA の差分**。

#### 3.4.5 自動搬入（`pullFromInventories`、L198-356）
- 対象: **真上 (y+1)** のインベントリのみ。
- `ISidedInventory` なら `getAccessibleSlotsFromSide(0 = DOWN)` から `canExtractItem` のものだけ。
- 燃料 or クラインの星 → スロット 0 へ。
- それ以外 → 入力ストレージ `inputStorage[0] .. inputStorage[1]-1` へ。
  （`IInventory` 経路では `FurnaceRecipes.smelting().getSmeltingResult(stack) == null` のものはスキップ、L322-325）

#### 3.4.6 自動搬出（`pushToInventories`、L359-464）
§3.3.4 とまったく同じロジック（UP 以外の 5 方向、Relay と MatterFurnace は除外、`iSide` 固定バグも同じ）。

#### 3.4.7 内部整理
- `pushSmeltStack()`（L119-155）: 入力ストレージ → 精錬スロット 1 へ詰める。
- `pushOutput()`（L157-196）: 精錬結果スロット → 出力ストレージへ詰める。

**1.21.1 では既に同等**: `DMFurnaceBlockEntity` が `pullFromInventories`（上から、L303-317）/
`pushToInventories`（下へ、L319-332）/ `CompactableStackHandler.compact()` を実装済み。
差分は「排出先が DOWN のみ vs UP 以外の 5 方向」「ホッパーを除外するか」だけ。

#### 3.4.8 config
なし。

---

### 3.5 司空の指輪 (Ring of the Space) — `RingFlightTeleport`

#### 3.5.1 基本定義

`items/RingFlightTeleport.java:62-68`
```java
super("RingFlightTeleport", (byte)3);   // ItemCharge、チャージ段階数 3（= charge 0..3 の 4 段階）
setNoRepair();
setMaxDamage(0);
setCreativeTab(ObjHandler.cTab);
```

#### 3.5.2 数値

`items/RingFlightTeleport.java:50-54`
```java
flyBaseSpeed = 0.022f
flySpeed = { 0.022, 0.044, 0.088, 0.176 }      // = base * {1, 2, 4, 8}
flyBaseEmc  = 0.16F
flyEmc   = { 0.16, 0.32, 0.64, 1.28 }          // = base * {1, 2, 4, 8}  EMC/tick
```

| charge | 飛行速度係数 | 飛行中 EMC 消費 (EMC/tick) | (EMC/秒) |
|---|---|---|---|
| 0 | 0.022 | 0.16 | 3.2 |
| 1 | 0.044 | 0.32 | 6.4 |
| 2 | 0.088 | 0.64 | 12.8 |
| 3 | 0.176 | 1.28 | 25.6 |

その他の定数:
- 飛行可能判定に必要な EMC: **256**（`onUpdate`、L98）
- テレポート 1 回のコスト: **3072 EMC**（`onItemRightClick`、L126,130）
- テレポート最大距離: **30.0 ブロック**（`setTeleportPoint`、L212）
- 上下移動係数: `moveFactor = 0.4F`（`proxies/ClientProxy.java:20`）
- 水平移動倍率: 通常 `flyingSpeed * 10.0F`、スニーク中 `flyingSpeed * 35.0F`（`ClientProxy.java:122`）
- ジャンプ 2 度押し判定のウィンドウ: **7 tick**（`ClientProxy.java:55`）

#### 3.5.3 毎 tick の処理（`onUpdate`、L71-119。**サーバ側のみ**）

```
1. world.isRemote なら即 return
2. NBT が無ければ作る
3. setIsHeld(stack, isHeldItem)
4. invSlot > 8 または entity がプレイヤーでない
      → canFlying = false、isFlyingMode = false にして return
        （= ホットバー 0..8 に無いと機能しない）
5. getEmc(stack) <= 0 かつ consumeFuel(player, stack, 256, false) == false
      → canFlying = false にして return
6. canFlying = true
7. flyingSpeed = flySpeed[getCharge(stack)]
8. isFlyingMode なら removeEmc(stack, flyEmc[getCharge(stack)])
9. invSlot を NBT に保存
```

#### 3.5.4 右クリック（テレポート）（`onItemRightClick`、L122-134）

```
サーバ側: isCharge = consumeFuel(player, stack, 3072, false)
teleportPlayer(world, player) が成功 かつ サーバ側 かつ isCharge
      → removeEmc(stack, 3072)
```
`teleportPlayer` は**両側で呼ばれる**（クライアント側ではパーティクル演出のため）。

`setTeleportPoint`（L210-278）:
1. プレイヤー視点 = `(posX, posY + 1.62, posZ)`
2. 視線ベクトル × 30.0 を終点として `world.rayTraceBlocks(start, end, false)`
3. ブロックにヒットしなければ `null`（テレポートしない）
4. ヒットした面 `sideHit` の `ForgeDirection` を取り、
   `(blockX + 0.5 + dx, blockY + dy, blockZ + 0.5 + dz)` を着地点にする
5. `sideHit == 0`（下面）の場合は `blockPosY--`

`teleportToChunkCoord`（L280-291）:
- サーバ側: `fallDistance = 0`
- クライアント側: `portal` パーティクルを 32 個スポーン
- `entityplayer.setPositionAndUpdate(x, y, z)`

（ソース中にコメントアウトされた EnderIO 風テレポート実装あり L237-277, L293-325。未使用）

#### 3.5.5 ワールドにドロップされたとき（`onEntityItemUpdate`、L140-154）
`canFlying` / `isFlyingMode` をともに `false` にする。

#### 3.5.6 飛行処理（クライアント側）

`proxies/ClientProxy.java:35-97` `doFlightOnSide(player, isFlying, flyingSpeed, isHold)`

```
allowFlying = !(capabilities.isCreativeMode || capabilities.allowFlying)
  → クリエイティブ、または他 MOD/ProjectE(SWRG など)でクリエイティブ飛行が許可されている場合は
    isFlying = false を返して何もしない

jump = movementInput.jump（更新前の値）
var3 = (movementInput.moveForward >= 0.8F)（更新前）
movementInput.updatePlayerMoveState()      ← 手動で入力ステート更新

allowFlying && !jump && movementInput.jump（= ジャンプを押し直した）
  → flyToggleTimer == 0 なら flyToggleTimer = 7
    そうでなければ isFlying を反転して flyToggleTimer = 0
    （＝ 7 tick 以内のジャンプ 2 度押しで飛行 ON/OFF）

[ダッシュ再実装] onGround && !var3 && moveForward >= 0.8F && !isSprinting()
                && 満腹度 > 6.0 && !isUsingItem() && !盲目
  → sprintToggleTimer == 0 なら 7 にセット、そうでなければ setSprinting(true)
  （updatePlayerMoveState を手動で呼ぶため vanilla のダッシュ判定が働かなくなるのを補う）

タイマーを減算

player.onGround && isFlying → isFlying = false（着地で解除）

isFlying なら movePlayerY() と movePlayerXZ()
```

`movePlayerY`（L99-111）
```
motionY = 0
sneak && !isHold  → motionY -= 0.4
jump              → motionY += 0.4
```
（`isHold` = 指輪を手に持っている場合はスニーク下降しない）

`movePlayerXZ`（L113-123）
```
moveForward != 0 || moveStrafe != 0 || flyingSpeed == 0.022f
  → motionX = motionZ = 0
moveFlying(moveStrafe, moveForward, sneak ? flyingSpeed*35.0F : flyingSpeed*10.0F)
```

#### 3.5.7 イベントフック（`events/FlightEventHookPEAA.java`）

| イベント | 処理 |
|---|---|
| `LivingUpdateEvent`（L30-44） | **クライアント側のみ**。プレイヤーのホットバーから指輪を取得し、`canFlying` なら `Flight()` を実行。`canFlying` でないのに `isFlyingMode` なら false に戻す |
| `LivingFallEvent`（L58-68） | 両側。ホットバーに `canFlying` な指輪があれば `event.setCanceled(true)`（**落下ダメージ完全無効**） |

`Flight()`（L46-54）:
`PEAACore.proxy.doFlightOnSide(player, isFlyingMode, flyingSpeed, isHeld)` を呼び、
戻り値が変わっていれば `syncIsFlyingMode()` で同期。

`syncIsFlyingMode()`（L78-85）:
状態が変化したときのみ、NBT を更新して `IsFlyingModeSyncPKT(invSlot, isFlying)` をサーバへ送信。

`getStack(player)`（L93-142）:
- ホットバー 0..8 を走査。
- NBT が無ければ作る。
- 最初に見つかった指輪を採用。以降、飛行モード中の指輪を見つけたら、それまでの候補が飛行モードでなければ差し替える。
- **飛行モード中の指輪が 2 つあると `InvalidParameterException` を投げる**（L126-127）。

#### 3.5.8 クライアント⇔サーバー間でやり取りするデータ

`network/IsFlyingModeSyncPKT.java`

| フィールド | 型 | 書式 |
|---|---|---|
| `invSlot` | int | `buf.writeInt` |
| `isFlying` | boolean | `buf.writeBoolean` |

登録（`network/PacketHandlerPEAA.java:16-17`）:
- discriminator 1, `Side.CLIENT` → `IsFlyingModeSyncPKTHandlerToClient`
- discriminator 0, `Side.SERVER` → `IsFlyingModeSyncPKTHandlerToServer`

サーバ受信（`IsFlyingModeSyncPKTHandlerToServer.java:16-24`）:
```java
player.inventory.mainInventory[pkt.getInvSlot()] が指輪なら
    stackTagCompound.setBoolean("isFlyingMode", pkt.getIsFlying())
    PacketHandlerPEAA.INSTANCE.sendToAll(new IsFlyingModeSyncPKT(...))   // ★ 全プレイヤーへブロードキャスト
```
クライアント受信（`IsFlyingModeSyncPKTHandlerToClient.java:16-23`）:
自分のプレイヤーの `mainInventory[invSlot]` が指輪なら `isFlyingMode` を上書き。

**[要確認] バグ**: `sendToAll` なので、マルチプレイでは**他プレイヤーの同じスロット番号の指輪まで状態が書き換わる**。
1.21.1 では該当プレイヤーにのみ返すべき。

#### 3.5.9 NBT（1.21.1 では Data Components にすべき項目）

`items/RingFlightTeleport.java:333-378`

| NBT キー | 型 | 意味 | 1.21.1 提案 |
|---|---|---|---|
| `canFlying` | boolean | 飛行可能状態（EMC がある / ホットバーにある） | **Data Component 不要**。毎 tick 再計算できる。クライアント側判定用に必要なら `DataComponent<Boolean>` |
| `isHeld` | boolean | 手に持っているか | 同上（`inventoryTick` の `isHeld` 引数から取得可能） |
| `flyingSpeed` | float | 現在の飛行速度 | **不要**。`charge` から算出可能 |
| `isFlyingMode` | boolean | 飛行 ON/OFF | **Data Component 必須**（クライアントが変更 → サーバへ同期する唯一の永続状態） |
| `invSlot` | int | 所持スロット番号 | **不要**（パケットに載せればよい） |
| （ProjectE 側）EMC | – | `ItemCharge` の貯蔵 EMC | `PEDataComponentTypes.STORED_EMC`（long） |
| （ProjectE 側）charge | – | チャージ段階 0..3 | `PEDataComponents.CHARGE`（int） |

#### 3.5.10 キー操作

| キー | 動作 |
|---|---|
| ジャンプ 2 度押し（7 tick 以内） | 飛行モード ON/OFF |
| ジャンプ | 上昇（+0.4/tick） |
| スニーク（手に持っていないとき） | 下降（-0.4/tick） |
| スニーク（移動時） | 水平速度 ×3.5 |
| W/A/S/D | 水平移動 |
| 右クリック | 視線先 30 ブロック以内にテレポート（3072 EMC） |
| ProjectE のチャージキー（既定 V / `PEKeybind.CHARGE`） | charge 0..3 を変更（速度と消費が 2 倍ずつ変化） |

#### 3.5.11 config
なし（ただし §3.6 のジェムブーツ連携が `spaceRing` カテゴリ）。

---

### 3.6 ジェムブーツの改変（`items/armor/GemFeetPEAA.java`）

`GemFeet` を継承し、2 メソッドを上書き。

#### 3.6.1 `onArmorTick`（L24-61）

サーバ側:
```java
playerMP.fallDistance = 0;
```

クライアント側:
```java
!capabilities.isFlying && PECore.proxy.isJumpPressed()   → motionY += 0.1
!onGround:
    motionY <= 0                                         → motionY *= 0.90
    !capabilities.isFlying
      && (spaceRing == null
          || (PEAAConfig.isHighSpeedMoveWhenLanding && !RingFlightTeleport.getIsFlyingMode(spaceRing)))
        moveForward < 0                                  → motionX *= 0.9,  motionZ *= 0.9
        moveForward > 0 && |motion|^2 < 3                → motionX *= 1.1,  motionZ *= 1.1
```

**PEAA の差分は追加された条件式（L46-47）のみ**:
ホットバーに司空の指輪があると、ジェムブーツの「空中前進加速」が働かなくなる。
config `isHighSpeedMoveWhenLanding = true` のときは、指輪が飛行モードでなければ加速が働く。
（1.21.1 のバニラ実装は `GemFeet.inventoryTick` L86-102 でこの条件が無いもの）

#### 3.6.2 `canProvideFlight`（L64-68）

```java
return player.getCurrentArmor(0) == stack && spaceRing == null;
```
→ **ホットバーに司空の指輪があるとジェムアーマーによる飛行を与えない**。
（指輪の `ClientProxy.doFlightOnSide` が `capabilities.allowFlying` を見て無効化されるのを防ぐため）

**1.21.1 での扱い**: 1.21.1 の `GemFeet` には `canProvideFlight` に相当するものが存在しない。
飛行は `InternalAbilities.shouldPlayerFly`（`refs/projecte/.../handlers/InternalAbilities.java:80-85`）が
**SWRG とアルカナの指輪のみ**に `NeoForgeMod.CREATIVE_FLIGHT` 属性を付与する形に変わっており、
ジェムアーマーは飛行を与えない。
→ **この改変は 1.21.1 では不要になる可能性が高い [要確認]**。
残るのは §3.6.1 の「空中前進加速の抑制」だけ。

#### 3.6.3 config
`spaceRing.enableHighSpeedMovementAbilityWhenLanding`（既定 `false`）。

---

### 3.7 水の祭器 / 水のオーブ

#### 3.7.1 `EvertideAmuletPEAA`（`items/EvertideAmuletPEAA.java`）

```java
shootProjectile(player, stack):
    if (!world.provider.isHellWorld) {
        world.spawnEntityInWorld(new EntityWaterProjectilePEAA(world, player));
        return true;
    }
    return false;
```
本体と同じ挙動で、**発射するエンティティのクラスだけを PEAA 版に変える**ためのオーバーライド。

1.21.1 の `EvertideAmulet.shootProjectile`（`refs/projecte/.../items/EvertideAmulet.java:84-94`）は
`ProjectEConfig.server.items.opEvertide` で判定し、`shootFromRotation` と効果音も付いている。
ProjectE 自身の実装が進化しているため、PEAA 版の差し替え動機（§3.7.2）だけを再現すればよい。

#### 3.7.2 `EntityWaterProjectilePEAA`（`entity/EntityWaterProjectilePEAA.java`）

`onUpdate`（L33-56、サーバ側のみ）:
```
ワールド内の全ロード済みエンティティを走査
  EntityLavaProjectile を見つけたら
    自分の (posX±1, posY±1, posZ±1) の整数座標グリッドを総当たりし
    (int)enti.posX / posY / posZ と一致するマスがあれば
      this.setDead(); enti.setDead();     // 相殺して両方消滅
```
→ **水のオーブと火のオーブがおよそ 1 ブロック以内ですれ違うと相殺して消える**。

**[要確認]** `loadedEntityList` の全走査は O(n)。1.21.1 では
`level.getEntitiesOfClass(EntityLavaProjectile.class, getBoundingBox().inflate(1))` で置き換えるべき。

#### 3.7.3 config
なし。

---

### 3.8 ツールチップ（`events/ToolTipEvent.java`）

`ItemTooltipEvent` を購読（クライアント側のみ。`proxies/ClientProxy.java:31` で登録）。

| 対象 | 追加行 |
|---|---|
| `collectorMK4` | `Generation Rate: 320 EMC/s` / `Max Storage: 60000 EMC` |
| `collectorMK5` | `Generation Rate: 1280 EMC/s` / `Max Storage: 60000 EMC` |
| `aeguMK1_off` | `Generation Rate: 40 EMC/s` |
| `aeguMK2_off` | `Generation Rate: 1000 EMC/s` |
| `aeguMK3_off` | `Generation Rate: 20000 EMC/s` |

すべてハードコードの英語。ローカライズされていない。

**1.21.1 提案**: `Block#appendHoverText` で実装し、`en_us.json` / `ja_jp.json` に翻訳キーを置く。
ProjectE の `PELang.EMC_MAX_GEN_RATE` / `EMC_MAX_STORAGE`（`refs/projecte/.../blocks/Collector.java:60-66`）に倣う。

---

### 3.9 config 全項目（`config/PEAAConfig.java`）

| カテゴリ | キー | 型 | 既定値 | 説明（原文） | 効果 |
|---|---|---|---|---|---|
| `recipes` | `enableArchangelSmiteRecipe` | boolean | `false` | can create ArchangelSmiteRecipe in survival | 大天使の指輪のレッドマター版レシピを追加（§4.7） |
| `spaceRing` | `enableHighSpeedMovementAbilityWhenLanding` | boolean | `false` | enable GemFeet's High-Speed movement ability when landing | 指輪所持中でも、飛行モードでなければジェムブーツの空中加速を有効化（§3.6.1） |

読み込み失敗時はログ「コンフィグファイルのロードに失敗しました」（L26）。

**1.21.1 提案**: `ModConfigSpec`（common または server）。`enableArchangelSmiteRecipe` は
レシピの有無なので **`ICondition`（ProjectE の `FullKleinStarsCondition` が参考例）** としてデータパック側で扱う方が素直。

---

## 4. レシピ一覧

登録は `gameObjs/ObjHandlerPEAA.java:55-79`。追加アイテム／ブロックへの **EMC 値設定は一切ない**。

### 4.1 Energy Collector MK4（L58）
```
C C C      C = ProjectE Energy Collector MK3 (ObjHandler.collectorMK3)
C M C      M = レッドマター (ObjHandler.matter meta=1)
C C C
```

### 4.2 Energy Collector MK5（L59）
```
C C C      C = Energy Collector MK4 (PEAA)
C M C      M = レッドマター
C C C
```

### 4.3 AEGU MK1（L61）
```
C C C      C = ProjectE Energy Collector MK3
C P C      P = ダークマターの台座 (ObjHandler.dmPedestal)
C C C
```

### 4.4 AEGU MK2（L62）
```
A A A      A = AEGU MK1（停止形）
A S A      S = クラインの星 Sphere (ObjHandler.kleinStars meta=4)
A A A
```
**[要確認]** `new ItemStack(kleinStars, 1, 4)` の meta 4 は
1.21.1 の `KleinTier.SPHERE`（`refs/projecte/.../items/KleinStar.java:61-67` の並び順 EIN,ZWEI,DREI,VIER,SPHERE,OMEGA）に相当。
EMC 充填量は問わない。

### 4.5 AEGU MK3 — 特殊レシピ `customRecipes/RecipeAEGUMk3.java`（L63-64）

```
A A A      A = AEGU MK2（停止形のアイテム）
A K A      K = 満タンのクラインの星 Omega (meta=5, EMC = 51,200,000)
A A A
```

`RecipeSorter.register("Ultimate AEGU Recipes", RecipeAEGUMk3.class, Category.SHAPED, "")`（L64）で
通常の shaped レシピより先に判定されるよう登録。

判定ロジック（`RecipeAEGUMk3.java:35-59`）:
1. `inv.getSizeInventory() < 9` なら不成立（= 3×3 作業台必須）。
2. 全 9 スロットが埋まっていること。1 つでも `null` なら不成立。
3. **中央（index 4）**: `ItemStack.areItemStacksEqual(input, fullKleinOmega)` **かつ**
   `ItemStack.areItemStackTagsEqual(input, fullKleinOmega)` を満たすこと。
   `fullKleinOmega` はコンストラクタで `KleinStar.setEmc(fullKleinOmega, EMCHelper.getKleinStarMaxEmc(fullKleinOmega))`
   により**満タンに設定されている**（L30）ので、実質「EMC 満タンのクラインの星 Omega 1 個ちょうど」。
4. **その他 8 スロット**: `input.getItem() == Item.getItemFromBlock(aeguMK2_off)`。
   （アイテムだけを比較。稼働形 `aeguMK2_on` は別アイテムなので不可）
5. 結果: `new ItemStack(aeguMK3_off)` を 1 個（L21, L62-65）。

**[要確認]** `areItemStacksEqual` は 1.7.10 では stackSize も比較する。よって中央は**ちょうど 1 個**でなければならない。

**1.21.1 提案**: NeoForge の `DataComponentIngredient` で
「`projecte:klein_star_omega` かつ `projecte:stored_emc == 51200000`」を表現すれば
通常の shaped レシピ JSON で書ける。これが最も簡潔。
（`CustomRecipe` + `RecipeSerializer` でも可）

### 4.6 司空の指輪（L67-70） — 無定形（shapeless）
```
Swiftwolf's Rending Gale ×1 (ObjHandler.swrg)
虚無の指輪 ×1              (ObjHandler.voidRing)
レッドマター ×7            (ObjHandler.matter meta=1)
```
合計 9 個。

### 4.7 大天使の指輪（config 有効時のみ、L73-77）
```
A F A      A = 弓 (Items.bow)
R I R      F = 羽根 (Items.feather)
A F A      R = レッドマター (ObjHandler.matter meta=1)
           I = 鉄の輪 (ObjHandler.ironBand)
```
ProjectE 標準はここが**ダークマター**（`refs/projecte/src/datagen/generated/data/projecte/recipe/archangel_smite.json`）。
PEAA は「置き換え」ではなく**追加**なので、有効時は 2 種類のレシピが共存する。

### 4.8 EMC 値
追加ブロック・アイテムに `EMCProxy.registerCustomEMC` 等の呼び出しは無い。
→ **すべて EMC 値なし（トランスミューテーションテーブルに登録されない）**。

**1.21.1 提案**: 不要なら何もしない。必要なら `CustomConversionProvider`（`api/data/CustomConversionProvider`）でデータ生成する。

---

## 5. ProjectE 本体への改変（ASM コアモッド）

コアプラグイン: `asm/PEAACoreCorePlugin.java`、モッドコンテナ `asm/PEAACoreContainer.java`（modid `PEAACore`, version `1.3.1`）。

有効な Transformer（`PEAACoreCorePlugin.java:15-17`）:
1. `CondenserTileTransformer`
2. `DMFurnaceTileTransformer`
3. `MK2TextureTransformer`
4. `ObjHandlerTransformer`

**`ToolTipEventTransformer` はコメントアウトされており無効**（`PEAACoreCorePlugin.java:17`。
クラス自身の Javadoc にも「上手く動かない為未実装」`ToolTipEventTransformer.java:11`）。

難読化名の対応表は `asm/MethodNameList.java` に定義されているが、
実際に `getName()` を呼んでいる箇所は上記 5 つの Transformer 内に見当たらない **[要確認]**。

---

### 5.1 `CondenserTileTransformer`

| 項目 | 内容 |
|---|---|
| 対象クラス | `moze_intel.projecte.gameObjs.tiles.CondenserTile` |
| 対象メソッド | `getProgressScaled` |
| 注入位置 | メソッド内 **3 番目の `IRETURN` の直前**（`CondenserTileTransformer.java:83-99`） |
| 注入内容 | `(int)(((long)this.displayEmc * 102L) / (long)this.requiredEmc)` を計算して積み、これを戻り値にする |

改変前 → 改変後（プレイヤーから見た違い）:
- 改変前: `displayEmc * 102 / requiredEmc` が int 演算のため、`displayEmc` が約 2100 万を超えると
  乗算で int オーバーフローし、**コンデンサーのプログレスバーが暴れる／逆走する／表示されない**。
- 改変後: long で計算するので、AEGU + MK2 コンデンサーの大量 EMC（最大 520,000 EMC/s 蓄積）でも正常表示。

`102` は ProjectE の `Constants.MAX_CONDENSER_PROGRESS` **[推測]**。

**1.21.1 での代替案**

| 優先度 | 方法 | 実現性 / 懸念 |
|---|---|---|
| ① | **対応不要** | 1.21.1 の `CondenserBlockEntity.displayEmc` / `requiredEmc` は `long`（`refs/projecte/.../block_entities/CondenserBlockEntity.java:44-45`）。GUI 側も long 前提。**この改変は完全に不要** |

---

### 5.2 `DMFurnaceTileTransformer`

| 項目 | 内容 |
|---|---|
| 対象クラス | `moze_intel.projecte.gameObjs.tiles.DMFurnaceTile` |
| 改変 1 | クラスの `superName` を `RMFurnaceTile` → `peaa/gameObjs/tiles/RMFurnaceTilePEAA` に変更（`DMFurnaceTileTransformer.java:68-79`） |
| 改変 2 | `<init>` 内の `RMFurnaceTile` 所有のメソッド呼び出し（= `super()`）を `RMFurnaceTilePEAA` に付け替え（L90-99） |

改変前 → 改変後（プレイヤーから見た違い）:
- DM かまどが RM かまど（PEAA 版）の実装を継承するようになり、以下を獲得する:
  - **真上のインベントリから自動搬入**（燃料・クラインの星 → 燃料スロット、精錬可能品 → 入力ストレージ）
  - **UP 以外の 5 方向の隣接インベントリへ自動搬出**（Relay / マターかまどは除外）
  - **鉱石（OreDict 名が `ore` で始まるもの）の 100% 倍化**（本来 DM かまどは 1.5 倍相当）
  - **EMC 消費 1.6/tick**
- なお `ticksBeforeSmelt` / `efficiencyBonus` は `DMFurnaceTile` 自身のコンストラクタで設定されるため
  DM かまどの精錬速度そのものは変わらない **[推測]**。

**[重要] 1.21.1 では継承方向が逆転している**:
1.7.10 は `DMFurnaceTile extends RMFurnaceTile`、
1.21.1 は `RMFurnaceBlockEntity extends DMFurnaceBlockEntity`（`refs/projecte/.../block_entities/RMFurnaceBlockEntity.java:14`）。

**1.21.1 での代替案**

| 優先度 | 方法 | 実現性 / 懸念 |
|---|---|---|
| ① ProjectE API / イベント | 該当 API なし。`getOreDoubleChance()` / `EMC_CONSUMPTION` / 搬入搬出範囲はいずれも `protected` or `private` で外部から差せない | **不可** |
| ② 継承・差し替えブロック | PEAA 独自の `MatterFurnace` サブクラス + 独自 `BlockEntityType` を登録し、レシピと EMC を ProjectE 版と同等にして「PEAA 版かまど」を別アイテムとして追加する | 可能。ただし**既存ワールドの ProjectE かまどは置き換わらない**／JEI にアイテムが 2 種並ぶ |
| ③ Mixin | `DMFurnaceBlockEntity` に対して: (a) `getOreDoubleChance()` を `@ModifyReturnValue` / `@Inject(cancellable)` で 1.0F に、(b) `pushToInventories` を `@Inject(at=TAIL)` で 5 方向版に拡張。(c) `EMC_CONSUMPTION`（`private static final long = 2`）の変更は `tickServer` が直接参照するため実質不可 | 現実的。**「DM かまどの 100% 鉱石倍化」と「5 方向排出」だけに絞ることを推奨** |

> 1.21.1 では **搬入・搬出は ProjectE が既に実装済み**（真上から搬入・真下へ搬出）。
> PEAA との差は「排出先が下だけか、上以外の 5 方向か」「ホッパー除外の有無」のみ。
> 実装優先度は低いと判断できる。

---

### 5.3 `MK2TextureTransformer`

| 項目 | 内容 |
|---|---|
| 対象クラス | `moze_intel.projecte.rendering.CondenserMK2ItemRenderer` / `moze_intel.projecte.rendering.CondenserMK2Renderer` |
| 対象メソッド | `<init>` |
| 注入位置 | **最初の LDC 定数**（`MK2TextureTransformer.java:74-82`） |
| 注入内容 | 最初の LDC を文字列 `"PEAA"` に置換 |

改変前 → 改変後（プレイヤーから見た違い）:
- レンダラが参照する `ResourceLocation` の名前空間が `projecte` → `PEAA` になり、
  Energy Condenser MK2 の**モデル／アイテムのテクスチャが PEAA 同梱のものに差し替わる**
  （`assets/peaa/textures/blocks/condenser_mk2.png`）。
- 加えて `CondenserMK2PEAA.registerBlockIcons`（`blocks/CondenserMK2PEAA.java:115-118`）が
  ブロックアイコンを vanilla の `obsidian` に設定している。

**1.21.1 での代替案**

| 優先度 | 方法 | 実現性 / 懸念 |
|---|---|---|
| ① リソース上書き | 1.21.1 は JSON モデル方式。PEAA の jar 内に `assets/projecte/textures/block/condenser_mk2.png` を同梱すれば**コード不要で上書きできる** | 最有力。ただし「同 ID のリソースを別 MOD が持つ」形になるのでロード順に依存しうる **[要確認]**。確実にしたければ `AddPackFindersEvent` で内蔵リソースパックを明示的に上乗せする |
| ② Mixin | 不要 | – |

---

### 5.4 `ObjHandlerTransformer`

対象クラス: `moze_intel.projecte.gameObjs.ObjHandler`。2 つのメソッドを書き換える。

#### 5.4.1 `<clinit>`（静的フィールド初期化） — `ObjHandlerTransformer.java:88-147`

`visitFieldInsn` で以下のフィールド名を検出し、その直前に PEAA 版インスタンスの生成コードを挿入する。

| 対象フィールド | 差し替え後 | 挿入コード |
|---|---|---|
| `condenserMk2` | `peaa.gameObjs.blocks.CondenserMK2PEAA` | `new CondenserMK2PEAA()`（L104-106） |
| `rmFurnaceOff` | `peaa.gameObjs.blocks.MatterFurnacePEAA` | `new MatterFurnacePEAA(false, true)`（L112-116） |
| `rmFurnaceOn` | 同上 | `new MatterFurnacePEAA(true, true)`（L122-126） |
| `everTide` | `peaa.gameObjs.items.EvertideAmuletPEAA` | `new EvertideAmuletPEAA()`（L132-134） |
| `gemFeet` | `peaa.gameObjs.items.armor.GemFeetPEAA` | `new GemFeetPEAA()`（L140-142） |

**[要確認]** `visitFieldInsn` は `GETSTATIC` でも呼ばれるため、`<clinit>` 内でこれらのフィールドを
**読み出している箇所でも余計なインスタンスが生成される**可能性がある。
意図としては「PUTSTATIC 直前の値の差し替え」。

改変前 → 改変後（プレイヤーから見た違い）:
- **Energy Condenser MK2** が PEAA 版になり、AEGU からの EMC 生成・隣接インベントリへの自動排出を得る（§3.3）。
- **RM かまど**（on/off 両方）が PEAA 版になり、自動搬入搬出・鉱石 100% 倍化・EMC 消費 1.6/tick を得る（§3.4）。
- **水の祭器**が PEAA 版になり、放つ水のオーブが火のオーブと相殺するようになる（§3.7）。
- **ジェムブーツ**が PEAA 版になり、司空の指輪所持中は飛行を与えず、空中加速も抑制される（§3.6）。

#### 5.4.2 `register()` — `ObjHandlerTransformer.java:158-196`

メソッド先頭（`visitCode`）に以下を挿入（L164-186）:
```java
GameRegistry.registerTileEntity(CondenserMK2TilePEAA.class, "CondenserMK2 Tile  PEAA");
GameRegistry.registerTileEntity(RMFurnaceTilePEAA.class,   "RM Furnace Tile PEAA");
EntityRegistry.registerModEntity(EntityWaterProjectilePEAA.class, "Water Water PEAA",
        10, PECore.instance, 256, 10, true);
```
加えて `visitLdcInsn`（L189-195）で、`register()` 内に現れる
`EntityWaterProjectile.class` の定数を `EntityWaterProjectilePEAA.class` に置換する。
→ ProjectE 自身が行う水オーブのエンティティ登録が PEAA 版クラスに向く。

**1.21.1 での代替案（項目ごと）**

##### (a) `condenserMk2` の差し替え（AEGU 連携 + 自動排出）— 最重要

| 優先度 | 方法 | 実現性 / 懸念 |
|---|---|---|
| ① ProjectE API | **AEGU 側から `IEmcStorage` 経由で EMC を注入する**。AEGU 群（または隣接判定を担う PEAA 独自 BlockEntity）が毎 tick、隣の Condenser MK2 の `PECapabilities.EMC_STORAGE_CAPABILITY` を取り `insertEmc(...)` する。`CondenserBlockEntity.canAcceptEmc()` はロック品が設定されていれば `true`（`refs/projecte/.../block_entities/CondenserBlockEntity.java:60-62, 122-143`）なので**ProjectE を一切改変せずに EMC 供給できる** | **最有力**。懸念: `getEmcInsertLimit()` は `getNeededEmc()`（`api/block_entity/BaseEmcBlockEntity.java:63-65`）なので Condenser の最大 EMC まで入る。挙動はほぼ同等になる |
| ② 継承・差し替えブロック | PEAA 独自の「AEGU コンデンサー」ブロックを新設し、そちらだけが AEGU 連携する | 可能だが、原典の「ProjectE の MK2 コンデンサーがそのまま強化される」体験が変わる |
| ③ Mixin | `CondenserMK2BlockEntity` に `@Inject` して tick 処理を追加 | ①で足りるので不要 |

**自動排出（`pushToInventories`）について**:

| 優先度 | 方法 | 実現性 / 懸念 |
|---|---|---|
| ① | AEGU 側 BlockEntity が毎 tick、隣接 Condenser の `Capabilities.ItemHandler.BLOCK` から出力を抜き、周囲インベントリへ押し込む | ProjectE 改変なしで実現可能。`CondenserMK2BlockEntity.createAutomationInventory()` の `automationOutput` は `WriteMode.OUT`（= 抽出可）なので**抜き出せる**（`CondenserMK2BlockEntity.java:40`） |
| ③ Mixin | `CondenserMK2BlockEntity#tickServer` に `@Inject(at=TAIL)` で押し出し処理 | 可能。①で足りるなら不要 |

##### (b) `rmFurnaceOff` / `rmFurnaceOn` の差し替え
→ §5.2 と同じ。**1.21.1 では搬入搬出が本体実装済みのため、優先度低**。

##### (c) `everTide` の差し替え（水オーブ ⇔ 火オーブ相殺）

| 優先度 | 方法 | 実現性 / 懸念 |
|---|---|---|
| ① イベント | NeoForge の `EntityTickEvent.Post`（または `ServerTickEvent`）を購読し、`EntityWaterProjectile` と `EntityLavaProjectile` の距離が 1 ブロック以内なら両方 `discard()` する | **ProjectE 改変ゼロで実現可能。最有力** |
| ② 継承・差し替え | PEAA 独自の水オーブエンティティを登録し、水の祭器を差し替える | ①より重い |
| ③ Mixin | `EntityWaterProjectile#tick` に注入 | 不要 |

##### (d) `gemFeet` の差し替え
→ §3.6.2 のとおり、1.21.1 では**飛行付与の改変は不要になる可能性が高い**。
残る「空中前進加速の抑制」は:

| 優先度 | 方法 | 実現性 / 懸念 |
|---|---|---|
| ① イベント | クライアント側の `PlayerTickEvent` で、司空の指輪が飛行モード中なら `GemFeet` が加えた速度を打ち消す | 加算後に打ち消す形になり、挙動が微妙にずれる |
| ③ Mixin | `GemFeet#inventoryTick` の該当ブロック（`GemFeet.java:95-101`）に `@Inject(cancellable)` | **こちらが素直**。注入先: `moze_intel.projecte.gameObjs.items.armor.GemFeet#inventoryTick` |

##### (e) TileEntity / Entity の追加登録
→ **1.21.1 では ASM 不要**。自 MOD の `DeferredRegister<BlockEntityType<?>>` / `DeferredRegister<EntityType<?>>` に登録する。

---

### 5.5 無効化されている Transformer

#### `ToolTipEventTransformer`（`asm/transform/ToolTipEventTransformer.java`）

| 項目 | 内容 |
|---|---|
| 状態 | **無効**。`PEAACoreCorePlugin.java:17` でコメントアウトされている |
| クラス Javadoc | 「上手く動かない為未実装」（L11） |
| 対象クラス | `moze_intel.projecte.events.ToolTipEvent` |
| 対象メソッド | `tTipEvent` |
| 意図した改変 | `ILOAD 5` の 3 回目の直後に `I2L` を挿入し、続く `IMUL` を `I2L` に、`I2L` を `LMUL` に置換して long 乗算に変える（L67-95） |
| 意図した効果 | ProjectE のツールチップに出る EMC 値が int オーバーフローするのを防ぐ |

**1.21.1 では不要**（EMC が全面的に `long`）。

---

## 6. 1.21.1 移植上の論点

### 6.1 1.21.1 の ProjectE に既に同等機能があり、不要になりそうなもの

| PEAA の機能 | 1.21.1 での状況 | 判定 |
|---|---|---|
| `CondenserTileTransformer`（int オーバーフロー修正） | EMC が全面 `long` | **不要** |
| `ToolTipEventTransformer`（同上） | 同上。そもそも無効 | **不要** |
| `CondenserMK2TilePEAA.condense()`（スタック一括変換） | `CondenserMK2BlockEntity.condense()`（`CondenserMK2BlockEntity.java:55-70`）が実装済み | **不要** |
| `CondenserMK2TilePEAA` の入出力スロット分離（42+42） | 1.21.1 は `createInput()` / `createOutput()` で 42+42 に分離済み | **不要** |
| `RMFurnaceTilePEAA.pullFromInventories` / `pushToInventories` | `DMFurnaceBlockEntity.java:303-332` が実装済み（搬入=真上 / 搬出=真下） | **ほぼ不要**（方向の差のみ） |
| RM かまどの鉱石 100% 倍化 | `RMFurnaceBlockEntity.getOreDoubleChance() = 1F` | **不要** |
| `GemFeetPEAA.canProvideFlight`（ジェムアーマー飛行の抑制） | ジェムアーマーはもう飛行を与えない（飛行は SWRG / アルカナのみ） | **不要の可能性大 [要確認]** |
| コレクター MK4/MK5 の GUI・Container 座標 | `CollectorMK3Container` / `AbstractCollectorScreen.MK3` と完全一致 | **流用可** |
| `GuiHandlerPEAA` / `PacketHandlerPEAA`（SimpleNetworkWrapper） | `MenuProvider` + `MenuType` / `CustomPacketPayload` に置換 | **書き直し** |
| `proxies/ClientProxy` `CommonProxy`（SidedProxy） | `@Mod(dist=)` / `FMLEnvironment.dist` / クライアント専用クラス分離 | **書き直し** |

### 6.2 1.7.10 固有で再現が難しい・意味が変わるもの

| 項目 | 内容 |
|---|---|
| **飛行操作の実装方式** | `ClientProxy.doFlightOnSide` は `EntityPlayerSP.movementInput` を直接読み、`updatePlayerMoveState()` を**手動で呼び直している**。1.21.1 では `LocalPlayer#input` を読むことはできるが `tick()` を手動で呼ぶ設計にはなっていない。→ `ClientTickEvent` で `LocalPlayer.input.jumping` / `forwardImpulse` / `isShiftKeyDown()` を読み、`setDeltaMovement` で速度を与える形に**設計から書き直す**。あわせて「ダッシュ 2 度押しの手動再実装」（`ClientProxy.java:64-80`）は不要になる |
| **`player.moveFlying(strafe, forward, factor)`** | 1.21.1 に同名 API は無い。`LivingEntity#moveRelative(float amount, Vec3 relative)` が近い。係数の意味も違うため**速度感の再調整が必要 [要確認]** |
| **`capabilities.allowFlying` の参照** | 1.21.1 の飛行許可は `NeoForgeMod.CREATIVE_FLIGHT` 属性ベース。「他 MOD が飛行を許可しているか」の判定は `player.getAbilities().mayfly` で近似できるが意味が完全には一致しない |
| **AEGU の on/off が別ブロック** | 1.21.1 では BlockState にすべき。**既存ワールドの移行は考慮不要**（新規実装のため） |
| **座標を `String[3][26]` で保持** | 1.21.1 では `List<BlockPos>` + `BlockPos.CODEC` で保存する |
| **`world.getBlock(x,y,z) instanceof AEGU`** | 1.21.1 では `level.getBlockState(pos).getBlock() instanceof AEGUBlock`。**チャンク未ロード時の扱い**に注意（`WorldHelper.getBlockEntity` のような安全な取得を使う） |
| **`ISidedInventory` / `IInventory`** | `IItemHandler` capability（`Capabilities.ItemHandler.BLOCK`）に置換。`BlockCapabilityCache` を使うと高速（`DMFurnaceBlockEntity.java:151-157` が手本） |
| **`EMCHelper.getEmcValue`** | `IEMCProxy.INSTANCE.getValue(...)` / 売却値は `getSellValue(...)` |
| **`OreDictionary` の `"ore"` 前置判定** | 1.21.1 はタグ。`Tags.Items.ORES` / `Tags.Items.RAW_MATERIALS`（`DMFurnaceBlockEntity.java:178-186` が手本） |
| **`sendToAll` による飛行状態同期** | 1.21.1 では該当プレイヤーのみに返す（`IPayloadContext#reply` など）。原典の挙動はバグなので再現しない |
| **`InvalidParameterException` を投げる**（`FlightEventHookPEAA.java:126`） | 例外でクラッシュさせずログ警告に留めるべき |
| **`registerModEntity` の ID 10 / 追跡 256** | 1.21.1 では `EntityType.Builder` の `clientTrackingRange` / `updateInterval` |
| **`ItemCharge`（`getCharge` / `consumeFuel` / `removeEmc`）** | `IItemCharge`（capability）と `ItemPE.consumeFuel` / `removeEmc`（static）に分離済み。`ItemDeferredRegister` が `IItemCharge` 実装アイテムに自動で capability を付ける（`refs/projecte/.../registration/impl/ItemDeferredRegister.java:49`）が、**これは ProjectE 内部の登録ヘルパーなので PEAA 側は自前で `RegisterCapabilitiesEvent#registerItem` する必要がある** |
| **`.pdn` ファイル** | `textures/blocks/aegu/aegu.pdn` / `top.pdn` / `top_4.pdn` / `collectors/other.pdn` は Paint.NET 作業ファイル。**移植対象外** |
| **AEGU 上面テクスチャ** | `aegu/` には `top.pdn` / `top_4.pdn` しか無く PNG が存在しない。AEGU は全面 `aegu{tier}.png` の 1 枚テクスチャ |
| **`collectors/other.png`** | `registerBlockIcons` は `other_4` / `other_5` しか参照しないため**未使用** |
| **言語ファイルの `item.KleinStarVertex.name`** | 対応するアイテムがソース中に存在しない**未使用エントリ**（en: `Klein Star Vartex` / ja: `クラインの星 Vartex`）。実装しない |
| **`AEGU_MK{n}_on` の表示名** | `_on` 専用の言語エントリが無い。BlockState に統合すれば問題は消える |

### 6.3 判断が必要な点（人間に決めてほしいこと）

1. **mod id とパッケージ名**。`peaa` / `net.nokopi.peaa` などを提案。原典は `PEAA`（大文字）だが 1.21.1 は小文字必須。
2. **AEGU を BlockState 1 ブロックに統合してよいか**（強く推奨）。統合すると `aeguMK1_off` / `aeguMK1_on` の 2 レジストリ名が 1 つになる。
3. **MK2 コンデンサーへの EMC 供給を、ProjectE 改変なしの「AEGU → `IEmcStorage.insertEmc`」方式にしてよいか**（§5.4.2(a) ①）。
   これを採ると Mixin ゼロで実装できる。ただし「MK2 コンデンサーが 26 個の AEGU に囲まれているか判定する」責務が AEGU 側に移る。
4. **MK2 コンデンサーの隣接インベントリ自動排出を再現するか**。1.21.1 の ProjectE には無い機能。
   再現する場合は AEGU 側から Condenser の ItemHandler を吸い出す方式（改変ゼロ）で良いか。
5. **DM かまどの鉱石 100% 倍化を再現するか**（1.21.1 の既定は 50%）。再現するなら **Mixin が必要**。
6. **かまどの EMC 消費 1.6/tick を再現するか**。1.21.1 は `long` の 2 固定で `private static final` のため Mixin でも書き換えにくい。
   → **既定の 2 のままにする**ことを推奨。
7. **かまどの排出方向を「下のみ（本体既定）」のままにするか、「上以外の 5 方向」にするか**。
8. **司空の指輪の飛行速度**。原典はスニーク時 `flyingSpeed * 35`（charge 3 で係数 6.16）と極端に速い。そのままにするか調整するか。
9. **落下ダメージ完全無効**（`LivingFallEvent` キャンセル）をそのまま再現するか。
10. **飛行モードの ON/OFF 操作方式**。原典は「ジャンプ 2 度押し」。1.21.1 では専用キーバインドに変える選択肢もある
    （右クリックはテレポートに使われているので衝突する）。
11. **大天使の指輪の代替レシピの実装方式**。config（`ModConfigSpec`）+ `ICondition` か、単純に常時追加するか。
12. **水オーブ ⇔ 火オーブの相殺を再現するか**。1.21.1 の水オーブは半径 3 のマグマを黒曜石化する仕様に進化しており、相殺の必要性が薄れている **[要確認]**。
13. **MK2 コンデンサーのテクスチャ差し替えを再現するか**（`assets/projecte/...` の上書き）。ProjectE の見た目を変えることになる。
14. **追加ブロック／アイテムに EMC 値を設定するか**（原典は未設定）。
15. **AEGU の稼働条件「26 マス中 25 個以上」をそのまま採用するか**。26 個ちょうどにする方が仕様として明快。
16. **コレクター MK4/MK5 が「AEGU が稼働形かどうか」を見ていない**点。原典どおり「AEGU が上にあれば常時フル稼働」でよいか、
    「稼働中の AEGU のときだけ」に直すか。

---

## 7. 実装順の提案

### Step 0: プロジェクト雛形
- `refs/neoforge-mdk` から build.gradle / gradle.properties / neoforge.mods.toml をコピーし、
  mod_id・license（`All Rights Reserved (private use only)`）を設定。
- ProjectE 1.21.1 を依存に追加（Maven 座標を確認する必要あり **[要確認]**）。
- **完了条件**: `./gradlew build` が通る。`./gradlew runClient` でタイトル画面まで起動し、MOD 一覧に表示される。

### Step 1: 共通基盤
- `PEAABlocks` / `PEAAItems` / `PEAABlockEntityTypes` / `PEAAMenuTypes` の `DeferredRegister` 群。
- クリエイティブタブ（ProjectE のタブに追加するか独自タブか要判断）。
- `en_us.json` / `ja_jp.json`（§2 の表から）、`docs/ASSETS.md` の雛形。
- テクスチャを `refs/peaa/.../textures/` から `textures/block/` `textures/item/` `textures/gui/` へ小文字スネークケースで配置。
- **完了条件**: ビルドが通る。クリエイティブタブが表示される。

### Step 2: Energy Collector MK4 / MK5（§3.1）
- `CollectorMK4BlockEntity extends CollectorMK1BlockEntity`（`getSunLevel` を上書き）／ MK5。
  → ProjectE の `CollectorMK1BlockEntity` が `public` かつコンストラクタが利用可能か要確認 **[要確認]**。
    不可なら `BaseEmcBlockEntity` から自前実装。
- `CollectorMK3Container` / `AbstractCollectorScreen.MK3` の流用可否を確認。
- ブロック（向き付き）、blockstate / model / 戦利品テーブル、レシピ JSON（§4.1, §4.2）。
- ツールチップ（`appendHoverText`）。
- **完了条件**: 設置 → 真上に任意の AEGU（Step 3 前なら仮ブロック）→ EMC が 320 / 1280 EMC/s で増える。GUI が開き数値が動く。
- **GameTest**: 光レベルに関係なく、真上が AEGU なら `getSunLevel()==16`、そうでなければ `0`。

### Step 3: AEGU（§3.2）
- 1 ブロック + `BooleanProperty GENERATING`（判断 2 次第）。
- 3 tier ぶんのレシピ（§4.3, §4.4）と MK3 の特殊レシピ（§4.5）。
- この時点では「Condenser との連携なし」で、稼働形への切り替えは未実装でよい。
- **完了条件**: 3 tier が設置でき、ドロップが正しく、ツールチップに生成レートが出る。

### Step 4: AEGU ⇔ Energy Condenser MK2 連携（§3.3 / §5.4.2(a)）
- AEGU 群を管理する BlockEntity（または AEGU 自身に BlockEntity を持たせる）を追加。
- 「周囲 26 マスのうち 25 個以上が AEGU」の判定と、稼働形への切り替え。
- 隣接 Condenser MK2 の `PECapabilities.EMC_STORAGE_CAPABILITY` に毎 tick `insertEmc(generateEmc / 20)`。
- **完了条件**: MK2 コンデンサーを 25 個以上の AEGU で囲むと AEGU が稼働形テクスチャになり、コンデンサーの EMC が増え続ける。
- **GameTest**: MK1×26 → 1040 EMC/s、MK3×26 → 520000 EMC/s、24 個では稼働しない。

### Step 5: 司空の指輪（§3.5）
- アイテム（`IItemCharge` 実装、`STORED_EMC` / `CHARGE` Data Component、`RegisterCapabilitiesEvent` で charge capability 登録）。
- `isFlyingMode` の Data Component とサーバ同期パケット（`CustomPacketPayload` + `StreamCodec`）。
- クライアント側の飛行制御（`ClientTickEvent` / `LocalPlayer.input`）。
- テレポート（`Level#clip` によるレイトレース、30 ブロック、3072 EMC）。
- `LivingFallEvent` で落下ダメージ無効。
- レシピ（§4.6）。
- **完了条件**: ホットバーに入れて EMC を入れ、飛行の ON/OFF・上下左右移動・右クリックテレポートが動く。落下ダメージを受けない。
- **GameTest**: charge 0〜3 で EMC 消費が 0.16 / 0.32 / 0.64 / 1.28 per tick になる（近似）。

### Step 6: ProjectE 側の改変（§5、Mixin 検討）
実装前に**注入先クラス・メソッド・注入位置を提示して確認を取ること**（CLAUDE.md のルール）。
- (6a) MK2 コンデンサーの自動排出（判断 4 次第）
- (6b) ジェムブーツの空中加速抑制（判断 8 と連動、`GemFeet#inventoryTick` への Mixin）
- (6c) 水オーブ ⇔ 火オーブ相殺（判断 12 次第、イベントで実装）
- (6d) DM かまどの鉱石 100% 倍化（判断 5 次第、Mixin）
- (6e) MK2 コンデンサーのテクスチャ差し替え（判断 13 次第、リソース上書き）
- **完了条件**: 各機能が個別に確認でき、ProjectE 単体の挙動を壊さない。

### Step 7: config・ツールチップ・仕上げ
- `ModConfigSpec`（§3.9）。
- 大天使の指輪の代替レシピ（§4.7、判断 11 次第）。
- 全レシピ・戦利品テーブル・モデルをデータ生成に寄せる。
- `docs/ASSETS.md` の完成、`docs/DEVIATIONS.md` に仕様差分を記録。
- **完了条件**: `./gradlew build` と `./gradlew runGameTestServer` が通る。

---

## 付録 A: 読んだファイル一覧

### `refs/peaa/src/main/java/peaa/`（全 37 ファイル。全件読了）
```
PEAACore.java
asm/MethodNameList.java
asm/PEAACoreContainer.java
asm/PEAACoreCorePlugin.java
asm/transform/CondenserTileTransformer.java
asm/transform/DMFurnaceTileTransformer.java
asm/transform/MK2TextureTransformer.java
asm/transform/ObjHandlerTransformer.java
asm/transform/ToolTipEventTransformer.java
config/PEAAConfig.java
events/FlightEventHookPEAA.java
events/ToolTipEvent.java
gameObjs/ObjHandlerPEAA.java
gameObjs/blocks/AEGU.java
gameObjs/blocks/CollectorPEAA.java
gameObjs/blocks/CondenserMK2PEAA.java
gameObjs/blocks/MatterFurnacePEAA.java
gameObjs/container/CollectorMK4Container.java
gameObjs/container/CollectorMK5Container.java
gameObjs/customRecipes/RecipeAEGUMk3.java
gameObjs/entity/EntityWaterProjectilePEAA.java
gameObjs/gui/GUICollectorMK4.java
gameObjs/gui/GUICollectorMK5.java
gameObjs/items/EvertideAmuletPEAA.java
gameObjs/items/RingFlightTeleport.java
gameObjs/items/armor/GemFeetPEAA.java
gameObjs/tiles/CollectorMK4Tile.java
gameObjs/tiles/CollectorMK5Tile.java
gameObjs/tiles/CondenserMK2TilePEAA.java
gameObjs/tiles/RMFurnaceTilePEAA.java
network/IsFlyingModeSyncPKT.java
network/IsFlyingModeSyncPKTHandlerToClient.java
network/IsFlyingModeSyncPKTHandlerToServer.java
network/PacketHandlerPEAA.java
proxies/ClientProxy.java
proxies/CommonProxy.java
utils/ConstantsPEAA.java
utils/GuiHandlerPEAA.java
```

### `refs/peaa/src/main/resources/`
```
mcmod.info                               読了
assets/peaa/lang/en_US.lang              読了（7 行）
assets/peaa/lang/ja_JP.lang              読了（8 行。先頭に BOM）
assets/peaa/textures/**                  ファイル名一覧のみ確認（バイナリ）
```

テクスチャ一覧（→ 1.21.1 配置案。正式な対応表は `docs/ASSETS.md` に書く）:

| 原典 | 1.21.1 配置案 | 備考 |
|---|---|---|
| `textures/blocks/aegu/aegu1.png` | `textures/block/aegu_mk1.png` | |
| `textures/blocks/aegu/aegu1gene.png` | `textures/block/aegu_mk1_on.png` | |
| `textures/blocks/aegu/aegu2.png` | `textures/block/aegu_mk2.png` | |
| `textures/blocks/aegu/aegu2gene.png` | `textures/block/aegu_mk2_on.png` | |
| `textures/blocks/aegu/aegu3.png` | `textures/block/aegu_mk3.png` | |
| `textures/blocks/aegu/aegu3gene.png` | `textures/block/aegu_mk3_on.png` | |
| `textures/blocks/aegu/aegu.pdn` | — | Paint.NET 作業ファイル。移植しない |
| `textures/blocks/aegu/top.pdn` | — | 同上 |
| `textures/blocks/aegu/top_4.pdn` | — | 同上 |
| `textures/blocks/collectors/front.png` | `textures/block/collector_front.png` | MK4/MK5 共用 |
| `textures/blocks/collectors/other_4.png` | `textures/block/collector_mk4_side.png` | |
| `textures/blocks/collectors/other_5.png` | `textures/block/collector_mk5_side.png` | |
| `textures/blocks/collectors/top_4.png` | `textures/block/collector_mk4_top.png` | |
| `textures/blocks/collectors/top_5.png` | `textures/block/collector_mk5_top.png` | |
| `textures/blocks/collectors/other.png` | — | コード上未参照 |
| `textures/blocks/collectors/other.pdn` | — | Paint.NET 作業ファイル |
| `textures/blocks/condenser_mk2.png` | `assets/projecte/textures/block/condenser_mk2.png`（上書き用） | 判断 13 次第 |
| `textures/gui/collector3.png` | `textures/gui/collector_mk4.png` 等 | ProjectE の `collector3.png` のコピーと思われる **[要確認]** |
| `textures/items/ring/ringFlightTeleport_off.png` | `textures/item/ring_of_the_space.png` | |
| `textures/items/ring/ringFlightTeleport_on.png` | `textures/item/ring_of_the_space_on.png` | |

### `refs/projecte/`（照合のため参照）
```
src/api/java/moze_intel/projecte/api/block_entity/BaseEmcBlockEntity.java
src/api/java/moze_intel/projecte/api/block_entity/IRelay.java
src/api/java/moze_intel/projecte/api/capabilities/PECapabilities.java
src/api/java/moze_intel/projecte/api/capabilities/item/IItemCharge.java
src/main/java/moze_intel/projecte/gameObjs/EnumCollectorTier.java
src/main/java/moze_intel/projecte/gameObjs/block_entities/CollectorMK1BlockEntity.java
src/main/java/moze_intel/projecte/gameObjs/block_entities/CollectorMK2BlockEntity.java
src/main/java/moze_intel/projecte/gameObjs/block_entities/CollectorMK3BlockEntity.java
src/main/java/moze_intel/projecte/gameObjs/block_entities/CondenserBlockEntity.java
src/main/java/moze_intel/projecte/gameObjs/block_entities/CondenserMK2BlockEntity.java
src/main/java/moze_intel/projecte/gameObjs/block_entities/DMFurnaceBlockEntity.java
src/main/java/moze_intel/projecte/gameObjs/block_entities/RMFurnaceBlockEntity.java
src/main/java/moze_intel/projecte/gameObjs/block_entities/EmcBlockEntity.java
src/main/java/moze_intel/projecte/gameObjs/block_entities/EmcChestBlockEntity.java
src/main/java/moze_intel/projecte/gameObjs/blocks/Collector.java
src/main/java/moze_intel/projecte/gameObjs/blocks/CondenserMK2.java
src/main/java/moze_intel/projecte/gameObjs/blocks/MatterFurnace.java
src/main/java/moze_intel/projecte/gameObjs/container/CollectorMK1Container.java
src/main/java/moze_intel/projecte/gameObjs/container/CollectorMK3Container.java
src/main/java/moze_intel/projecte/gameObjs/customRecipes/RecipeShapelessKleinStar.java
src/main/java/moze_intel/projecte/gameObjs/customRecipes/FullKleinStarsCondition.java
src/main/java/moze_intel/projecte/gameObjs/entity/EntityWaterProjectile.java
src/main/java/moze_intel/projecte/gameObjs/entity/EntityLavaProjectile.java（冒頭 60 行）
src/main/java/moze_intel/projecte/gameObjs/gui/AbstractCollectorScreen.java
src/main/java/moze_intel/projecte/gameObjs/items/ItemPE.java
src/main/java/moze_intel/projecte/gameObjs/items/KleinStar.java
src/main/java/moze_intel/projecte/gameObjs/items/EvertideAmulet.java
src/main/java/moze_intel/projecte/gameObjs/items/armor/GemFeet.java
src/main/java/moze_intel/projecte/gameObjs/items/armor/GemArmorBase.java
src/main/java/moze_intel/projecte/gameObjs/items/rings/SWRG.java
src/main/java/moze_intel/projecte/gameObjs/registries/PEBlocks.java
src/main/java/moze_intel/projecte/gameObjs/registries/PEItems.java（grep）
src/main/java/moze_intel/projecte/gameObjs/registries/PERecipeSerializers.java
src/main/java/moze_intel/projecte/handlers/InternalAbilities.java
src/main/java/moze_intel/projecte/network/packets/to_server/KeyPressPKT.java
src/main/java/moze_intel/projecte/utils/PEKeybind.java
src/datagen/generated/data/projecte/recipe/{collector_mk3,archangel_smite,klein_star_omega}.json
src/datagen/generated/assets/projecte/lang/en_us.json（grep）
```

---

## 付録 B: 読めなかった・理解しきれなかった箇所

| 箇所 | 内容 |
|---|---|
| ProjectE 1.7.10 のソース | refs に含まれていない。`CollectorMK1Tile` のコンストラクタ引数の意味、`Constants.COLLECTOR_MK3_GEN`、`Constants.CONDENSER_MK2_GUI` / `CONDENSER_MK2_RENDER_ID` / `MAX_CONDENSER_PROGRESS`、`ItemCharge` の `pe_` 前置、`RMFurnaceTile.EMC_CONSUMPTION` の元値、`DMFurnaceTile` の `ticksBeforeSmelt` / `efficiencyBonus` は **1.21.1 版からの [推測]** |
| `asm/MethodNameList.java` | 定義されているが、5 つの Transformer 内に `MethodNameList.getName(...)` の呼び出しが見当たらない。開発中の名残か、`FMLDeobfuscatingRemapper` に置き換えられたと思われる **[要確認]** |
| `ak.sampleflight.FlightEventHook` | `PEAACore.java:12` で import されているが未使用。外部 MOD（A.K. 氏のサンプル）への参照で、refs には含まれない |
| テクスチャの内容 | PNG はバイナリのため中身未確認。`gui/collector3.png` が ProjectE の同名ファイルのコピーか改変版かは未確認 **[要確認]** |
| `ObjHandlerTransformer` の `<clinit>` 書き換えの正確なバイトコード的挙動 | `visitFieldInsn` が GETSTATIC でも発火する点、元の値がスタックに残る点について、1.7.10 実機で問題が出なかった理由は未検証 **[要確認]** |
| `CondenserMK2PEAA.createTileEntity` が使うブロックインスタンスの `x/y/z` | ワールド再読み込み時にどう振る舞うか未検証。原典では周囲 AEGU の再登録が失敗している可能性がある **[要確認]** |
| ProjectE 1.21.1 の Maven 座標 / 依存記述 | `refs/projecte/build.gradle` は未読。Step 0 で確認が必要 |
