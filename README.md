# Akka HTTP API Rate Limit App

This app implements sliding window algorithm for implementing rate limit on api.

It has two actors: 
1. RateLimitActor : Checks the limit after receiving a request from route.
If request is in limit then It goes to HotelRegistryActor for completion else 429 "Too Man Request" is sent to caller.
2. HotelRegistryActor : Search hotel by city name and respond with searched hotels for the city.


RateLimits are configurable for global and at apiKey level via application.conf
# Running

Run this using [sbt](http://www.scala-sbt.org/).  

```bash
sbt run
```

And then go to <http://localhost:9000> to see the running web application.

## Supported APIs

#### 1. Hotel List By City Name : {{HOST}}/v1/hotels

##### Method: Post

##### Request: 
{
    "cityName": "Bangkok", 
    "apiKey": "4",
    "sort" : "Asc"
}

"cityName" and "apiKey" are mandatory parameters.

"sort" is optional and by price. If provided then it can be "Asc" for ascending order and "Desc" for descending order.
 
##### Response : 
{
    "message": "Ok",
    "hotels": [
        {
            "city": "Bangkok",
            "hotelId": 11,
            "room": "Deluxe",
            "price": 60
        },
        {
            "city": "Bangkok",
            "hotelId": 15,
            "room": "Deluxe",
            "price": 900
        },
        {
            "city": "Bangkok",
            "hotelId": 1,
            "room": "Deluxe",
            "price": 1000
        },
        {
            "city": "Bangkok",
            "hotelId": 6,
            "room": "Superior",
            "price": 2000
        },
        {
            "city": "Bangkok",
            "hotelId": 8,
            "room": "Superior",
            "price": 2400
        },
        {
            "city": "Bangkok",
            "hotelId": 18,
            "room": "Sweet Suite",
            "price": 5300
        },
        {
            "city": "Bangkok",
            "hotelId": 14,
            "room": "Sweet Suite",
            "price": 25000
        }
    ]
}
##### URL : 
    http://localhost:9000/v1/hotels

