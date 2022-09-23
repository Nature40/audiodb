#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
WHITE='\033[0;97m'
NC='\033[0m' # No Color
echo -e ""
echo -e "${WHITE}--------------------------------------------------------------------------------------------------${NC}"
echo -e "${RED}!! only proceed if you stopped a running AudioDB/PhotoDB instance !!${NC}"
echo -e "${GREEN}Do you want to remove all database cache files?${NC}"
echo -e "${WHITE}This may be needed after update to a newer AudioDB/PhotoDB Version.${NC}"
echo -e "${WHITE}At next start of AudioDB/PhotoDB the database cache will be regenerated.${NC}"
echo -e "${WHITE}--------------------------------------------------------------------------------------------------${NC}"
select yn in "Yes" "No" "Cancel"; do
    case $yn in
        Yes ) break;;
        No ) exit;;
	Cancel ) exit;;
    esac
done

unlink label_store.mv.db
unlink label_store.trace.db  
unlink photo_cache.mv.db
unlink photo_cache.trace.db
unlink sample_cache.mv.db
unlink sample_cache.trace.db
unlink thumb_cache.mv.db
unlink thumb_cache.trace.db

echo -e "${GREEN}Done. Now you may start AudioDB/PhotoDB. It may take some time to regenerate database caches.${NC}"