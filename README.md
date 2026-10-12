# xeij-zkbd-winusb

## 簡易マニュアル

1. [Zadig](https://zadig.akeo.ie/) 等を使い、下記デバイスの USB ドライバを WinUSB に差し替える :
    * ```ZUIKI X68000Z Keyboard (Interface 0)```
    * ```ZUIKI X68000Z Keyboard (Interface 1)```
2. 改造版 XEiJ を起動する
3. ```入力``` - ```Zキーボード``` メニュー配下で下記設定をする :
    1. ```自動 (WinUSB 優先)``` または ```WinUSB のみ``` を選択する
    2. ```接続``` する

> [!WARNING]
> * ドライバを差し替えると、 Windows で Z キーボードを外付けキーボードとして使えなくなります
> * ARM64 Windows でドライバを差し替える場合、下記手順が必要になる場合があります :
>     1. セキュアブートを無効にする
>     2. 無署名ドライバを利用可能にする

## 連絡先

https://github.com/68fpjc/xeij-zkbd-winusb

本リポジトリは、XEiJ の作者である Makoto Kamada さんとは無関係です。
