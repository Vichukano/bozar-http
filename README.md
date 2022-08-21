# bozar-http

## Actor model multithreading http client for stress testing

**Arguments:**

1 - request url

2 - number of clients

3 - number of requests per client

4 - connection timeout in seconds

5 - response timeout in seconds

6 - message string


**example:**
java -jar bozar-http.jar http://localhost:8082/test 100 3 5 5 {"message": "Hello world!"}