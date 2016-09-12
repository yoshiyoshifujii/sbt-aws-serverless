ScriptedPlugin.scriptedSettings
scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Dfile.encoding=UTF-8", "-Xmx1024M", "-XX:MaxMetaspaceSize=256M", "-Dplugin.version=" + version.value)
}
scriptedBufferLog := false