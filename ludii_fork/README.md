## Game Generation
This repository exclusively contains the Generation classes used by the [VSCode extension](https://github.com/PadLex/Ludii-VSCode-extension). The `alex` branch of my [fork](https://github.com/PadLex/Ludii/tree/alex) of the ludii repository contains most of my work.

### Symbol Mapper
`approaches.symbolic.SymbolMapper` Maps symbols to the symbols which can be used to initialize them. It pre-computes all combinations of clauses with different optional parameters, and-groups and or-groups. Given a set of previously selected parameters, the possibilities for the next parameter are filtered on-request.

### GenerationNodes
`approaches.symbolic.nodes.*` These 8 classes are used to represent the tree-like structure of ludii games. Each node corresponds to a symbol and is responsible for interfacing with the SymbolMapper to obtain possible child/parameter nodes. They are also responsible for compilation, which allows them to cache previously compiled ludeme branches when a ludeme tree is modified. They make use of the `SymbolMapper`

### Partial Compiler
`approaches.symbolic.PartialCompiler` Compiles possibly broken game descriptions into a tree of `GenerationNodes`. It's advantages over the standard compiler are that:
 * It returns partial compilations states. Meaning that a game generator can recover by replacing a problematice ludeme without re-compiling the whole game.
 * It reppresents games as trees of GenerationNodes, which enables quick alterations to the ludeme tree.
 * It's slighly faster than the standard compiler, and there are still some valuable optimizations that could be made.

### API
`approaches.symbolic.api.*` These 4 classes interface with the VSCode extension.

## Evaluation Metrics

The main methods in the updated evaluation system can be found in EvalGames.java, which can be located in `supplementary/experiments/eval/EvalGames.java`.  I have also added a new metric named Systematicity, which can be found in `metrics/designer/Systematicity.java`.  The main methods that I have added are as follow (they are all commented, for further insight examine said comments):

* getEvaluationScores - Simply evaluates a game.  Provides multiple input and output options, so simplifies internal use of the evaluation function.

* defaultEvaluationFast - Quickly evaluates a game with some tested settings.

* defaultEvaluationSlow - Evaluates a game with some tested settings more thoroughly.

* loadDB - Loads two pre-built CSV files into memory for later use.

* recommendScore - Recommends an evaluation score for a game based its' nearest neighbors - either BoardGameGeek scores or metric scores can be used to do this.  For this method, loadDB is necessary.

## Recommender System 

* Firstly, since I'm using files that are larger than 100 MB I used git LFS to push it into main, so install git LFS and pull the large files 
using "git lfs pull" (Sorry for this)

* To run my code run the Main in 'GUI/WelcomeDialogue' which is meant to introduce the user to the application

* For Alex to integrate the code, you are able to access the recommended games in "GUI/ReccomenderStarter" in line 312, 
I presented it as a String titled desc_format which should be the descriptions in the format you want, I printed the output
so you can see, let me know if I should change it. 
