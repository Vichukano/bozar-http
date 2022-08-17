# bozar-http

## Actor model multithreading http client for stress testing

**Arguments:**

1 - request url

2 - number of clients

3 - connection timeout in seconds

4 - response timeout in seconds

5 - message string


**example:**
java -jar bozar-http.jar http://localhost:8082/test 100 5 5 {"message": "Hello world!"}