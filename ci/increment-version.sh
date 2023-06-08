#get the current pom version and store it into a variable
version=$(mvn -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -q)
#increment the second digit of the version and overwrite the variable
version=$(echo ${version} |  awk -F'.' '{print $1"."$2"."$3+1}' |  sed s/[.]$//)
#update the pom with the new version
mvn -U versions:set -DnewVersion=${version}