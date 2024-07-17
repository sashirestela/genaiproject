RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
RESET='\033[0m'

echo -e "${GREEN}"
echo '   ________          __  ____      ____      ______      __                                              '
echo '  / ____/ /_  ____ _/ /_/  _/___  / __/___  / ____/___  / /_              ________  ______   _____  _____'
echo ' / /   / __ \/ __ \/ __// // __ \/ /_/ __ \/ / __/ __ \/ __ \   ______   / ___/ _ \/ ___/ | / / _ \/ ___/'
echo '/ /___/ / / / /_/ / /__/ // / / / __/ /_/ / /_/ / /_/ / /_/ /  /_____/  (__  )  __/ /   | |/ /  __/ /    '
echo '\____/_/ /_/\__,_/\__/___/_/ /_/_/  \____/\____/\____/_.___/           /____/\___/_/    |___/\___/_/     '
echo '                                                                                                         '
echo -e "${RESET}"

source environment.sh

cd chatbot-server

command="mvn exec:java -Dexec.mainClass=com.encora.genai.app.WebApplication -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -Dorg.slf4j.simpleLogger.logFile=output.log"

eval $command