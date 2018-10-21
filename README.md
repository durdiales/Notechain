## Motivation

This repository contains some code examples focused on developing a Blockchain example using the generic framework Scorex.

In order to get a deeper knowledge about how functional programming may help to develop modern applications, focus will be on how to develop some multi-tier system.

# Specifications

-	Git
-	IDE - IntelliJ 18.x (https://www.jetbrains.com/idea/download/download-thanks.html?platform=windows&code=IIC)
-	sbt 0.13.5 or later (http://www.scala-sbt.org/download.html)
-	Scala 2.11.8 or later (https://downloads.lightbend.com/scala/2.12.4/scala-2.12.4.msi)
-   Scorex
-   Postman (https://www.getpostman.com/)  

## How to build scorex version


In order to use this project, we need to build scorex by hand to this specific commit.
Commit: `6a100ea0`

1. Download Scorex: `git clone https://github.com/ScorexFoundation/Scorex.git`
2. We need to go to the local Scorex repository and checkout to the specified commit: `git checkout 6a100ea0`

3. Local Install for Scorex
- For linux: `sbt localInstall`
- For windows: `sbt publishLocal`

## How to run the project

We need to run the main, using a `setting.conf` file:
```
sbt "run ./src/main/resources/settings.conf"
```

## Basic concepts of Scorex-2

Introduction
- It is experimental and still raw new major release of the Scorex
- Generic Framework
- Does not force to a chain structure or blocks

### NodeViewHolder

It is the type that represents the state maintained by the nodes. It is clearly separated from the logic of the system, and the presence of mutability is explicit.
It consists of the following parts:

1. History: previous activity history. It would be what represents the blockchain
2. Minimal state: minimum information that allows to validate incoming requests. Represents the status of the chain, which can be extracted from the history. these would be in the case of a cryptocurrency, for example, a collection of accounts and their balance sheets, with their respective public keys.
+ memory pool: buffer to store temporary information, for example of requests to validate
+ vault: structure to store private information of the node, for example private keys, in which case it would be called Wallet

### Proof and Proposition

The propositions are in almost all the components of scorex, so they are an important element to understand.
It is a way to keep information safe, and that you can only interact (read, modify, etc) with it if a test is provided that satisfies that proposition.

This Proof is defined as `Proof [P <: Proposition]` in scorex

A Proof can be represented by a signature that is added to a transaction, which can only be generated with a private key, but can be checked with the corresponding public key.


## Show basic concepts of scorex-2
## Explain the model
## Expose a rest as example execution
