![](https://raw.githubusercontent.com/TypeMonkey/DChess/master/icon.png)
# DemocraticChess (DChess)

*A democratized take on the classical game of chess*

## Motivation
I recently read about Garry Kasparov and [his famous game of chess that was played over the Internet](https://en.wikipedia.org/wiki/Kasparov_versus_the_World)

My skills in chess are laughable. But my dedication to act out my most useless and asinine of programming ideas are not much so. And so, when I read this story, I took great inspiration.

## Structure
DChess is structured into two distinct parts: `core` and `net`

The `core` component of DChess houses the chess logic of DChess. It stores the code that manages the movements of units, as well as the current board information.

The `net` component of DChess is the network code that allows for multiple players of the same team to vote on a move. It's split further into subcomponents of `server` and `client`. 

The `client` subcomponent contains client-side network code, as well as the client's graphical user interface. The `server` subcomponent contains the server-side network code. There's nothing stopping anyone from hosting their own DChess server.

Currently, a DChess server is hosted on the IP `35.163.164.101` at port `9999`.

## Gameplay
It's the exact same rules as chess, but instead of a classical one-vs-one scenario, a team of multiple players control a team on the chess board.

To move a unit per turn, players on each team must vote on a move for a unit. The move that gets the most votes is the move that will be implemented at the end of the turn. If there's a ties, then no move is made for that turn.

There's a time limit for vote submissions for the current voting team (minimum of 15 seconds). If a plurality vote isn't reached by the end of this time limit, no move is made for that turn. If no vote has been received by the end of the limit, then no move will be implemented.

Game sessions can also be setup to have a break in-between turns, acting as a moment for teams to strategize and reflect on life choices.

## Dependencies and System Requirements
DChess would not be possible without [Netty 4.1](https://github.com/netty/netty) and would not be remotely playable without [JUnit 4](https://junit.org/junit4/) - as well as their respective dependencies.

DChess requires a Java Runtime Environment of 1.8. The [client installer](https://github.com/TypeMonkey/DChess-Installer) installs the DChess client with a compliant JDK so this requirement is already taken care of.

## Releases
Binaries are [available](https://github.com/TypeMonkey/DChess-Installer/releases) for the DChess client (v1.0). Every future version will have a corresponding installer, posted as a release for the above respository. 

## Roadmap
Current roadmap for DChess:
1. __Core Development__ - Currently, the chess logic in `core` can only check for the win condition: killing of the enemy king. But, it cannot detect check, checkmates, and tie conditions. 
	- **_Plan_** : Use an established Chess API instead of developing my own. 
2. __More Than Chess__ - The name implies that the game is only democratizing chess, but the same can easily be done with other classics like checkers. 
	- **_Plan_** : Allow for players to develop their own custom games, in the form of mods.
3. __Beyond Plurality__ - The only supported mode of voting is by plurality (First Past the Post), but there are so many interesting voting methods! 
	- **_Plan_** : Implement a small set of voting systems for users to choose from, as well as allowing custom voting systems.

 

## Contact
DChess is maintained and created by Jose Guaro.

If you have any concerns regarding DChess or would just like to reach out, feel free to contact me at [anothertypemonkey@gmail.com](anothertypemonkey@gmail.com).



