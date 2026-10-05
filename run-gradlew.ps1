$javaExe = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\java.exe"
$gradleWrapperJar = "C:\repo\campilot\gradle\wrapper\gradle-wrapper.jar"

Set-Location "C:\repo\campilot"

# Run gradle wrapper
& $javaExe -Dorg.gradle.appname=gradlew -classpath $gradleWrapperJar org.gradle.wrapper.GradleWrapperMain assembleDebug
