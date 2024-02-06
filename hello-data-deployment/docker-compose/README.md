## Requirements

### Operating system

#### At least 10GB of RAM available for docker under following systems

- Linux
- MacOS
- Windows

### Software installed

- docker
- docker-compose
- docker-desktop (optional)

### Prepare environment

Make sure the `host.docker.internal` is added to the /etc/hosts file with either of these options:

- **Linux/MacOS**: add `127.0.0.1 host.docker.internal` to the `/etc/hosts` file.
- **Windows**: Enable in Docker Desktop under `Settings -> General -> Use WSL 2 based engine` the settings: `Add the *.docker.internal names to the host's etc/hosts file (Requires password)`
    - Make sure Docker-Desktop entered it correctly in `C:\Windows\System32\drivers\etc\hosts`. There were some [cases](https://github.com/kanton-bern/hellodata-be/issues/21#issuecomment-1913578206) where it added wrongly. It should look something like:

```sh
# Added by Docker Desktop
<YOUR-IP-HERE> host.docker.internal
<YOUR-IP-HERE> gateway.docker.internal
# To allow the same kube context to work on the host and the container:
127.0.0.1 kubernetes.docker.internal
# End of section
```

Also be sure there is **no Postgres running** on port 5432 as these will conflict with the upspinning Postgres of HelloDATA.

If you are on [Mac](#mac), [Windows](#windows) or general [FAQ](#faq), please check the enhanced instructions on the bottom.

## Quick Start

First **pull and build** all required images.

Please don't forget to run it again after some time in order to fetch the latest changes, or use command below to always **fetch/build** before start (takes longer).

```sh
docker-compose pull
```

To start everything in a **single command**, run:

```sh
docker-compose up -d
```

To start everything in a single command and **always build/fetch latest images**, run:

```sh
docker-compose pull;  docker-compose up -d --build
```

To **prune** the environment, run:

```sh
docker-compose down --volumes --remove-orphans
```

The start might take some time.

After all started, go to [localhost:8080](http://localhost:8080/) in your browser and log in with user: *admin / admin*

## FAQ

- **Filebrowser login:** `admin/admin`. After successful login, the user should see the dbt-docs shared storage. Also, files can be opened in local file explorer from `./docker-compose/shared` path.

## Mac

- **Mac**: And for images unlike intel (e.g. Apple Sillicon), ensure that you are on latest Docker Desktop, and that you enable `Use Rosetta for x86/amd64 emulation on Apple Silicon` under `Settings -> General`. This setting substantially boosts the speed of non-native containers. Find more on [Docker Desktop Settings](https://docs.docker.com/desktop/settings/mac/?uuid=740D92D0-4D7C-4DD7-9DFD-8AF8D62F42F7) and [Multi-platform images](https://docs.docker.com/build/building/multi-platform/).
- **Platform architecture:** If you are on a Mac or another `arm64` architecture, you mostly likely get the message `requested image's platform (linux/amd64) does not match the detected host platform (linux/arm64/v8) and no specific platform was requested`. It should still work, but much slower. 


## Windows

- If you use Windows native (not WSL or WSL2), ensure the LF (line feeds) are defined in Linux style. Use a tools like [dos2linux](https://linux.die.net/man/1/dos2unix) to
  convert, or make sure in your IDE (e.g., IntelliJ has the option to set).
