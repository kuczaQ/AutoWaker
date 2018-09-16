#!/bin/bash

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m' # no color
RED='\033[0;31m'

echo -e "${BLUE}Pulling from git...${NC}"
git pull && \

echo -e "${BLUE}Compiling...${NC}" &&\
javac ./src/com/adam/*/*.java -cp libs/*.jar -d ./build &&\
echo -e "${GREEN}Success!${NC}" ||\

echo -e "${RED}Failure!${NC}" &&\
exit 1

