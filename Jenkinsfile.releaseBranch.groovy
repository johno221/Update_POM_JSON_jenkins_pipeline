import java.io.File


//update Pom version 
def getPomPath() {
    return "backend/version/pom.xml"
}

String pomPath = getPomPath()

def getPomVersion() {
    def matcher = readFile(pomPath) =~ '<version>(.+)-SNAPSHOT</version>'
    matcher ? matcher[0][1] : null  
}   

def increaseMinorVersion(currentVersion) {
    def parts = currentVersion.split('\\.')
    parts[1] = (parts[1] as int) + 1
    return parts.join('.')
} 

def updatePomVersion(newVersion) {
    def currentVersion = getPomVersion()
    def pomContent = readFile(pomPath)
    def updatedPomContent = pomContent.replaceAll(/<version>${currentVersion}-SNAPSHOT<\/version>/, "<version>${newVersion}-SNAPSHOT</version>")
    return updatedPomContent
}

//update Json version 
def getJsonPath() {
    return "frontend/slexng-webapp/package.json"
}

String jsonPath = getJsonPath()

def getJsonVersion() {
    def matcher = readFile(jsonPath) =~ '"version": "(.+)-SNAPSHOT",'
    matcher ? matcher[0][1] : null  
}   

def increaseJsonMinorVersion(currentJsonVersion) {
    def parts = currentJsonVersion.split('\\.')
    parts[1] = (parts[1] as int) + 1
    return parts.join('.')
} 

def updateJsonVersion(newJsonVersion) {
    def currentJsonVersion = getJsonVersion()
    def jsonContent = readFile(jsonPath)
    def updatedJsonContent = jsonContent.replaceAll(/"version": "${currentJsonVersion}-SNAPSHOT",/, """"version": "${newJsonVersion}-SNAPSHOT",""")
    return updatedJsonContent
}


pipeline {
        stages {
            stage('Upgrade minor version') {
                        steps {
                            script {
                                checkout([$class: 'GitSCM', branches: [[name: ${branchName} ]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout'],], userRemoteConfigs: [[url: "https://gitlab-dmz.whitestein.com/lsps-projects/slex-ng.git", credentialsId: 'gitlabJenkins']]])

                                sh """
                                git pull main  --rebase
                                git checkout -b fea-automatic-upgrade-minor-version
                                """

                                echo "### upgrade pom.xml minor version ###"
                                def currentVersion = getPomVersion()
                                def newVersion = increaseMinorVersion(currentVersion)
                                def updatedVersion = updatePomVersion(newVersion)                                                                               

                                    if (currentVersion != newVersion) {
                                        writeFile file: pomPath, text: updatedVersion
                                        println("Pom.xml version updated from ${currentVersion}-SNAPSHOT to ${newVersion}-SNAPSHOT")
                                    } else {
                                        println("Failed to retrieve current version from pom.xml")
                                    }                           
                                
                                echo "### upgrade Package.json minor version ###"
                                def currentJsonVersion = getJsonVersion()
                                def newJsonVersion = increaseJsonMinorVersion(currentJsonVersion)
                                def updatedJsonVersion = updateJsonVersion(newJsonVersion)

                                    if (currentJsonVersion != newJsonVersion) {
                                        writeFile file: jsonPath, text: updatedJsonVersion
                                        println("Package.json version updated from ${currentJsonVersion}-SNAPSHOT to ${newJsonVersion}-SNAPSHOT")
                                    } else {
                                        println("Failed to retrieve current version from Package.json")
                                    } 

                                sh """
                                git add  ${pomPath} ${jsonPath}
                                git commit -m "minor version of package.json and pom.xml upgrade"
                                git push --set-upstream fea-automatic-upgrade-minor-version
                                git checkout main
                                git merge fea-automatic-upgrade-minor-version  
                                git push main        
                                """
                            }               
                        }
                    }
        }


        post {
            always {
                script {
                    
                        sh """
                        git checkout main
                        git pull main --rebase
                        git branch -D fea-automatic-upgrade-minor-version 
                        git push main --delete fea-automatic-upgrade-minor-version
                        """
                }
            }
        }  
}
