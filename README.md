# Recaf Launcher

A simple solution for running Recaf.

## Usage

All ***command line*** usage assumes you are running the command similar to `java -jar launcher.jar ...`:
 - Where `launcher.jar` is the file name you saved the launcher as.
 - Where `...` includes additional arguments, details of which can be found below.

All ***graphical user interface*** usage assumes you are running the application via `javaw -jar launcher.jar`
 - The GUI is shown automatically when no `std-out` is found, this occurs when using `javaw` instead of `java`
 - The CLI can be used even with `javaw` when specifying `--headless`

### Auto
 - Checks local system compatibility
 - Keeps JavaFX up-to-date
 - Keeps Recaf up-to-date
 - Runs Recaf
```
Usage: <launcher> auto
```

### Run
 - Runs the currently installed version of Recaf
```
Usage: <launcher> run
  -j, --java=<javaExecutable>
         Path of Java executable to use when running Recaf.
         Not specifying a value will use the same Java executable used by the
           launcher.
```

### Compatibility
 - Checks for a compatible version of Java
 - Checks if the current Java runtime includes JavaFX
    - Bundling JavaFX _can_ work, but its your responsibility to ensure the bundled version is compatible with Recaf
    - Ideally use a JDK that does not bundle JavaFX and let the launcher pull in JavaFX
```
Usage: <launcher> compatibility [-ifx] [-ss]
      -ifx, --ignoreBundledFx
         Ignore problems with the local system's bundled JavaFX version
      -ss, --skipSuggestions
         Skip solutions to detected problems
```

### Update Recaf
 - Keeps Recaf up-to-date
```
Usage: <launcher> update
```
If you want to be on the bleeding edge of things there is an alternative command:
```
Usage: <launcher> update-ci [-b=<branch>]
Installs the latest artifact from CI
  -b, --branch=<branch>   Branch name to pull from.
                          By default, no branch is used.
                          Whatever is found first on the CI will be grabbed.
```

### Update JavaFX
 - Keeps Recaf's local JavaFX cache up-to-date with the current release of JavaFX
 - Can be configured to use specific versions of JavaFX if desired
 - Can be configured to delete old versions of JavaFX in the cache automatically
```
Usage: <launcher> update-jfx [-cfk] [-maxc=<maxCacheCount>]
                             [-maxs=<maxCacheSize>] [-v=<version>]
  -c, --clear               Clear the dependency cache
  -f, --force               Force re-downloading even if the local install
                              looks up-to-date
  -k, --keepLatest          Keep latest cached dependency in the cache when
                              clearing
      -maxc, --maxCacheCount=<maxCacheCount>
                            Clear the dependency cache when this many files
                              occupy it
      -maxs, --maxCacheSize=<maxCacheSize>
                            Clear the dependency cache when this many bytes
                              occupy it
  -v, --version=<version>   Target JavaFX version to use, instead of whatever
                              is the latest
```

### Check Recaf's version
 - Prints out the version of Recaf installed via the launcher
```
Usage: <launcher> version
```

### Setting a default action
 - Allows specifying a default action to run when no arguments are specified. 
   For commands with spaces in them, surround the whole command with quotes.
   - Useful case: `set-default-action auto` - This will make the launcher immediately update Recaf and run it
     without any additional user input required.
 - Only acknowledged when running the launcher via `java` as `javaw` will open the GUI.
```
Usage: <launcher> set-default-action <action>
     <action>   The action to run. Should match one of the launcher commands.
```