# Why have an external launcher?

There are two main reasons:

1. Remove self-dependency injection of JavaFX in the Recaf app itself.
   - In the older iterations of Recaf we would do everything this launcher does in the background. Managing the JavaFX dependency externally ensures that we don't have to do anything nasty or hacky.
2. Remove auto-updater behavior from the Recaf app itself.
   - Recaf can still tell you that there is an update available, but it won't attempt to automate the process by itself.
   - This is more friendly towards environments like Brew/AUR which manage the app _(vs the app managing itself)_.

## How was this handled in the past?

If you ran Recaf 2.X with JavaFX on the classpath, nothing special happens. However, if you did not have JavaFX on the classpath...

1. Recaf would check for the latest version of JavaFX and then download the artifacts deemed to be the closest fit for your operating system.
2. Recaf would then hijack the module system and disable reflection access restrictions
3. Recaf can now use unrestricted reflection to grab an instance of the internal application classloader and inject JavaFX into it
   - Because the internals moved around a bit between versions, there's some conditional logic to tweak how this is done before and after certain releases
   - This loader has a private field `ucp` which is a wrapper holding q `List<URL>` pointing to all classpath entries
   - Normally this list will only hold the standard path + anything given via `-cp` but we want to add JavaFX
   - Doing `List.add` by itself does not notify the runtime of any changes, but thankfully they expose an `addUrl(URL)` method
      - We call that for each of the JavaFX artifacts we downloaded earlier
   - If nothing has thrown an exception so far, JavaFX should now magically load when we first reference it later on during execution

## But now with a launcher I have an extra layer between me and Recaf

This is why the launcher offers `set-default-action` as a command. You can configure the launcher to do `auto` if you want to always update then run Recaf, or `run` if you want to run Recaf without ever updating. Once the value is set any use of `java -jar launcher.jar` will immediately run the specified action _(and in turn, open Recaf)_.

If you are on a system which has paired `.jar` files with running them via `javaw` instead of `java` the GUI will open instead. In these cases you can make a `.bat` or `.sh` file to run the `java -jar launcher.jar` command. If you insist on using `javaw` specify the `-x` argument. This would change the command to `javaw -jar launcher.jar -x`.