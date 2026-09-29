# 開発環境のセットアップ

## 1. 参考資料の取得（任意）

**ビルドには不要です。** ProjectE は CurseMaven から自動取得されるので、クローンしてすぐ
`./gradlew build` が通ります。

ただし実装を変えるときは、`CLAUDE.md` のルールにより **NeoForge / ProjectE の API を記憶で
書かず、実物のソースで確認する**ことになっています。その参考資料は他者の著作物なので
リポジトリには含めていません。必要なら以下を `refs/` に自分でクローンしてください。
SHA は本プロジェクトが参照した時点のものです。

| 配置先 | リポジトリ | コミット |
|---|---|---|
| `refs/peaa/` | <https://github.com/Ryokusitai/PEAA> | `87591549d20d1bcc67f23798678d8a0db8e29d03` |
| `refs/projecte/` | <https://github.com/sinkillerj/ProjectE> | `f432b0c66837759fb0731c9144dc53176b949c5d` |
| `refs/neoforge-mdk/` | <https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle> | `4e1be6e906e1b32a753e3580af4ea1bcc3dbc79e` |
| `refs/neoforge-docs/` | <https://github.com/neoforged/Documentation>（`versioned_docs/version-1.21.1` のみ） | `2924fdb885d6357821a05ed6a0bd6ca9d1502517` |

`refs/` は**読み取り専用**として扱ってください（`CLAUDE.md`）。

## 2. ProjectE の依存について（手作業は不要）

ProjectE は公開 Maven リポジトリを持っていません。`refs/projecte/build.gradle` の `publishing` ブロックには
`repositories` の指定がなく、配布は CurseForge のみです。そこで **CurseMaven** 経由で取得しています。
ProjectE 自身も CurseForge 限定の依存（Jade）を同じ方法で入れています（`refs/projecte/build.gradle:234, 276-277`）。

```groovy
// build.gradle
implementation "curse.maven:projecte-${projecte_curse_project_id}:${projecte_curse_file_id}"
```
```properties
# gradle.properties
projecte_curse_project_id=226410
projecte_curse_file_id=6611984
```

`gradle` が自動で取得するので、jar を手動で用意する必要はありません。

### バージョンを変えたいとき

CurseForge のファイル一覧から目的のファイルを開き、URL 末尾の数字を `projecte_curse_file_id` に入れ替えます。

```
https://www.curseforge.com/minecraft/mc-mods/projecte/files/6611984
                                                            ^^^^^^^ これ
```

`neoforge.mods.toml` の `versionRange` も必要に応じて合わせてください。

### 注意

この方式では**ビルド時にネットワーク接続が必要**です（初回取得後は Gradle のキャッシュが効きます）。

## 3. ビルドと実行

```bash
./gradlew build            # コンパイル + jar 生成
./gradlew runClient        # クライアント起動
./gradlew runGameTestServer # GameTest 実行
./gradlew runData          # データ生成（Step 2 以降）
```

初回は Gradle 9.2.1・NeoForge 21.1.251・Minecraft 1.21.1 のアセットをダウンロードするため、
ネットワーク接続と数 GB の空き容量、十数分の時間が必要です。

## 4. 落とし穴: インライン定数を変えたら clean する

`PEAACore.MODID` は `public static final String` なので、**コンパイル時定数**として
`@EventBusSubscriber(modid = ...)` や `@GameTestHolder(...)` に**値ごと焼き込まれます**。

Gradle の増分コンパイルはこの依存を追いきれず、定数を変えても一部のクラスが再コンパイル
されずに**古い値を保持したまま残ります**。`build/classes` や `build/tmp` を消しても、
ビルドキャッシュから同じものが復元されるので直りません。

症状は**静かで分かりにくい**です。実際に 2026-09-30 の mod id 変更（`peaa` → `peaa_reforged`）で
こうなりました。

- `@EventBusSubscriber` が古い mod id のまま → **イベントハンドラが登録されない**
  （水オーブの相殺が無反応になった）
- `@GameTestHolder` が古い mod id のまま → **そのクラスの GameTest が収集対象から外れる**
  （33 件あるはずのテストが 31 件になり、壊れたことに気づけない）

**mod id など定数を変えたら、必ず次を実行してください。**

```bash
./gradlew clean --no-build-cache
./gradlew build --no-build-cache
./gradlew runGameTestServer      # 件数が想定どおりか必ず確認する
```

`clean` は `build/moddev/` も消すため、実行前に run 用のクラスパスを作り直す必要があります。

```bash
./gradlew writeClientLegacyClasspath writeServerLegacyClasspath           writeDataLegacyClasspath writeGameTestServerLegacyClasspath
```

焼き込まれた値は次で確認できます。

```bash
javap -v build/classes/java/main/peaa/events/ProjectileCollisionHandler.class | grep -A3 EventBusSubscriber
```

---

## 5. バージョン

`gradle.properties` で管理しています。

| 項目 | 値 | 出典 |
|---|---|---|
| Minecraft | 1.21.1 | `refs/neoforge-mdk/gradle.properties` |
| NeoForge | 21.1.251 | 同上。ProjectE の要求は `[21.1.119,)`（`refs/projecte/gradle.properties`）なので条件を満たす |
| Parchment | 1.21.1 / 2024.11.17 | `refs/neoforge-mdk/gradle.properties`（ProjectE も同じ） |
| Java | 21 | `build.gradle` |
| ProjectE | 1.1.0 以上 | `refs/projecte/gradle.properties`。取得は CurseMaven（file id 6611984） |
