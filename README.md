# Android VectorPathTweaker Plugin
Android customize vector drawable plugin. support multiple pathData gramma, no need to manually modify the data. ([中文文档](https://github.com/KnIfER/AndroidVectorPathTweaker/blob/master/README_CN.md))

# Feature
- Support to translate, scale, flip and transpose android vector drawables.
- Support modify chained path data。(M...zM...z).
- Revert modifications by a single click if needed（revert）.
- Modify continuously. Click 'rebase' and all afterward mod will be base on the current state.
- Realtime updating and previewing.

# Preview
![image](https://github.com/KnIfER/AndroidVectorPathTweaker/blob/master/preview/preview.jpg)

# Install
## Online installation
- Step 1: Open AndroidStudio or IDEA.
- Step 2: Preferences -> Plugins -> Browse repositories...
- Step 3: Search VectorPathTweaker and install. Then restart IDE.

## Local installation
- Step 1: Download AndroidLocalizePlugin.zip file.
- Step 2: Open AndroidStudio or IDEA.
- Step 3: Preferences -> Plugins -> Install plugin from disk...
- Step 4: Select VectorPathTweaker.zip and Restart IDE.

# Usage
- Step 1: Open one xml vector drawable.
- Step 2: Select the part of pathData where you wants to modify.
- Step 3: Right click. In the context menu select 'Tweak Vector Path'.
- Step 4: Make sure you have the same viewport size in the tweaker dialog .
- Step 5: Open AndroidStudio's Preview Panel and adjust the vector to your need！

# Thanks
- Airsaid：[AndroidLocalizePlugin (as a nice tempate)](https://github.com/Airsaid/AndroidLocalizePlugin)
- huachao1001 : [IDEA CN TUTs](https://blog.csdn.net/huachao1001/article/details/53885981)

# ContactMe
- Blog: [https://www.jianshu.com/u/77921c0f8d4f](https://www.jianshu.com/u/77921c0f8d4f)
- QQ: 302772670

# License
```
Copyright 2020 KnIfER. https://github.com/KnIfER
Copyright 2018 Airsaid. https://github.com/airsaid

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
