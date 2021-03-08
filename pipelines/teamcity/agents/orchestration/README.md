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
Go to UI (`localhost:8111`)
- Authorize "Orchestration agent" when UI completes initialization
- Setup git root
- Install Azure Key Vault plugin
- Enable versionioned settings 
