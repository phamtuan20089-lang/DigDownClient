# DigDown Client — source

Mã nguồn được lấy từ `DigDownClient-1.0.0-sources.jar` (do Fabric Loom tạo ra) và đã được
đối chiếu với `DigDownClient-1.0.0.jar`: đủ 9 file `.java` (20 class, gồm cả class lồng nhau)
và 58/58 chuỗi ký tự trong source đều khớp với bản đã biên dịch.

## Thông tin mod
- Mod id: `digdown`, môi trường: client
- Minecraft `1.21.11`, Java `>= 21`, Fabric Loader `>= 0.18.0` (build bằng 0.19.2), cần Fabric API
- Build gốc: Gradle 9.9.1, Fabric Loom 1.14.10

## Cấu trúc
```
src/main/java/dev/digdown/client/   # code Java
    DigDownClient.java              # entrypoint (ClientModInitializer)
    DigDownCore.java                # khởi tạo, phím mở GUI (Right Shift)
    BuiltinModules.java             # Sprint, Auto Walk, Fullbright, HUD
    Module.java, ModuleManager.java # khung module
    ClickGuiScreen.java             # giao diện ClickGUI
    DigDownHud.java, UiFont.java, ClientSettings.java
src/main/resources/
    fabric.mod.json                 # "version" là ${version}, Gradle sẽ thay lúc build
    assets/digdown/                 # font (Baloo, Noto — giấy phép OFL) và lang
```

## Lưu ý
- Source dùng tên **intermediary** của Minecraft (`class_310`, `method_1551`, ...), không phải tên dễ đọc (Yarn/Mojang).
- Trong file upload không có file build (`build.gradle`, `gradle.properties`, `settings.gradle`),
  chỉ có code và resources.
