# Server setup
Create network for Teamcity hosts
> docker network create teamcity-network

Run server
> docker run -it --rm --name teamcity-server-instance \
-v ~/TeamcityETL/teamcity-data:/data/teamcity_server/datadir \
-v ~/TeamcityETL/teamcity-logs:/opt/teamcity/logs \
-p 8111:8111 \
--network teamcity-network \
jetbrains/teamcity-server

Run client

>docker run -it -e SERVER_URL=http://teamcity-server-instance:8111  \
    -u 0 \
    -v ~/TeamcityETL/agent-conf:/data/teamcity_agent/conf \
    -v /var/run/docker.sock:/var/run/docker.sock  \
    -v ~/TeamcityETL/buildagent/work:/opt/buildagent/work \
    -v ~/TeamcityETL/buildagent/temp:/opt/buildagent/temp \
    -v ~/TeamcityETL/buildagent/tools:/opt/buildagent/tools \
    -v ~/TeamcityETL/buildagent/plugins:/opt/buildagent/plugins \
    -v ~/TeamcityETL/buildagent/system:/opt/buildagent/system \
    --network teamcity-network \
    jetbrains/teamcity-agent


     --network teamcity-network