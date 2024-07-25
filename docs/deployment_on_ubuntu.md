# Deployment on Ubuntu

Install Java, e.g.:

```bash
sudo apt update
sudo apt install openjdk-11-jdk
java -version
```

Download distribution package, extract and make excutable:
```bash
unzip *.zip
chmod +x *.sh
```

# Running on Ubuntu

Start application server:

```bash
./audio.sh
```

Open web-interface on a browser.
Local URL with default port 8080:

http://127.0.0.1:8080/

Stop server by key crtl-c or by closing the terminal.