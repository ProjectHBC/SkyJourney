# SkyJourney

SkyJourneyは、[Valkyrien Skies 2](https://valkyrienskies.org/) (VS2) 環境下でのゲームプレイ体験を向上させるための、最適化およびバグ修正Modです。  
このModは、VS2導入環境で発生しがちなパフォーマンス問題（地形Bakingによるラグ）や、村人のAI挙動（船上での就職・補充問題）などを修正・改善します。  
  
ゆっくり実況者のHegadelの[空飛ぶ拠点で旅をする](https://www.youtube.com/playlist?list=PLviBljJRqhECceg-JE9yRw5EpnrxutIg5) シリーズを快適にプレイする目的で作成しています。
<img src="https://i.ytimg.com/vi/N-VOYABfogw/hqdefault.jpg?sqp=-oaymwEXCNACELwBSFryq4qpAwkIARUAAIhCGAE=&rs=AOn4CLCQvPmP0IJkhZZTFJWyAfJEAO8dWA" />

## 主な機能 (Features)

### 1. 地形物理演算の最適化 (Terrain Baking Optimization)
*   **動的な範囲制限**: プレイヤーや稼働中の船の周囲（デフォルト: Y高度 ±32ブロック）のみ物理判定を有効にします。
*   Big Globeなど上下の限界突破されているワールド時に(多分)有効です。

OFF
<img width="1441" height="180" alt="image" src="https://github.com/user-attachments/assets/258d4f8d-c376-40dd-972b-8e00965c5102" />  
ON 
<img width="1529" height="257" alt="image" src="https://github.com/user-attachments/assets/3d8b7901-5d3d-4667-b515-7ab23c74df97" />

### 2. 村人AIの船上対応 (Villager AI Improvements)
*   **船上のPOI検索**: 船に設置された職業ブロックを正しく認識し、バニラ同様の挙動をできるようにします。

### 3. その他の修正 (Quality of Life Fixes)
*   **スニーク挙動の修正**: 船上や斜面でのスニーク時の挙動を改善。
*   **ブロック設置判定の修正**: 船上で自分の位置にブロックをおける問題を修正。

## 動作環境 (Requirements)
*   Minecraft (Fabric)
*   Fabric API
*   Valkyrien Skies 2 (2.3.0-beta.10)
*   Cloth Config API (設定画面用)
*   Mod Menu (設定画面へのアクセス用)

## 免責  
作者は、本Modの使用により生じた直接的または間接的な損害、損失、または不具合について、一切の責任を負いません。  
本Modの利用はユーザー自身の責任において行ってください。


## License

This mod is licensed under the MIT License.

This mod interacts with Valkyrien Skies 2, which is licensed under the GNU Lesser
General Public License v3.0 (LGPL-3.0). Valkyrien Skies 2 is distributed separately
and is not included in this project.