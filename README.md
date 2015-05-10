TradeRouteOptimizer
===================

A Java application for computing optimal trade routes for the game Elite Dangerous. Routes start from your current location, and include as many hops as you specify. The program will choose the best route and which commodities to buy and sell at each station. 

Features
========
- Guaranteed to find the optimal route (give or take errors in the data).
- Downloads the data from [eddb.io](http://eddb.io/).
- Specify the max hop distance, minimum landing pad size, max distance from the star to the station, and optionally a 'via' system.

How does it work?
=================
The application uses a process known as [branch and bound](http://en.wikipedia.org/wiki/Branch_and_bound). For every hop, it computes all stations that are in range, what the profit would be when traveling to those stations, and what stations could be reached from there, and so forth (this is the branch part). But large branches of the tree are not investigated because it would be impossible to beat the current best solution, even if you got the maximum profit you can get for every remaining hop (the bound part).

To speed up the process, some things are precomputed: for every station, which station is within a 100 LY range, and what would be the maximum profit you could make on a hop to that station?

Screenshot
==========
<img src="https://github.com/schuemie/TradeRouteOptimizer/blob/master/extra/screenshot.png" alt="Screenshot" title="Screenshot" />

Getting started
===============
- Go to the [Release](https://github.com/schuemie/TradeRouteOptimizer/releases) tab on top, and download the latest release jar file.  
- You'll need Java, so if you don't have Java installed on your machine, get it [here](https://java.com/download/).
- Double click the jar file. This will start the application.
- Go to **Data** and select **Download data**. This will download the data from [eddb.io](http://eddb.io/)
- After downloading has completed (the 'hourglass' pointer has gone away), go to **Data** and select **Preprocess data**. Then click **Ok**. This will prepare the data for use.
- Now you are ready! Type in the name of your starting station or system, set the other options and click **Compute route**. 

Tip: when you move your mouse over the various entry fields you will get some more information.

Acknowledgements
================
This application was written by Martijn Schuemie. Data is provided by [eddb.io](http://eddb.io/). [Elite: Dangerous](https://www.elitedangerous.com/) and all associated media are the intellectual property of [Frontier Developments](http://www.frontier.co.uk/).

License
=======
This software is released in the public domain.