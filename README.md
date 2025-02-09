
# Distributed Algorithms Project
This repository contains the implementation  of the coding project for EPFL course CS-451 Distributed Algorithms.

The main goal was to implement key communication primitives for decentralized systems, ensuring reliable message exchange even in the presence of failures and asynchronous execution. The project consists of three main milestones:

1. **Perfect Links** - Ensuring reliable, ordered message delivery over an unreliable network.
2. **FIFO Broadcast** - Implementing a broadcast protocol with FIFO message ordering.
3. **Lattice Agreement** - Enabling processes to reach agreement on ordered sets of values.

Check out the [project description](./ProjectDescription.pdf) for more details.

## Evaluation Criteria
The project was evaluated based on the following criteria:
- **Correctness:** The implementation should satisfy the properties of the abstraction. The implementation should be robust to failures and asynchrony.
  
- **Performance:** The implementation should be efficient. 

The correctness of the implementation gives a passing grade, while the performance gives the remaining grade. 

The mark obtained for this project was **X.X/6.0**. *(still waiting for the results :') )*.

## System Model

- **Processes:** A fixed set of $n$ processes.
- **Failures:** Among $n = 2f + 1$ processes, at most $f$ can fail by crashing. A process that fails is said to be faulty.
- **Asynchrony:** The processes are asynchronous: a process proceeds at its own arbitrary (and non-deterministic)
speed. Moreover, the communication network is also asynchronous: message delays are finite, but arbitrarily
big.
- **Communication:** Processes communicate by exchanging messages over an authenticated point-to-point
network. In addition to the communication being asynchronous, messages can also be lost, delayed or
reordered.

## Implemented Abstractions

### Perfect Links

**Interface:**
- send($p$: Process, $m$: Message) - Sends a message $m$ to process $p$.
- deliver($p$: Process, $m$: Message) - Delivers a message $m$ from process $p$.
  

**Properties:**
- **Validity:** If a correct process sends a message to another correct process, it will eventually be delivered.
- **No Duplication:** Messages are delivered exactly once.
- **No Creation:** Only sent messages are delivered.

### FIFO Broadcast

**Interface:** 
- broadcast($m$ : Message) - Broadcasts a message $m$ to all processes.
- deliver($p$ : Process, $m$ : Message) - Delivers a message $m$ from process $p$.


**Properties:**
- **Validity:** If a correct process broadcasts a message, then all correct processes eventually deliver it.
- **No Duplication:** Messages are delivered exactly once.
- **No Creation:** Only broadcast messages are delivered.
- **Uniform Agreement:** If a message m is delivered by some process,
then m is eventually delivered by every correct process.
- **FIFO Order:** If some process broadcasts message m1 before it broadcasts message m2, then
no correct process delivers m2 unless it has already delivered m1.

### Lattice Agreement

**Interface:**
- propose($I_i$ : Set) - Proposes a set $I_i$.
- decide($O_i$ : Set) - Decides on a set $O_i$.

**Properties:**
- **Validity:** Let a process $P_i$ decide a set $O_i$, then $I_i \subseteq O_i$ and $O_i \subseteq \bigcup_{j=1}^{n} I_j$.
- **Consistency:** Let a process $P_i$ decide a set $O_i$ and let a process $P_j$ decide a set $O_j$, then $O_i \subseteq O_j$ or $O_j \subseteq O_i$.
- **Termination:** All correct processes eventually decide.


## Compiling and executing the code
This repository has 3 tags corresponding to the 3 milestones of the project. To compile and run the code for a specific milestone, checkout the corresponding tag and follow the instructions below.

The `build.sh` compiles the code and creates a jar file. The `run.sh` script runs the code with the following arguments:

`./run.sh --id ID --hosts HOSTS --output OUTPUT CONFIG`

Refer to the [project description](./ProjectDescription.pdf) for more details on how to execute the system, expected output, etc.

For the 3rd milestone, a validation Python script has been developed, `verify.py`.  The scripts generate random inputs and check if the implemented lattice agreement abstraction satisfies the properties of validity, consistency, and termination.