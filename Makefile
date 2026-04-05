all: build

build:
    ./gradlew assembleDebug

install:
    ./gradlew installDebug

run:
    adb shell am start -n com.example.voiceassistant/.MainActivity

clean:
    ./gradlew clean