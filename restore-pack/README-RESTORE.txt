DigDown Client restoration pack

Restored ClickGuiScreen from build (11) into build (18) binary and source archive.
The older ClickGuiScreen includes tween animation, right-click settings zones, and window move/resize behavior.
Other classes and resources remain from build (18).

Use the restored JAR as a test client first. This is a binary class replacement, not a full Gradle rebuild.
For source-based builds, replace dev/digdown/client/ClickGuiScreen.java in the newer project with the
provided ClickGuiScreen-restored.java, then run the project's normal Gradle build.
