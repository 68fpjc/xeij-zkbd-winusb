# XEiJ の Z キーボード機能の解析

対象バージョン: XEiJ 0.26.10.08（`xeij/xeij/` 配下のソース）。行番号はこのバージョンのもの（`~` 付きは概数）。

## 1. 概要

「Z キーボード」は X68000 Z 用 USB キーボードを PC (Windows) に接続して、エミュレータ上の X68000 のキーボードとして使うための機能。次の 2 つを担う。

| 役割 | 内容 |
|------|------|
| 入力 | ホスト OS の `KeyEvent` を Z キーボード専用のキーマップ（`KBD_DEFAULT_MAP_ZKEY`）で X68000 のキーコードに変換する。Z キーボード固有のイベントの癖を補正する |
| LED 制御 | X68000 側が指示する LED 状態（かな・ローマ字・コード入力・CAPS・INS・ひらがな・全角）を、USB HID の Feature レポートとして実機キーボードへ送る |

キー入力そのものは通常の Java の `KeyEvent` 経由であり、特別なデバイスアクセスは不要。**HID に直接アクセスするのは LED 送信だけ**（`ZKeyLEDPort`）。

動作は Windows 限定。起動時の自動接続は `XEiJ.prgIsWindows` で制限されている（`Keyboard.java:6857`）。

## 2. 関係するファイル

| ファイル | 役割 |
|----------|------|
| `ZKeyLEDPort.java` | Windows の HID API を Java の FFM（`java.lang.foreign`）経由で呼び、LED データを送る。キー叩き・トグル状態取得も持つ |
| `Keyboard.java` | Z キーボードの制御全般（接続・切断、キーマップ、入力補正、LED 状態管理、送信スレッド、LED デモ、I/O ポート、メニュー） |
| `MC68901.java` | X68000 の MFP の UDR 書き込みから `Keyboard.kbdSetLedStatus` / `kbdSetLedBrightness` を呼ぶ（`1735-1742`） |
| `MemoryMappedDevice.java` | `$00E9F040` 付近のメモリマップド I/O を `Keyboard.kbdZKeyIO*` に振り分ける（`5148-5354`） |
| `XEiJ.java` | 入力メニューへ Z キーボードメニューを追加（`4155`）。メモリマップのコメント（`9033`） |
| `Settings.java` | 設定キー `zkeyboard` / `zkeymap`（`234-235`） |

機能全体のコンパイル時スイッチは `Keyboard.KBD_ZKEY_ON = true`（`5739`）。`false` にすると関連コードはすべて無効になる。

## 3. 接続処理

### 3.1 トリガー

- 起動時 `kbdInit` が設定 `zkeyboard=on` かつ Windows のとき `kbdZKeyOnRequest = true` にする（`6857`）。
- `kbdReset` で `!kbdZKeyOn && kbdZKeyOnRequest` なら `kbdZKeyOpen` を呼ぶ（`7419-7421`）。つまり実際の接続はリセット時に行われる。
- メニュー「入力 → Z キーボード → 接続」のチェックボックスでも `kbdZKeyOpen` / `kbdZKeyClose` を呼ぶ（`6894-6906`）。切断はタイマースレッドで実行される。

### 3.2 デバイスの探索（`ZKeyLEDPort.open`、`ZKeyLEDPort.java:~580-910`）

1. `HidD_GetHidGuid` で HID クラス GUID を得る。
2. `SetupDiGetClassDevsA`（`DIGCF_DEVICEINTERFACE | DIGCF_PRESENT`）でデバイス情報セットを作る。
3. `SetupDiEnumDeviceInterfaces` で列挙し、各インターフェイスについて `SetupDiGetDeviceInterfaceDetailW` を 2 回呼んで（サイズ取得→本取得）デバイスパスを得る。
4. パスを小文字化し、部分文字列 **`vid_33dd&pid_0011&mi_01&col05`** を含むものをターゲットとする。
   - VID `0x33DD`、PID `0x0011`、USB インターフェイス `MI_01`、HID コレクション `COL05`。
   - 複数一致した場合は後に列挙されたものが採用される（`targetPath` が上書きされる）。
5. `CreateFileW`（アクセス権 0、`FILE_SHARE_READ|WRITE`、`OPEN_EXISTING`）で開く。読み書きアクセスなしで開き、`HidD_SetFeature` だけを使う。

失敗時は `IOException`（"device not found" など）を投げる。`kbdZKeyOpen` は握りつぶし、「Z キーボードが接続されていないか制御できません」と標準出力に出して `kbdZKeyOnRequest = false` に戻す（`8676-8690`）。

デバッグフラグ有効時は各 HID デバイスの VID/PID/Usage/レポート長などを `printCaps` で標準出力に表示する。VID/PID 等の調査に使える。

### 3.3 接続後の処理（`kbdZKeyOpen`、`8669-8721`）

1. LED 送信スレッド開始（`senderStart`）。
2. 現在のキーマップを「Z 以外」用に退避し、Z キーボード用キーマップを復元（`kbdCurrentMapNonZ` ⇄ `kbdCurrentMapZ`）。
3. ホストの NumLock 状態を保存し、消灯していれば `hitKey(VK_NUM_LOCK)` で点灯させる。このとき `kbdIgnoreNumLock = true` として自分が叩いた NumLock を入力から除外する。
4. `kbdZKeyOn = true` にし、「LED の動作確認」メニューを有効化する。

### 3.4 切断（`kbdZKeyClose`、`8725-8768`）

デモ終了 → NumLock を元に戻す → 送信スレッド終了 → `send(0L)` で全 LED 消灯 → ポートを閉じる → キーマップを Z 用に保存し、Z 以外用を復元。終了時 (`kbdTini`) にも呼ばれる（`7398`）。

## 4. キー入力の扱い

### 4.1 判定条件

Z キーボードモードは `KBD_ZKEY_ON && kbdZKeyPort != null` で判定する（キーリスナー内、`7736`, `7779`）。ポートが開いている間だけ Z 専用の補正が有効になる。

### 4.2 イベントの補正（`kbdStart` のキーリスナー、`7733-7822`）

Z キーボードは一部キーを別の仮想キーコードで送ってくる、あるいは押下／解放が正しく届かない。ソースのコメントによる事情と対処は次のとおり。

| X68000 キー | ホスト側のキー | 問題 | 対処 |
|-------------|----------------|------|------|
| BREAK | 半角 `VK 244` / 全角 `VK 243` | pressed 時に released が発火し、いつ離されたか分からない | pressed を無視。released で「押して離す」を即時実行 |
| CAPS | 英数字 `VK 240` → `F17` | pressed 時に released が発火するが XF4 でも発火するため区別不能 | released を無視。pressed で「押して離す」 |
| XF4 | ひらがな `VK 242` → `F16` | pressed 時に released が発火する | released を無視。pressed で押下し `kbdXF4Locked` に保持。**他のキーが離されたとき**に XF4 を離す |
| CLR | NumLock | NumLock が消灯していると、テンキーの `5` と `=` を区別できない | released 時に消灯していれば `hitKey(VK_NUM_LOCK)` で点灯させ、直後に発火する NumLock を `kbdIgnoreNumLock` で 1 回無視する |

Z キーボード以外では Windows キー（`VK_WINDOWS`）のみ「押して離す」の特別扱いがある（`7761`）。

入力後の変換は通常キーボードと共通の `kbdInput`（`8317-8445`）。`PPI.ppiInput`（JOYKEY）、F11/F12 のボタン機能、Alt+Shift ショートカットを先に処理した後、`keyCode<<16 | rawCode<<4 | keyLocation` の整数値を `kbdCurrentMap` の 3 列（1 キーにつき最大 3 つの割り当て）と照合する。

### 4.3 キーマップ（`KBD_DEFAULT_MAP_ZKEY`、`6441-6555`）

- 要素は `0xCCCC_RRR_L`（`keyCode` 16bit、`rawCode` 12bit、`keyLocation` 4bit）。1 つの X68000 キーに最大 3 割り当て。インデックスは `3*xo`（xo は X68000 のキーコード昇順の番号）。
- 通常のマップとの主な違いは、PC 側で扱いにくいキーを F13〜F24 の仮想キーに割り当てていること（コメントによる対応）。

| X68000 キー | Z キーボードで届く仮想キー |
|-------------|---------------------------|
| 記号入力 / 登録 / HELP | Volume Mute / Down / Up → `F22` / `F23` / `F24` |
| XF1 / XF5 | Alt（左 / 右） |
| XF2 | 無変換 `VK 29` → `F14` |
| XF3 | 変換 `VK 28` → `F15` |
| XF4 | ひらがな `VK 242` → `F16` |
| かな | Print Screen → `F18` |
| ローマ字 | Scroll Lock → `F19` |
| コード入力 | Pause |
| CAPS | 英数字 `VK 240` → `F17` |
| ひらがな | Win（左）→ `F20` |
| 全角 | Win（右）→ `F21` |
| BREAK | 半角 `VK 244` / 全角 `VK 243` |
| OPT.1 | `F11` → `F13` |
| OPT.2 | `F12` |
| CLR | NumLock（`VK 0x90`、`keyLocation=4`） |

テンキーは `keyLocation = 4`（`KEY_LOCATION_NUMPAD`）で通常キーと区別される。NumLock 消灯時は Home/PgUp/矢印として届くため、NumLock 点灯を維持する仕組みが必要になる。

※ F13〜F24 はキーボード側のファームウェアが送出するキーという前提のコメントであり、実機は未確認。

### 4.4 キーマップの保存

- 設定キー: `zkeymap`（Z 以外は `keymap`）。先頭要素 `-3` はバージョンマーカー、続いて割り当て配列。デフォルトと同一なら空配列で保存する（`7402-7410`）。
- `kbdCurrentMapNonZ` / `kbdCurrentMapZ` の二重管理があり、接続・切断のたびに入れ替わる。キーボード設定ウィンドウの「初期値」ボタンは接続中は Z 用の既定値を使う（`7879`）。

## 5. LED 制御

### 5.1 LED 状態の流れ

```
X68000 プログラム
  └ MFP UDR へ LED コマンドを書く（bit7=1）
      └ MC68901.mfpWriteUDR → Keyboard.kbdSetLedStatus(d)
          ├ 画面上のキーボードの LED を更新
          └ kbdZKeyStatus を更新 → kbdZKeyUpdate()
              └ 明るさを掛けて 64bit データ化 → senderSend(data)
                  └ 送信スレッド（10ms 間隔）→ ZKeyLEDPort.send(data)
                      └ HidD_SetFeature（65 バイト）
```

- X68000 では LED ビットは 0=点灯。`kbdSetLedStatus` が反転して保持する（`8610-8636`）。対象は 7 個（かな、ローマ字、コード入力、CAPS、INS、ひらがな、全角）。
- 明るさ指示は MFP の `0x54-0x57` コマンド。`kbdSetLedBrightness` が 4 段階（0=明るい … 3=暗い）を保持し、Z キーボード側へは **255 / 128 / 64 / 32** に変換して渡す（`8772-8788`）。
- `kbdZKeyStatus` は X68000 側の状態をポートが開いていなくても保持する。接続時に現在値が反映される。

### 5.2 Feature レポートの形式（`ZKeyLEDPort.send`、`~920-960`）

- 長さ 65 バイト（Report ID 1 + データ 64）。
- `byte[0]` = `10`（Report ID）、`byte[1]` = `248`（0xF8、コマンド種別と思われる。意味はソースに記載なし）。
- LED は 1 個 1 バイトの輝度（0=消灯、255=最大）で、バイト位置は `{7, 8, 9, 10, 11, 13, 14}`（**12 を飛ばす**）。

| データのビット位置 | レポートのバイト位置 | LED |
|-------------------|----------------------|-----|
| 7..0 | 7 | かな |
| 15..8 | 8 | ローマ字 |
| 23..16 | 9 | コード入力 |
| 31..24 | 10 | CAPS |
| 39..32 | 11 | INS |
| 47..40 | 13 | ひらがな |
| 55..48 | 14 | 全角 |

バイト 12 は未使用で、ファームウェア側の仕様と思われるが根拠となる記述はない。

### 5.3 送信スレッド（`Keyboard.java:~9104-9175`）

- 送信間隔を 10ms 以上空けるため専用スレッドを使う（`SENDER_INTERVAL`）。
- `senderSend` は、前回と同じデータなら何もせず、異なるデータならキューをクリアして最新だけを入れる（キュー長 10）。連続して LED が更新されても最終値だけが送られる。
- 切断時は `senderEnd` の後に直接 `send(0L)` して消灯する（スレッド終了とのレースを避けるため。コメントあり）。

### 5.4 LED 動作確認（デモ、`Keyboard.java:~8997-9100`）

メニュー「LED の動作確認」でスレッドを起動し、7 個の LED を左右に往復するグラデーション点灯させる（40ms 間隔、60 段階）。デモ中は通常の LED 更新・I/O ポート送信が抑止され、終了時に `kbdZKeyUpdate` で実状態へ戻す。

## 6. I/O ポート（X68000 側から LED を直接制御）

`$00E9F040`〜`$00E9F04F`（16 バイト）に Z キーボード制御用ポートがマップされる（`KBD_ZKEY_IO_ADDRESS`、`KBD_ZKEY_IO_SIZE`、`5760-5761`）。`MemoryMappedDevice` の拡張ボード領域 1（XB1）のバイト・ワード・ロングの読み書きから呼ばれる。

| アドレス | 内容 |
|----------|------|
| `$00E9F040` | かな LED 輝度 |
| `$00E9F041` | ローマ字 |
| `$00E9F042` | コード入力 |
| `$00E9F043` | CAPS |
| `$00E9F044` | INS |
| `$00E9F045` | ひらがな |
| `$00E9F046` | 全角 |
| `$00E9F047` | 予約 |
| `$00E9F048` | bit0 = programmable（0=ノーマル、1=プログラマブル）、bit1 = send（1 で送信）、bit2-7 予約 |
| `$00E9F049`〜`F` | 予約。読むと `$FF` |

- `$00E9F040-47` は `kbdZKeyIOData`（64bit）の各バイトに対応し、バイト単位で 0〜255 の輝度をそのまま書ける。
- ノーマルモードでは MFP 経由の LED 状態が使われ、I/O ポートへの書き込みは保持されるだけ。
- プログラマブルモードに入ると MFP 由来の LED 更新は止まり、`$00E9F048` の bit1 を立てると `kbdZKeyIOData & 0x00FFFFFFFFFFFFFF` を `senderSend` する（`8832-8855`）。ノーマルモードへ戻すと `kbdZKeyUpdate` で通常状態に復帰する。
- `kbdReset` で `kbdZKeyIOData` とモードは初期化される（`7423-7425`）。
- デモ中・ポート未接続の間は送信されない（ポートの値自体は更新される）。
- この I/O ポートは XEiJ 独自で、X68000 Z 実機側の仕様との関係は未確認。`XEiJ.java:9033` のコメントは `$00E9F040～$00E9F047` と書かれているが、実際の範囲は 16 バイト。

## 7. 設定・メニュー

| 項目 | 内容 |
|------|------|
| メニュー | 入力 → Z キーボード（`Z keyboard` / 「Z キーボード」）。「接続」「LED の動作確認」「デバッグフラグ」 |
| `zkeyboard` | `on` / `off`（既定 `off`）。接続要求の保存 |
| `zkeymap` | Z キーボード用キーマップ（`-3` マーカー付き） |
| 接続状態 | 起動時は要求だけ。接続に失敗すると要求は `false` に戻り、設定にも `off` で保存される |

## 8. 気づいた点（要検証）

1. **非 Windows での接続**: メニューの「接続」は OS を問わず表示される。`ZKeyLEDPort` のコンストラクタは `SymbolLookup.libraryLookup("hid"...)` 等を呼ぶため、非 Windows では `IOException` 以外の例外になる可能性がある（`kbdZKeyOpen` は `IOException` しか捕捉しない）。実行して確認していない。
2. **ステータスビットのコメント不整合**: `kbdZKeyStatus` のコメント（`5742`）は「bit7=全角」だが、実際は 7 ビットで bit6=全角（`kbdSetLedStatus` と `kbdZKeyUpdate` のループ上限が 7）。
3. **デバイス識別が決め打ち**: VID/PID/`MI_01`/`COL05` の固定文字列で判定する。ファームウェアのバージョンや別個体で値が異なると接続できない可能性がある。
4. **コマンド値**: Feature レポートの先頭 `10` と `248` の意味（Report ID・コマンド）はコード内に説明がなく、キーボード側の HID レポート記述子の確認が必要。
5. **XF4 の保持**: XF4 は他のキーが離されたときにまとめて離される。XF4 単独で押して離すと、次の別キー解放まで押されたままになる。意図した設計（コメントあり）だが、挙動の把握として重要。
6. **スレッド安全性**: `kbdZKeyPort` は複数スレッド（イベントディスパッチ、送信スレッド、タイマー、エミュレータ）から参照されるが `volatile` ではない。切断時に `senderEnd` を先に呼ぶことで競合を避ける構造になっている。

## 9. 改造時の着眼点

| 目的 | 触る場所 |
|------|----------|
| 別機種のキーボードに対応する | `ZKeyLEDPort.open` の識別文字列、`send` のレポート形式、`KBD_DEFAULT_MAP_ZKEY` |
| LED の明るさ段階を変更する | `kbdZKeyUpdate` の 255/128/64/32 |
| 送信間隔・キュー | `SENDER_INTERVAL`、`senderQueue` |
| X68000 側から LED を細かく制御する | `kbdZKeyIOWriteByte` 周辺と I/O ポート仕様 |
| 非 Windows 対応 | `ZKeyLEDPort` の HID/SetupAPI 呼び出し全体。`hitKey` / `isKeyToggled` も Windows API（`SendInput` / `GetKeyState`）依存 |
| キー入力の補正を追加 | `kbdStart` 内の `keyPressed` / `keyReleased` の Z キーボード分岐 |

## 10. WinUSB 経路（追加実装）

`ZKeyUsbPort`（libusb を FFM で呼ぶ）で USB HID レポートを直接読み、PowerToys 等なしで全キーを使える。

### モード（Z キーボードメニュー、次回接続から有効）
- HID only（既定）: 従来方式（`ZKeyLEDPort`）。
- Auto (WinUSB first): WinUSB を試し、失敗したら HID にフォールバック。
- WinUSB only: WinUSB のみ。

### 準備
1. Zadig で Interface 0 と Interface 1 の両方（VID 33DD / PID 0011）を WinUSB に変更する。
   この間、Windows ではキーボードとして使えなくなる。
2. `libusb-1.0.dll`（libusb 1.0.29）は x64 / ARM64 版を jar（`xeij/libusb/`）に同梱しており、一時ディレクトリに展開して使う。通常は配置不要。
   別の DLL を使いたい場合の探索順: `-Dxeij.libusb=パス` → カレント → jar のディレクトリ → システム既定 → 同梱版。
3. Java 26 以上（`Enable-Native-Access` は MANIFEST に設定済み）。

### 元に戻す
デバイスマネージャーでデバイスとドライバーを削除して再接続、または Zadig で HidUsb に戻す。

### 備考
- 6KRO（7 個目以降のキーは無視）。
- 修飾キー: Ctrl=CTRL、Shift=SHIFT、LAlt=XF1、RAlt=XF5、LWin=ひらがな、RWin=全角。
- WinUSB 時はキーマップ入れ替えと NumLock 処理を行わない。
