# Recaf Launcher

A simple solution for running Recaf.

## Usage

All usage assumes you are running the command similar to `java -jar launcher.jar ...`:
 - Where `launcher.jar` is the file name you saved the launcher as.
 - Where `...` includes additional arguments, details of which can be found below.

### Auto: The all-in one command.
 - Checks local system compatibility
 - Keeps JavaFX up-to-date
 - Keeps Recaf up-to-date
 - Runs Recaf
```
Usage: <launcher> auto
```

### Compatibility: Checking if the local system meets requirements for running Recaf
 - Checks for a compatible version of Java
 - Checks if the current Java runtime includes JavaFX
    - Bundling JavaFX _can_ work, but its your responsibility to ensure the bundled version is compatible with Recaf
    - Ideally use a JDK that does not bundle JavaFX and let the launcher pull in JavaFX
```
Usage: <launcher> compatibility [ifx] [ss]
  ifx       Ignore problems with the local systems bundled JavaFX version
  ss        Skip solutions to detected problems
```

### Update Recaf: Self explanatory
 - Keeps Recaf up-to-date
```
Usage: <launcher> update
```

### Update JavaFX: Self explanatory
 - Keeps Recaf's local JavaFX cache up-to-date with the current release of JavaFX
 - Can be configured to use specific versions of JavaFX if desired
 - Can be configured to delete old versions of JavaFX in the cache automatically
```
Usage: <launcher> update-jfx [c] [f] [maxc=<maxCacheCount>]
                             [maxs=<maxCacheSize>] [v=<version>]
      c                      Clear the dependency cache
      f                      Force re-downloading even if the local install
                               looks up-to-date
      k                      Keep latest cached dependency in the cache when
                               clearing
      maxc=<maxCacheCount>   Clear the dependency cache when this many files
                               occupy it
      maxs=<maxCacheSize>    Clear the dependency cache when this many bytes
                               occupy it
      v=<version>            Target JavaFX version to use, instead of whatever
                               is the latest
```

### Check Recaf's version: Self explanatory
 - Prints out the version of Recaf installed via the launcher
```
Usage: <launcher> version
```