MrsQG is a semantics-based question generation system for single English sentences. For instance, given a sentence "Kim gave Sandy a cat", MrsQG generates the following questions:

  * 4.50: To what did Kim give a cat?
  * 4.37: What gave Sandy a cat?
  * 3.72: Who gave Sandy a cat?
  * 2.85: Which animal did Kim give Sandy?
  * 2.73: To who did Kim give a cat?
  * 2.30: Kim gave Sandy a cat?


## Features ##

  * Question types: who, what, when, where, which, how, how many/much and yes/no
  * Deep parsing with [PET](http://wiki.delph-in.net/moin/PetTop)
  * Chart generation with [LOGON](http://wiki.delph-in.net/moin/LogonTop)/[LKB](http://wiki.delph-in.net/moin/LkbTop)
  * The [English Resource Grammar](http://lingo.stanford.edu/erg.html) as the backbone
  * [Minimal Recursion Semantics](http://www.cl.cam.ac.uk/~aac10/papers/mrs.pdf) as the theory support
  * Output ranking with maximum entropy and language models
  * PList output to [NPCEditor](http://vhtoolkit.ict.usc.edu/index.php/NPCEditor) for restricted-domain question answering systems

## System Requirement ##

  * 5 GB disk space
  * 4~16 GB RAM
  * Linux Operating System
  * Java 1.6

Follow the [Installation](Installation.md) page to install/configure/run MrsQG.