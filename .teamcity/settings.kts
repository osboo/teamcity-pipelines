import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.githubConnection
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.finishBuildTrigger

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

    buildType(StopEtlAgent)
    buildType(StartEtlAgent)

    params {
        param("NODE_LABEL", "Agent1")
        param("RESOURCE_GROUP", "RG-TeamcityETL")
        text("SP_PASSWORD", "%keyvault:KeyVault-TeamcityETL/password%", allowEmpty = true)
        text("SP_TENANT_ID", "%keyvault:KeyVault-TeamcityETL/tenant%", label = "SP_TENANT", description = "Service Principle tenant ID", allowEmpty = true)
        text("SP_APP_ID", "%keyvault:KeyVault-TeamcityETL/appId%", label = "SP_APP_ID", description = "Service Principle Name e.g. a5d00de1-0610-...", display = ParameterDisplay.HIDDEN, allowEmpty = true)
        param("SP_NAME", "%keyvault:keyvault-teamcityetl/name%")
    }

    features {
        feature {
            id = "PROJECT_EXT_2"
            type = "OAuthProvider"
            param("displayName", "Azure Key Vault")
            param("resource-uri", "https://vault.azure.net")
            param("secure:client-secret", "credentialsJSON:5891b518-9fbd-4dc9-be37-a8345292bf60")
            param("client-id", "5a6ee2d9-16d1-41db-bd93-9743ab5e28f6")
            param("providerType", "teamcity-azurekeyvault")
            param("tenant-id", "3c88c0aa-b591-4711-9244-a26df4bcce13")
        }
        githubConnection {
            id = "PROJECT_EXT_5"
            displayName = "GitHub.com"
            clientId = "606ab541040b93842264"
            clientSecret = "credentialsJSON:c0b2e1d5-a91c-470a-a347-e62cdabc9087"
        }
    }
}

object StartEtlAgent : BuildType({
    name = "Start ETL Agent"
    description = "Starts ETL agent in Azure"

    params {
        param("SP_APP_ID", "%keyvault:KeyVault-TeamcityETL/appId%")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "Start VM"
            scriptContent = """
                az login --service-principal -u %SP_NAME% -p %SP_PASSWORD% --tenant %SP_TENANT_ID%
                az vm start --name %NODE_LABEL% --resource-group %RESOURCE_GROUP% && az vm wait --name %NODE_LABEL% --resource-group %RESOURCE_GROUP% --custom "instanceView.statuses[?code=='PowerState/running']"
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "mcr.microsoft.com/azure-cli"
        }
        script {
            name = "Authorize Agent"
            scriptContent = """
                echo get new agent credentials
                echo call to Teamcity Server to authorize the agent
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
    description = "Starts ETL agent in Azure"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "Stop VM"
            scriptContent = """
                az login --service-principal -u %SP_NAME% -p %SP_PASSWORD% --tenant %SP_TENANT_ID%
                az vm deallocate --name %NODE_LABEL% --resource-group %RESOURCE_GROUP%
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
