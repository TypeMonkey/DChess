# DChess

*A modification of the classical game with a sprinkle of good ole' democracy*

## Why? Why would you make this?
I recently read about Garry Kasparov and [his famous game of chess that was played over the Internet](http://https://en.wikipedia.org/wiki/Kasparov_versus_the_World)

Now, my skills in chess are laughable. But my dedication to act out my most useless and asinine of programming ideas are not much so. And so, when I read this story, I couldn't but have an urge to make it a reality.

_And I totally didn't work on this project for two weeks straight, on four hour night of sleep, just so I can fill my resume with projects to hopefully get a job after this year. I'm definitely not desperate for a job and this is definitely not my **_final_** year at UCSD and definitely am not panicking at my lack of career options_

## How does DChess work?
DChess is structured into two distinct parts: `core` and `net`

The `core` component of DChess is just the "chess" part of DChess. It stores the code that manages the movements of units, as well as the current board information.

The `net` component of DChess is the network code that allows for multiple players of the same team to vote on a move.

When a player connects to a DChess server, they are given two options: **join a game session** or **create a game session**

Each game session is given a unique identifier, in the form of a [UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier) , that can be used as a sort of "invite" code to allow players to join a session.

Currently, there's only **one** DChess server which runs on Amazon Lightsail, and is tied to the IP Address **35.166.15.181** and port **9999**.

## How does a DChess game play out?
It's the exact same rules as chess, but instead of a classical one-vs-one scenario, a team of multiple players control a team on the chess board.

To move a unit per turn, players on each team must vote on a move for a unit. The move that gets the most votes is the move that will be implemented at the end of the turn. 

Oh, and there's a time limit per turn (minimum of 15 seconds at default if no time limit is explicitly set when a session is created). If there's no vote from either team that is the plurality vote by the end of the limit, then no move will be implemented at the end of the turn. If no vote has been received by the end of the limit, then no move will be implemented.

##Dependencies and System Requirements
DChess would not be possible without [Netty 4.1](https://github.com/netty/netty) and would not be remotely playable without [JUnit 4](https://junit.org/junit4/)

DChess requires a Java Runtime Environment of 1.8 or above. 

## Roadmap and the Future
I will be releasing the compiled, runnable jars for the client and server versions of DChess on October 12.

As for the roadmap for DChess, there are still a lot of ideas I'd like to implement to really expand on the voting aspect, as well as ideas to extend classical chess. For now though, I will focus largely on bug fixes and server-side performance adjustments.

## Contact
DChess is maintained and was created by Jose Guaro.

If you have any concerns regarding DChess or would just like to reach out, feel free to contact me at [jguaro@ucsd.edu](jguaro@ucsd.edu).



