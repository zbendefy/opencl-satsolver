# opencl-satsolver

This is a simple Brute force sat solver, written in Java, and OpenCL (using the JOCL bindings). It basically parses the input expression, and tries every single variable configuration.

There is a test app included, that can be used to try the libs features.

## Trying the library

Download the SatSolverTest.jar from the repository (or build it from the source), and run it from the command line:

java -jar SatSolverTest.jar

## Features:
  * Supports expressions up to 62 boolean variables
  * AND, OR, NEGATE operators
  * Can evaluate expressions like: A & B | (C & !D)
  * Can load .CNF files

Note that above 24-26 variables calculation times can be very-very long.



#### There are 2 methods implemented:
 * The Single/simple solver will return a byte array where each byte represents a solution.
 * The Multi solver will pack 8 results into a byte (each bit represents a solution), to save on memory (and to allow more variables). 
For larger problems, the multi solver method is automatically selected.
