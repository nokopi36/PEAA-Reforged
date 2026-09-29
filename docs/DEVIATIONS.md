# 仕様からの逸脱記録

`docs/SPEC.md`（原典 PEAA 1.7.10 の仕様）から意図的に変えた点と、その理由。

書式: 対象 / 原典の挙動 / 本実装 / 理由 / 決定日・決定者

---

## D-001: クリエイティブタブを PEAA 独自にする

- **対象**: `peaa.gameObjs.registries.PEAACreativeTabs`（SPEC §2.1, §2.3）
- **原典**: 追加ブロック・アイテムを ProjectE のクリエイティブタブ `ObjHandler.cTab` に入れていた
  （`refs/peaa/.../blocks/AEGU.java:36`、`items/RingFlightTeleport.java:67`、`ObjHandlerPEAA` 経由）
- **本実装**: `peaa:main` という独自の `CreativeModeTab` を登録し、そこに全追加要素を入れる
- **理由**: ProjectE 側のタブ構成変更の影響を受けない。追加要素が一覧しやすい。
  SPEC §6.3 判断 3 として提示し、利用者が「PEAA 独自タブを作る」を選択した
- **決定**: 2026-09-24 / 利用者の選択

## D-002: コレクター GUI テクスチャを自前で持たず ProjectE のものを参照する

- **対象**: コレクター MK4/MK5 の Screen（SPEC §3.1.6）
- **原典**: `assets/peaa/textures/gui/collector3.png` を同梱し、`peaa:textures/gui/collector3.png` を参照
  （`refs/peaa/.../gui/GUICollectorMK4.java:17`）
- **本実装**: 同梱せず `projecte:textures/gui/collector3.png` を直接参照する
- **理由**: 原典の同梱ファイルは ProjectE の `collector3.png` とバイト単位で同一だった
  （md5 `233137f635b5b7cd45619203c3e77cf4`）。ProjectE は必須依存なので必ず存在する。
  重複アセットを避け、ProjectE 側の更新に追従できる。詳細は `docs/ASSETS.md`
- **決定**: 2026-09-24 / 実装者判断（Step 1）

## D-003: 公開用の Maven publish 設定を作らない

- **対象**: `build.gradle`
- **原典**: —（1.7.10 の PEAA は CurseForge 配布だった）
- **本実装**: MDK 雛形にあった `maven-publish` プラグインと `publishing { ... }` ブロックを削除
- **理由**: CLAUDE.md の「ライセンスと利用範囲」で私的利用専用・公開しない方針のため
- **決定**: 2026-09-24 / 実装者判断（Step 1）

## D-004: AEGU の稼働状態を BlockState にする

- **対象**: `peaa.gameObjs.blocks.AEGUBlock`（SPEC §2.1, §3.2.6）
- **原典**: tier ごとに停止形・稼働形の 2 ブロックを登録し、`world.setBlock` で差し替えて切り替えていた
  （`refs/peaa/.../gameObjs/ObjHandlerPEAA.java:39-44`、`blocks/AEGU.java:68-93`）。registry 名は計 6 個
- **本実装**: tier ごとに 1 ブロック（`peaa:aegu_mk1/2/3`）+ `BooleanProperty generating`
- **理由**: 1.21.1 では BlockState が正規の表現。原典も `getItemDropped` で常に停止形をドロップしていた
  （`blocks/AEGU.java:126-137`）ので、アイテムとしては元から 1 種類。
  言語ファイルに `_on` 用エントリが無い問題（SPEC §2.1 の注記）も同時に解消する。
  今後 AEGU に BlockEntity を持たせる際も、`setBlock` による差し替えで BlockEntity が破棄される問題を回避できる
- **決定**: 2026-09-24 / 利用者の選択（SPEC §6.3 判断 2）

## D-005: コレクターを ProjectE から継承せず自前実装する

- **対象**: `peaa.gameObjs.block_entities.CollectorPEAABlockEntity` / `container.CollectorPEAAContainer` /
  `client.gui.CollectorPEAAScreen`（SPEC §3.1）
- **原典**: `CollectorPEAA extends Collector`、`CollectorMK4Tile extends CollectorMK1Tile` で ProjectE を継承
- **本実装**: `BaseEmcBlockEntity`（ProjectE の公開 API）から再実装。Container / Screen も自前
- **理由**: 技術的に継承できない。(1) 生成量・容量は `EnumCollectorTier` からしか設定できず、
  MK4/MK5 に相当する定数が無く NeoForge の拡張可能 enum 宣言も無い、(2) `emcGen` が `private final`、
  (3) 生成式が呼ぶ `getSunLevel(Level, BlockPos)` は**静的メソッド**なので override が効かない
  （1.7.10 ではインスタンスメソッドだったため原典は override できていた）
- **影響**: ProjectE 本体がコレクターのロジックを変更しても自動追従しない。
  スロット配置・GUI 座標・燃料アップグレード・クラインの星充填・EMC 送出・リレーボーナスは
  ProjectE 1.21.1 の実装に合わせて写している
- **決定**: 2026-09-24 / 実装者判断（Step 2、事前に利用者へ説明・承認済み）

## D-006: AEGU MK3 のレシピ判定を部分一致にする

- **対象**: `peaa.datagen.PEAARecipeProvider#fullKleinStarOmega`（SPEC §4.5）
- **原典**: `ItemStack.areItemStacksEqual` + `areItemStackTagsEqual` で NBT 全体の完全一致を要求
  （`refs/peaa/.../customRecipes/RecipeAEGUMk3.java:48-49`）
- **本実装**: `DataComponentIngredient.of(false, projecte:stored_emc, 51200000, klein_star_omega)`
  ＝「EMC が満タンのクラインの星 Omega」だけを条件にする部分一致
- **理由**: 1.21.1 の完全一致は既定コンポーネント（`minecraft:rarity`, `max_stack_size` 等）まで比較対象に含み、
  レシピ JSON にそれらが焼き込まれてしまう。ProjectE 側が既定値を変えると壊れる一方、
  レシピが本当に要求している条件は EMC 量のみ。部分一致なら名前を変えた星も通る（原典では弾かれた）
- **決定**: 2026-09-24 / 実装者判断（Step 2）

## D-007: 言語ファイルをデータ生成に移行

- **対象**: `src/main/resources/assets/peaa_reforged/lang/*.json` → `peaa.datagen.PEAALangProvider`
- **理由**: CLAUDE.md の「可能な限りデータ生成で出力」に従う。Step 1 では項目が 1 個だったため手書きにしていた
- **決定**: 2026-09-24 / 実装者判断（Step 2）

## D-008: AEGU が生成した EMC を AEGU 側で緩衝する

- **対象**: `peaa.gameObjs.block_entities.AEGUBlockEntity`（SPEC §3.3.2）
- **原典**: コンデンサーのタイルを差し替え、`this.addEMC(generateEmc / 20)` で**受け入れ判定を迂回して**
  コンデンサーのバッファへ直接書き込んでいた（`refs/peaa/.../tiles/CondenserMK2TilePEAA.java:54`）
- **本実装**: AEGU が `IEmcStorage.insertEmc` で注入する。ProjectE のコンデンサーは変換先アイテムが
  設定されるまで EMC を受け取らない（`CondenserBlockEntity.java:60-62, 122-143`）ため、
  渡せなかった分は AEGU 内部の `buffer` に貯めておき、受け入れ可能になった時点で注入する
- **理由**: ProjectE を一切改変せずに済む（CLAUDE.md の「原則として API・イベント・継承で代替」）。
  当初は Mixin を使う判断だったが、緩衝方式なら EMC を失わずに同じ結果が得られると分かったため変更した
- **残る差**: ターゲット未設定のコンデンサーの GUI に出る EMC 値が 0 のままになる（原典は増えていく）。
  ターゲット未設定のコンデンサーは何も生産しないため、通常プレイでの差は無い。
  また AEGU を壊すとその AEGU の緩衝分は失われる
- **決定**: 2026-09-24 / 利用者の選択（SPEC §6.3 判断 3）

## D-009: AEGU グループの状態を AEGU 側に持たせる

- **対象**: `peaa.gameObjs.block_entities.AEGUBlockEntity`（SPEC §3.2.3〜3.2.7, §3.3.2）
- **原典**: 差し替えたコンデンサーのタイルが `coordAEGU`（`String[3][26]`）・`numAEGU`・`generateEmc` を保持し、
  AEGU は設置・破壊時にそこへ登録／解除していた
- **本実装**: ProjectE の BlockEntity を差し替えられないため、各 AEGU が自分で
  「周囲にコンデンサーがちょうど 1 個あるか」「そのコンデンサーの周囲 26 マスに AEGU が 25 個以上あるか」を判定し、
  **自分の生成量だけ**を注入する。合計は原典の `generateEmc` と一致する
- **副次的な差**:
  - 登録リストを NBT に持たないので、ワールド再読み込み時の再登録漏れ（SPEC 付録 B の [要確認]）が原理的に起きない
  - グループ成立／解散の反映に最大 1 秒かかる場合がある（バニラは直接隣接する 6 ブロックにしか変更を通知しないため、
    設置・破壊時に周囲を明示的に起こしたうえで、20 tick の定期再判定を保険として置いている）
- **決定**: 2026-09-24 / 実装者判断（Step 4、技術的制約）

## D-010: コンデンサーの自動排出は 1 tick あたり 1 スロットまで

- **対象**: `AEGUBlockEntity#pushCondenserOutput`（SPEC §3.3.4）
- **原典**: 毎 tick、出力スロット全体（43〜84）を走査して押し込んでいた
- **本実装**: 1 tick につき 1 スロット分だけ移動する。グループ内で排出を担当するのは 1 個の AEGU だけ
  （25 個が同じ処理を重複して行うのを避けるため、座標が最小の AEGU を担当に選ぶ）
- **理由**: 毎秒 20 スタックはコンデンサーの生産速度をはるかに上回るため実害が無く、毎 tick の処理量が一定になる
- **なお原典のバグは再現しない**: `iSide` が 0 固定だった問題と、最終スロットが排出されない off-by-one
  （SPEC §3.3.4 の [要確認] 2 件）は修正した実装になっている
- **決定**: 2026-09-24 / 実装者判断（Step 4）

## D-011: 飛行状態の同期を送信者のみに返す

- **対象**: `peaa.network.SetFlyingPayload`（SPEC §3.5.8）
- **原典**: サーバが受信後 `PacketHandlerPEAA.INSTANCE.sendToAll(...)` で**全プレイヤーに**ブロードキャストしていた
  （`refs/peaa/.../network/IsFlyingModeSyncPKTHandlerToServer.java:23`）
- **本実装**: 送信者自身のスタックだけを更新し、通常のインベントリ同期で戻す
- **理由**: 原典の実装はマルチプレイで**他プレイヤーの同じスロット番号の指輪まで飛行状態が書き換わる**バグ。
  SPEC §3.5.8 でも [要確認] として記録済み。再現しない
- **決定**: 2026-09-24 / 実装者判断（Step 5）

## D-012: 指輪の状態は `flying` のみを保存する

- **対象**: `peaa.gameObjs.items.RingOfTheSpace` / `PEAADataComponents`（SPEC §3.5.9）
- **原典**: `canFlying` / `isHeld` / `flyingSpeed` / `isFlyingMode` / `invSlot` の 5 項目を NBT に保存
- **本実装**: `peaa:flying`（＝ `isFlyingMode`）だけを Data Component 化。他の 4 つは毎 tick 算出する
  （EMC は ProjectE の `projecte:stored_emc`、チャージ段階は `projecte:charge` が持つ）
- **理由**: SPEC §3.5.9 の提案どおり。保存する必要があるのはクライアント発の永続状態だけ
- **決定**: 2026-09-24 / 実装者判断（Step 5、SPEC に沿う）

## D-013: 飛行中の指輪が複数あっても例外を投げない

- **対象**: `peaa.client.RingFlightController#findRingSlot`（SPEC §6.2）
- **原典**: ホットバーに飛行モード中の指輪が 2 つあると `InvalidParameterException` を投げていた
  （`refs/peaa/.../events/FlightEventHookPEAA.java:126-127`）
- **本実装**: 最初に見つかった飛行中の指輪を使う。例外は投げない
- **理由**: 想定外の状態でゲームをクラッシュさせない。SPEC §6.2 に記載の方針どおり
- **決定**: 2026-09-24 / 実装者判断（Step 5）

## D-014: 指輪のツールチップにチャージ段階を表示する

- **対象**: `peaa.gameObjs.items.RingOfTheSpace#appendHoverText`（SPEC §3.5）
- **原典**: 司空の指輪にツールチップは無かった（`events/ToolTipEvent.java` はコレクターと AEGU のみ）
- **本実装**: 現在の速度段階（`0 / 3` 形式）、飛行中の消費 EMC/tick、チャージキーの案内を表示する
- **理由**: チャージ段階はゲーム内で一切見えないのに、速度だけでなく
  **慣性の有無まで左右する**（SPEC §3.5.6: charge 0 のときだけ毎 tick 水平速度がリセットされる）。
  段階が分からないと挙動が不可解に見える。実際に動作確認時に「慣性が働いていないのでは」という指摘が出た
- **重複回避**: 貯蔵 EMC は ProjectE 本体の `ToolTipEvent` が `projecte:stored_emc` を持つアイテムに自動表示するため出さない
- **決定**: 2026-09-24 / 利用者の選択

## D-015: config によるレシピ制御をデータパックの読み込み条件に置き換える

- **対象**: `peaa.gameObjs.customRecipes.ArchangelSmiteRecipeCondition` / `peaa.config.PEAAConfig`（SPEC §3.9, §4.7）
- **原典**: `addRecipes()` の中で `if (PEAAConfig.registerArchangelSmiteRecipe)` を見て
  `GameRegistry.addRecipe` を呼ぶかどうか分岐していた（`refs/peaa/.../gameObjs/ObjHandlerPEAA.java:73-77`）
- **本実装**: レシピは JSON（データパック）なので、レシピファイルに `neoforge:conditions` を埋め込み、
  その条件が config 値を読む形にした。ProjectE も同方式（`PERecipeConditions.java:15-16`）
- **挙動の差**: config を変更してもレシピの有効・無効が切り替わるのは**データパック再読み込み時**
  （`/reload` または再起動）。原典は MOD ロード時に決まるので、そもそもゲーム中の変更は効かなかった
- **決定**: 2026-09-24 / 利用者の選択（SPEC §6.3 判断 11）

## D-016: config 項目 `enableHighSpeedMovementAbilityWhenLanding` を先送り（解消済み）

- **対象**: `peaa.config.PEAAConfig`（SPEC §3.9）
- **本実装**: SPEC §3.9 の 2 項目のうち、まず `enableArchangelSmiteRecipe` だけを追加した
- **理由**: もう一方はジェムブーツの空中加速（SPEC §3.6）を制御する値で、消費側が未実装。
  何も効果のない項目を config ファイルに並べると混乱するため、§3.6 の実装と同時に追加する
- **決定**: 2026-09-24 / 実装者判断（Step 6）
- **解消**: 2026-09-25。[D-020](#d-020-ジェムブーツの空中加速抑制を-mixin-で実装) で消費側を実装した際に
  `ENABLE_HIGH_SPEED_MOVEMENT_WHEN_LANDING` を追加済み。参照は
  `RingOfTheSpace#suppressesGemBootsBoost`。SPEC §3.9 の 2 項目とも揃ったので、この逸脱は残っていない

## D-017: 究極型 AEGU の EMC 値を補うマッパーを追加（既定 OFF）

- **対象**: `peaa.emc.PEAAEMCMapper`（SPEC §4.8, §6.3 判断 14）
- **原典**: 追加ブロック・アイテムに EMC 値を一切設定していない
- **本実装**: 既定 OFF のまま、任意で有効化できる `IEMCMapper` を 1 つ追加した。
  有効時は究極型 AEGU にだけ「発展型 AEGU ×8 + クラインの星 Omega」という変換を宣言する
- **背景**: 明示的に EMC を登録していないのは原典どおりだが、**ProjectE はレシピから EMC を自動導出する**ため、
  コレクター MK4/MK5・AEGU MK1/MK2・司空の指輪には値が付く。
  究極型 AEGU だけ付かないのは、そのレシピが**充填済み**のクラインの星 Omega を要求するからで、
  ProjectE は材料をデータコンポーネント込みで識別する（`BaseRecipeTypeMapper.java:203` → `NSSItem.java:34-39`）。
  `projecte:klein_star_omega{stored_emc:51200000}` を作るレシピは存在しないので方程式が解けない
- **既定を OFF にした理由**: SPEC §4.8 に忠実であるため。既定では今までと同じ挙動
- **on/off の場所**: `config/ProjectE/mapping.toml` の `[mappers.peaa] enabled`。
  ProjectE がマッパーごとに自動生成する（`MappingConfig.java:52-64`）ので PEAA 側では config を持たない
- **既知の限界**: 変換はアイテムでしか書けず EMC を直接材料にできないため、
  宣言した値は星の充填分 51,200,000 を含まず、実際の製作コストより低くなる。
  厳密な値が必要なら `/projecte setemc` で上書きできる
- **ユーザー設定との優先関係**: `/projecte setemc` は `setValueBefore`（`CustomEMCMapper.java:30`）、
  本マッパーは `addConversion` を使う。`MappingCollector.java:86-96` のとおり前者が後者を上書きするので、
  **手で設定した値が常に優先される**
- **決定**: 2026-09-25 / 利用者の選択

## D-018: 水オーブ相殺をエンティティ差し替えではなくイベントで実装

- **対象**: `peaa.events.ProjectileCollisionHandler`（SPEC §3.7）
- **原典**: ASM で ProjectE の水オーブを自前サブクラス `EntityWaterProjectilePEAA` に差し替え、
  その `onUpdate` で相殺判定していた（`refs/peaa/.../entity/EntityWaterProjectilePEAA.java:33-56`）
- **本実装**: エンティティは差し替えず、`EntityTickEvent.Post` で水オーブを監視して
  半径 1 ブロック内の火オーブと相殺させる。ProjectE の改変は不要
- **副次的な改善**: 原典は毎 tick ワールド内の**全ロード済みエンティティ**を走査し、
  切り捨てた整数座標を突き合わせていた（O(n)）。境界ボックス検索に置き換えた
- **1.21.1 での必要性**: 1.21.1 の両オーブは互いの投射物に干渉しない（水オーブは半径 3 のマグマを黒曜石化、
  火オーブは半径 3 の水を蒸発させるだけ）。すれ違っても消えないため、この実装が無いと相殺は起きない
- **決定**: 2026-09-25 / 利用者の選択（SPEC §6.3 判断 12）

## D-019: MK2 コンデンサーのテクスチャを内蔵リソースパックで差し替え

- **対象**: `peaa.client.PEAAResourcePacks` / `src/main/resources/peaa_condenser_texture/`（SPEC §5.3）
- **原典**: ASM で ProjectE のレンダラー内のテクスチャ名前空間を `projecte` → `PEAA` に書き換えていた
  （`MK2TextureTransformer.java:74-82`）
- **本実装**: `assets/projecte/textures/block/condenser_mk2.png` を差し替えるリソースを同梱し、
  `AddPackFindersEvent` で `Pack.Position.TOP` ・常時有効の内蔵パックとして登録する
- **なぜパックにしたか**: 素の assets として置くと、同じリソースパスを 2 つの MOD が持つ形になり、
  どちらが勝つかは MOD のロード順に依存する。パックとして明示的に最上位へ置くことで確定させた。
  リソースパック画面にも表示されるので、何が効いているか確認できる
- **前提の確認**: ProjectE の `condenser_mk2` ブロックモデルは `particle` テクスチャのみを持ち要素が無い＝
  専用レンダラー描画。原典の 64×64 テクスチャは ProjectE のものと同じ UV レイアウトのモデルアトラスなので、
  変換なしでそのまま使える（`docs/ASSETS.md` の [要確認] はこれで解消）
- **決定**: 2026-09-25 / 利用者の選択（SPEC §6.3 判断 13）

## D-020: ジェムブーツの空中加速抑制を Mixin で実装

- **対象**: `peaa.mixin.GemFeetMixin` / `RingOfTheSpace#suppressesGemBootsBoost`（SPEC §3.6）
- **原典**: ASM で `ObjHandler.gemFeet` を `GemFeetPEAA` に差し替えていた
- **本実装**: **Mixin 1 件**。`moze_intel.projecte.gameObjs.items.armor.GemFeet#inventoryTick` の
  `player.zza` 読み取りに `@ModifyExpressionValue` を当て、抑制時に 0 を返す
- **なぜこの注入位置か**: 加速・減速は `if (player.zza < 0) ... else if (player.zza > 0)` の分岐だけで決まる。
  `zza` が読まれるのはこの 2 箇所のみなので、0 を返せば**その分岐だけ**が無効化される。
  落下減衰とジャンプ補助には触れず、ProjectE のロジックを書き換えるのではなく条件式への入力を変えるだけで済む
- **原典の改変のうち不要になったもの**: `canProvideFlight`（指輪所持中にジェムアーマーの飛行を無効化）。
  1.21.1 では飛行を与えるのは SWRG とアルカナの指輪だけで（`InternalAbilities.java:80-85`）、
  ジェムアーマーはそもそも飛行を与えないため対象が存在しない
- **設計上の注意**: 抑制条件は Mixin ではなく `RingOfTheSpace` 側に置いた。Mixin を薄く保ち、
  条件表（SPEC §3.6.1）を GameTest で直接検証できるようにするため
- **既知のリスク**: `require = 2` のため、ProjectE 側の実装が変わって注入点が消えると**ゲームが起動しなくなる**。
  黙って挙動が変わるより起動時に気づける方を選んだ。ProjectE は CurseMaven でバージョン固定している
- **決定**: 2026-09-25 / 実装者判断（Step 8、事前に注入先を提示して承認を得た）

## D-021: DM かまどの鉱石倍化と搬出方向を再現し、EMC 消費のみ見送り

- **対象**: `peaa.mixin.DMFurnaceBlockEntityMixin`（SPEC §3.4）
- **原典**: ASM で `DMFurnaceTile` の親クラスを PEAA 版 RM かまどタイルに差し替え、
  鉱石 100% 倍化・EMC 消費 1.6/tick・上以外 5 方向への搬出をまとめて獲得していた
- **本実装**: 同一 Mixin クラスに**注入 2 件**
  - `DMFurnaceBlockEntity#getOreDoubleChance` に `@ModifyReturnValue` を当てて 0.5F → 1.0F にする。
    RM かまどは自身で 1F を返すため影響を受けない
  - `DMFurnaceBlockEntity#tickServer` の **TAIL** に `@Inject` し、水平 4 方向へ出力を押し出す
    （`peaa.util.InventoryPush#pushFurnaceOutputSideways`）。真下は ProjectE が同じ tick の直前に
    処理しているので、合計で「上以外の 5 方向」になる
- **判断 6（EMC 消費 2 → 1.6）を見送った理由**: **実装不能**。1.21.1 の EMC は `long` で小数を表現できず、
  加えて `EMC_CONSUMPTION` は `private static final long` として `tickServer` のバイトコードに定数畳み込みされている
- **判断 7（搬出を 5 方向に）の実装**: 2026-09-30 に利用者の要望で実装した。当初は
  「private な `pushToInventories` への注入は壊れやすい」として見送っていたが、**そこには注入していない**。
  `tickServer` は public static・オーバーロードなし・早期 return なしで、`pushToInventories` を無条件に呼ぶため、
  その TAIL に載せれば private メソッドの内部構造に依存せずに済み、原典の方向ループ順（DOWN → 水平）とも一致する。
  RM かまどは `tickServer` を継承しているので同時に対象になる
- **ホッパーの扱い**: ProjectE は真下がホッパーのとき搬出を飛ばす（ホッパー自身のペースで吸わせるため）。
  これは水平方向には広げていない。横のホッパーはかまどから自力で吸い出せないので、
  除外すると永久に何も受け取れなくなり原典と食い違う
- **毎 tick のコスト**: 出力が空なら capability 参照の前に return する。アイドル中のかまどは追加コストゼロ。
  ProjectE のような `BlockCapabilityCache` は使っていない（`@Unique` フィールドと `setLevel` への 2 箇所目の注入が必要で、
  得られるものに見合わない）
- **1.21.1 で元から満たされていたもの**: 真上からの自動搬入、真下への自動搬出、RM かまどの鉱石 100% 倍化
- **粗鉱石（raw materials）の扱い**: 原典の条件は「OreDict 名が `ore` で始まる」で、1.7.10 に粗鉱石は存在せず
  鉱石ブロックを直接精錬するのが普通だった。したがって**原典の仕様は鉱石ブロックについて忠実に再現されている**。
  1.21.1 で追加された粗鉱石は ProjectE が意図的に鉱石の 2/3 の確率に下げており（`DMFurnaceBlockEntity.java:178-186`）、
  そこには手を入れない。実測値は以下のとおりで、`MixinGameTests#doublingRatesPerInputType` に固定してある

  | 入力 | DM | RM |
  |---|---|---|
  | 鉱石ブロック（`c:ores`） | 1.0 | 1.0 |
  | 粗鉱石（`c:raw_materials`） | 0.667 | 0.667 |
  | 鉱石以外 | 0.0 | 0.0 |

  粗鉱石を精錬すると**両方のかまどで倍化したりしなかったりする**が、これは期待どおりの挙動であり
  Mixin の不適用ではない。紛らわしいのでテストの Javadoc にも明記した
- **リレー除外のテスト**: GameTest サーバーには EMC マップが存在しない（ProjectE は `OnDatapackSyncEvent` で
  構築し、それにはプレイヤーが要る）。リレーは EMC を持つアイテムしか受け取らないので、実地テストは
  除外の有無にかかわらず通ってしまう。そのため `InventoryPush#isRefusedTarget` を直接検証する形にした。
  かまど側は側面が「燃料 IN + 出力 OUT」なので、石炭を使えば実地で検出できる
- **決定**: 2026-09-25 / 利用者の選択（SPEC §6.3 判断 5・6、粗鉱石の扱いは 2026-09-25 に追加確認）。
  判断 7 は 2026-09-30 に利用者の要望で実装に変更
