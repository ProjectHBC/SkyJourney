# SkyJourney
[![release](https://img.shields.io/github/release/ProjectHBC/SkyJourney?logo=github)](https://github.com/ProjectHBC/SkyJourney/releases)
[![license](https://img.shields.io/github/license/ProjectHBC/SkyJourney?logo=github)](https://github.com/ProjectHBC/SkyJourney/blob/master/LICENSE)
[![downloads](https://img.shields.io/github/downloads/ProjectHBC/SkyJourney/total?logo=github)](https://github.com/ProjectHBC/SkyJourney/releases/latest)  

--- 

**[日本語版はこちら / Japanese version here](README.ja.md)**

---

SkyJourney is an optimization and bug-fix mod that improves the gameplay experience in a [Valkyrien Skies 2](https://valkyrienskies.org/) (VS2) environment.  
It fixes and improves performance issues commonly seen in VS2 setups (lag caused by terrain baking) as well as villager AI behavior (issues with villagers taking jobs / restocking while aboard a ship).  

It was created so that Hegadel, a Japanese commentary creator, could comfortably play through his [空飛ぶ拠点で旅をする](https://www.youtube.com/playlist?list=PLviBljJRqhECceg-JE9yRw5EpnrxutIg5) series.
<img src="https://i.ytimg.com/vi/N-VOYABfogw/hqdefault.jpg?sqp=-oaymwEXCNACELwBSFryq4qpAwkIARUAAIhCGAE=&rs=AOn4CLCQvPmP0IJkhZZTFJWyAfJEAO8dWA" />  

### Download here -> [Latest Release](https://github.com/ProjectHBC/SkyJourney/releases/latest)

## **CAUTION!**  
Operation is not intended for environments other than the specified system requirements. We may be unable to address issues occurring in versions other than those described.  

The video series this mod was made for has now come to an end, so from here on I will only address extremely urgent, game-breaking bugs. I won't be doing any further active development. Thanks for understanding!

## Features

### 1. Terrain Baking Optimization
*   **Dynamic range limiting**: Enables physics calculations only within a certain range around the player or an active ship (default: ±32 blocks vertically).
*   Likely useful (probably) in worlds like Big Globe where the vertical build limit has been extended.

OFF
<img width="1441" height="180" alt="image" src="https://github.com/user-attachments/assets/258d4f8d-c376-40dd-972b-8e00965c5102" />  
ON 
<img width="1529" height="257" alt="image" src="https://github.com/user-attachments/assets/3d8b7901-5d3d-4667-b515-7ab23c74df97" />

### 2. Villager AI Improvements
*   **Onboard POI detection**: Correctly recognizes job-site blocks placed on ships, restoring vanilla-like villager behavior.

### 3. Quality of Life Fixes
*   **Sneak behavior fix**: Improved sneaking behavior on ships and slopes.
*   **Block placement fix**: Fixed an issue where blocks could be placed at the player's own position while on a ship.
*   **Balloon projectile fix**: Fixed thrown projectiles not popping balloons on hit.
*   **Drawer fix**: Fixed Drawers not working while aboard a ship.
*   **Sheep behavior fix**: Fixed sheep not eating grass blocks while standing on top of them.


## Requirements
*   Minecraft (Fabric)
*   Fabric API
*   Valkyrien Skies 2 (2.3.0-beta.10)
*   Cloth Config API (for the settings screen)
*   Mod Menu (to access the settings screen)

## Disclaimer  
The author assumes no responsibility for any direct or indirect damage, loss, or malfunction resulting from the use of this mod.  
Use of this mod is entirely at your own risk.


## License

This mod is licensed under the MIT License.

This mod interacts with Valkyrien Skies 2, which is licensed under the GNU Lesser
General Public License v3.0 (LGPL-3.0). Valkyrien Skies 2 is distributed separately
and is not included in this project.
