import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2020.2"

project {

    vcsRoot(TeamcityETLVcsRoot)

    buildType(StopEtlAgent)
    buildType(StartEtlAgent)

    params {
        param("AZURE_NODE_LABEL", "Agent1")
        param("ETL_AGENT_NAME", "ETL Agent 1")
        param("RESOURCE_GROUP", "RG-TeamcityETL")
        text("SP_PASSWORD", "%keyvault:KeyVault-TeamcityETL/password%")
        text("SP_TENANT_ID", "%keyvault:KeyVault-TeamcityETL/tenant%", label = "SP_TENANT", description = "Service Principle tenant ID")
        text("SP_APP_ID", "%keyvault:KeyVault-TeamcityETL/appId%", label = "SP_APP_ID", description = "Service Principle Name e.g. a5d00de1-0610-...")
        param("SP_NAME", "%keyvault:keyvault-teamcityetl/name%")
        param("TEAMCITY_API_TOKEN", "%keyvault:keyvault-teamcityetl/agent-api-token%")
        param("TEAMCITY_SERVER_URL","http://teamcity-etl-server.westeurope.cloudapp.azure.com")
    }

    features {
        feature {
            id = "Azure Key Vault plugin"
            type = "OAuthProvider"
            param("displayName", "Azure Key Vault")
            param("resource-uri", "https://vault.azure.net")
            param("secure:client-secret", "credentialsJSON:5891b518-9fbd-4dc9-be37-a8345292bf60")
            param("client-id", "5a6ee2d9-16d1-41db-bd93-9743ab5e28f6")
            param("providerType", "teamcity-azurekeyvault")
            param("tenant-id", "3c88c0aa-b591-4711-9244-a26df4bcce13")
        }
    }
}
    object StartEtlAgent : BuildType({
        name = "Start ETL Agent"
        description = "Starts ETL agent in Azure"

        steps {
            script {
                name = "Start VM"
                scriptContent = """
                    az login --service-principal -u %SP_NAME% -p %SP_PASSWORD% --tenant %SP_TENANT_ID%
                    az vm start --name %AZURE_NODE_LABEL% --resource-group %RESOURCE_GROUP% && az vm wait --name %AZURE_NODE_LABEL% --resource-group %RESOURCE_GROUP% --custom "instanceView.statuses[?code=='PowerState/running']"
                """.trimIndent()
                dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
                dockerImage = "mcr.microsoft.com/azure-cli"
            }
            script {
                name = "Wait until Agent is connected"
                scriptContent = """
                    urlencode() {
                      python3 -c 'import urllib.parse, sys; print(urllib.parse.quote(sys.argv[1]))' "${"$"}1"
                    }
                    encoded_name=$(urlencode "%ETL_AGENT_NAME%")
                    
                    while true; do
                      response=$(curl -s -H "Accept: application/json" --header "Authorization: Bearer %TEAMCITY_API_TOKEN%" \
                        %TEAMCITY_SERVER_URL%:8111/app/rest/agents/name:${"$"}encoded_name?fields=connected,authorized)
                    
                      is_connected=$(echo ${"$"}response | python3 -c "import sys, json; print(json.load(sys.stdin)['connected'])")
                      is_authorized=$(echo ${"$"}response | python3 -c "import sys, json; print(json.load(sys.stdin)['authorized'])")
                    
                      if [ "${"$"}is_connected" = "True" ] && [ "${"$"}is_authorized" = "True" ] ; then
                        echo "Agent %ETL_AGENT_NAME% connected"
                        break
                      fi
                      echo "${"$"}ETL_AGENT_NAME status: ${"$"}response"
                      sleep 5
                    done
                """.trimIndent()
            }
            script {
                name = "Mount Azure Storage"
                scriptContent = """
                    echo download blobfuse
                    echo mount local link to Azure Blob Storage
                """.trimIndent()
            }
        }

        features {
            dockerSupport {
            }
        }
    })

    object StopEtlAgent : BuildType({
        name = "Stop ETL Agent"
        description = "Stops ETL agent in Azure"

        steps {
            script {
                name = "Stop VM"
                scriptContent = """
                    az login --service-principal -u %SP_NAME% -p %SP_PASSWORD% --tenant %SP_TENANT_ID%
                    az vm deallocate --name %AZURE_NODE_LABEL% --resource-group %RESOURCE_GROUP%
                """.trimIndent()
                dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
                dockerImage = "mcr.microsoft.com/azure-cli"
            }
        }

        triggers {
            finishBuildTrigger {
                buildType = "${StartEtlAgent.id}"
                successfulOnly = true
            }
        }

        features {
            dockerSupport {
            }
        }

        dependencies {
            snapshot(StartEtlAgent) {
            }
        }
    })

    object TeamcityETLVcsRoot : GitVcsRoot({
        name = "github-teamcity-etl"
        url = "https://github.com/osboo/teamcity-pipelines"
        branch = "refs/heads/main"
        authMethod = password {
            userName = "osboo"
            password = "credentialsJSON:54315422-314d-441b-afa4-b4c1490d4086"
        }
        param("oauthProviderId", "PROJECT_EXT_5")
    })
