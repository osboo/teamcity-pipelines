# Server/client setup
## Instal server dependencies
Install git:
```
sudo apt update
sudo apt install git
git config --global user.name "osboo"
git config --global user.email "osboo.osboo@gmail.com"
```
Install docker
```
sudo apt-get install apt-transport-https ca-certificates curl software-properties-common

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -

sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu  $(lsb_release -cs)  stable" 

sudo apt-get update

sudo apt-get install docker-ce

sudo curl -L "https://github.com/docker/compose/releases/download/1.28.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose
```

## Run Teamcity
call `docker-compose up`
This starts server and orchestration clients

## Configure Teamcity server
Go to UI (e.g. `localhost:8111`)
- Authorize "Orchestration agent" when UI completes initialization
- Setup git root
- Install Azure Key Vault plugin (AKV is supposed to get secrets to access ETL agent)
- Restart server
- Enable versionioned settings 

## Troubleshooting
In case
```
Looks like some mandatory directories are not writable (see above).
teamcity_1         |     TeamCity container is running under 'tcuser' (1000/1000) user.

sudo chown -R 1000:1000 teamcity-data
sudo chown -R 1000:1000 teamcity-logs
```

## ETL Agent setup
- git (see above)
- docker (see above)

```
mkdir /home/osboo
cd /home/osboo
git clone https://github.com/osboo/teamcity-pipelines.git
```

Add `docker-compose` as startup service to ETL
```
sudo touch /etc/systemd/system/docker-compose-app.service
sudo vim /etc/systemd/system/docker-compose-app.service
```
Past following code:
```
# /etc/systemd/system/docker-compose-app.service

[Unit]
Description=Docker Compose Application Service
Requires=docker.service
After=docker.service

[Service]
WorkingDirectory=/home/osboo/teamcity-pipelines/.teamcity/agents/etl-agent
ExecStart=/usr/local/bin/docker-compose up
ExecStop=/usr/local/bin/docker-compose down
TimeoutStartSec=0
Restart=on-failure
StartLimitIntervalSec=60
StartLimitBurst=3

[Install]
WantedBy=multi-user.target
```
Then enable agent:

`systemctl enable docker-compose-app`

Restart VM

Authorize agent in UI when it appears