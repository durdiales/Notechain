## Motivation

This repository contains some code examples focused on developing a Blockchain example using the generic framework Scorex.

In order to get a deeper knowledge about how functional programming may help to develop modern applications, focus will be on how to develop some multi-tier system.

## Specifications

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

### Introduction
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

### Box

A Box is an information container, which is protected by a Proposition. The contents of the Box will only be accessible if the proof that fulfills the proposal is provided.
The Box is the base element of information in scorex.


### NodeViewModifier aka Transaction

Transactions in scorex are something that can modify the state of a node. Scorex provides a `Transaction` type that is nothing more than a special case of` NodeViewModifier`. Most transactions are going to be something that creates, modifies or destroys `Box`s

### Block

A Block is nothing more than a group of transactions, and in fact, extends just like `Transaction` from` NodeViewModifier`, only that through another trait: `PersistentNodeViewModifier`.
This is because the transactions as such are not saved in the history (Blockchain) and are the blocks that are saved, so they have the functionality to persist in the chain.

Still, it's not a mandatory concept in Scorex. You can design a system that does not use blocks.

### History

Scorex uses a generic way of representing the BlockChain that, in fact, does not force it to be a linear structure, but could also have a tree structure, for example.
This concept is `History`. The `BlockChain` class defined in Scorex is a subtype of` History`

## Http Resquest Examples:

1.- POST - create or update noteGroups and notes

```
Method: POST
Request URL: http://localhost:9085/note
Headers:
        Header name: Content-Type
		Header value: application/json
Body:
        Body Content type: application/json
		Editor view: Text input
		{
		    "title": "notebook 1",
		    "notes": ["task1", "task2", "Task3"]
		}
```

2.- GET - get all noteGroups

```
Method: GET
Request URL: http://localhost:9085/note
Headers:
        Header name: Content-Type
		Header value: application/json
```

3.- GET - get noteGroup with <id>

```
Method: GET
Request URL: http://localhost:9085/note/<id>
for example: "id": "6NudbTsm3j2BYQqtkpyCC1HbuDSoMnsDcdVBrbfoz9zg",

Headers:
        Header name: Content-Type
		Header value: application/json
```

4.- GET - get all chain blocks

```
Method: GET
Request URL: http://localhost:9085/debug/chain
Headers:
        Header name: Content-Type
		Header value: application/json
```

5.- GET - get last <number> of block references from chain

```
Method: GET
Request URL: http://localhost:9085/stats/tail/<number>
for example: <number> = 10

Headers:
        Header name: Content-Type
		Header value: application/json
```