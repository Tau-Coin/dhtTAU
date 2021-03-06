# Build doc

There are three parts in dhtTAU:
- taucoin-core
- taucoin-android
- taucoin-linux

Building parts can be configured from `dhtTAU/setting.gradle`, default including all.


You just need to configure java environment to build taucoin-core.

> 1) Download the jdk software from https://java.com/en/download/manual.jsp
	
> 2) you have downloaded jdk software, for example: jdk-8u271-linux-x64.tar.gz.
		>  - Type `tar -zvxf jdk-8u271-linux-x64.tar.gz` in your software directory.

> 3) Declare environment variables in `~/.bashrc`:
> - `JAVA_HOME=${Your software directory}/jdk1.8.0_271`
> - `JRE_HOME=${JAVA_HOME}/jre`
> - `PATH=$PATH:${JAVA_HOME}/bin`

> 4) Execuate `source ~/.bashrc` in your terminal.

> 5) Type `./gradlew build -x test` in dhtTAU directory to build taucoin-core.
>> PS: taucoin-linux can be built tugh the same way.

If you are an android developer or intresting in android development, we recommend that you build taucoin-core && taucoin-android.
#### Use Android-Studio to build SDK environment
To build taucoin-android, the SDK environment is needed, and we recommend that using android-studio IDE to configure the SDK environment.

> 1) Download the android-studio IDE from https://developer.android.google.com

> 2) You have downloaded A-S IDE, for example: android-studio-ide-201.6953283-linux.tar.gz

   > - Type `tar -zvxf android-studio-ide-201.6953283-linux.tar.gz` in your software directory.
   > - You will get a directory named `android-studio`.

> 3) Enter `${Your software directory}/android-studio/bin`, and execuate `studio.sh` to launch the android-studio IDE.

> 4) SDK Tools-28.0.0 is needed to install from IDE SDK manager. 

> 5) Declare environment variables in `~/.bashrc`:
> - `ANDROID_HOME=${Your andorid sdk directory}/Sdk`

> 6) Execuate `source ~/.bashrc` in your terminal.
> 
> 7) You can build the project from android-studio IDE, also in terminal cmd window using: `./gradlew build  -x test`.

#### Use command-line-tools to build SDK environment
If you are using a server and no IDE avaiable, you can use command-line-tools to build the SDK environment.

> 1) Download the command-line-tools software from https://developer.android.google.com

> 2) You have downloaded the software, for example: commandlinetools-linux-6858069_latest.zip

   > - Type `commandlinetools-linux-6858069_latest.zip` in your working directory.
   > - You will get a directory named `cmdline-tools`.
   > - Remove the `cmdline-tools` into your SDK installing directory, for example:`${Your andorid sdk directory}/Sdk`
  
> 3) Declare environment variables in `~/.bashrc`:
> - `ANDROID_HOME=${Your andorid sdk directory}/Sdk`
> - `PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin`

> 5) Execuate `source ~/.bashrc` in your terminal.

> 6) Enter `${ANDROID_HOME}/cmdline-tools/bin`, and execuate `./sdkmanager --install --sdk_root=${ANDROID_HOME} "tools"` to download and build the SDK environment.

> 7) You can build the project in terminal cmd window using: `./gradlew build  -x test`.

<strong>Any building problem please send an issue to us.</strong>
