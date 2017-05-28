
1) Usage of test-jars --> Bad practice --> Move code to separate test-util jar module
2) AppServiceTest in provides/direct relies on system-properties to load jar produced by other module
3) HttpServerTest in console/server relies on console.war
4) console/servlets module compiled to something with a name completely unrelated to the project
5) test/svt requires samples project
6) Allmost all tests relying on successfull SSL handshakes in wsclient-javax.websocket are failling
