# SkyJourney
[![release](https://img.shields.io/github/release/ProjectHBC/SkyJourney?logo=github)](https://github.com/ProjectHBC/SkyJourney/releases)
[![license](https://img.shields.io/github/license/ProjectHBC/SkyJourney?logo=github)](https://github.com/ProjectHBC/SkyJourney/blob/master/LICENSE)
[![downloads](https://img.shields.io/github/downloads/ProjectHBC/SkyJourney/total?logo=github)](https://github.com/ProjectHBC/SkyJourney/releases/latest)  

--- 

**[English version here / 英語版はこちら](README.md)**

---

SkyJourneyは、[Valkyrien Skies 2](https://valkyrienskies.org/) (VS2) 環境下でのゲームプレイ体験を向上させるための、最適化およびバグ修正Modです。  
このModは、VS2導入環境で発生しがちなパフォーマンス問題（地形Bakingによるラグ）や、村人のAI挙動（船上での就職・補充問題）などを修正・改善します。  
  
ゆっくり実況者のHegadelの [空飛ぶ拠点で旅をする](https://www.youtube.com/playlist?list=PLviBljJRqhECceg-JE9yRw5EpnrxutIg5) シリーズを快適にプレイする目的で作成しています。
<img src="https://i.ytimg.com/vi/N-VOYABfogw/hqdefault.jpg?sqp=-oaymwEXCNACELwBSFryq4qpAwkIARUAAIhCGAE=&rs=AOn4CLCQvPmP0IJkhZZTFJWyAfJEAO8dWA" />  

### ダウンロードはこちら -> [Latest Release](https://github.com/ProjectHBC/SkyJourney/releases/latest)

## **注意！**  
動作環境以外での動作を想定していません。記述バージョン以外での障害には対応しかねます。  

使用していた動画シリーズが完結したため、今後は極めて緊急度の高い(ゲームプレイに支障をきたすレベルの重大な)障害以外には対応しません。今後の積極的な開発は行いません。ご理解のほどよろしくお願いいたします。

## 主な機能

### 1. 地形物理演算の最適化
*   **動的な範囲制限**: プレイヤーや稼働中の船の周囲（デフォルト: Y高度 ±32ブロック）のみ物理判定を有効にします。
*   Big Globeなど上下の限界突破されているワールド時に(多分)有効です。

OFF
<img width="1441" height="180" alt="image" src="https://github.com/user-attachments/assets/258d4f8d-c376-40dd-972b-8e00965c5102" />  
ON 
<img width="1529" height="257" alt="image" src="https://github.com/user-attachments/assets/3d8b7901-5d3d-4667-b515-7ab23c74df97" />

### 2. 村人AIの船上対応
*   **船上のPOI検索**: 船に設置された職業ブロックを正しく認識し、バニラ同様の挙動をできるようにします。

### 3. その他の修正
*   **スニーク挙動の修正**: 船上や斜面でのスニーク時の挙動を改善。
*   **ブロック設置判定の修正**: 船上で自分の位置にブロックをおける問題を修正。
*   **風船への投擲物の挙動変更**：風船に投擲系が当たった場合でも割れないよう修正。
*   **Drawerの挙動を修正**：Drawerを船でも使用できなかった問題を修正。
*   **羊の動作を修正**：羊が草ブロックの上でも食べない問題を修正。


## 動作環境
*   Minecraft (Fabric)
*   Fabric API
*   Valkyrien Skies 2 (2.3.0-beta.10)
*   Cloth Config API (設定画面用)
*   Mod Menu (設定画面へのアクセス用)

## 免責  
作者は、本Modの使用により生じた直接的または間接的な損害、損失、または不具合について、一切の責任を負いません。  
本Modの利用はユーザー自身の責任において行ってください。


## ライセンス

本Modは MIT License の下で提供されています。

本Modは Valkyrien Skies 2 と連携しますが、Valkyrien Skies 2 は GNU Lesser
General Public License v3.0 (LGPL-3.0) の下でライセンスされています。Valkyrien Skies 2 は
本プロジェクトには含まれておらず、別途配布されています。
