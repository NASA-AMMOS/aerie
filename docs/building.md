# Building Aerie

## Pre-Requisites 
| Name    | Version     | Notes                         | Download                                  |
|---------|-------------|-------------------------------|-------------------------------------------|
| OpenJDK | 11.0.X      | HotSpot JVM                   | https://adoptopenjdk.net/                 |
| Buck    | v2019.10.X+ | See [Buck docs](https://buck.build/setup/getting_started.html) on installation | https://github.com/facebook/buck/releases |

## Instructions
0. Satisfy all pre-requisites 
1. Clone this repository locally 
2. Navigate to the cloned repository; this will be referred to as the `Aerie Root Directory` 
3. Create a file named `.buckconfig.local` in the Aerie Root Directory 
4. Copy the following:  
  
        [tools]  
            javac = "C:/Program Files/AdoptOpenJDK/jdk-11.0.4.11-hotspot/bin/javac.exe"  
            java_for_tests = "C:/Program Files/AdoptOpenJDK/jdk-11.0.4.11-hotspot/bin/java.exe"   

5. Replace the `javac` and `java_for_tests` values with your respective paths 
6. Run `buck build //...` from the Aerie Root Directory
7. Navigate to `Aerie Root Directory/buck-out/gen` to examine/distribute your build artifacts
