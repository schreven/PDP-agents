rmdir tournament\tour /S /q

java -jar ../logist/logist.jar -new tour ./agents

pause

java -jar ../logist/logist.jar -run tour ./config/auction.xml

pause

java -jar ../logist/logist.jar -score tour