# AudioDB

This repository contains sources, documentation and release packages of AudioDB.  

Requirements: [Java 11 (or newer)](https://adoptium.net)

Supported operation systems: **Ubuntu** and **Windows**

AudioDB releases: https://github.com/Nature40/audiodb/releases

# Deployment on Ubuntu

Install Java, e.g.:

```bash
sudo apt update
sudo apt install openjdk-11-jdk
java -version
```

Download AudioDB package, extract and make excutable:
```bash
unzip *.zip
chmod +x *.sh
```

# Running on Ubuntu

Start AudioDB server:

```bash
./audio.sh
```

Open AudioDB web-interface on a browser.
Local URL with default port 8080:

http://127.0.0.1:8080/

Stop server by key crtl-c or by closing the terminal.


# Deployment on Windows

Install [Java](https://adoptium.net)

Download AudioDB [package](https://github.com/Nature40/audiodb/releases) and extract.

# Running on Windows

Start AudioDB server: Run the start script from the **command line** or just **double click** at the script file:

```
win_audio.cmd
```

Open AudioDB web-interface on a **web browser**.  
Local URL with default port 8080:

http://127.0.0.1:8080/

Stop server by key **ctrl-c** or by closing the terminal.

# Configuration

AudioDB settings are in YAML file `config.yaml`